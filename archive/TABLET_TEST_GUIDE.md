# Tablet Test Guide — AppOrganizer v1.4.21

**Amaç:** Gerçek tablet'te AppOrganizer'ı test ederek wide-screen (landscape) uyumluluğunu ve performansını doğrula.

**Ön Koşullar:**
- Tablet USB kablolu bağlı ve ADB debug mode aktif
- APK: `c:\Users\hekizoglu\Documents\AppOrganizer\app\build\outputs\apk\debug\app-debug.apk`
- ADB: `C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe`

---

## Kurulum Adımları

### 1. ADB Bağlantısını Doğrula
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
```

**Beklenen çıktı:** Tablet'in device ID'si listelenmeli.

### 2. Tablet Model ve Android Sürümünü Öğren
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell getprop ro.product.model
& $adb shell getprop ro.build.version.release
```

### 3. APK'yı Kur
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$apk = "c:\Users\hekizoglu\Documents\AppOrganizer\app\build\outputs\apk\debug\app-debug.apk"
& $adb install -r $apk
```

Beklenen: **Success** mesajı.

### 4. Uygulamayı Başlat
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.MainActivity"
```

---

## Test Senaryoları

### Test 1: Onboarding (Kurulum Tamamlama)
**Adımlar:**
1. Uygulamayı aç — WELCOME ekranı görmeli
2. **THEME_SELECT:** Tema seç (Pixel Görünümü veya Türkuaz önerilen)
3. **QUICK_SETTINGS:** Kategorileri seç, başla'ya tıkla
4. **SET_LAUNCHER:** "AppOrganizer'ı default launcher yap" dialogu — AYARLA'ya tıkla
5. **DONE:** Ev ekranına dön

**Kontrol:**
- ✅ Adımlar sırasıyla ilerliyor mu?
- ✅ Crash/ANR yok mu?
- ✅ DONE sonrası HomeScreen açılıyor mu?

---

### Test 2: HomeScreen — Tablet Geniş Ekran
**Ekranı Portrait tutarak kontrol:**
1. Ana ekrana dön
2. Klasörleri gözle:
   - 8 klasör grid görünüyor mu? (4×2 layout tablet için)
   - Dock altta sabit görünüyor mu?
   - Arama çubuğu üstte var mı?
   - Notification badge (kırmızı sayı) var mı?

**Kontrol:**
- ✅ UI elemanleri ekrana sığıyor mu? (overflow yok mu?)
- ✅ Scroll smooth mi?
- ✅ Klasörlere dokunca açılıyor mu?
- ✅ Dock editlenebiliyor mu? (uzun basış → drag)

---

### Test 3: AllAppsDrawer — Tablet Genişliği
**Adımlar:**
1. Ekranın altından yukarı swipe et (Drawer açılır)
2. Uygulamaları gözle

**Kontrol:**
- ✅ 5-6 sütun grid görünüyor mu? (tablet için optimize)
- ✅ Scroll smooth mi?
- ✅ Uygulamalar açılıyor mu?
- ✅ Arama çalışıyor mu?

---

### Test 4: Rotasyon Test (ÖNEMLİ) — Landscape
**Adımlar:**
1. Tablet'i landscape (yatay) döndür
2. HomeScreen nasıl görünüyor?
   - Grid reflow mu ediyor?
   - Dock hala sabit mi?
3. AllAppsDrawer aç
4. Tekrar portrait döndür

**Kontrol (Landscape):**
- ✅ Layout crash/ANR yapmıyor mu?
- ✅ Elementler overflow yapmıyor mu?
- ✅ Dock görünür mü?
- ✅ Grid mantıklı şekilde reflow ediyor mu?

**Kontrol (Portrait Dönüş):**
- ✅ Başlangıç düzenine geri dönerek çöküyor mu?
- ✅ Crash/ANR yok mu?

---

### Test 5: Settings Ekranı (Tablet Genişliği)
**Adımlar:**
1. HomeScreen → 3 satır ⋮ menü → Settings
2. Ekranı gözle:
   - Başlık, gruplar (Tema, Görünüm, Haber Şeridi, Yönetim) görünüyor mu?
   - Toggles düzgün görünüyor mu?

**Kontrol:**
- ✅ Responsive layout var mı? (tablet genişliğinde padding/margin var mı?)
- ✅ Scroll gerekli mi? (okay varsa)
- ✅ Toggle'lar çalışıyor mu?

---

### Test 6: Performans ve Crash/ANR
**Adımlar:**
1. Logcat'i başlat (background):
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat AndroidRuntime:E > tablet_crash.log 2>&1 &
   ```
2. 5-10 dakika tablet'i kullan:
   - HomeScreen → klasöre gir → geri dön
   - AllAppsDrawer aç/kapat
   - Landscape/Portrait dönüş
   - Arama yap
3. Logcat'i durdur (`Ctrl+C`)

**Kontrol:**
- ✅ `AndroidRuntime: FATAL EXCEPTION` yok mu?
- ✅ ANR dialogu çıktı mı?
- ✅ App freeze oldu mu?

---

### Test 7: Bildirim Şeridi (HomeTickerRow)
**Adımlar:**
1. HomeScreen'de kaydır (yukarıdan aşağıya)
2. Haber şeridini gözle (klasör istatistikleri + bildirim özeti)

**Kontrol:**
- ✅ Görünüyor mu?
- ✅ Kaydırılabilir mi?
- ✅ Item'lere dokunca hedef açılıyor mu?

---

## Sonuç Şablonu

Test tamamlandıktan sonra aşağıdaki raporu doldu:

```
## Tablet Test Sonucu

**Cihaz:**
- Model: [tablet model, örn: Samsung Galaxy Tab S10]
- Android: [version, örn: 14.0]
- Ekran: [resolution, örn: 2880 × 1800]

**Kurulum:**
- ✅/❌ APK başarıyla kuruldu
- ✅/❌ Onboarding tamamlandı (WELCOME→THEME→SETTINGS→LAUNCHER→DONE)

**HomeScreen (Portrait):**
- ✅/❌ 8 klasör grid görünüyor
- ✅/❌ Dock sabit
- ✅/❌ Arama çubuğu var
- ✅/❌ Overflow yok

**AllAppsDrawer:**
- ✅/❌ 5-6 sütun grid
- ✅/❌ Smooth scroll
- ✅/❌ Uygulamalar açılıyor

**Rotasyon (Landscape):**
- ✅/❌ Layout reflow
- ✅/❌ Crash/ANR yok
- ✅/❌ Portrait dönüş sorunsuz

**Settings:**
- ✅/❌ Responsive layout
- ✅/❌ Toggle'lar çalışıyor

**Crash/ANR:**
- Hata sayısı: [n]
- Logcat (ilk 5): [hatalar]

**Genel Uyumluluk:** ✅ BAŞARILI / ⚠️ MINOR ISSUES / ❌ BLOCKER

**Notlar:** [ek gözlemler]
```

---

## Teknik Notlar

### Tablet Ekran Boyutları (Ortak)
| Model | Ekran | Çözünürlük | DPI |
|-------|-------|-----------|-----|
| iPad 10.9" | 10.9" | 2360×1640 | 264 |
| Samsung Tab S10 | 11" | 2880×1800 | 240 |
| Xiaomi Pad 6 | 11.5" | 2880×1920 | 225 |
| Lenovo Tab M11 | 11" | 2000×1200 | 200 |

### AppOrganizer Tablet-Specific Ayarları
- **HomeScreen Grid:** 4 klasör/satır (8 toplam)
- **AllAppsDrawer Grid:** 5-6 sütun (tablet genişliğine göre)
- **Dock:** Altta 4-5 slot (wide screen: 6 slot olabilir)

### Logcat Crash Kontrolü
```powershell
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb logcat | Select-String "AndroidRuntime:E|ANR|FATAL"
```

---

**Son Güncelleme:** 2026-07-23  
**Hazırlayan:** Claude Code
