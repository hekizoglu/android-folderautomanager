<#
.SYNOPSIS
    telegram_notify.ps1 — AppOrganizer Telegram bildirim helper'ı.

.DESCRIPTION
    Telegram'a mesaj ve/veya dosya (APK) gönderir.
    Token ve Chat ID .env dosyasından okunur (plaintext token KULLANMAZ).
    .env formatı:
        TELEGRAM_BOT_TOKEN=123:ABC...
        TELEGRAM_CHAT_ID=937179261

.PARAMETER Message
    Gönderilecek metin (caption veya tek başına mesaj).

.PARAMETER File
    Gönderilecek dosya yolu (opsiyonel — örn. APK). Verilirse Message caption olur.

.EXAMPLE
    .\scripts\telegram_notify.ps1 -Message "Döngü 85 tamam ✅"

.EXAMPLE
    .\scripts\telegram_notify.ps1 -Message "BUILD #18 hazır" -File "app\build\outputs\apk\debug\app-debug.apk"
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$Message,
    [string]$File,
    [string]$EnvPath = ".env"
)

$ErrorActionPreference = "Stop"

# --- .env oku ---
function Get-EnvValue {
    param([string]$Key, [string]$Path)
    if (-not (Test-Path $Path)) {
        # Ortam değişkeninden dene (CI/GitHub Actions secret)
        $val = [Environment]::GetEnvironmentVariable($Key)
        if ($val) { return $val }
        throw ".env bulunamadı ve ortam değişkeni $Key boş: $Path"
    }
    $line = Get-Content $Path | Where-Object { $_ -match "^\s*$Key\s*=" } | Select-Object -First 1
    if (-not $line) { throw "$Key .env içinde yok" }
    return ($line -replace "^\s*$Key\s*=\s*", "").Trim().Trim('"')
}

$token  = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN" -Path $EnvPath
$chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID"   -Path $EnvPath

# --- Gönder ---
if ($File) {
    if (-not (Test-Path $File)) { throw "Dosya bulunamadı: $File" }
    $sizeMB = [math]::Round((Get-Item $File).Length / 1MB, 1)
    Write-Host "📤 Dosya gönderiliyor ($sizeMB MB): $File" -ForegroundColor Cyan
    $url = "https://api.telegram.org/bot$token/sendDocument"
    & curl.exe -s -X POST $url `
        -F "chat_id=$chatId" `
        -F "caption=$Message" `
        -F "document=@$File" | Out-Null
} else {
    Write-Host "📤 Mesaj gönderiliyor..." -ForegroundColor Cyan
    $url = "https://api.telegram.org/bot$token/sendMessage"
    & curl.exe -s -X POST $url `
        -F "chat_id=$chatId" `
        -F "text=$Message" | Out-Null
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Telegram gönderildi." -ForegroundColor Green
} else {
    Write-Host "❌ Telegram gönderimi başarısız (exit $LASTEXITCODE). api.telegram.org erişimi engelli olabilir." -ForegroundColor Red
    exit 1
}
