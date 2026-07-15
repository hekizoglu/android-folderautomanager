param(
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"

$taskName = "AppOrganizer_CodexRoadmap_15Min"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$runnerScript = Join-Path $scriptDir "run_codex_roadmap_tick.ps1"

if (-not (Test-Path $runnerScript)) {
    throw "Runner script not found: $runnerScript"
}

Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null

$startAt = (Get-Date).AddMinutes(1)
$trigger = New-ScheduledTaskTrigger `
    -Once `
    -At $startAt `
    -RepetitionInterval (New-TimeSpan -Minutes 15) `
    -RepetitionDuration (New-TimeSpan -Days 3650)

$action = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$runnerScript`" -TaskName `"$taskName`"" `
    -WorkingDirectory $projectRoot

$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 12)

Register-ScheduledTask `
    -TaskName $taskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Description "15 dakikada bir guardli Codex roadmap devam gorevi" `
    -RunLevel Highest `
    -Force | Out-Null

Write-Host "Gorev olusturuldu: $taskName" -ForegroundColor Green
Write-Host ("Ilk planli calisma: {0}" -f $startAt.ToString("yyyy-MM-dd HH:mm:ss")) -ForegroundColor Cyan

if ($StartNow) {
    Start-ScheduledTask -TaskName $taskName
    Write-Host "Gorev hemen baslatildi." -ForegroundColor Cyan
}
