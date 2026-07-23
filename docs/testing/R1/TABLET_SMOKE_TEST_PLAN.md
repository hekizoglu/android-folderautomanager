# R1 Tablet Smoke Test Plan — AppOrganizer Launcher

**Date:** 2026-07-22  
**Target:** Android 33+ (Emulator only, no physical device)  
**Scope:** Layout validation, rotation handling, multi-pane readiness  

---

## 1. Emulator Readiness Status

### Available AVDs
- `Pixel6_AOSP33` (Phone profile, x86_64, Android 33)
- `Pixel7_API33` (Phone profile, x86_64, Android 33)

### Tablet Infrastructure Gap
❌ **Issue:** No dedicated tablet AVD (e.g., "Pixel_Tablet_API34", "Xiaomi_HyperOS_API34") currently configured.  
❌ **Constraint:** SDK only has Android 33 system images; API 34/35 not installed.  

### Recommendation
**For R1 pilot:** Use `Pixel6_AOSP33` with **forced landscape rotation** testing to validate:
- Horizontal pager layout (8 klasör/page)
- Text readability at expanded widths
- Button/icon spacing in landscape
- No UI overflow or clipping

**For full tablet validation (R2+):** Create dedicated tablet AVD once Android 34+ system images are available.

---

## 2. R1 Smoke Test Scenario (Phone Emulator + Landscape)

### Prerequisites
- Latest `app-debug.apk` built with `.\gradlew assembleDebug`
- Emulator: `Pixel6_AOSP33` (boot ~30s)
- ADB available: `C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe`

### Test Flow

| Step | Screen/Action | Expected Result | Checks |
|------|--------------|-----------------|--------|
| **1** | Emulator boot | Device ready, logcat clean | No AndroidRuntime:E crashes for 10s |
| **2** | APK install | APK installed, no errors | `adb shell pm list packages \| grep apporganizer` shows package |
| **3** | App launch | MainActivity opens, onboarding visible | No crash in logcat; WELCOME screen renders |
| **4** | Onboarding: WELCOME | Welcome text, "Next" button visible | Text readable, button tappable (tap to proceed) |
| **5** | Onboarding: THEME_SELECT | Theme options (Pixel/Custom), thumbnails | No layout overflow; both themes render (select any) |
| **6** | Onboarding: QUICK_SETTINGS | Settings toggles, text | Toggles responsive; text not clipped (landscape: wider layout expected) |
| **7** | Onboarding: SET_LAUNCHER | Launcher selection, "Next" button | Dialog renders; no button cutoff |
| **8** | Onboarding: DONE | Completion screen | HomeScreen transition smooth |
| **9** | HomeScreen (Portrait) | Klasör cards, 1-8 klassörü visible, HorizontalPager | Cards layout clean; pager responsive (swipe to next) |
| **10** | Rotate to Landscape | HomeScreen landscape | Cards reflow to wider layout; no overflow; text still readable |
| **11** | Rotate back to Portrait | HomeScreen portrait | Layout restored correctly |
| **12** | Open Folder | FolderScreen shows apps in klasör | Text readable; scroll works; back button accessible |
| **13** | Rotate Folder to Landscape | Folder apps in landscape | Grid/list adapts; no clipping |
| **14** | AllAppsDrawer (swipe left or button) | Drawer opens with blur(20.dp) | Drawer renders; apps list scrollable; blur effect visible |
| **15** | SettingsScreen | Settings options, toggles, buttons | All sections visible (no height overflow); version/device info readable |
| **16** | Rotate SettingsScreen | Settings in landscape | All toggles/buttons accessible; no horizontal scroll needed |
| **17** | Back navigation | Return to HomeScreen | Navigation smooth; state preserved (e.g., klasör selection) |
| **18** | Check logcat | No crashes post-navigation | `adb logcat \| grep -i "error\|crash"` yields no AppOrganizer entries |

### Landscape-Specific Validations
- **Text Wrapping:** Verify no single-line text truncation; expected: proper word wrap or multi-line
- **Button Spacing:** Minimum 48dp tap target maintained
- **Pager Width:** HorizontalPager at HomeScreen doesn't overflow screen width
- **Safe Area:** No UI behind system bars (status bar + nav bar)

---

## 3. Test Execution Steps

### Boot Emulator
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$emu = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\emulator\emulator.exe"

# Kill existing emulator if running
& $adb devices | Select-String "emulator" | ForEach-Object { & $adb -s $_.Split()[0] emu kill }

# Launch Pixel6 in no-snapshot mode
& $emu -avd Pixel6_AOSP33 -no-snapshot-save &

# Wait for device to come online (poll ADB)
$timeout = 60
$elapsed = 0
while ($elapsed -lt $timeout) {
    $online = & $adb devices | Select-String "device$"
    if ($online) { Write-Host "Device online"; break }
    Start-Sleep -Seconds 2
    $elapsed += 2
}
```

### Install APK
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "C:\Users\hekizoglu\Documents\AppOrganizer\app\build\outputs\apk\debug\app-debug.apk"

& $adb install -r $apk
```

### Check for Crashes (First 10s)
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "Monitoring logcat for crashes (10s)..."
$proc = Start-Process -NoNewWindow -PassThru -FilePath $adb -ArgumentList "logcat AndroidRuntime:E"
Start-Sleep -Seconds 10
Stop-Process -InputObject $proc -Force -ErrorAction SilentlyContinue
```

### Launch App
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"

& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.MainActivity"
```

### Manual Rotation Testing (in Emulator)
- **Portrait:** Default (or Ctrl+Left arrow in extended controls)
- **Landscape:** Ctrl+Right arrow (or Settings > Display Rotation)
- **Observe:** Screenshot after each rotation, compare layout

### Screenshot & Log Collection
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$screenshotDir = "C:\Users\hekizoglu\Documents\AppOrganizer\R1_screenshots"

if (-not (Test-Path $screenshotDir)) { New-Item -ItemType Directory $screenshotDir }

# Pull screenshot
& $adb shell screencap -p /sdcard/screenshot.png
& $adb pull /sdcard/screenshot.png "$screenshotDir\screenshot_$(Get-Date -Format 'yyyyMMdd_HHmmss').png"

# Pull logcat
& $adb logcat -d > "$screenshotDir\logcat_$(Get-Date -Format 'yyyyMMdd_HHmmss').txt"
```

---

## 4. Test Result Reporting Template

```markdown
### R1 Tablet Smoke Test — [Date] [Time]

**Emulator:** Pixel6_AOSP33  
**APK Version:** [versionCode].[versionName]  
**Build Date:** [date]

#### Boot Status
- Boot time: [___]s
- Logcat clean (first 10s): ✅ / ❌
- Device online: ✅ / ❌

#### Installation
- APK install: ✅ Success / ❌ Failed
- Package verified: ✅ / ❌

#### Onboarding Flow
| Screen | Portrait OK | Landscape OK | Notes |
|--------|-------------|--------------|-------|
| WELCOME | ✅ / ❌ | ✅ / ❌ | [text readable?] |
| THEME_SELECT | ✅ / ❌ | ✅ / ❌ | [thumbnails render?] |
| QUICK_SETTINGS | ✅ / ❌ | ✅ / ❌ | [toggles responsive?] |
| SET_LAUNCHER | ✅ / ❌ | ✅ / ❌ | [button accessible?] |
| DONE | ✅ / ❌ | — | [transition smooth?] |

#### HomeScreen
- Cards layout (portrait): ✅ / ❌
- Pager responsive (landscape): ✅ / ❌
- Text readability (landscape): ✅ / ❌
- No overflow: ✅ / ❌

#### Navigation Screens
- FolderScreen (landscape): ✅ / ❌
- AllAppsDrawer: ✅ / ❌
- SettingsScreen (landscape): ✅ / ❌
- Back navigation: ✅ / ❌

#### Crashes
- AndroidRuntime:E count: [___]
- AppOrganizer-specific errors: [none / list]

#### Screenshots Attached
- [screenshot_20260722_120000.png] (WELCOME portrait)
- [screenshot_20260722_120030.png] (WELCOME landscape)
- [...]

#### Summary
**Status:** ✅ PASS / ⚠️ PARTIAL / ❌ FAIL  
**Issues:** [List any blockers or minor issues]  
**Readiness for Tablet Release:** [Assessment]
```

---

## 5. API 34/35 Tablet Roadmap (Future)

### Required Steps (R2+)
1. Download Android 34/35 system images via SDK Manager
   - API 34: `android-34` (tablet-optimized layout available)
   - API 35: `android-35` (latest; Edge-to-Edge mandatory)
2. Create tablet AVD via `avdmanager`:
   ```powershell
   $sdkDir = "C:\Users\hekizoglu\AppData\Local\Android\Sdk"
   & "$sdkDir\cmdline-tools\latest\bin\avdmanager.bat" create avd `
     -n "Pixel_Tablet_API34" `
     -k "system-images;android-34;google_apis;x86_64" `
     -d "Tablet"
   ```
3. Configure tablet metrics in AVD hardware config:
   - Screen resolution: 1600x2560 (10" tablet typical)
   - DPI: 240
   - RAM: 4GB
4. Repeat smoke test with landscape-first validation

---

## 6. Known Constraints & Notes

| Issue | Impact | Mitigation |
|-------|--------|-----------|
| Only API 33 available | Can't test Predictive Back (Android 13+), Edge-to-Edge (Android 15) | Use landscape rotation on phone AVD; schedule API 34+ testing for R2 |
| Phone-profile AVD | Landscape not "true" tablet layout | Accept as pilot; plan tablet AVD creation |
| No tablet skin | Can't test exact tablet navbar behavior | Focus on Compose layout responsiveness |

---

## 7. Checklist for Test Execution

- [ ] Ensure APK built: `.\gradlew assembleDebug`
- [ ] Android SDK path set correctly (`$ANDROID_HOME` or hardcoded)
- [ ] Emulator booted and online
- [ ] APK installed and package verified
- [ ] Logcat monitored during app launch
- [ ] Onboarding completed (all 5 screens)
- [ ] HomeScreen tested in portrait + landscape
- [ ] All navigation screens (Folder, AllApps, Settings) tested
- [ ] Rotation changes tested and layout adapted
- [ ] Screenshots captured for each key state
- [ ] Final logcat dump collected (no errors)
- [ ] Test results documented in [Test Result] section
- [ ] Issues logged to ROADMAP.md or COZULEMEYEN_SORUNLAR.md if applicable

---

**Next:** Once test complete, report findings to Hüseyin via Telegram with summary (crashes/OK, key observations, readiness assessment).
