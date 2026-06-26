# register_audit_cron.ps1
# 15 dakikada bir audit.ps1 calistiracak Windows Task Scheduler gorevini olusturur.

$taskName = "AppOrganizer_Audit_15min"
$scriptPath = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\scripts\audit.ps1"
$workDir = "C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager"

# Gorev zaten var mi kontrol et
$existing = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Gorev zaten mevcut: $taskName — yeniden olusturuluyor..." -ForegroundColor Yellow
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
}

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`" -SendTelegram"
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).Date.AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 15)
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType S4U -RunLevel Highest

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description "AppOrganizer local denetim — 15 dakikada bir"

Write-Host "Gorev olusturuldu: $taskName" -ForegroundColor Green
Write-Host "Zamanlama: Her 15 dakika bir" -ForegroundColor Cyan
Write-Host "Script: $scriptPath" -ForegroundColor Cyan
