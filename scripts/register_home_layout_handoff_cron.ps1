param(
    [string]$TaskName = "AppOrganizer_StatsToHomeLayout_Handoff",
    [int]$EveryMinutes = 3,
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"

if ($EveryMinutes -lt 1 -or $EveryMinutes -gt 1439) {
    throw "EveryMinutes must be between 1 and 1439."
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$handoffScript = Join-Path $scriptDir "start_home_layout_after_stats_cron.ps1"

if (-not (Test-Path $handoffScript)) {
    throw "Handoff script not found: $handoffScript"
}

Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null

$startAt = (Get-Date).AddMinutes(1)
$trigger = New-ScheduledTaskTrigger `
    -Once `
    -At $startAt `
    -RepetitionInterval (New-TimeSpan -Minutes $EveryMinutes) `
    -RepetitionDuration (New-TimeSpan -Days 30)

$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$handoffScript`" -WatcherTaskName `"$TaskName`"" `
    -WorkingDirectory $projectRoot

$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 30)

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Description "Starts the home layout roadmap cron after the stats/health roadmap cron finishes." `
    -Force | Out-Null

Write-Host "Scheduled handoff task created: $TaskName" -ForegroundColor Green
Write-Host ("Interval: every {0} minute(s)" -f $EveryMinutes) -ForegroundColor Cyan
Write-Host ("First planned run: {0}" -f $startAt.ToString("yyyy-MM-dd HH:mm:ss")) -ForegroundColor Cyan

if ($StartNow) {
    Start-ScheduledTask -TaskName $TaskName
    Write-Host "Handoff task started now." -ForegroundColor Cyan
}
