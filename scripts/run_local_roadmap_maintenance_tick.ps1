param(
    [string]$TaskName = "AppOrganizer_LocalRoadmap_15Min",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$automationDir = Join-Path $projectRoot ".local-roadmap-automation"
$logDir = Join-Path $automationDir "logs"
$lockFile = Join-Path $automationDir "maintenance.lock"
$stateFile = Join-Path $automationDir "maintenance_state.json"
$counterFile = Join-Path $automationDir "tick_count.txt"
$roadmapPath = Join-Path $projectRoot "ROADMAP_AI_AUDIT_2026-07-14.md"
$devirPath = Join-Path $projectRoot "CODEX_DEVIR_2026-07-15.md"
$cycleScript = Join-Path $scriptDir "run_local_denetim_cycle.ps1"
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"

New-Item -ItemType Directory -Force -Path $automationDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$runId = Get-Date -Format "yyyyMMdd-HHmmss"
$logFile = Join-Path $logDir "local-roadmap-$runId.log"
$script:previousState = $null

function Write-Log {
    param([string]$Message)
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

function Get-PreviousState {
    if (-not (Test-Path $stateFile)) { return $null }
    try {
        return Get-Content $stateFile -Raw | ConvertFrom-Json
    } catch {
        return $null
    }
}

function Save-State {
    param(
        [string]$Status,
        [string]$Message,
        [string]$NextItem = "",
        [int]$TickNumber = 0
    )
    $payload = [ordered]@{
        task_name = $TaskName
        run_id = $runId
        status = $Status
        message = $Message
        next_item = $NextItem
        tick_number = $TickNumber
        updated_at = (Get-Date).ToString("o")
        log_file = $logFile
    }
    $payload | ConvertTo-Json | Set-Content -Path $stateFile -Encoding UTF8
}

function Should-SendTelegram {
    param([string]$Status, [string]$Message)
    if (-not $script:previousState) { return $true }
    if ($Status -notin @("error", "failed", "blocked")) { return $true }
    if ($script:previousState.status -ne $Status) { return $true }
    if ($script:previousState.message -ne $Message) { return $true }
    try {
        $last = [datetime]$script:previousState.updated_at
        return ((Get-Date) - $last) -ge (New-TimeSpan -Hours 6)
    } catch {
        return $true
    }
}

function Send-Telegram {
    param([string]$Message)
    if (-not (Test-Path $telegramScript)) {
        Write-Log "Telegram script missing"
        return
    }
    try {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $telegramScript -Message $Message -EnvPath (Join-Path $projectRoot ".env") | Out-Null
        Write-Log "Telegram sent"
    } catch {
        Write-Log "Telegram failed: $($_.Exception.Message)"
    }
}

function Acquire-Lock {
    if (Test-Path $lockFile) {
        $existing = Get-Content $lockFile -Raw | ConvertFrom-Json
        $existingPid = [int]$existing.pid
        $alive = Get-Process -Id $existingPid -ErrorAction SilentlyContinue
        if ($alive) {
            throw "Existing maintenance run active (pid=$existingPid)"
        }
        Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
    }

    [ordered]@{
        pid = $PID
        started_at = (Get-Date).ToString("o")
        log_file = $logFile
    } | ConvertTo-Json | Set-Content -Path $lockFile -Encoding UTF8
}

function Release-Lock {
    if (Test-Path $lockFile) {
        Remove-Item -LiteralPath $lockFile -Force -ErrorAction SilentlyContinue
    }
}

function Get-TickNumber {
    if (-not (Test-Path $counterFile)) {
        Set-Content -Path $counterFile -Value "0" -Encoding UTF8
    }
    $raw = Get-Content $counterFile -ErrorAction SilentlyContinue | Select-Object -First 1
    $current = 0
    if ($raw -match '^\d+$') {
        $current = [int]$raw
    }
    $next = $current + 1
    Set-Content -Path $counterFile -Value $next -Encoding UTF8
    return $next
}

function Get-NextRoadmapItem {
    if (-not (Test-Path $roadmapPath)) {
        return "Roadmap dosyasi bulunamadi"
    }

    $lines = Get-Content $roadmapPath
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^##\s+(P\d+\.\d+)\s+—\s+(.+)$') {
            $itemCode = $matches[1]
            $itemTitle = $matches[2].Trim()
            $statusLine = ""
            for ($j = $i + 1; $j -lt [Math]::Min($i + 6, $lines.Count); $j++) {
                if ($lines[$j] -match '^\*\*Durum:\*\*\s*(.+)$') {
                    $statusLine = $matches[1]
                    break
                }
            }
            if ($statusLine -notmatch 'Tamamlandı|✅') {
                return "$itemCode - $itemTitle"
            }
        }
    }

    if (Test-Path $devirPath) {
        $devirLines = Get-Content $devirPath
        $suggested = $devirLines | Where-Object { $_ -match '^\d+\.\s+\*\*(P\d+\.\d+)\s+—\s+(.+?)\*\*' } | Select-Object -First 1
        if ($suggested) {
            return ($suggested -replace '^\d+\.\s+\*\*', '' -replace '\*\*.*$', '')
        }
    }

    return "Acilacak roadmap maddesi bulunamadi"
}

function Get-CurrentHead {
    $head = git rev-parse HEAD 2>$null
    if ($LASTEXITCODE -ne 0) { return "" }
    return ($head | Select-Object -First 1).Trim()
}

try {
    $script:previousState = Get-PreviousState
    Acquire-Lock
    Write-Log "Lock acquired"

    if (-not (Test-Path $cycleScript)) {
        throw "run_local_denetim_cycle.ps1 bulunamadi"
    }

    Set-Location $projectRoot
    $tickNumber = Get-TickNumber
    $mode = if ($tickNumber % 4 -eq 0) { "Full" } else { "Resolve" }
    $nextItem = Get-NextRoadmapItem
    $headBefore = Get-CurrentHead

    $startMessage = "AppOrganizer yerel roadmap turu basladi (#$tickNumber, $mode). Siradaki acik madde: $nextItem"
    Write-Log $startMessage
    Save-State -Status "running" -Message $startMessage -NextItem $nextItem -TickNumber $tickNumber
    Send-Telegram $startMessage

    if ($DryRun) {
        Write-Log "DryRun enabled, maintenance cycle skipped"
        Save-State -Status "dry_run" -Message "Dry run completed" -NextItem $nextItem -TickNumber $tickNumber
        exit 0
    }

    $cycleArgs = @(
        "-Mode", $mode,
        "-SendTelegram"
    )

    Write-Log "Starting local denetim cycle"
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $cycleScript @cycleArgs 2>&1 | Tee-Object -FilePath $logFile -Append
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        $message = "Yerel roadmap turu hata ile bitti (#$tickNumber, $mode, exit=$exitCode)."
        Write-Log $message
        Save-State -Status "failed" -Message $message -NextItem $nextItem -TickNumber $tickNumber
        if (Should-SendTelegram -Status "failed" -Message $message) {
            Send-Telegram $message
        }
        exit $exitCode
    }

    $headAfter = Get-CurrentHead
    $headChanged = $headBefore -ne "" -and $headAfter -ne "" -and $headBefore -ne $headAfter
    $resultMessage = if ($headChanged) {
        "Yerel roadmap turu tamamlandi (#$tickNumber, $mode). Denetim scripti yeni commit uretti. Siradaki acik madde: $nextItem"
    } else {
        "Yerel roadmap turu tamamlandi (#$tickNumber, $mode). Yeni commit yok, denetim ve raporlama guncellendi. Siradaki acik madde: $nextItem"
    }
    Write-Log $resultMessage
    Save-State -Status "completed" -Message $resultMessage -NextItem $nextItem -TickNumber $tickNumber
    Send-Telegram $resultMessage
} catch {
    $message = $_.Exception.Message
    Write-Log "Fatal error: $message"
    Save-State -Status "error" -Message $message
    if (Should-SendTelegram -Status "error" -Message $message) {
        Send-Telegram "AppOrganizer yerel roadmap otomasyonu durdu: $message"
    }
    exit 1
} finally {
    Release-Lock
    Write-Log "Lock released"
}
