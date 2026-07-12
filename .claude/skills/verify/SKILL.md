---
name: verify
description: "AppOrganizer degisiklik dogrulamasi: debug APK'yi derle, emulatore kur, degisen kodun ekranina surup gozlemle. Kod degisikligi sonrasi 'calisiyor mu' sorusunun cevabi testler degil, calisan launcher'dir."
---

# AppOrganizer Verify — calisan uygulamada gozlem

Degisiklik hangi ekrana dokunuyorsa oraya git, gor, ekran goruntusuyle kanitla. Test/typecheck kosmak dogrulama DEGILDIR.

## Build (tek komut)
```powershell
cd "c:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug -PskipGoogleServices --console=plain -q
```
Kilit/incremental hatasi cikarsa: `.claude/skills/apk-teslim/SKILL.md` icindeki "Bilinen Sorunlar" tablosu (java process kill → tam temizlik siralamasi).

## Handle: emulator
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$emu = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\emulator\emulator.exe"
# AVD: Pixel6_AOSP33; baslat: Start-Process $emu -ArgumentList "-avd","Pixel6_AOSP33","-no-snapshot-save","-no-boot-anim"
# boot bekle: getprop sys.boot_completed == "1"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
```
Alternatif: agir/cok-ekranli dogrulamayi `emulator-tester` agent'ina devret (.claude/agents/emulator-tester.md — ayni prosedurun agent hali).

## Yuzeye surme
- **Launcher home** (ticker, dock, klasorler, arama cubugu): `am start -n com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity` → 8 sn bekle.
- **Yonetim/rapor ekranlari**: ONCE `am force-stop com.armutlu.apporganizer`, sonra `am start -n .../.presentation.ui.MainActivity --es open_route <rota>` → 12 sn bekle (cold start yavas; erken screenshot splash yakalar ~40KB).
  Rotalar: settings, settings_{launcher,notifications,appearance,apps,stats,security,about}, search_settings, reports_center, wrapped_report, notification_report, privacy_report, dashboard.
- **Onboarding**: `pm clear com.armutlu.apporganizer` → LauncherActivity. Beklenen sira: WELCOME → THEME → QUICK_SETTINGS → SET_LAUNCHER → DONE.
- **Etkilesim**: `input tap X Y`, `input text "..."`, `input swipe` (basili tut = ayni noktaya swipe 800ms). Koordinat: screenshot al + Read ile bak (goruntu 1080x2400; Read 900x2000 gosterir, x1.2 carp) veya `uiautomator dump`.

## Gozlem/kanit
```powershell
& $adb shell screencap -p /sdcard/s.png; & $adb pull /sdcard/s.png <scratchpad>\s.png   # Read ile bak
& $adb logcat -d AndroidRuntime:E *:S    # bos = crash yok
```

## Tuzaklar (bu repoda kanitli)
- `am start` "delivered to top-most instance" uyarisi = ekran ONE GELMEDI; force-stop ile tekrarla.
- Gri "App Organizer" DeviceDefault basligi gorursen splash sirasi bozulmus demektir (D234 regresyonu) — FAIL.
- Emulatore drag&drop calismaz; o akisi "dogrulanamadi (emulator)" olarak raporla.
- APK guncellemesi accessibility servis baglantisini koparir.
- Ticker haberleri gunluk seed'lidir — ayni gun ayni metinler gorunur, bu bug degildir.
