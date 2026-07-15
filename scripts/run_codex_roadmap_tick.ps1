param(
    [string]$TaskName = "AppOrganizer_CodexRoadmap_15Min",
    [string]$PromptFile = "scripts/codex_roadmap_tick_prompt.md",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$automationDir = Join-Path $projectRoot ".codex-automation"
$logDir = Join-Path $automationDir "logs"
$stateFile = Join-Path $automationDir "roadmap_tick_state.json"
$lockFile = Join-Path $automationDir "roadmap_tick.lock"
$finalMessageFile = Join-Path $automationDir "last_codex_message.txt"
$promptPath = Join-Path $projectRoot $PromptFile
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"

New-Item -ItemType Directory -Force -Path $automationDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $logDir "codex-roadmap-$runId.log"

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

function Send-Telegram {
    param([string]$Message)
    if (-not (Test-Path $telegramScript)) {
        Write-Log "Telegram script missing, skip: $telegramScript"
        return
    }
    try {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $telegramScript -Message $Message -EnvPath (Join-Path $projectRoot ".env") | Out-Null
        Write-Log "Telegram sent"
    } catch {
        Write-Log "Telegram failed: $($_.Exception.Message)"
    }
}

function Save-State {
    param(
        [string]$Status,
        [string]$Message,
        [int]$ExitCode = 0
    )
    $payload = [ordered]@{
        task_name = $TaskName
        run_id = $runId
        status = $Status
        message = $Message
        exit_code = $ExitCode
        updated_at = (Get-Date).ToString("o")
        log_file = $logFile
    }
    $payload | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
}

function Release-Lock {
    if (Test-Path $lockFile) {
        Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
    }
}

function Acquire-Lock {
    if (Test-Path $lockFile) {
        try {
            $existing = Get-Content $lockFile -Raw | ConvertFrom-Json
            $existingPid = [int]($existing.pid)
            $existingStarted = [datetime]$existing.started_at
            $alive = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
            if ($alive) {
                throw "Existing run active (pid=$existingPid, started=$existingStarted)"
            }
            if ((Get-Date) - $existingStarted -lt (New-TimeSpan -Hours 12)) {
                Write-Log "Removing stale lock for dead pid=$existingPid"
            }
        } catch {
            throw $_
        }
        Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
    }

    $payload = [ordered]@{
        pid = $PID
        started_at = (Get-Date).ToString("o")
        task_name = $TaskName
        log_file = $logFile
    }
    $payload | ConvertTo-Json | Set-Content -Path $lockFile -Encoding UTF8 -NoNewline
}

function Test-GitDangerState {
    $gitDir = Join-Path $projectRoot ".git"
    $markers = @(
        "MERGE_HEAD",
        "CHERRY_PICK_HEAD",
        "REVERT_HEAD",
        "BISECT_LOG"
    ) | ForEach-Object { Join-Path $gitDir $_ }
    $rebaseApply = Join-Path $gitDir "rebase-apply"
    $rebaseMerge = Join-Path $gitDir "rebase-merge"

    foreach ($marker in $markers + @($rebaseApply, $rebaseMerge)) {
        if (Test-Path $marker) {
            return $true
        }
    }
    return $false
}

function Get-DirtySummary {
    $status = & git status --short 2>$null
    if (-not $status) { return "clean" }
    return ($status | Select-Object -First 20) -join "; "
}

try {
    Acquire-Lock
    Write-Log "Lock acquired"

    if (-not (Get-Command codex -ErrorAction SilentlyContinue)) {
        throw "codex command not found"
    }
    if (-not (Test-Path $promptPath)) {
        throw "Prompt file not found: $promptPath"
    }
    if (Test-GitDangerState) {
        $msg = "Otomasyon durdu: merge/rebase/cerry-pick gibi riskli git durumu algilandi."
        Write-Log $msg
        Save-State -Status "blocked" -Message $msg -ExitCode 2
        Send-Telegram "AppOrganizer otomasyon durdu: merge/rebase gibi riskli git durumu algilandi."
        exit 2
    }

    $dirtySummary = Get-DirtySummary
    Write-Log "Worktree summary: $dirtySummary"

    $dynamicPrompt = @"
Automation context:
- Task name: $TaskName
- Run id: $runId
- Current time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz")
- Worktree summary: $dirtySummary
- Log file: $logFile
- Final message file: $finalMessageFile

"@ + (Get-Content $promptPath -Raw)

    Set-Content -Path $finalMessageFile -Value "" -Encoding UTF8
    Save-State -Status "running" -Message "Codex roadmap tick started"
    Send-Telegram "AppOrganizer otomasyon turu basladi ($runId). Codex siradaki roadmap maddesine devam ediyor."

    if ($DryRun) {
        Write-Log "DryRun enabled, codex exec skipped"
        Save-State -Status "dry_run" -Message "Dry run completed"
        exit 0
    }

    $codexArgs = @(
        "exec",
        "--cd", $projectRoot,
        "--search",
        "--sandbox", "danger-full-access",
        "--ask-for-approval", "never",
        "--dangerously-bypass-approvals-and-sandbox",
        "--output-last-message", $finalMessageFile,
        "-"
    )

    Write-Log "Starting codex exec"
    $dynamicPrompt | & codex @codexArgs 2>&1 | Tee-Object -FilePath $logFile -Append
    $exitCode = $LASTEXITCODE

    $finalMessage = ""
    if (Test-Path $finalMessageFile) {
        $finalMessage = (Get-Content $finalMessageFile -Raw).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($finalMessage)) {
        $finalMessage = "Codex turu tamamlandi; ayrintilar log dosyasinda."
    }

    if ($exitCode -eq 0) {
        Write-Log "Codex completed successfully"
        Save-State -Status "completed" -Message $finalMessage -ExitCode $exitCode
        Send-Telegram ("AppOrganizer otomasyon turu tamamlandi ($runId). " + $finalMessage)
    } else {
        Write-Log "Codex exited with code $exitCode"
        Save-State -Status "failed" -Message $finalMessage -ExitCode $exitCode
        Send-Telegram ("AppOrganizer otomasyon turu hata ile bitti ($runId, exit=$exitCode). " + $finalMessage)
        exit $exitCode
    }
} catch {
    $message = $_.Exception.Message
    Write-Log "Fatal error: $message"
    Save-State -Status "error" -Message $message -ExitCode 1
    Send-Telegram "AppOrganizer otomasyon fatal hata: $message"
    exit 1
} finally {
    Release-Lock
    Write-Log "Lock released"
}
