$taskName = "AppOrganizer_LocalDenetim_21min"
$scriptPath = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\run_local_denetim_cycle.ps1"

$existing = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existing) {
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
}

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 21)
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType S4U -RunLevel Highest

Register-ScheduledTask `
    -TaskName $taskName `
    -Action $action `
    -Trigger $trigger `
    -Settings $settings `
    -Principal $principal `
    -Description "21 dakikada bir local denetim dongusu calistirir."

Write-Host "Gorev olusturuldu: $taskName" -ForegroundColor Green
Write-Host "Zamanlama: Her 21 dakika" -ForegroundColor Cyan
Write-Host "Script: $scriptPath" -ForegroundColor Cyan
