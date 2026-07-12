# AppOrganizer — Play Store Öncesi Güvenlik Denetimi

**Tarih:** 2026-07-13 · **Kapsam:** Salt okuma statik denetim (kod değişikliği yok) · **Denetleyen:** Claude (agent)

---

## 1. AndroidManifest.xml — Bileşen Maruziyeti

**Dosya:** `app/src/main/AndroidManifest.xml`

| Bileşen | exported | Koruma | Değerlendirme |
|---|---|---|---|
| `MainActivity` (satır 53-63) | `true` | Yok (sadece MAIN/LAUNCHER intent-filter) | Gerekli — launcher ikonu için zorunlu. Ancak `EXTRA_OPEN_ROUTE`/`open_category` extra'ları imzasız dışarıdan tetiklenebilir (bkz. §6). |
| `LauncherActivity` (satır 70-87) | `true` | HOME/DEFAULT intent-filter | Gerekli — sistem launcher rolü için zorunlu davranış. |
| `LauncherAccessibilityService` (satır 90-104) | `true` | `BIND_ACCESSIBILITY_SERVICE` permission | Sistem zorunluluğu (AccessibilityService her zaman exported+permission ile bildirilir) — normal. |
| `FileProvider` (satır 107-115) | `false` | grantUriPermissions ile scoped | Doğru yapılandırılmış. |
| `AppNotificationListenerService` (satır 118-126) | `false` | `BIND_NOTIFICATION_LISTENER_SERVICE` | Doğru — exported=false + sistem izni. |
| `BackupSyncService` (satır 129-132) | `false` | — | Doğru; zaten no-op stub (bkz. §4). |
| `AppFirebaseMessagingService` (satır 135-141) | `false` | — | Doğru. |
| `PackageChangeReceiver` (satır 144-154) | `true` | `<data android:scheme="package"/>` + PACKAGE_* actions | **BİLGİ** — PACKAGE_ADDED/REMOVED/CHANGED sistem-korumalı broadcast'lerdir (`android:protectionLevel="signature|privileged"` sistemde), üçüncü parti app bu action'ları taklit edip gönderemez. exported=true olması standart ve zorunludur (SDK 26 sonrası manifest-registered receiver bu action'lar için exported olmalı). Risk yok. |

**allowBackup:** `true` (satır 41) — `dataExtractionRules` ile kısmen scope edilmiş (bkz. §2 — DB dahil, hassas alan riski var).
**usesCleartextTraffic:** Manifest'te bildirilmemiş; `network_security_config.xml` üzerinden `cleartextTrafficPermitted="false"` (hem base hem domain-config) — **düzgün kısıtlanmış.**
**debuggable:** Manifest'te elle işaretlenmemiş; `debug` build type'ta `isDebuggable = true` (build.gradle.kts satır 78) — normal Gradle davranışı, **release'te bu flag Gradle tarafından otomatik false yapılır**, manifest'te sabit `android:debuggable` yok. Risk yok, ama release APK'nın keystore olmadan debug imzayla alınabildiği bir uyarı var (bkz. §9).

**ŞİDDET: BİLGİ** — Manifest genel olarak temiz. Play reject riski: **yok**.

---

## 2. NotificationListenerService — İçerik Saklama İddiası ÇELİŞKİSİ

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/service/AppNotificationListenerService.kt`

Kod yorumu (satır 22-26): *"her bildirim `notification_events` tablosuna loglanır: yalnızca paket adı + zaman damgası — içerik saklanmaz, veri cihazda kalır."*

Bu iddia **`notification_events` tablosu için doğru** (satır 58-60: sadece `packageName` + `postedAt` insert ediliyor). Ancak kod, bildirim başlığı+metnini (`title: text` birleşik hali, satır 41-52) ayrı bir `_latestTexts` StateFlow'da tutuyor ve bu veri **kalıcı Room DB'ye yazılıyor**:

- `AppNotificationListenerService.kt:52` → `_latestTexts.update { it + (pkg to combined) }` (title+text birleşik ham bildirim metni)
- `LauncherViewModel.kt:214-230` → bu flow'u dinleyip her değişiklikte `repository.updateNotificationText(pkg, text)` çağırıyor
- `AppDao.kt:205-206` → `UPDATE apps SET notificationText = :text WHERE packageName = :packageName` (Room tablosuna kalıcı yazım)
- `AppDatabase.kt:58-61` → migration v4→v5 ile `notificationText TEXT NOT NULL DEFAULT ''` sütunu şemaya eklenmiş — bu **tasarım kararı**, kaza değil.

**Gösterim noktaları (gerçek bildirim metni ekranda görünüyor):**
- `NiagaraComponents.kt:150-152` — `app.notificationText` (opt-in `notifTextEnabled` flag'i ile)
- `FolderScreen.kt:294,306-307` — `"${latestNotifApp.appName}: ${latestNotifApp.notificationText}"` ticker/özet metni olarak

**Sonuç:** Kod yorumundaki "içerik saklanmaz" cümlesi yalnızca `notification_events` analiz tablosu için doğru; `apps.notificationText` sütunu **gerçek bildirim içeriğini kalıcı SQLite DB'de saklıyor** ve UI'da (badge/ticker özetinde) gösteriyor. Bu, banka/mesajlaşma bildirimi OTP kodu, kişi adı gibi hassas içeriğin cihazda disk'e yazılması anlamına gelir.

**Data Safety formu riski:** Google Play Data Safety formunda "Bildirim erişimi" için "veri toplanmıyor/paylaşılmıyor, sadece cihazda" beyanı verilecekse bu DOĞRU olabilir (üçüncü tarafa gitmiyor — DeepSeek/Firebase'e gönderilmiyor, sadece local DB), ANCAK NotificationListenerService kullanım amacı beyanında ("Bildirim erişimi neden gerekli") sadece "badge sayısı" değil **"bildirim metnini gösterme"** amacının da açıkça belirtilmesi gerekir — mevcut kod yorumu bu kullanımı gizliyor/yanlış yansıtıyor.

**allowBackup ile kesişim:** `data_extraction_rules.xml` `<include domain="database" path="." />` ile TÜM Room DB'yi (dolayısıyla `apps.notificationText` sütununu) Android bulut yedeğine (cloud-backup) ve cihazlar arası aktarıma (device-transfer) dahil ediyor — sadece `deepseek_prefs.xml` sharedpref'i hariç tutulmuş, DB hiç exclude edilmemiş.

**ŞİDDET: YÜKSEK** — Play reject riski: **var (Data Safety formu tutarsızlığı riski)** + gerçek gizlilik açığı (bildirim içeriği kalıcı disk + cloud backup'a dahil).
**Önerilen fix:** (1) Kod yorumunu düzelt — "içerik saklanmaz" iddiasını kaldır veya `notificationText` özelliğini ayrı belgele. (2) `data_extraction_rules.xml`'e `apps.notificationText` sütununu içeren tabloyu hariç tutacak bir kural ekle (Room tekil sütun exclude XML ile yapılamıyorsa, ayrı bir DB dosyası/tablo stratejisi düşünülmeli veya `allowBackup` bu tablo için ayrı ele alınmalı). (3) Data Safety formunda bildirim metni gösterimini açıkça beyan et.

---

## 3. Accessibility Service — Beyan vs Fiili Kullanım UYUMSUZLUĞU

**Dosyalar:** `LauncherAccessibilityService.kt`, `app/src/main/res/xml/accessibility_service_config.xml`

Fiili kod (`LauncherAccessibilityService.kt`, 14 satır):
```kotlin
override fun onServiceConnected() {}
override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
override fun onInterrupt() {}
```
**Tüm metodlar boş — servis hiçbir şey yapmıyor.** Manifest yorumu (satır 89) "drag & drop ile gruplandırma" diyor ama kodda drag&drop mantığı yok; sınıf bir iskelet (stub).

Config beyanı (`accessibility_service_config.xml`) buna rağmen **çok geniş**:
- `android:canRetrieveWindowContent="true"` — ekran içeriğini okuma izni (kullanılmıyor)
- `android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged|typeViewClicked|typeViewFocused"` — geniş event seti (kullanılmıyor)
- `android:canPerformGestures="true"` — jest simülasyonu izni (kullanılmıyor)
- `android:isAccessibilityTool="true"`

**ŞİDDET: YÜKSEK** — Play reject riski: **var**. Google Play, Accessibility API'yi ilan edilen amaç dışında/kullanılmayan geniş izinlerle bildiren uygulamaları proaktif tarıyor ve reddediyor ("Accessibility API kullanım politikası" ihlali) — özellikle `canRetrieveWindowContent=true` fiilen kullanılmıyorsa reject/askıya alma riski yüksek.
**Önerilen fix:** Servis gerçekten kullanılmıyorsa (1) manifest'ten ve config'den tamamen kaldır, veya (2) fiilen drag&drop için kullanılacaksa sadece gereken event tiplerini bildir ve `canRetrieveWindowContent`'i yalnızca gerekliyse true bırak; Play Console "Accessibility izin beyanı" formunu buna göre doldur.

---

## 4. Veri Sızıntısı Yüzeyleri

**CategoryLLMFallback.kt** (DeepSeek çağrısı):
- Yalnızca `packageName` gönderiliyor (satır 38-52, 163-179 `buildPrompt`) — uygulama adı/kullanıcı verisi gönderilmiyor. **Temiz.**
- HTTPS zorunlu: `https://api.deepseek.com/v1/chat/completions` (satır 109) + `network_security_config.xml` domain-config ile cleartext engellenmiş. **Doğru.**
- API key: kullanıcı tarafından Ayarlar'dan girilip `AppPrefs` (SharedPreferences, plaintext) içinde saklanıyor (`AppPrefs.kt:385-389`) — hardcoded değil ama **plaintext SharedPreferences'te** duruyor; `data_extraction_rules.xml` bunu `deepseek_prefs.xml` olarak exclude ediyor gibi görünse de gerçek dosya adı `AppPrefs.PREFS_NAME` ile eşleşiyor mu doğrulanmalı (bkz. not altta).

**AppAnalytics.kt (Firebase):**
- `package_name` KASITLI olarak gönderilmiyor (satır 49-50 kod yorumu ile açıkça belgelenmiş, privacy-first tasarım).
- `search_performed` sorgu içeriği değil, `query.length.coerceAtMost(5)` (uzunluk, maks 5) gönderiliyor — arama sorgusu metni sızmıyor. **İyi tasarlanmış.**

**Backup JSON (`BackupManager.exportToJson`):**
- `notificationText` alanı **JSON'a dahil EDİLMEMİŞ** (satır 34-47) — yalnızca `notificationCount` var. **Doğru/güvenli.**
- Diğer alanlar (kategori, kullanım sayacı, dock, klasör özelleştirme) kişisel/hassas değil.

**ŞİDDET: DÜŞÜK** (genel olarak temiz) — ama plaintext API key deposu **BİLGİ** seviyesinde not edilir (Android Keystore/EncryptedSharedPreferences önerilir, zorunlu değil çünkü kullanıcının kendi API key'i, üçüncü şahıs verisi değil).
**Play reject riski: yok** bu madde için (§2'deki bildirim metni ayrı ve daha kritik).

---

## 5. Sır Yönetimi

```
grep -rn "sk-|api_key|apikey|token|Bearer" app/src/ --include=*.kt -i
```
Sonuç: **Hiçbir hardcoded secret bulunamadı.** Bulunan tüm eşleşmeler:
- `CategoryLLMFallback.kt` — parametre olarak geçirilen `apiKey` (kullanıcı girdisi, hardcoded değil)
- `SettingsAppsSection.kt` — UI'da kullanıcının kendi key'ini girdiği `PasswordVisualTransformation` korumalı alan
- `AppPrefs.kt` — get/set fonksiyonları, sabit değer yok

`.env` git'e commit edilmemiş: `git ls-files | grep -i "\.env"` → **sonuç yok.**
Keystore/JKS taraması: yalnızca `scripts/create_release_keystore.ps1` (script dosyası, keystore'un kendisi değil) bulundu — `*.jks` git'te yok.

**ŞİDDET: BİLGİ** — Play reject riski: **yok.**

---

## 6. Intent Güvenliği

**MainActivity route injection (MainActivity.kt:80,87,91-97 + AppNavigation.kt:58-68):**
```kotlin
pendingRoute.value = intent?.getStringExtra(EXTRA_OPEN_ROUTE)   // MainActivity.kt:80
...
navController.navigate(route) { ... }                            // AppNavigation.kt:60
```
`MainActivity` `exported=true` olduğu için **herhangi bir üçüncü parti uygulama** `EXTRA_OPEN_ROUTE`/`open_category` extra'sı taşıyan bir Intent göndererek uygulamanın hangi ekranını açacağını belirleyebilir. Route seti (`Routes.kt` sabitleri) sabit string'lerden oluştuğu ve NavHost'ta tanımsız route için `navigate()` exception fırlatıp yakalanmadığı senaryoda **crash tetiklenebilir** (DoS-benzeri, düşük etkili). Kod enjeksiyonu/RCE riski yok (yalnızca dahili nav route string'i, WebView/eval yok) ama uygulama kontrolü dışarıdan zorlanabiliyor.

`open_category` ile `viewModel.setSelectedCategory(categoryId)` (satır 92-96) — kategori ID'si de dışarıdan enjekte edilebilir; sadece filtre state'i değiştirir, veri sızdırmaz.

**tel:/smsto: (AllAppsDrawer.kt:308,336, HomeScreenComponents.kt:1165,1220,1251):**
- `Uri.parse("tel:$phone")` / `Uri.parse("smsto:$phone")` — `phone` rehberden geliyor, `Uri.encode()` UYGULANMAMIŞ. Rehber verisi kullanıcının kendi cihazından geldiği ve harici/internet kaynaklı olmadığı için pratik enjeksiyon riski düşük, ama savunma amaçlı encode edilmesi iyi pratik olurdu.
- `wa.me/$normalized` — `phone.filter { it.isDigit() || it == '+' }` ile **düzgün normalize edilmiş**, güvenli.
- `market://search?q=" + Uri.encode(query)` — **doğru encode edilmiş.**

**ŞİDDET: ORTA** (route injection — crash/DoS potansiyeli, veri sızıntısı yok) — Play reject riski: **düşük/yok** (bu davranış tek başına Play politikası ihlali değil, ama sertleştirme önerilir).
**Önerilen fix:** `EXTRA_OPEN_ROUTE` değerini bilinen `Routes.*` sabitleri listesine karşı whitelist doğrulaması yaparak kabul et; bilinmeyen route'u sessizce yoksay.

---

## 7. WebView / Dosya Erişimi

- **WebView kullanımı yok** (proje genelinde WebView import/kullanımı bulunamadı).
- **FilesIndexer.kt** — `MediaStore` ContentResolver sorguları kullanıyor (`MediaStore.Images/Video/Audio/Downloads`), ham dosya yolu (`getExternalStorageDirectory`/`getExternalFilesDir`) **kullanılmıyor**. `RELATIVE_PATH` sütunu yalnızca görüntüleme amaçlı okunuyor (satır 88, 97) — path'e doğrudan erişim yok, tamamen `ContentUris`/`content://` URI üzerinden çalışıyor. **Android 16 kuralına tam uyumlu.**
- Opt-in: yalnızca `AppPrefs.isSearchSourceFilesEnabled(context)` açıksa çalışıyor (satır 38).

**ŞİDDET: BİLGİ** — Play reject riski: **yok.** Bu proje için CLAUDE.md'deki D187 audit bulgusuyla tutarlı.

---

## 8. ProGuard/R8 — Log Stripping

**Dosya:** `app/proguard-rules.pro` (177 satır, tam okundu)

- **Log/Timber stripping kuralı YOK.** Dosyada `-assumenosideeffects` direktifi hiç kullanılmamış; aksine `-keep class timber.log.** { *; }` (satır 109) ile Timber sınıfları **korunuyor** (obfuscate/strip edilmiyor, sadece isim koruması — ama zaten çağrılar kaldırılmıyor).
- **`AppOrganizerApp.kt:27`** → `Timber.plant(Timber.DebugTree())` **KOŞULSUZ** çalışıyor — `BuildConfig.DEBUG` veya `isDebug` kontrolü YOK (satır 32'deki `isDebug` değişkeni yalnızca Crashlytics/Analytics toggle'ı için kullanılıyor, Timber.plant için değil).
- Sonuç: **release APK'da da tüm `Timber.d/e/w` çağrıları aktif çalışır** ve Logcat'e yazılır.

**Bildirim metniyle kesişim:** `AppNotificationListenerService.kt` içinde doğrudan bildirim metnini loglayan bir `Timber.d(combined)` çağrısı **bulunamadı** (kontrol edildi, satır 36-66) — bu spesifik dosyada içerik loglanmıyor. Ancak `PackageChangeReceiver.kt` gibi diğer dosyalarda paket adı + kategori bilgisi loglanıyor (örn. satır 62 `"Added new app to DB: $packageName"`) — hassas değil ama genel prensip olarak release'te gereksiz logging var.

**ŞİDDET: ORTA** — Play reject riski: **yok doğrudan**, ama iyi pratik ihlali: release APK'da debug log seviyesi commercial/production hijyeni açısından önerilmez; saldırgan `adb logcat` ile (fiziksel erişim + USB debugging açıksa) uygulama davranışını gözlemleyebilir. Şu an bildirim İÇERİĞİ bu kanaldan sızmıyor (doğrulandı) ama gelecekte birisi debug amaçlı `Timber.d(combined)` eklerse anında sızıntı olur.
**Önerilen fix:** `AppOrganizerApp.kt:27`'yi `if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())` ile sar; release'te ya hiç log basma ya da yalnızca Crashlytics'e giden bir `Timber.Tree` kullan.

---

## 9. Ek Bulgu — Release İmzalama Uyarısı

**Dosya:** `app/build.gradle.kts:66-74`

`keystore.properties` yoksa release build **debug keystore ile imzalanıyor** (bilinçli tasarım, D236 notuyla belgelenmiş). Bu APK Play Store'a **yüklenemez** (Play zaten debug-signed AAB'yi reddeder) — dolayısıyla doğrudan bir Play reject riski değil ama CI/CD sürecinde yanlışlıkla debug-signed bir dosyanın gönderilmemesi için `keystore.properties` varlığının release pipeline'ında zorunlu kontrol edilmesi önerilir.

**ŞİDDET: DÜŞÜK/BİLGİ** — Play reject riski: **yok** (Play kendisi zaten engeller), operasyonel risk var.

---

## Özet Tablo

| # | Alan | Şiddet | Play Reject Riski | Özet |
|---|------|--------|--------------------|------|
| 1 | Manifest bileşen maruziyeti | BİLGİ | Yok | Tüm exported bileşenler gerekçeli; PackageChangeReceiver sistem-korumalı action kullanıyor |
| 2 | NotificationListener içerik iddiası | **YÜKSEK** | **Var** | `apps.notificationText` gerçek bildirim metnini kalıcı DB'de saklıyor + UI'da gösteriyor; kod yorumu "içerik saklanmaz" iddiasıyla çelişiyor; cloud-backup'a dahil |
| 3 | Accessibility Service beyan/kullanım uyumsuzluğu | **YÜKSEK** | **Var** | Servis boş stub ama config geniş izin (canRetrieveWindowContent, gestures) bildiriyor |
| 4 | Veri sızıntısı yüzeyleri (DeepSeek/Analytics/Backup) | DÜŞÜK | Yok | Paket adı dışında veri gitmiyor, HTTPS zorunlu, backup'ta notificationText yok |
| 5 | Sır yönetimi | BİLGİ | Yok | Hardcoded secret yok, `.env`/keystore git'te değil |
| 6 | Intent güvenliği (route injection) | ORTA | Düşük/Yok | Exported MainActivity route'u whitelist'siz kabul ediyor; crash potansiyeli var, veri sızıntısı yok |
| 7 | WebView/dosya erişimi | BİLGİ | Yok | WebView yok; FilesIndexer tamamen MediaStore/SAF uyumlu |
| 8 | ProGuard log stripping | ORTA | Yok | Timber.plant koşulsuz; release'te debug log aktif (şu an bildirim içeriği bu yolla sızmıyor ama risk taşıyan altyapı) |
| 9 | Release imzalama fallback | DÜŞÜK | Yok | keystore.properties yoksa debug-signed release — operasyonel uyarı |

**En kritik 2 madde (Play reject + gerçek gizlilik riski):** §2 (bildirim içeriği DB'de kalıcı + iddiayla çelişki) ve §3 (Accessibility Service aşırı geniş beyan/boş kullanım).
