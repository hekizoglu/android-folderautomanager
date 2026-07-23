<#
.SYNOPSIS
    15 dakikalik otomatik denetim gorevini Windows Task Scheduler'a kaydeder.
#>

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$auditScript = Join-Path $scriptDir "audit.ps1"

if (-not (Test-Path $auditScript)) {
    throw "audit.ps1 bulunamadi: $auditScript"
}

$taskName = "AppOrganizer_LocalAudit_15min"
$taskDescription = "AppOrganizer projesi icin her 15 dakikada bir local denetim calistirir."

if (-NOT ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "[setup] Yonetici izni gerekli. Yonetici olarak calistirin veya su komutu kullanin:" -ForegroundColor Yellow
    Write-Host "  Start-Process powershell -ArgumentList '-ExecutionPolicy Bypass -File `"$PSCommandPath`"' -Verb RunAs" -ForegroundColor Yellow
    exit 1
}

$action = New-ScheduledTaskAction -Execute "PowerShell.exe" -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$auditScript`""
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).Date.AddHours(4) -RepetitionInterval (New-TimeSpan -Minutes 15)
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -DontStopOnIdleEnd -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$principal = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -RunLevel Highest -LogonType S4U

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings -Principal $principal -Description $taskDescription -Force

Write-Host "[setup] Denetim gorevi kaydedildi: $taskName (her 15 dakika)" -ForegroundColor Green
