$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"

Write-Host "Step 1: Swipe down and tap Get Started..." -ForegroundColor Cyan
& $adb -s $s shell input swipe 500 1800 500 300 300
Start-Sleep -Seconds 1
& $adb -s $s shell input tap 540 2090
Start-Sleep -Seconds 2

Write-Host "Step 2: Tap Apply..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 1980
Start-Sleep -Seconds 2

Write-Host "Step 3: Tap Save and Continue..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 2090
Start-Sleep -Seconds 2

Write-Host "Step 4: Tap Use this layout..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 620
Start-Sleep -Seconds 2

Write-Host "Step 5: Tap Not Now (Launcher)..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 1530
Start-Sleep -Seconds 2

Write-Host "Step 6: Tap Skip (Backup)..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 1970
Start-Sleep -Seconds 2

Write-Host "Step 7: Tap Start..." -ForegroundColor Cyan
& $adb -s $s shell input tap 540 1330
Start-Sleep -Seconds 3

Write-Host "Done! Dumping Home Screen UI..." -ForegroundColor Green
& $adb -s $s shell uiautomator dump /sdcard/real_home_ui.xml
& $adb -s $s shell cat /sdcard/real_home_ui.xml
