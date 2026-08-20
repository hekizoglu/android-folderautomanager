# Schedule 10-Hour Wake & Hibernate Script
# Registers a Windows Scheduled Task with WakeToRun enabled, then hibernates the PC.

$ErrorActionPreference = "Stop"

$taskName = "WakeAndNotifyAntigravity"
$wakeTime = (Get-Date).AddHours(10)
$scriptPath = "c:\Users\hekizoglu\Documents\AppOrganizer\scripts\wake_and_notify.ps1"

Write-Host ("Zamanlanmış görev hazırlanıyor. Uyanma zamanı: {0:yyyy-MM-dd HH:mm:ss}" -f $wakeTime) -ForegroundColor Cyan

# Remove old task if exists
Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue

# Create Action
$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-ExecutionPolicy Bypass -WindowStyle Hidden -File `"$scriptPath`""

# Create Trigger (Exact 10 hours from now)
$trigger = New-ScheduledTaskTrigger -Once -At $wakeTime

# Create Settings with WakeToRun enabled
$settings = New-ScheduledTaskSettingsSet -WakeToRun -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -Priority 1

# Register Task
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -User $env:USERNAME | Out-Null

Write-Host "Zamanlanmış görev kaydedildi ve Wake-Timer (Uyanma Zamanlayıcısı) aktif edildi." -ForegroundColor Green
Write-Host "Bilgisayar 10 saat sonra ($($wakeTime.ToString('HH:mm:ss'))) otomatik olarak uyanacak, Telegram'a mesaj atacak ve Antigravity IDE'yi açacaktır." -ForegroundColor Yellow
