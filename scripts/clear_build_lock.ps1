# CS-3 acil workaround — sadece bu projenin app\build'ini temizler, admin GEREKMEZ.
# Kullanim: .\scripts\clear_build_lock.ps1  (veya cift tikla)
$repoRoot = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $repoRoot "app\build"

Write-Host "Bu projeye ait 'java.exe' Gradle daemon surecleri durduruluyor..." -ForegroundColor Cyan
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue

if (Test-Path $buildDir) {
    Write-Host "Temizleniyor: $buildDir (yeniden uretilebilir build ciktisi)" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $buildDir
} else {
    Write-Host "Temizlenecek build ciktisi yok: $buildDir" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Tamam. '.\gradlew assembleDebug' ile yeniden build alabilirsiniz." -ForegroundColor Green
