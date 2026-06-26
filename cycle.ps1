# cycle.ps1 — AppOrganizer tam döngü scripti
# Kullanım: .\cycle.ps1 ["commit mesajı"]
# Sıra: git pull → build → commit → push → Telegram APK

param(
    [string]$CommitMessage = "chore: döngü sonu build"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$projectRoot = "c:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager"
$apkPath     = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
$envFile     = "$projectRoot\.env"

Set-Location $projectRoot

# ─── Yardımcı: .env parse ───────────────────────────────────────────────────
function Read-EnvFile {
    param([string]$Path)
    $vars = @{}
    if (Test-Path $Path) {
        Get-Content $Path | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith("#") -and $line -contains "=") {
                $parts = $line -split "=", 2
                $vars[$parts[0].Trim()] = $parts[1].Trim()
            }
        }
    }
    return $vars
}

# ─── ADIM 1: git pull --rebase ───────────────────────────────────────────────
Write-Host ""
Write-Host "[1/8] git pull --rebase origin main ..." -ForegroundColor Cyan
git pull --rebase origin main
if (-not $?) {
    Write-Host "[HATA] git pull basarisiz. Cakismalari coz ve tekrar dene." -ForegroundColor Red
    exit 1
}
Write-Host "[1/7] Uzaktan sync tamam." -ForegroundColor Green

# ─── ADIM 2: Build ───────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[2/7] assembleDebug basliyor ..." -ForegroundColor Cyan
.\gradlew assembleDebug --build-cache --parallel
if ($LASTEXITCODE -ne 0) {
    Write-Host "[HATA] Build basarisiz. Loglari kontrol et." -ForegroundColor Red
    Write-Host "       Ipucu: .\build.ps1 -Clean ile temizleyip tekrar dene." -ForegroundColor Yellow
    exit 1
}
Write-Host "[2/7] Build basarili." -ForegroundColor Green

# ─── ADIM 3: APK boyutu ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/7] APK boyutu hesaplaniyor ..." -ForegroundColor Cyan
if (-not (Test-Path $apkPath)) {
    Write-Host "[HATA] APK bulunamadi: $apkPath" -ForegroundColor Red
    exit 1
}
$apkSizeMB = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
Write-Host "[3/7] APK boyutu: $apkSizeMB MB" -ForegroundColor Green

# ─── ADIM 4-5: git add + commit ──────────────────────────────────────────────
Write-Host ""
Write-Host "[4/7] Degisiklikler staging'e aliniyor ..." -ForegroundColor Cyan
git add -A
if (-not $?) {
    Write-Host "[HATA] git add basarisiz." -ForegroundColor Red
    exit 1
}

Write-Host "[5/7] Commit olusturuluyor: '$CommitMessage' ..." -ForegroundColor Cyan
git diff --cached --quiet
if ($?) {
    Write-Host "[5/7] Commit edilecek degisiklik yok, atlaniyor." -ForegroundColor Yellow
} else {
    git commit -m $CommitMessage
    if (-not $?) {
        Write-Host "[HATA] git commit basarisiz." -ForegroundColor Red
        exit 1
    }
    Write-Host "[5/7] Commit tamam." -ForegroundColor Green
}

# ─── ADIM 6: git push ────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[6/7] git push origin main ..." -ForegroundColor Cyan
git push origin main
if (-not $?) {
    Write-Host "[HATA] git push basarisiz." -ForegroundColor Red
    exit 1
}
Write-Host "[6/7] Push tamam." -ForegroundColor Green

# ─── ADIM 7: Telegram APK gönder ─────────────────────────────────────────────
Write-Host ""
Write-Host "[7/7] Telegram'a APK gonderiliyor ..." -ForegroundColor Cyan

$env_vars = Read-EnvFile -Path $envFile
$botToken = $env_vars["TELEGRAM_BOT_TOKEN"]
$chatId   = $env_vars["TELEGRAM_CHAT_ID"]

if (-not $botToken -or -not $chatId) {
    Write-Host "[UYARI] .env dosyasinda TELEGRAM_BOT_TOKEN veya TELEGRAM_CHAT_ID bulunamadi." -ForegroundColor Yellow
    Write-Host "        Telegram gonderimi atlandi." -ForegroundColor Yellow
} else {
    $timestamp = Get-Date -Format "HH:mm"
    $caption   = "Yeni build | APK: $apkSizeMB MB | $($env:USERNAME) | $timestamp"

    $curlArgs = @(
        "-s", "-X", "POST",
        "https://api.telegram.org/bot$botToken/sendDocument",
        "-F", "chat_id=$chatId",
        "-F", "caption=$caption",
        "-F", "document=@$apkPath"
    )

    $result = curl.exe @curlArgs 2>&1
    if ($result -match '"ok":true') {
        Write-Host "[7/7] Telegram gonderimi basarili." -ForegroundColor Green
    } else {
        Write-Host "[UYARI] Telegram gonderimi basarisiz veya dogrulanamadi." -ForegroundColor Yellow
        Write-Host "        Yanit: $result" -ForegroundColor DarkGray
    }
}

# ─── ADIM 8: Local Denetim ───────────────────────────────────────────────────
Write-Host ""
Write-Host "[8/8] Local denetim calistiriliyor ..." -ForegroundColor Cyan
$auditScript = Join-Path $projectRoot "scripts\audit.ps1"
if (Test-Path $auditScript) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $auditScript -SendTelegram
    Write-Host "[8/8] Denetim tamam." -ForegroundColor Green
} else {
    Write-Host "[UYARI] audit.ps1 bulunamadi, denetim atlandi." -ForegroundColor Yellow
}

# ─── Bitis ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Döngü tamamlandı — APK Telegram'a gonderildi, denetim çalıştırıldı" -ForegroundColor Green
Write-Host "  APK boyutu : $apkSizeMB MB" -ForegroundColor White
Write-Host "  Commit     : $CommitMessage" -ForegroundColor White
Write-Host "  Zaman      : $(Get-Date -Format 'yyyy-MM-dd HH:mm')" -ForegroundColor White
Write-Host ""
