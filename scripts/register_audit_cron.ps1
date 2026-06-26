$legacyTask = "AppOrganizer_LocalDenetim_21min"
$auditTaskName = "AppOrganizer_LocalDenetim_HourlyAudit"
$resolveTaskName = "AppOrganizer_LocalDenetim_Resolve15"
$fullCmd = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_full.cmd"
$resolveCmd = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_resolve.cmd"
$fullRun = '"' + $fullCmd + '"'
$resolveRun = '"' + $resolveCmd + '"'
$startAudit = (Get-Date).AddMinutes(1)
$startResolve = $startAudit.AddMinutes(15)

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
