# CS-3 acil workaround: sadece Gradle daemon'u durdurur ve bu projenin app\build'ini temizler.
# Kullanim: .\scripts\clear_build_lock.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$buildDir = Join-Path $repoRoot "app\build"
$gradlew = Join-Path $repoRoot "gradlew.bat"

Write-Host "Gradle daemon durduruluyor..." -ForegroundColor Cyan
if (Test-Path $gradlew) {
    Push-Location $repoRoot
    try {
        & $gradlew --stop | Out-Host
    } finally {
        Pop-Location
    }
} else {
    Write-Host "gradlew.bat bulunamadi; daemon stop atlandi." -ForegroundColor Yellow
}

if (Test-Path $buildDir) {
    Write-Host "Temizleniyor: $buildDir (yeniden uretilebilir build ciktisi)" -ForegroundColor Yellow
    Remove-Item -Recurse -Force $buildDir
} else {
    Write-Host "Temizlenecek build ciktisi yok: $buildDir" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Tamam. '.\gradlew assembleDebug' ile yeniden build alabilirsiniz." -ForegroundColor Green
