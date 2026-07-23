# Play Store Submission Checklist — AppOrganizer v1.0.0

**Hedef:** Production release gönderimine hazırlanma.  
**Son güncelleme:** 2026-07-23  
**Durum:** Pre-submission (code feature-complete, store assets pending)

---

## 1. Permissions & Capabilities

### QUERY_ALL_PACKAGES (Runtime)
- **Neden:** AppOrganizer launcher olarak tüm uygulamaları bulup kategorilere sınıflandırması gerekir
- **Google Form Durumu:** [ ] Doldurulması gerekir — "Uygulama keşfi" kategorisini seç
- **Belge:** https://support.google.com/googleplay/android-developer/answer/10158779 (QUERY_ALL_PACKAGES beyanı)
- **Form Link:** Google Play Console → App content → Target API level → QUERY_ALL_PACKAGES usage (Google'a raporlama formunu aç)
- **Gerekli Metinler:**
  - Title: "Application Discovery"
  - Description: "AppOrganizer launcher as default needs to discover all apps and categorize them intelligently."

### Other Critical Permissions
- `android.permission.QUERY_ALL_PACKAGES` ✅ (AndroidManifest.xml satır 17)
- `android.permission.POST_NOTIFICATIONS` ✅ (notification badge)
- `android.permission.NOTIFICATION_LISTENER_SERVICE` ✅ (bildirim analizi)
- `android.permission.BIND_ACCESSIBILITY_SERVICE` ✅ (App Shortcuts + Accessibility)

---

## 2. Data Safety (Play Console → App content → Data safety)

### Analytics & Firebase Crashlytics
- [x] Firebase Analytics **açık** — D205'te real project bağlı
- [x] Crashlytics **açık** — hata raporlaması
- [ ] **Form satırları:**
  - Shared Preferences, Firebase DB → **non-personal** (opsiyonel, encrypted)
  - Personal veri **gönderilmiyor** — uygulamayı kategorilere sıralama/skor verisi içeriyor, uygulama adı/paket X

### NotificationListenerService
- [x] Bildirim erişimi — **kaydedilen veri:** paket adı + timestamp (içerik YOK)
- [x] Veritabanında 7 gün retention (otomatik silme)
- [ ] **Form:** "Notification data (package name, timestamp only — no content)"

### File Access (SAF + Backup)
- [x] SAF kullanarak yedek dosyaları okur/yazar (Drive, local)
- [x] `getExternalFilesDir()` yok (internal only)
- [ ] **Form:** "Backup files via SAF — no personal data"

### Device & App History
- [x] `UsageStatsManager` → ön plan süresi (adet/ms, kimlik YOK)
- [x] `PackageManager.getInstalledPackages()` → app metadata (launcher işlevi)
- [ ] **Form:** "App usage stats (for smart organization) — no personal identifiers"

### Sensitive Permissions (AI/LLM)
- [x] DeepSeek LLM API — **seçmeli**, local fallback (kategori tahminleri)
- [x] Sorgu → "burada hangi türden uygulama?" şablonu (app name X, paket X)
- [ ] **Form:** "Optional LLM for app categorization (no app names/packages sent)"

---

## 3. Content Rating (IARC / PlayConsoleDe)
- [ ] **Doldurulması gerekir:**
  - Adultness / Veri Güvenliği ✅ (clean)
  - Financial / Stokta ✅ (hayır)
  - COPPA (13 yaş altı) ✅ (hayır, K-12 eğitim de yok)
  - Violence, horror ✅ (hayır)
  - Language ✅ (TR, EN şarkı/kelime temiz)
  - Result: **Suitable for 3+**

---

## 4. Privacy Policy & Legal
- [ ] **Privacy Policy URL:** GitHub Pages `/docs/privacy_policy.html` (henüz deploy edilmedi)
  - [ ] `privacy_policy.md` yazılacak: veri toplama (Analytics + Crashlytics), NotificationListener, usage stats, SAF backup, LLM optionality, retention
  - [ ] GitHub Pages branch (`gh-pages`) oluştur, publish et
  - [ ] URL: `https://hekizoglu.github.io/AppOrganizer/privacy_policy.html` (example)
- [ ] **Terms of Service:** Basit (launcher, bilinen tüm veri politikaları Privacy'ye referans)
- [ ] **Store Listing Metni:**
  - Title ✅: "App Organizer — Smart Launcher"
  - Short Desc: "Organize apps into smart categories automatically."
  - Full Desc: "Auto-categorize 3700+ apps, one-tap access, dark mode, widgets, search, notification control."
  - Keywords: launcher, organizer, auto-categorize, productivity

---

## 5. App Signing & Release Build
- [ ] **Release Keystore oluştur:**
  ```bash
  keytool -genkey -v -keystore release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias apporg_release
  ```
- [ ] `keystore.properties` (git-ignored):
  ```
  storeFile=/path/to/release.jks
  storePassword=***
  keyAlias=apporg_release
  keyPassword=***
  ```
- [ ] `app/build.gradle.kts` signing config (release build type'a bağla)
- [ ] `./gradlew bundleRelease` — imzalı AAB üret
- [ ] AAB boyutu < 150 MB (bölünme yok)
- [ ] **APK backup:** Telegram bot history'den alınan son APK (kayıt için)

---

## 6. Store Listing Assets
- [ ] **Feature Graphic (1024×500):** Turkuaz tema, "Smart App Organizer" başlığı
- [ ] **App Icon (512×512):** AppOrganizer icon, transparent bg, App Store format
- [ ] **Screenshots (5–8, landscape):**
  1. HomeScreen klasörler (main feature)
  2. AllAppsDrawer arama
  3. Settings theme/organize
  4. Permissions guide
  5. Dashboard/stats (opsiyonel)
- [ ] **Preview Video (30s, optional):** "Tap folder → auto-categorized apps" demo

---

## 7. Version & Build Numbers
- [ ] `versionCode = 1` (Play Console v1.0.0)
- [ ] `versionName = "1.0.0"`
- [ ] `compileSdk = 35` (current)
- [ ] `minSdk = 31` (Android 12+)
- [ ] `targetSdk = 35`

---

## 8. Pre-Launch QA
- [ ] **Clean Device Test (Pixel6 emulator):**
  - Fresh install → Onboarding → HomeScreen classdirs, all tabs, search, settings ✅
  - Permissions: one-by-one accept, all dialogs render ✅
  - Notifications: badge count visible, report screen ✅
  - Crash logs: Crashlytics kanıt
- [ ] **AAB → APK split test:** bundleRelease AAB'yi Google Play Console'de test et
- [ ] **Lint / Detekt / Build:** 0 error
- [ ] **APK Size:** ~28–32 MB (acceptable)

---

## 9. Console Setup
- [ ] App name: "App Organizer"
- [ ] Bundle ID: `com.armutlu.apporganizer` ✅
- [ ] Release track: **Internal Testing** (first), then **Open Testing** (beta)
- [ ] Rollout: 100% (production)
- [ ] Country: Turkey (primary), select English-speaking countries

---

## Submission Checklist
- [ ] QUERY_ALL_PACKAGES form submitted to Google
- [ ] Data Safety form filled (Analytics, Notifications, no personal data)
- [ ] Content rating anketi tamamlandı
- [ ] Privacy Policy URL live + Play Console'de test
- [ ] Signed release AAB ready
- [ ] Store assets uploaded (icon, feature graphic, screenshots)
- [ ] Pre-launch QA passed
- [ ] Release notes (1.0.0) yazıldı
- [ ] Rollout 100% hazır

---

**Not:** Play Store incelemesi 1–3 gün sürer. Rejection duyarsanız REJECTION_REASON.md'ye kaydedip düzeltme yapılır.
