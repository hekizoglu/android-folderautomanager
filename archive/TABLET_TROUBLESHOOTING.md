# Tablet Test Sorunları Giderme Rehberi

## Sorunu Bulabilirsiniz

### 1. **ADB Bağlantı Sorunu**

**Sorun:** `adb devices`'te tablet görünmüyor
- Sadece "List of devices attached" ve boş satırlar

**Çözüm Adımları:**

1. **USB Hata Ayıklamayı Aç**
   - Tablet > Ayarlar > Geliştirici Seçenekleri > USB Hata Ayıklama: **AÇIK**
   - Tablet'te "USB Hata Ayıklamayı Onayla?" diyalogu çıkarsa: **ONAYLA**

2. **USB Kablo Kontrol Et**
   - Kablonun iyi takılı olduğunu kontrol et
   - Kablo kütüphaneler arasında test et
   - Bilgisayarı yeniden başlatmayı dene

3. **ADB Daemon Yenile**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb kill-server
   & $adb devices
   ```

4. **USB Modunu Kontrol Et**
   - Tablet > Ayarlar > Geliştirici Seçenekleri > USB Konfigürasyonu: **Dosya Aktarımı (MTP)** veya **PTP**

---

### 2. **APK Kurulum Sorunu**

**Sorun:** `adb install` başarısız
- `cmd: Can't find service: package`
- `INSTALL_FAILED_INVALID_APK`
- `INSTALL_FAILED_INSUFFICIENT_STORAGE`

**Çözüm:**

1. **APK Bütünlüğünü Kontrol Et**
   ```powershell
   $apk = "C:\Users\hekizoglu\Documents\AppOrganizer\app\build\outputs\apk\debug\app-debug.apk"
   (Get-Item $apk).Length / 1MB  # ~27-30 MB olmalı
   ```

2. **Tablet Depolama Alanını Kontrol Et**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell df -h /data  # 1 GB+ boş alan gerekir
   ```

3. **Eski Versiyonu Kaldır**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb uninstall com.armutlu.apporganizer
   Start-Sleep -Seconds 2
   & $adb install -r $apk
   ```

4. **Başka bir SDK API testin**
   - Tablet'i yeniden başlat
   - Bilgisayarı yeniden başlat
   - APK'yı yeniden derle: `.\gradlew assembleDebug`

---

### 3. **Uygulama Açılmıyor / Crash**

**Sorun:** Başlat komutundan sonra uygulama çöküyor veya açılmıyor

**Çözüm:**

1. **Logcat Crash Log Kontrol Et**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat -b all | Select-String "FATAL|Exception|Error"
   ```

2. **Cache Temizle**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell pm clear com.armutlu.apporganizer
   & $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.MainActivity"
   ```

3. **Permission Sorunları**
   - Launcher Permission: Settings > Uygulamalar > AppOrganizer > Kullanılan İzinleri Gözle
   - Notification Listener: Settings > Uygulamalar > Özel Uygulama Erişimi > Bildiri Dinleyicisi > AppOrganizer: AÇIK

4. **Firebase Sorunu (googleservices.json yok)**
   - Uygulama başlatılıyorsa `-PskipGoogleServices` ile derlenmiş demektir (beklenmiş)
   - Crashlytics/Analytics olmadan çalışacaktır

---

### 4. **Onboarding Tamamlanamıyor**

**Sorun:** WELCOME → THEME_SELECT → ... adımlarında tıklama çalışmıyor

**Çözüm:**

1. **Dokunmatik Ekran Test Et**
   - Tablet'te ayarların başka yerini dokunarak test et
   - Dokunmatik ekran düzgün çalışıyorsa: uygulama sorunu

2. **Cache Temizle ve Yeniden Başlat**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell pm clear com.armutlu.apporganizer
   & $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.MainActivity"
   ```

3. **Onboarding State Sıfırla**
   - Settings > Uygulamalar > AppOrganizer > Depolamayı Temizle
   - Veya: `adb shell` içinde:
     ```
     adb shell
     sqlite3 /data/data/com.armutlu.apporganizer/databases/app_database.db
     UPDATE app_preferences SET onboarding_done = 0;
     ```

---

### 5. **Layout Overflow (Ekrana Sığmıyor)**

**Sorun:** Tablet'te elementler ekranın dışına taşıyor

**Kontrol Edilecekler:**

1. **Ekran Yoğunluğu (DPI)**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell getprop ro.sf.lcd_density
   ```
   - Normal: 160-240 DPI
   - Yüksek DPI (320+): Layout padding azalabilir

2. **Ekran Boyutu**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell wm size
   ```

3. **Loglarda Measur Hatası**
   ```powershell
   & $adb logcat | Select-String "measure|layout|size"
   ```

**Düzeltme:**
- Compose kod incelenecek: `HomeScreen.kt` → padding/maxWidth değerleri
- Tablet: `maxWidth` 600.dp+ olmalı
- Test: `android:theme="@style/Theme_AppOrganizer"` kaynak yoğunluğu kontrol et

---

### 6. **Rotasyon Crash/ANR**

**Sorun:** Landscape döndüğünde crash veya metin kırpılması

**Çözüm:**

1. **Configuration Change Kontrol Et**
   - AndroidManifest.xml'de `android:configChanges` kontrolü
   - Gerekliyse: `configChanges="orientation|screenSize|smallestScreenSize"`

2. **Recomposition Sorunları**
   ```powershell
   & $adb logcat | Select-String "Performing unstable|Skipped.*frames"
   ```

3. **State Kaynak Kodları**
   - `rememberSaveable`, `mutableStateOf` state'leri kontrol et
   - Rotasyon sırasında state kaybı olmadığını doğrula

---

### 7. **Performans Sorunu (Jank/Freeze)**

**Sorun:** Scroll pürüzlü, UI donuyor

**Kontrol Listesi:**

1. **Logcat Jank Tespiti**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat | Select-String "skipped.*frames|Skipped"
   ```

2. **Battery Historian (isteğe bağlı)**
   - Arka planda ağır işlem yapılıyor mu?
   - Timer/coroutine leak var mı?

3. **Hızlı Kontrol**
   - HomeScreen grid scroll
   - AllAppsDrawer scroll
   - Arama yazma sırasında UI freeze mi?

**Düzeltme:**
- Icon loading asynchronous mı? (`produceState`, `LaunchedEffect`)
- DB query main thread'de mi? (Room suspend function kullan)
- State update batching?

---

### 8. **Bildirimi Alma / Badge Sorunu**

**Sorun:** Bildirim badge görünmüyor

**Çözüm:**

1. **NotificationListenerService İzni**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell settings get secure enabled_notification_listeners
   ```
   - `com.armutlu.apporganizer/.AppNotificationListenerService` içinde olmalı

2. **İznin Verilmesi**
   - Tablet > Settings > Uygulamalar > Özel Uygulama Erişimi > Bildiri Dinleyicisi > AppOrganizer: **AÇIK**

3. **Badge Tercihi**
   - Settings > Görünüm > Bildirim Badgesi: **AÇIK**
   - Ayarı Kapatıp Açarak Test Et

4. **Tesisi Sıfırla**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell pm clear com.armutlu.apporganizer
   ```

---

### 9. **Launcher Varsayılan Atanmıyor**

**Sorun:** SET_LAUNCHER adımında "Varsayılan Launcher Ayarla" çalışmıyor

**Çözüm:**

1. **RoleManager API (Android 12+)**
   - Tablet: Settings > Uygulamalar > Varsayılan Uygulamalar > Launcher > AppOrganizer: SEÇ
   - Veya: Settings > Uygulamalar > AppOrganizer > Varsayılan Uygulamalar > Launcher Rolü: **KUR**

2. **El ile Ayarla**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell cmd role set_role_holder android.app.role.HOME com.armutlu.apporganizer
   ```

3. **Eski Android Sürümleri (< 12)**
   - Tablet > Settings > Uygulamalar > Varsayılan Uygulamalar > Launcher > AppOrganizer

---

### 10. **Google Play Services Sorunu**

**Sorun:** Firebase/Google Services hatası

**Not:** Bu uygulama `-PskipGoogleServices` ile derlenirse GMS olmadan çalışır (tasarlandı)

**Çözüm:**

1. **GMS Kontrolü**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb shell pm dump com.google.android.gms | Select-String "version"
   ```

2. **GMS Olmadan Çalıştırma**
   - Uygulamada GMS başarısız varsa: `CategoryLLMFallback` (DeepSeek) fallback yapacak
   - Firebase Analytics/Crashlytics null-guarded

---

## Hızlı Teşhis Komutu

```powershell
# Tüm bilgileri topla
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== TABLET BİLGİLERİ ===" -ForegroundColor Cyan
& $adb devices -l
Write-Host ""

Write-Host "=== ORTAM ===" -ForegroundColor Cyan
& $adb shell getprop ro.product.model
& $adb shell getprop ro.build.version.release
& $adb shell wm size
Write-Host ""

Write-Host "=== DEPOLAMA ===" -ForegroundColor Cyan
& $adb shell df -h /data
Write-Host ""

Write-Host "=== APP YÜKLÜ MÜ? ===" -ForegroundColor Cyan
& $adb shell pm list packages | Select-String "apporganizer"
Write-Host ""

Write-Host "=== SON CRASH ===" -ForegroundColor Cyan
& $adb logcat -d | Select-String "FATAL" | Select-Object -Last 5
```

---

## İletişim

Tablet test sırasında soruntu varsa:

1. **Logcat Log'u Gönder**
   ```powershell
   & $adb logcat > tablet_full.log 2>&1
   # (10 saniye bekle, Ctrl+C)
   ```
   - `tablet_full.log`'u Telegram'a gönder

2. **Screenshots**
   ```powershell
   & $adb shell screencap -p /sdcard/screenshot.png
   & $adb pull /sdcard/screenshot.png
   ```

3. **Sorunun Adı + Adımlar + Beklenen ≠ Gerçek** formatıyla rapor et

---

**Son Güncelleme:** 2026-07-23  
**Versiyon:** v1.4.21
