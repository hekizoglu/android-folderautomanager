param(
    [string]$Serial = "emulator-5554"
)

$ErrorActionPreference = "Stop"
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$pkg = "com.armutlu.apporganizer"
$reportDir = "build\reports\emulator-test"

New-Item -ItemType Directory -Path $reportDir -Force | Out-Null

Write-Host "[1/6] Installing APK..." -ForegroundColor Cyan
& $adb -s $Serial install -r "app\build\outputs\apk\debug\app-debug.apk"

Write-Host "[2/6] Bypassing Onboarding..." -ForegroundColor Cyan
& $adb -s $Serial shell "am force-stop $pkg"
& $adb -s $Serial shell "run-as $pkg touch files/install_marker"
$prefsXml = '<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><map><boolean name=\"onboarding_done\" value=\"true\" /></map>'
& $adb -s $Serial shell "run-as $pkg sh -c 'mkdir -p shared_prefs && echo `$prefsXml > shared_prefs/com.armutlu.apporganizer_preferences.xml'"

Write-Host "[3/6] Starting LauncherActivity..." -ForegroundColor Cyan
& $adb -s $Serial shell "am start -n $pkg/.presentation.ui.launcher.LauncherActivity"
Start-Sleep -Seconds 5

Write-Host "[4/6] Capturing Home Screen..." -ForegroundColor Cyan
$homePng = Join-Path $reportDir "home_screen.png"
cmd.exe /c "`"$adb`" -s $Serial exec-out screencap -p > `"$homePng`""
& $adb -s $Serial shell uiautomator dump /sdcard/home_ui.xml
& $adb -s $Serial shell cat /sdcard/home_ui.xml > (Join-Path $reportDir "home_ui.xml")

Write-Host "[5/6] Checking Logcat for Fatal Crashes..." -ForegroundColor Cyan
$fatalLogs = & $adb -s $Serial logcat -d -v brief -t 1000 | Select-String "FATAL EXCEPTION"

$summary = @(
    "=== EMULATOR TEST SUMMARY ===",
    "Date: $(Get-Date)",
    "Device: $Serial",
    "Package: $pkg",
    "Home Screen Image: $homePng",
    "Fatal Crash Count: $($fatalLogs.Count)"
)
if ($fatalLogs.Count -gt 0) {
    $summary += "FATAL LOGS:"
    $summary += $fatalLogs
} else {
    $summary += "STATUS: PASS - No crashes detected!"
}

$summaryPath = Join-Path $reportDir "test_summary.txt"
$summary | Set-Content -Path $summaryPath
Write-Host "Test output saved to $summaryPath" -ForegroundColor Green
Get-Content $summaryPath
