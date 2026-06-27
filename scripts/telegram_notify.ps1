<#
.SYNOPSIS
    Telegram'a mesaj veya dosya gonderir.
#>

param(
    [Parameter(Mandatory = $true)]
    [string]$Message,
    [string]$File,
    [string]$EnvPath = ".env"
)

$ErrorActionPreference = "Stop"

function Get-EnvValue {
    param(
        [string]$Key,
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        $value = [Environment]::GetEnvironmentVariable($Key)
        if ($value) { return $value }
        throw ".env bulunamadi ve ortam degiskeni bos: $Key"
    }

    $line = Get-Content $Path | Where-Object { $_ -match "^\s*$Key\s*=" } | Select-Object -First 1
    if (-not $line) {
        throw "$Key .env icinde yok"
    }

    return ($line -replace "^\s*$Key\s*=\s*", "").Trim().Trim('"')
}

$token = Get-EnvValue -Key "TELEGRAM_BOT_TOKEN" -Path $EnvPath
$chatId = Get-EnvValue -Key "TELEGRAM_CHAT_ID" -Path $EnvPath

if ($File) {
    if (-not (Test-Path $File)) {
        throw "Dosya bulunamadi: $File"
    }

    $sizeMB = [math]::Round((Get-Item $File).Length / 1MB, 1)
    Write-Host ("Dosya gonderiliyor ({0} MB): {1}" -f $sizeMB, $File) -ForegroundColor Cyan
    $url = "https://api.telegram.org/bot$token/sendDocument"

    & curl.exe -s -X POST $url `
        -F "chat_id=$chatId" `
        -F "caption=$Message" `
        -F "document=@$File" | Out-Null
} else {
    Write-Host "Mesaj gonderiliyor..." -ForegroundColor Cyan
    $url = "https://api.telegram.org/bot$token/sendMessage"

    & curl.exe -s -X POST $url `
        -F "chat_id=$chatId" `
        -F "text=$Message" | Out-Null
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Telegram gonderildi." -ForegroundColor Green
} else {
    Write-Host "Telegram gonderimi basarisiz oldu." -ForegroundColor Red
    exit 1
}
