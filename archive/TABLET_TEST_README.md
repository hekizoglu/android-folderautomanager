# Tablet Test — AppOrganizer v1.4.21

## 📱 Durum: **Test Hazırlanmışsa**

**APK:** ✅ Derlenmiş ve hazır (27.69 MB)  
**Test Scripti:** ✅ Hazır (PowerShell otomasyonu)  
**Dokümentasyon:** ✅ Kapsamlı (test rehberleri + troubleshooting)

---

## 🚀 Hızlı Başlangıç

### Seçenek 1: Otomatik Test (Tavsiye)
Tablet **USB kablolu bağlıyken:**

```powershell
cd "C:\Users\hekizoglu\Documents\AppOrganizer"
.\run_tablet_test.ps1
```

**Ne Yapıyor:**
- ADB cihazını bul
- Tablet model + Android sürümü öğren
- APK'yı kur
- Uygulamayı başlat
- 5 dakika boyunca crash monitoring (background)
- Sonuç rapor et

**Çıktı:** Console'da ✅/❌ durum + crash sayısı

---

### Seçenek 2: Manuel Test

1. **Tablet Hazırlama**
   ```powershell
   # Aşağıdaki dosyayı aç ve adım adım takip et:
   TABLET_TEST_CHECKLIST.txt
   ```

2. **Detaylı Test Rehberi**
   ```
   TABLET_TEST_GUIDE.md
   ```
   - 7 test senaryosu
   - Her biri için kontrol listesi
   - Beklenen sonuçlar

3. **Sorun Giderme**
   ```
   TABLET_TROUBLESHOOTING.md
   ```
   - 10 yaygın sorun
   - Adım adım çözümler
   - Teşhis komutları

---

## 📋 Test Dosyaları

### 1. **TABLET_TEST_CHECKLIST.txt** (7.1K)
   **Hızlı referans kartı** — yapış notları gibi tablet testine taşı
   - Adb komutları
   - 7 test senaryosu
   - Kontrol listesi
   - Sonuç raporu şablonu

### 2. **TABLET_TEST_GUIDE.md** (6.2K)
   **Detaylı rehber** — kurulum + 7 test senaryosu
   - Kurulum adımları
   - Her test için neler kontrol edilmeli
   - Beklenen sonuçlar
   - Tablet ekran boyutları tablosu

### 3. **TABLET_TROUBLESHOOTING.md** (9.2K)
   **Sorun giderme rehberi** — tablet test sırasında sorun çıkarsa
   - 10 yaygın sorun
   - Her sorun için 2-4 çözüm
   - Teşhis komutları
   - Logcat analizi

### 4. **run_tablet_test.ps1** (Otomasyonu)
   **PowerShell test otomasyonu**
   ```powershell
   # Tablet bağlıyken:
   .\run_tablet_test.ps1
   
   # veya özel parametre:
   .\run_tablet_test.ps1 -TestDurationSeconds 600  # 10 dakika
   ```

---

## ✅ Test Kontrol Listesi

| # | Test | Status | Detay |
|---|------|--------|-------|
| 1 | Onboarding | 📋 Rehberde | WELCOME→THEME→SETTINGS→LAUNCHER→DONE |
| 2 | HomeScreen (Portrait) | 📋 Rehberde | 8 klasör grid, dock, arama |
| 3 | AllAppsDrawer | 📋 Rehberde | 5-6 sütun grid, scroll |
| 4 | Rotasyon (Landscape) | 📋 Rehberde | Layout reflow, crash/ANR kontrol |
| 5 | Settings | 📋 Rehberde | Responsive, toggle'lar |
| 6 | Bildirim Şeridi | 📋 Rehberde | Görünüş, scroll |
| 7 | Performans | 📋 Rehberde | Jank, crash, ANR |

---

## 🔧 Tablet Bağlantı Sorunu?

**ADB görünmüyor:**
1. Tablet: Settings > Geliştirici Seçenekleri > **USB Hata Ayıklamayı Aç**
2. Tablet'te çıkan "USB Hata Ayıklamayı Onayla" diyaloğuna **ONAYLA** tıkla
3. USB kablonun iyi takılı olduğunu kontrol et
4. Bu komut çalıştır:
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb kill-server
   & $adb devices -l
   ```

**Hâlâ çalışmıyorsa:** `TABLET_TROUBLESHOOTING.md` → **ADB Bağlantı Sorunu** bölümüne bak

---

## 📊 Sonuç Raporu

Test tamamlandıktan sonra (manual ya da otomatik):

```
✅ TABLET TEST BAŞARILI
Cihaz: Samsung Galaxy Tab S10 (Android 14)
Kurulum: ✅ OK
HomeScreen: ✅ OK
AllAppsDrawer: ✅ OK
Rotasyon: ✅ OK
Settings: ✅ OK
Crash/ANR: 0
Genel: ✅ BAŞARILI — Production Hazır
```

**Raporu gönder:**
1. Telegram'a (Hüseyin) metin olarak kopia-yapıştır
2. Logcat dosyası varsa ekle: `tablet_crash.log`
3. Screenshots varsa ekle

---

## 🎯 APK Bilgisi

```
Dosya: app/build/outputs/apk/debug/app-debug.apk
Boyut: 27.69 MB
Paket: com.armutlu.apporganizer
Versiyon: 1.4.21
Build: Debug (test amaçlı)
```

**APK yeniden derleme gerekiyorsa:**
```powershell
cd "C:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug -PskipGoogleServices
# ~2-3 dakika sürer
```

---

## 📱 Tablet Uyumluluğu Nedir?

AppOrganizer **tablet optimizasyonu:**
- HomeScreen: 4×2 grid (8 klasör)
- AllAppsDrawer: 5-6 sütun (tablet genişliğine göre)
- Dock: 4-5 slot (wide screen: 6)
- Responsive layout: Landscape döndüğünde reflow
- Full-screen: EdgeToEdge (status bar ile harmony)

**Test edelim:**
1. Portrait: Normal 4×2 grid
2. Landscape: Layout genişliyor (5×2 veya 4×3 grid olabilir)
3. Geri dönüş: Crash yapmıyor
4. Scroll: Smooth, jank yok

---

## 💬 İletişim

Tablet test sırasında **bloke olursan:**

1. **Sorun + Ekran Görüntüsü → Telegram'a gönder**
2. **Logcat dump'ı gönder:**
   ```powershell
   $adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat -d > tablet_logcat_dump.txt
   # tablet_logcat_dump.txt'i gönder
   ```

---

## 📅 Son Güncelleme

- **Tarih:** 2026-07-23
- **Versiyon:** 1.4.21
- **Durum:** ✅ Test Hazırlanmış (tablet bağlanmayı bekliyor)
- **Dosya Sayısı:** 4 (checklist, guide, troubleshoot, script)
- **Total:** ~22 KB dokümentasyon + 1 PowerShell script

---

**Hazırladı:** Claude Code  
**Proje:** AppOrganizer  
**Test Türü:** Tablet Uyumluluğu (Wide-screen, Landscape, Onboarding, Performance)
