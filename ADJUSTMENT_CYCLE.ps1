# ADJUSTMENT_CYCLE.ps1 — AppOrganizer Düzeltme Döngüsü
# Görev: Kod hatalarını ve mantık hatalarını tespit edip düzeltir
# Kullanım: .\ADJUSTMENT_CYCLE.ps1 [commit_mesaji]
# Sıra: syntax check → build → commit → push → build verification

param(
    [string]$CommitMessage = "fix: düzeltme döngüsü"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$projectRoot = "c:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager"
$apkPath     = "$projectRoot\app\build\outputs\apk\debug\app-debug.apk"
$envFile     = "$projectRoot\.env"

Set-Location $projectRoot

# ─── ADIM 1: Kotlin Syntax Check ───────────────────────────────────────────────
Write-Host ""
Write-Host "[1/5] Kotlin syntax kontrolü..." -ForegroundColor Cyan
$kotlinCheck = .\gradlew compileDebugKotlin --quiet 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[HATA] Kotlin derleme hatası. Logları kontrol et:" -ForegroundColor Red
    $kotlinCheck | Select-String -Pattern "error|Error" | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
Write-Host "[1/5] Syntax kontrolü tamam." -ForegroundColor Green

# ─── ADIM 2: Build ───────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[2/5] assembleDebug başlıyor..." -ForegroundColor Cyan
.\gradlew assembleDebug --build-cache --parallel
if ($LASTEXITCODE -ne 0) {
    Write-Host "[HATA] Build başarısız." -ForegroundColor Red
    exit 1
}
Write-Host "[2/5] Build başarılı." -ForegroundColor Green

# ─── ADIM 3: APK boyutu ──────────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/5] APK boyutu kontrol ediliyor..." -ForegroundColor Cyan
if (Test-Path $apkPath) {
    $apkSizeMB = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
    Write-Host "[3/5] APK boyutu: $apkSizeMB MB" -ForegroundColor Green
} else {
    Write-Host "[UYARI] APK bulunamadı" -ForegroundColor Yellow
}

# ─── ADIM 4-5: git add + commit + push ──────────────────────────────────────────────
Write-Host ""
Write-Host "[4/5] Değişiklikler staging'e aliniyor..." -ForegroundColor Cyan
git add -A
git diff --cached --quiet
if ($?) {
    Write-Host "[4/5] Commit edilecek değişiklik yok." -ForegroundColor Yellow
} else {
    git commit -m "[DÜZELTME DÖNGÜSÜ] $CommitMessage"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[HATA] git commit başarısız." -ForegroundColor Red
        exit 1
    }
    Write-Host "[4/5] Commit tamam." -ForegroundColor Green
}

Write-Host ""
Write-Host "[5/5] git push origin main ..." -ForegroundColor Cyan
git push origin main
if ($LASTEXITCODE -ne 0) {
    Write-Host "[HATA] git push başarısız." -ForegroundColor Red
    exit 1
}
Write-Host "[5/5] Push tamam." -ForegroundColor Green

# ─── Bitis ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Düzeltme döngüsü tamamlandı" -ForegroundColor Green
Write-Host "  Commit     : $CommitMessage" -ForegroundColor White
Write-Host ""