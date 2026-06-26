$taskName = "AppOrganizer_LocalDenetim_21min"
$scriptPath = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_cycle.cmd"
$taskRun = '"' + $scriptPath + '"'
$startTime = (Get-Date).AddMinutes(1).ToString("HH:mm")

Start-Process schtasks.exe -ArgumentList @("/Delete", "/TN", $taskName, "/F") -Wait -NoNewWindow 2>$null
$create = Start-Process schtasks.exe -ArgumentList @(
    "/Create",
    "/SC", "MINUTE",
    "/MO", "21",
    "/TN", $taskName,
    "/TR", $taskRun,
    "/ST", $startTime,
    "/F"
) -Wait -NoNewWindow -PassThru

if ($create.ExitCode -ne 0) {
    throw "schtasks gorev kaydi basarisiz oldu."
}

Write-Host "Gorev olusturuldu: $taskName" -ForegroundColor Green
Write-Host "Zamanlama: Her 21 dakika" -ForegroundColor Cyan
Write-Host "Komut: $taskRun" -ForegroundColor Cyan
