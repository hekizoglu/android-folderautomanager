$StartNow = $false
if ($args -contains "-StartNow") {
    $StartNow = $true
}

$legacyTask = "AppOrganizer_LocalDenetim_21min"
$auditTaskName = "AppOrganizer_LocalDenetim_HourlyAudit"
$resolveTaskName = "AppOrganizer_LocalDenetim_Resolve15"
$fullCmd = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_full.cmd"
$resolveCmd = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_resolve.cmd"
$fullRun = '"' + $fullCmd + '"'
$resolveRun = '"' + $resolveCmd + '"'
$now = Get-Date
$nextFullHour = $now.AddHours(1)
$nextAuditHour = Get-Date -Date $nextFullHour -Hour $nextFullHour.Hour -Minute $now.Minute -Second 0
$todayAudit = Get-Date -Hour 4 -Minute 0 -Second 0
$todayResolve = Get-Date -Hour 4 -Minute 15 -Second 0

if ($StartNow) {
    $startAudit = $nextAuditHour
    $startResolve = $startAudit.AddMinutes(15)
} elseif ($now -lt $todayAudit) {
    $startAudit = $todayAudit
    $startResolve = $todayResolve
} else {
    $startAudit = $todayAudit.AddDays(1)
    $startResolve = $todayResolve.AddDays(1)
}

foreach ($taskName in @($legacyTask, $auditTaskName, $resolveTaskName)) {
    Start-Process schtasks.exe -ArgumentList @("/Delete", "/TN", $taskName, "/F") -Wait -NoNewWindow 2>$null
}

$auditCreate = Start-Process schtasks.exe -ArgumentList @(
    "/Create",
    "/SC", "HOURLY",
    "/MO", "1",
    "/TN", $auditTaskName,
    "/TR", $fullRun,
    "/ST", $startAudit.ToString("HH:mm"),
    "/F"
) -Wait -NoNewWindow -PassThru

if ($auditCreate.ExitCode -ne 0) {
    throw "Saatlik audit gorevi olusturulamadi."
}

$resolveCreate = Start-Process schtasks.exe -ArgumentList @(
    "/Create",
    "/SC", "HOURLY",
    "/MO", "1",
    "/TN", $resolveTaskName,
    "/TR", $resolveRun,
    "/ST", $startResolve.ToString("HH:mm"),
    "/F"
) -Wait -NoNewWindow -PassThru

if ($resolveCreate.ExitCode -ne 0) {
    throw "15 dakika sonraki resolve gorevi olusturulamadi."
}

Write-Host "Gorevler olusturuldu." -ForegroundColor Green
Write-Host "$auditTaskName -> her saat basi tam denetim" -ForegroundColor Cyan
Write-Host "$resolveTaskName -> her saatten 15 dk sonra cozum/build/push hazirligi" -ForegroundColor Cyan
Write-Host ("Ilk audit: {0}" -f $startAudit.ToString("yyyy-MM-dd HH:mm")) -ForegroundColor Cyan
Write-Host ("Ilk resolve: {0}" -f $startResolve.ToString("yyyy-MM-dd HH:mm")) -ForegroundColor Cyan
