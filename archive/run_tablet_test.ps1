# Tablet Test Otomasyonu — AppOrganizer v1.4.21
# Kullanım: .\run_tablet_test.ps1
# Ön koşul: Tablet USB kablolu bağlı, ADB debug mode aktif

param(
    [string]$CrashLogFile = "tablet_crash.log",
    [int]$TestDurationSeconds = 300  # 5 dakika
)

$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "c:\Users\hekizoglu\Documents\AppOrganizer\app\build\outputs\apk\debug\app-debug.apk"
$packageName = "com.armutlu.apporganizer"
$mainActivity = ".presentation.ui.MainActivity"

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     AppOrganizer Tablet Test — v1.4.21                    ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Step 1: ADB Devices
Write-Host "[1/7] ADB bağlantısını kontrol ediliyor..." -ForegroundColor Yellow
$devices = & $adb devices -l
$deviceList = $devices | Select-String "device$" | Where-Object { $_ -notmatch "emulator" }

if (-not $deviceList) {
    Write-Host "❌ Tablet bulunamadı!" -ForegroundColor Red
    Write-Host "Bağlantı kontrolü:" -ForegroundColor Gray
    $devices | ForEach-Object { Write-Host $_ }
    exit 1
}

$deviceId = $deviceList[0].Split()[0]
Write-Host "✅ Tablet bulundu: $deviceId" -ForegroundColor Green
Write-Host ""

# Step 2: Tablet Bilgisi
Write-Host "[2/7] Tablet bilgisi alınıyor..." -ForegroundColor Yellow
$model = & $adb -s $deviceId shell getprop ro.product.model
$android = & $adb -s $deviceId shell getprop ro.build.version.release
$screen = & $adb -s $deviceId shell wm size | Select-String "Physical size"

Write-Host "  Model: $model" -ForegroundColor Cyan
Write-Host "  Android: $android" -ForegroundColor Cyan
Write-Host "  Ekran: $screen" -ForegroundColor Cyan
Write-Host ""

# Step 3: APK Kur
Write-Host "[3/7] APK kuruluyor ($apk)..." -ForegroundColor Yellow
$installResult = & $adb -s $deviceId install -r $apk 2>&1
if ($installResult -match "Success") {
    Write-Host "✅ Kurulum başarılı" -ForegroundColor Green
} else {
    Write-Host "❌ Kurulum başarısız!" -ForegroundColor Red
    Write-Host $installResult
    exit 1
}
Write-Host ""

# Step 4: Uygulamayı Başlat
Write-Host "[4/7] Uygulama başlatılıyor..." -ForegroundColor Yellow
& $adb -s $deviceId shell am start -n "$packageName/$mainActivity" | Out-Null
Start-Sleep -Seconds 3
Write-Host "✅ Uygulama başlatıldı" -ForegroundColor Green
Write-Host ""

# Step 5: Logcat Başlat (Background)
Write-Host "[5/7] Crash monitoring başlatılıyor (background)..." -ForegroundColor Yellow
$logcatProcess = Start-Process -FilePath $adb -ArgumentList "-s", $deviceId, "logcat", "AndroidRuntime:E" `
    -RedirectStandardOutput $CrashLogFile `
    -NoNewWindow -PassThru

Write-Host "✅ Logcat aktif — $CrashLogFile'a yazılıyor" -ForegroundColor Green
Write-Host ""

# Step 6: Test İçeriği
Write-Host "[6/7] Test yapılıyor ($TestDurationSeconds saniye)..." -ForegroundColor Yellow
Write-Host "  Tavsiyeler:" -ForegroundColor Gray
Write-Host "  • HomeScreen'de gezin" -ForegroundColor Gray
Write-Host "  • Klasörlere gir/çık" -ForegroundColor Gray
Write-Host "  • AllAppsDrawer aç (yukarı swipe)" -ForegroundColor Gray
Write-Host "  • Landscape/Portrait dönüş yap" -ForegroundColor Gray
Write-Host "  • Arama yap" -ForegroundColor Gray
Write-Host ""

$startTime = Get-Date
$remaining = $TestDurationSeconds

while ($remaining -gt 0) {
    Write-Progress -Activity "Test çalışıyor" -Status "$remaining saniye kaldı" -PercentComplete (($TestDurationSeconds - $remaining) / $TestDurationSeconds * 100)
    Start-Sleep -Seconds 5
    $remaining -= 5

    # Check if app crashed
    $logContent = Get-Content $CrashLogFile -ErrorAction SilentlyContinue
    if ($logContent -match "FATAL EXCEPTION") {
        Write-Host ""
        Write-Host "❌ CRASH TESPIT EDİLDİ!" -ForegroundColor Red
        break
    }
}
Write-Progress -Activity "Test çalışıyor" -Completed

Write-Host ""
Write-Host "✅ Test süresi tamamlandı" -ForegroundColor Green

# Step 7: Sonuçlar
Write-Host ""
Write-Host "[7/7] Sonuçlar toplanıyor..." -ForegroundColor Yellow

# Logcat'i durdur
Stop-Process -InputObject $logcatProcess -Force -ErrorAction SilentlyContinue
Wait-Process -InputObject $logcatProcess -ErrorAction SilentlyContinue

# Crash Log Analizi
$crashLog = Get-Content $CrashLogFile -ErrorAction SilentlyContinue
$crashLines = @($crashLog | Select-String "AndroidRuntime: FATAL|ANR|Exception")
$crashCount = $crashLines.Count

if ($crashCount -eq 0) {
    Write-Host "✅ Crash/ANR: 0 tespit edilmedi" -ForegroundColor Green
} else {
    Write-Host "❌ Crash/ANR: $crashCount tespit edildi" -ForegroundColor Red
    Write-Host "  İlk 5:" -ForegroundColor Gray
    $crashLines | Select-Object -First 5 | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                   TEST SONUÇ ÖZETİ                         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "Cihaz: $model (Android $android)" -ForegroundColor Cyan
Write-Host "Kurulum: ✅ Başarılı" -ForegroundColor Green
Write-Host "Test Süresi: $TestDurationSeconds saniye" -ForegroundColor Cyan
Write-Host "Crash/ANR: $(if ($crashCount -eq 0) { '✅ 0' } else { "❌ $crashCount" })" -ForegroundColor $(if ($crashCount -eq 0) { 'Green' } else { 'Red' })
Write-Host "Genel: $(if ($crashCount -eq 0) { '✅ BAŞARILI' } else { '⚠️ HATA VAR' })" -ForegroundColor $(if ($crashCount -eq 0) { 'Green' } else { 'Yellow' })
Write-Host ""
Write-Host "Detaylı log: $CrashLogFile"
Write-Host ""

# Cleanup
Remove-Item $CrashLogFile -ErrorAction SilentlyContinue
