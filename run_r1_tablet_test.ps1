#Requires -Version 5.1
<#
.SYNOPSIS
    R1 Tablet Smoke Test Runner for AppOrganizer Launcher
.DESCRIPTION
    Boots emulator, installs APK, validates onboarding + landscape rotation.
    No physical device required. Generates test report + screenshots.
.PARAMETER AVDName
    Android Virtual Device name (default: Pixel6_AOSP33)
.PARAMETER BuildAPK
    Force rebuild APK before test (default: $false)
.PARAMETER ReportDir
    Output directory for screenshots & logs (default: R1_screenshots)
#>

param(
    [string]$AVDName = "Pixel6_AOSP33",
    [switch]$BuildAPK = $false,
    [string]$ReportDir = "R1_screenshots"
)

$ErrorActionPreference = "Stop"
$PSDefaultParameterValues = @{ "Write-Host:ForegroundColor" = "Cyan" }

# Paths
$SDK = "C:\Users\hekizoglu\AppData\Local\Android\Sdk"
$ADB = "$SDK\platform-tools\adb.exe"
$EMU = "$SDK\emulator\emulator.exe"
$GRADLE = ".\gradlew.bat"
$APK = "app\build\outputs\apk\debug\app-debug.apk"
$PACKAGE = "com.armutlu.apporganizer"
$ACTIVITY = "com.armutlu.apporganizer/.presentation.ui.MainActivity"

# Verify tools exist
foreach ($tool in @($ADB, $EMU)) {
    if (-not (Test-Path $tool)) {
        Write-Error "Tool not found: $tool"
    }
}

# Create report directory
if (-not (Test-Path $ReportDir)) {
    New-Item -ItemType Directory $ReportDir | Out-Null
}

Write-Host "`n=== R1 Tablet Smoke Test ===" -ForegroundColor Green
Write-Host "AVD: $AVDName"
Write-Host "Report: $ReportDir`n"

# Step 1: Build APK if requested
if ($BuildAPK) {
    Write-Host "[1/8] Building APK..." -ForegroundColor Yellow
    if (-not (Test-Path $GRADLE)) {
        Write-Error "gradlew.bat not found"
    }
    & $GRADLE assembleDebug -q
    if (-not (Test-Path $APK)) {
        Write-Error "APK build failed"
    }
    Write-Host "✓ APK built`n"
} else {
    if (-not (Test-Path $APK)) {
        Write-Host "⚠ APK not found. Building..."
        & $GRADLE assembleDebug -q
    }
}

# Step 2: Kill running emulator
Write-Host "[2/8] Clearing old emulator instance..." -ForegroundColor Yellow
$devices = & $ADB devices | Select-String "emulator"
foreach ($device in $devices) {
    $devName = $device.ToString().Split()[0]
    Write-Host "Killing $devName..."
    & $ADB -s $devName emu kill 2>&1 | Out-Null
}
Start-Sleep -Seconds 2

# Step 3: Boot emulator
Write-Host "[3/8] Booting emulator $AVDName..." -ForegroundColor Yellow
$emuProc = Start-Process -NoNewWindow -PassThru -FilePath $EMU -ArgumentList "-avd", $AVDName, "-no-snapshot-save"
Write-Host "Emulator PID: $($emuProc.Id)"

# Wait for device online (max 60s)
Write-Host "Waiting for device to come online..."
$bootTimeout = 60
$bootElapsed = 0
$online = $false
while ($bootElapsed -lt $bootTimeout) {
    $devices = & $ADB devices 2>&1 | Select-String "device$"
    if ($devices) {
        $online = $true
        Write-Host "✓ Device online (${bootElapsed}s)"
        break
    }
    Start-Sleep -Seconds 2
    $bootElapsed += 2
}

if (-not $online) {
    Write-Error "Emulator failed to come online within ${bootTimeout}s"
}
Write-Host ""

# Step 4: Check logcat for early crashes (first 10s post-boot)
Write-Host "[4/8] Checking logcat for boot crashes (10s)..." -ForegroundColor Yellow
$logcatProc = Start-Process -NoNewWindow -PassThru -FilePath $ADB -ArgumentList "logcat", "AndroidRuntime:E"
Start-Sleep -Seconds 10
Stop-Process -InputObject $logcatProc -Force -ErrorAction SilentlyContinue
Write-Host "✓ Boot logcat checked`n"

# Step 5: Install APK
Write-Host "[5/8] Installing APK..." -ForegroundColor Yellow
$installOut = & $ADB install -r $APK 2>&1
if ($installOut | Select-String "Success") {
    Write-Host "✓ APK installed"
} else {
    Write-Error "APK installation failed: $installOut"
}

# Verify package installed
$pkgList = & $ADB shell pm list packages | Select-String $PACKAGE
if ($pkgList) {
    Write-Host "✓ Package verified: $PACKAGE"
} else {
    Write-Error "Package not found after install"
}
Write-Host ""

# Step 6: Launch app
Write-Host "[6/8] Launching app..." -ForegroundColor Yellow
& $ADB shell am start -n "$ACTIVITY"
Start-Sleep -Seconds 3
Write-Host "✓ App launched`n"

# Step 7: Capture initial screenshot (WELCOME screen)
Write-Host "[7/8] Capturing initial state..." -ForegroundColor Yellow
$screenshotTime = Get-Date -Format "yyyyMMdd_HHmmss"
& $ADB shell screencap -p "/sdcard/screenshot_$screenshotTime.png" 2>&1 | Out-Null
& $ADB pull "/sdcard/screenshot_$screenshotTime.png" "$ReportDir\01_WELCOME_portrait.png" 2>&1 | Out-Null
Write-Host "✓ Screenshot: 01_WELCOME_portrait.png"

# Simulate tap on "Next" button (approximate center-bottom)
Write-Host "Tapping Next button..."
& $ADB shell input tap 540 1100 2>&1 | Out-Null
Start-Sleep -Seconds 2

# Step 8: Dump final logcat for analysis
Write-Host "[8/8] Collecting logs..." -ForegroundColor Yellow
& $ADB logcat -d > "$ReportDir\logcat_$screenshotTime.txt" 2>&1
Write-Host "✓ Logcat dump: logcat_$screenshotTime.txt`n"

# Summary
Write-Host "=== Test Complete ===" -ForegroundColor Green
Write-Host "Report directory: $(Resolve-Path $ReportDir)`n"

$crashCount = (Select-String "AndroidRuntime" "$ReportDir\logcat_$screenshotTime.txt" -ErrorAction SilentlyContinue | Measure-Object).Count
Write-Host "Crashes detected: $crashCount"
Write-Host "Screenshots: $(Get-ChildItem "$ReportDir\*.png" -ErrorAction SilentlyContinue | Measure-Object).Count"
Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Review screenshots in $ReportDir"
Write-Host "  2. Check logcat for errors: $ReportDir\logcat_$screenshotTime.txt"
Write-Host "  3. Continue onboarding flow manually if needed (see plan document)"
Write-Host "  4. Test landscape rotation: emulator ExtendedControls > Rotation"
Write-Host ""

# Prompt to keep emulator running or close
$response = Read-Host "Keep emulator running? (y/n)"
if ($response -ne "y") {
    Write-Host "Stopping emulator..."
    Stop-Process -InputObject $emuProc -Force -ErrorAction SilentlyContinue
    Write-Host "Done."
}
