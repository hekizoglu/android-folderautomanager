# Wake and Notify Script for Antigravity IDE & Telegram Notification
# Triggered automatically after 10 hours by Windows Task Scheduler

param(
    [string]$WorkspacePath = "c:\Users\hekizoglu\Documents\AppOrganizer"
)

$ErrorActionPreference = "Continue"

# 1. Telegram Notification
$EnvPath = Join-Path $WorkspacePath ".env"
$scriptPath = Join-Path $WorkspacePath "scripts\telegram_notify.ps1"

$message = "💻 Bilgisayar 10 saatlik sürenin ardından otomatik olarak açıldı!`n🚀 Antigravity IDE başlatıldı ve çalışma alanı hazır."

if (Test-Path $scriptPath) {
    try {
        & powershell -ExecutionPolicy Bypass -File $scriptPath -Message $message -EnvPath $EnvPath
        Write-Host "Telegram bildirimi gönderildi." -ForegroundColor Green
    } catch {
        Write-Host "Telegram bildirim hatası: $_" -ForegroundColor Red
    }
}

# 2. Antigravity IDE Launch
$ideExe = "C:\Users\hekizoglu\AppData\Local\Programs\Antigravity IDE\Antigravity IDE.exe"
try {
    if (Test-Path $ideExe) {
        Start-Process -FilePath $ideExe -ArgumentList "`"$WorkspacePath`""
    } elseif (Get-Command agy -ErrorAction SilentlyContinue) {
        & agy $WorkspacePath
    } else {
        Start-Process $WorkspacePath
    }
} catch {
    Write-Host "IDE başlatılamadı: $_" -ForegroundColor Red
}
