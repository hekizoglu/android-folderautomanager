$StartNow = $false
if ($args -contains "-StartNow") {
    $StartNow = $true
}

$ErrorActionPreference = "Stop"

$legacyTask = "AppOrganizer_LocalDenetim_21min"
$legacyResolveTask = "AppOrganizer_LocalDenetim_Resolve15"
$auditTaskName = "AppOrganizer_LocalDenetim_HourlyAudit"
$resolveTaskName = "AppOrganizer_LocalDenetim_Resolve05"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$cycleScript = Join-Path $scriptDir "run_local_denetim_cycle.ps1"

$now = Get-Date
$nextFullHour = $now.AddHours(1)
$nextAuditHour = Get-Date -Date $nextFullHour -Hour $nextFullHour.Hour -Minute $now.Minute -Second 0
$todayAudit = Get-Date -Hour 4 -Minute 0 -Second 0
$todayResolve = Get-Date -Hour 4 -Minute 5 -Second 0

if ($StartNow) {
    $startAudit = $nextAuditHour
    $startResolve = $startAudit.AddMinutes(5)
} elseif ($now -lt $todayAudit) {
    $startAudit = $todayAudit
    $startResolve = $todayResolve
} else {
    $startAudit = $todayAudit.AddDays(1)
    $startResolve = $todayResolve.AddDays(1)
}

foreach ($taskName in @($legacyTask, $legacyResolveTask, $auditTaskName, $resolveTaskName)) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue | Out-Null
}

$repetitionInterval = New-TimeSpan -Hours 1
$repetitionDuration = New-TimeSpan -Days 3650

$auditAction = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$cycleScript`" -Mode Full -CommitMessage `"chore: scheduled local audit cycle`"" `
    -WorkingDirectory $projectRoot

$resolveAction = New-ScheduledTaskAction `
    -Execute "powershell.exe" `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$cycleScript`" -Mode Resolve -CommitMessage `"chore: scheduled resolve audit cycle`"" `
    -WorkingDirectory $projectRoot

$auditTrigger = New-ScheduledTaskTrigger -Once -At $startAudit -RepetitionInterval $repetitionInterval -RepetitionDuration $repetitionDuration
$resolveTrigger = New-ScheduledTaskTrigger -Once -At $startResolve -RepetitionInterval $repetitionInterval -RepetitionDuration $repetitionDuration

$settings = New-ScheduledTaskSettingsSet `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 72)

Register-ScheduledTask -TaskName $auditTaskName -Action $auditAction -Trigger $auditTrigger -Settings $settings -Description "Saatlik tam local denetim" -Force | Out-Null
Register-ScheduledTask -TaskName $resolveTaskName -Action $resolveAction -Trigger $resolveTrigger -Settings $settings -Description "Tam denetimden 5 dakika sonra local denetim resolve turu" -Force | Out-Null

Write-Host "Gorevler olusturuldu." -ForegroundColor Green
Write-Host "$auditTaskName -> her saat basi tam denetim" -ForegroundColor Cyan
Write-Host "$resolveTaskName -> her saatten 5 dk sonra cozum/build/push hazirligi" -ForegroundColor Cyan
Write-Host ("Ilk audit: {0}" -f $startAudit.ToString("yyyy-MM-dd HH:mm")) -ForegroundColor Cyan
Write-Host ("Ilk resolve: {0}" -f $startResolve.ToString("yyyy-MM-dd HH:mm")) -ForegroundColor Cyan
