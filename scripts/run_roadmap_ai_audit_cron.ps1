param(
    [string]$TaskName = "AppOrganizer_RoadmapAIAudit_15Min",
    [string]$RoadmapFile = "ROADMAP_AI_AUDIT_2026-07-14.md",
    [string]$PromptFile = "scripts/roadmap_ai_audit_cron_prompt.md",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$automationDir = Join-Path $projectRoot ".roadmap-ai-audit-cron"
$logDir = Join-Path $automationDir "logs"
$stateFile = Join-Path $automationDir "state.json"
$lockFile = Join-Path $automationDir "run.lock"
$lastMessageFile = Join-Path $automationDir "last_codex_message.txt"
$roadmapPath = Join-Path $projectRoot $RoadmapFile
$promptPath = Join-Path $projectRoot $PromptFile
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"

New-Item -ItemType Directory -Force -Path $automationDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $logDir "roadmap-ai-audit-$runId.log"

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

function Send-Telegram {
    param([string]$Message)
    if (-not (Test-Path $telegramScript)) {
        Write-Log "Telegram script missing, skipped"
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
        [int]$ExitCode = 0,
        [string]$CurrentItem = ""
    )
    [ordered]@{
        task_name = $TaskName
        run_id = $runId
        status = $Status
        message = $Message
        current_item = $CurrentItem
        exit_code = $ExitCode
        updated_at = (Get-Date).ToString("o")
        log_file = $logFile
    } | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
}

function Acquire-Lock {
    if (Test-Path $lockFile) {
        $existing = $null
        try { $existing = Get-Content $lockFile -Raw | ConvertFrom-Json } catch { $existing = $null }
        if ($existing -and (Get-Process -Id ([int]$existing.pid) -ErrorAction SilentlyContinue)) {
            throw "Previous automation run still active (pid=$($existing.pid))"
        }
        Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
    }
    [ordered]@{
        pid = $PID
        started_at = (Get-Date).ToString("o")
        run_id = $runId
        log_file = $logFile
    } | ConvertTo-Json | Set-Content -Path $lockFile -Encoding UTF8
}

function Release-Lock {
    Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
}

function Test-GitDangerState {
    $gitDir = Join-Path $projectRoot ".git"
    $markers = @("MERGE_HEAD", "CHERRY_PICK_HEAD", "REVERT_HEAD", "BISECT_LOG") |
        ForEach-Object { Join-Path $gitDir $_ }
    $markers += Join-Path $gitDir "rebase-apply"
    $markers += Join-Path $gitDir "rebase-merge"
    foreach ($marker in $markers) {
        if (Test-Path $marker) { return $true }
    }
    return $false
}

function Resolve-CodexPath {
    $cmd = Get-Command codex -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $candidateRoots = @(
        (Join-Path $env:USERPROFILE ".vscode\extensions"),
        (Join-Path $env:LOCALAPPDATA "Programs"),
        (Join-Path $env:USERPROFILE ".codex")
    ) | Where-Object { $_ -and (Test-Path $_) }

    foreach ($root in $candidateRoots) {
        $match = Get-ChildItem -Path $root -Filter "codex.exe" -Recurse -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($match) { return $match.FullName }
    }
    return $null
}

function Get-FirstPendingItem {
    if (-not (Test-Path $roadmapPath)) { throw "Roadmap not found: $roadmapPath" }
    $lines = Get-Content $roadmapPath
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\*\*Durum:\*\*\s*(?:⏳\s*)?Bekliyor\b') {
            for ($j = $i; $j -ge 0; $j--) {
                if ($lines[$j] -match '^#{1,6}\s+(.+)$') {
                    return [pscustomobject]@{
                        Title = $Matches[1].Trim()
                        Line = $i + 1
                    }
                }
            }
            return [pscustomobject]@{ Title = "Unknown roadmap item"; Line = $i + 1 }
        }
    }
    return $null
}

function Stop-ThisTask {
    try {
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction Stop | Out-Null
        Write-Log "Scheduled task unregistered: $TaskName"
    } catch {
        & schtasks.exe /delete /tn $TaskName /f | Out-Null
        Write-Log "Scheduled task delete fallback exit=$LASTEXITCODE"
    }
}

try {
    Acquire-Lock
    Write-Log "Lock acquired"

    if (Test-GitDangerState) {
        $msg = "Git merge/rebase/cherry-pick state detected; automation paused."
        Save-State -Status "blocked" -Message $msg -ExitCode 2
        Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron durdu: riskli git durumu var."
        exit 2
    }

    $pending = Get-FirstPendingItem
    if (-not $pending) {
        $msg = "No '**Durum:** Bekliyor' items remain in $RoadmapFile."
        Save-State -Status "finished" -Message $msg
        Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron tamamlandi: bekleyen madde kalmadi. Gorev kapatiliyor."
        Stop-ThisTask
        exit 0
    }

    $codexPath = Resolve-CodexPath
    if (-not $codexPath) { throw "codex command not found" }
    if (-not (Test-Path $promptPath)) { throw "Prompt not found: $promptPath" }

    $dirty = (& git -C $projectRoot status --short 2>$null) -join "; "
    if ([string]::IsNullOrWhiteSpace($dirty)) { $dirty = "clean" }

    $dynamicPrompt = @"
Automation run id: $runId
Current pending roadmap item: $($pending.Title)
Status line: ${RoadmapFile}:$($pending.Line)
Worktree summary: $dirty
Log file: $logFile

"@ + (Get-Content $promptPath -Raw)

    Set-Content -Path $lastMessageFile -Value "" -Encoding UTF8
    Save-State -Status "running" -Message "Started Codex for $($pending.Title)" -CurrentItem $pending.Title
    Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron basladi ($runId): siradaki madde $($pending.Title)"

    if ($DryRun) {
        Write-Log "DryRun: Codex exec skipped for $($pending.Title)"
        Save-State -Status "dry_run" -Message "Dry run completed" -CurrentItem $pending.Title
        exit 0
    }

    $codexArgs = @(
        "exec",
        "--cd", $projectRoot,
        "--sandbox", "danger-full-access",
        "--dangerously-bypass-approvals-and-sandbox",
        "--output-last-message", $lastMessageFile,
        "-"
    )

    Write-Log "Starting Codex for $($pending.Title)"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $dynamicPrompt | & $codexPath @codexArgs 2>&1 | Tee-Object -FilePath $logFile -Append
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    $finalMessage = ""
    if (Test-Path $lastMessageFile) {
        $finalMessage = (Get-Content $lastMessageFile -Raw).Trim()
    }
    if ([string]::IsNullOrWhiteSpace($finalMessage)) {
        $finalMessage = "Codex run finished; see log file."
    }

    if ($exitCode -eq 0) {
        Save-State -Status "completed" -Message $finalMessage -CurrentItem $pending.Title
        Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron turu bitti ($runId): $($pending.Title). $finalMessage"
    } else {
        Save-State -Status "failed" -Message $finalMessage -ExitCode $exitCode -CurrentItem $pending.Title
        Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron hata verdi ($runId, exit=$exitCode): $($pending.Title). $finalMessage"
        exit $exitCode
    }
} catch {
    $message = $_.Exception.Message
    Write-Log "Fatal: $message"
    Save-State -Status "error" -Message $message -ExitCode 1
    Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron fatal hata: $message"
    exit 1
} finally {
    Release-Lock
    Write-Log "Lock released"
}
