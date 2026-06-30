# Registers a Windows scheduled task that sends a debug APK every 6 hours.

param(
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$sendScript = Join-Path $scriptDir "send_debug_build.ps1"
$taskName = "AppOrganizer_DebugBuildDelivery_6h"

if (-not (Test-Path $sendScript)) {
    throw "send_debug_build.ps1 not found: $sendScript"
}

$startAt = if ($StartNow) {
    (Get-Date).AddMinutes(2)
} else {
    (Get-Date).AddHours(6)
}

Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null

$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$sendScript`"" `
    -WorkingDirectory $projectRoot

$trigger = New-ScheduledTaskTrigger `
    -Once `
    -At $startAt `
    -RepetitionInterval (New-TimeSpan -Hours 6) `
    -RepetitionDuration (New-TimeSpan -Days 3650)

$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2)

$principal = New-ScheduledTaskPrincipal `
    -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive `
    -RunLevel Limited

Register-ScheduledTask `
    -TaskName $taskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description "Builds AppOrganizer debug APK and sends it to Telegram every 6 hours." `
    -Force | Out-Null

Write-Host "[cron] Registered task: $taskName" -ForegroundColor Green
Write-Host ("[cron] First run: {0}" -f $startAt.ToString("yyyy-MM-dd HH:mm")) -ForegroundColor Cyan
Write-Host "[cron] Interval: 6 hours" -ForegroundColor Cyan
