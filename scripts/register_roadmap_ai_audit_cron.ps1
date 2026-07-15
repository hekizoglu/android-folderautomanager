param(
    [string]$TaskName = "AppOrganizer_RoadmapAIAudit_15Min",
    [int]$EveryMinutes = 15,
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"

if ($EveryMinutes -lt 1 -or $EveryMinutes -gt 1439) {
    throw "EveryMinutes must be between 1 and 1439."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runnerScript = Join-Path $scriptDir "run_roadmap_ai_audit_cron.ps1"

if (-not (Test-Path $runnerScript)) {
    throw "Runner script not found: $runnerScript"
}

Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null

$startAt = (Get-Date).AddMinutes(1)
$trigger = New-ScheduledTaskTrigger `
    -Once `
    -At $startAt `
    -RepetitionInterval (New-TimeSpan -Minutes $EveryMinutes) `
    -RepetitionDuration (New-TimeSpan -Days 3650)

$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$runnerScript`" -TaskName `"$TaskName`"" `
    -WorkingDirectory $projectRoot

$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 12)

try {
    Register-ScheduledTask `
        -TaskName $TaskName `
        -Action $action `
        -Trigger $trigger `
        -Settings $settings `
        -Description "Completes ROADMAP_AI_AUDIT_2026-07-14.md pending items one by one with Codex." `
        -Force | Out-Null
} catch {
    Write-Host "Register-ScheduledTask failed, using schtasks fallback..." -ForegroundColor Yellow
    $taskCommand = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$runnerScript`" -TaskName `"$TaskName`""
    $startTime = $startAt.ToString("HH:mm")
    & schtasks.exe /create /f /sc minute /mo $EveryMinutes /tn $TaskName /tr $taskCommand /st $startTime | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "schtasks fallback failed (exit=$LASTEXITCODE)"
    }
}

Write-Host "Scheduled task created: $TaskName" -ForegroundColor Green
Write-Host ("Interval: every {0} minute(s)" -f $EveryMinutes) -ForegroundColor Cyan
Write-Host ("First planned run: {0}" -f $startAt.ToString("yyyy-MM-dd HH:mm:ss")) -ForegroundColor Cyan

if ($StartNow) {
    try {
        Start-ScheduledTask -TaskName $TaskName
    } catch {
        & schtasks.exe /run /tn $TaskName | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Could not start scheduled task (schtasks exit=$LASTEXITCODE)"
        }
    }
    Write-Host "Scheduled task started now." -ForegroundColor Cyan
}
