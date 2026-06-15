# build.ps1 — AppOrganizer akıllı build scripti
# Kullanım: .\build.ps1 [-Clean] [-Release]
# -Clean : Kilitli build dizinlerini temizleyip sıfırdan build al
# -Release: assembleDebug yerine bundleRelease çalıştır

param(
    [switch]$Clean,
    [switch]$Release
)

$project = "C:\Users\hekizoglu\Documents\AppOrganizer"
Set-Location $project

$target = if ($Release) { "bundleRelease" } else { "assembleDebug" }

if ($Clean) {
    Write-Host "[build.ps1] Daemon durduruluyor..." -ForegroundColor Yellow
    .\gradlew --stop 2>$null
    Start-Sleep -Seconds 3

    # Java süreçlerini öldür
    Get-Process java,javaw -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2

    # Kilitli dizinleri robocopy ile temizle
    $empty = New-Item -ItemType Directory -Force "$env:TEMP\empty_robocopy_placeholder" -ErrorAction SilentlyContinue
    foreach ($dir in @("app\build\tmp\kapt3", "app\build\kotlin", "app\build\generated\hilt", "app\build\intermediates\hilt")) {
        if (Test-Path "$project\$dir") {
            robocopy "$empty" "$project\$dir" /MIR /NFL /NDL /NJH /NJS /NC /NS 2>$null | Out-Null
            Remove-Item -Recurse -Force "$project\$dir" -ErrorAction SilentlyContinue
        }
    }
    Remove-Item "$empty" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[build.ps1] Temizlendi." -ForegroundColor Green
}

Write-Host "[build.ps1] Build basliyor: $target ..." -ForegroundColor Cyan
.\gradlew $target --build-cache --parallel

if ($LASTEXITCODE -eq 0) {
    $apk = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        $sizeMB = [math]::Round((Get-Item $apk).Length / 1MB, 1)
        Write-Host "[build.ps1] BUILD BASARILI — APK: $sizeMB MB" -ForegroundColor Green
    } else {
        Write-Host "[build.ps1] BUILD BASARILI" -ForegroundColor Green
    }
} else {
    Write-Host "[build.ps1] BUILD HATALI — -Clean ile retry yapiliyor..." -ForegroundColor Red
    if (-not $Clean) {
        & $PSCommandPath -Clean
    } else {
        Write-Host "[build.ps1] Clean sonrasi da hata — logları kontrol et." -ForegroundColor Red
    }
}
