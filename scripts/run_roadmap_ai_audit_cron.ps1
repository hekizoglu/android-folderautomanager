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

function Get-PendingCount {
    if (-not (Test-Path $roadmapPath)) { return 0 }
    $count = 0
    foreach ($line in (Get-Content $roadmapPath)) {
        if ($line -match '^\*\*Durum:\*\*\s*(?:\S+\s*)?Bekliyor\b') {
            $count++
        }
    }
    return $count
}

function Send-Telegram {
    param(
        [string]$Message,
        [string]$File = ""
    )
    if (-not (Test-Path $telegramScript)) {
        Write-Log "Telegram script missing, skipped"
        return
    }
    try {
        $pendingSuffix = "Kalan Bekliyor: $(Get-PendingCount)"
        $messageWithPending = if ($Message -match 'Kalan Bekliyor:') {
            $Message
        } else {
            "$Message`n$pendingSuffix"
        }
        $args = @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", $telegramScript,
            "-Message", $messageWithPending,
            "-EnvPath", (Join-Path $projectRoot ".env")
        )
        if (-not [string]::IsNullOrWhiteSpace($File)) {
            $args += @("-File", $File)
        }
        & powershell.exe @args | Out-Null
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

function Get-GitStatusText {
    $lines = & git -C $projectRoot status --short 2>$null
    if (-not $lines) { return "" }
    return ($lines -join "`n").Trim()
}

function Assert-CleanWorktree {
    $status = Get-GitStatusText
    if (-not [string]::IsNullOrWhiteSpace($status)) {
        throw "Worktree is dirty before starting a new item. Previous work must be committed or resolved first.`n$status"
    }
}

function Resolve-DirtyWorktreeBeforeNewItem {
    $status = Get-GitStatusText
    if ([string]::IsNullOrWhiteSpace($status)) { return }

    Write-Log "Dirty worktree found before new item; attempting recovery checkpoint."
    Save-State -Status "checkpointing_dirty_worktree" -Message $status -ExitCode 0
    Send-Telegram "AppOrganizer cron onceki turdan kalan degisiklikleri buldu; yeni maddeye baslamadan recovery commit/push deniyor."
    $checkpoint = Invoke-GitCheckpoint "recover previous cron worktree"
    Send-Telegram "AppOrganizer cron recovery checkpoint tamamlandi: $checkpoint. Yeni maddeye devam edilecek."
}

function Convert-ToCommitSlug {
    param([string]$Text)
    $slug = ($Text -replace '[^\p{L}\p{Nd}\s._-]+', ' ' -replace '\s+', ' ').Trim()
    if ($slug.Length -gt 80) { $slug = $slug.Substring(0, 80).Trim() }
    if ([string]::IsNullOrWhiteSpace($slug)) { return "roadmap item" }
    return $slug
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
        if ($lines[$i] -match '^\*\*Durum:\*\*\s*(?:\S+\s*)?Bekliyor\b') {
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

function Invoke-GitCheckpoint {
    param([string]$ItemTitle)

    $status = Get-GitStatusText
    if ([string]::IsNullOrWhiteSpace($status)) {
        Write-Log "No worktree changes to checkpoint."
        return "no_changes"
    }

    Write-Log "Checkpointing worktree changes for $ItemTitle"
    & git -C $projectRoot add -A .
    if ($LASTEXITCODE -ne 0) { throw "git add failed (exit=$LASTEXITCODE)" }
    & git -C $projectRoot reset --quiet -- .roadmap-ai-audit-cron app/build 2>$null

    & git -C $projectRoot diff --cached --quiet
    if ($LASTEXITCODE -eq 0) {
        Write-Log "No staged changes after exclusions."
        return "no_staged_changes"
    }

    $commitTitle = Convert-ToCommitSlug $ItemTitle
    & git -C $projectRoot commit -m "Complete roadmap item: $commitTitle"
    if ($LASTEXITCODE -ne 0) { throw "git commit failed (exit=$LASTEXITCODE)" }

    & git -C $projectRoot fetch origin
    if ($LASTEXITCODE -ne 0) { throw "git fetch failed (exit=$LASTEXITCODE)" }

    & git -C $projectRoot rebase origin/main
    if ($LASTEXITCODE -ne 0) {
        throw "git rebase origin/main failed (exit=$LASTEXITCODE). Manual resolution required."
    }

    & git -C $projectRoot push
    if ($LASTEXITCODE -ne 0) { throw "git push failed (exit=$LASTEXITCODE)" }

    $hash = (& git -C $projectRoot rev-parse --short HEAD).Trim()
    Write-Log "Checkpoint pushed: $hash"
    return $hash
}

function Invoke-GradleWithLockRetry {
    param([string[]]$GradleArgs)

    $gradle = Join-Path $projectRoot "gradlew.bat"
    & $gradle @GradleArgs
    if ($LASTEXITCODE -eq 0) { return }

    Write-Log "Gradle failed (exit=$LASTEXITCODE), running build lock cleanup and retrying: $($GradleArgs -join ' ')"
    $clearScript = Join-Path $scriptDir "clear_build_lock.ps1"
    if (Test-Path $clearScript) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $clearScript | Out-Null
    }
    & $gradle @GradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle command failed after retry (exit=$LASTEXITCODE): $($GradleArgs -join ' ')"
    }
}

function Complete-AllPendingWork {
    $msg = "No pending items remain in $RoadmapFile. Running final build, sending APK, deleting roadmap file."
    Save-State -Status "finalizing" -Message $msg
    Send-Telegram "AppOrganizer roadmap cron final basladi: bekleyen madde kalmadi. Final build + APK + roadmap silme yapiliyor."

    Assert-CleanWorktree
    Invoke-GradleWithLockRetry @("compileDebugKotlin", "-PskipGoogleServices", "--console=plain")
    Invoke-GradleWithLockRetry @("testDebugUnitTest", "-PskipGoogleServices", "--console=plain")
    Invoke-GradleWithLockRetry @("assembleDebug", "-PskipGoogleServices", "--console=plain")

    $apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) { throw "Final APK not found: $apkPath" }
    Send-Telegram "AppOrganizer roadmap tum bekleyenleri bitirdi. Final debug APK gonderiliyor." -File $apkPath

    if (Test-Path $roadmapPath) {
        Remove-Item -LiteralPath $roadmapPath -Force
        Write-Log "Roadmap file deleted after completion: $roadmapPath"
    }

    $historyPath = Join-Path $projectRoot "HISTORY.md"
    Add-Content -Path $historyPath -Encoding UTF8 -Value @"

## Roadmap Cron Finalizasyonu - $(Get-Date -Format "yyyy-MM-dd HH:mm")

**Yapilanlar:** `$RoadmapFile` icinde bekleyen madde kalmadigi icin final kalite kapisi calistirildi, debug APK Telegram'a gonderildi ve roadmap dosyasi silindi.

**Kalite kapisi:** `compileDebugKotlin -PskipGoogleServices`, `testDebugUnitTest -PskipGoogleServices`, `assembleDebug -PskipGoogleServices` basarili.
"@

    $hash = Invoke-GitCheckpoint "finalize completed roadmap and delete $RoadmapFile"
    Save-State -Status "finished" -Message "Final APK sent, roadmap deleted, checkpoint=$hash"
    Send-Telegram "AppOrganizer roadmap cron tamamlandi: APK gonderildi, roadmap dosyasi silindi, commit/push=$hash. Gorev kapatiliyor."
    Stop-ThisTask
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

    Resolve-DirtyWorktreeBeforeNewItem

    $pending = Get-FirstPendingItem
    if (-not $pending) {
        Complete-AllPendingWork
        exit 0
    }

    $codexPath = Resolve-CodexPath
    if (-not $codexPath) { throw "codex command not found" }
    if (-not (Test-Path $promptPath)) { throw "Prompt not found: $promptPath" }

    $dynamicPrompt = @"
Automation run id: $runId
Current pending roadmap item: $($pending.Title)
Status line: ${RoadmapFile}:$($pending.Line)
Worktree summary: clean
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
        $checkpoint = Invoke-GitCheckpoint $pending.Title
        $messageWithCheckpoint = "$finalMessage`nCheckpoint: $checkpoint"
        Save-State -Status "completed" -Message $messageWithCheckpoint -CurrentItem $pending.Title
        Send-Telegram "AppOrganizer ROADMAP_AI_AUDIT cron turu bitti ($runId): $($pending.Title). Checkpoint=$checkpoint. $finalMessage"
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
