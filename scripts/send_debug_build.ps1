# Builds the debug APK and sends it to Telegram.
# Intended for both manual use and the 6-hour scheduled delivery task.

param(
    [switch]$NoTelegram
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$buildScript = Join-Path $projectRoot "build.ps1"
$telegramScript = Join-Path $scriptDir "telegram_notify.ps1"
$apkPath = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
$envPath = Join-Path $projectRoot ".env"

Set-Location $projectRoot

function Send-TelegramMessage {
    param(
        [string]$Message,
        [string]$File = ""
    )

    if ($NoTelegram) {
        Write-Host "[send-build] Telegram disabled. Message would be:" -ForegroundColor Yellow
        Write-Host $Message
        return
    }

    if (-not (Test-Path $telegramScript)) {
        throw "telegram_notify.ps1 not found: $telegramScript"
    }

    if ($File) {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $telegramScript -Message $Message -File $File -EnvPath $envPath
    } else {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $telegramScript -Message $Message -EnvPath $envPath
    }
}

if (-not (Test-Path $buildScript)) {
    throw "build.ps1 not found: $buildScript"
}

$startedAt = Get-Date
$branch = (git rev-parse --abbrev-ref HEAD 2>$null)
$commit = (git rev-parse --short HEAD 2>$null)
if (-not $branch) { $branch = "unknown-branch" }
if (-not $commit) { $commit = "unknown" }

Write-Host "[send-build] Building debug APK..." -ForegroundColor Cyan
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript
$buildExit = $LASTEXITCODE

if ($buildExit -ne 0) {
    $message = "AppOrganizer build FAILED`nBranch: $branch`nCommit: $commit`nTime: $($startedAt.ToString('yyyy-MM-dd HH:mm'))"
    Send-TelegramMessage -Message $message
    exit $buildExit
}

if (-not (Test-Path $apkPath)) {
    $message = "AppOrganizer build produced no APK`nBranch: $branch`nCommit: $commit`nExpected: $apkPath"
    Send-TelegramMessage -Message $message
    exit 1
}

$sizeMB = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
$finishedAt = Get-Date
$caption = "AppOrganizer debug build`nBranch: $branch`nCommit: $commit`nAPK: $sizeMB MB`nBuilt: $($finishedAt.ToString('yyyy-MM-dd HH:mm'))"

Send-TelegramMessage -Message $caption -File $apkPath
Write-Host "[send-build] Done. APK: $sizeMB MB" -ForegroundColor Green
