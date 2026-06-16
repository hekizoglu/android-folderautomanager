# ROADMAP.md — AppOrganizer Yol Haritası

> Sprint yönetimi + Play Store yayını. Claude her döngü sonunda tamamlananları işaretler, yeni görevleri "Döngüden Gelen"e ekler.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 ile işaretlenir.

---

## 🎯 Hedef
Play Store yayını → Production AAB hazır ✅, kalan: Privacy Policy + görseller + content rating + son ProGuard kontrolü.

---

## 🔥 Aktif Sprint

### HyperOS Tarzı Klasör Açılışı — Blur + Animasyon

> **Hedef:** Klasör açılınca arka plan blur olsun, sheet scale+fade ile açılsın (HyperOS/MIUI tarzı frosted glass)
> **Araştırma:** 2026-06-15 — Haze kütüphanesi (chrisbanes/haze, API 21+) en uygun çözüm
> **Kısıt:** minSdk=26 → `Modifier.blur()` API 31+ gerektiriyor → Haze kütüphanesi API 21+ destekli, bu projeyi kapsar

#### Teknik Plan

**Adım 1 — Haze bağımlılığı**
```kotlin
// app/build.gradle.kts
implementation("dev.chrisbanes.haze:haze:1.5.0")
```

**Adım 2 — HomeScreen: hazeState + haze modifier**
```kotlin
val hazeState = remember { HazeState() }

// Arka plan içerik (klasörler, pager) — blur kaynağı
Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
    // Mevcut HomeScreen içeriği
}
```

**Adım 3 — FolderSheet: ModalBottomSheet → tam ekran Popup + blur**
```kotlin
// containerColor = Color.Transparent (opak değil)
// hazeChild(hazeState, style = HazeStyle(blurRadius=20.dp, tint=Color(0x99000000)))
// enter: fadeIn + scaleIn(0.9f) — HyperOS scale efekti
// exit:  fadeOut + scaleOut(0.9f)
```

**Adım 4 — Ayar toggle**
```kotlin
// AppPrefs.KEY_FOLDER_BLUR (varsayılan: true)
// SettingsScreen'de "Klasör Blur Efekti" toggle
```

#### Dosya Değişiklikleri
| Dosya | Değişiklik |
|-------|-----------|
| `app/build.gradle.kts` | Haze dependency |
| `presentation/ui/launcher/HomeScreen.kt` | `hazeState` + `.haze()` modifier |
| `presentation/ui/launcher/FolderSheet.kt` | `ModalBottomSheet` → blur container + scale animasyon |
| `utils/AppPrefs.kt` | `KEY_FOLDER_BLUR` toggle |
| `presentation/ui/screens/SettingsScreen.kt` | Blur toggle UI |

- [x] ~~**[BLUR-1]** Haze kütüphanesi ekle + HomeScreen hazeState~~ ✅ Haze VerifyError yaptı → kaldırıldı; `Modifier.blur(20.dp)` AllAppsDrawer'da, frosted tint FolderSheet'te (2026-06-15 Döngü 46)
- [x] ~~**[BLUR-2]** FolderSheet blur + scale animasyon~~ ✅ `containerColor = Color(0xE61A1A2A)` frosted tint — API bağımsız çözüm (2026-06-15 Döngü 46)
- [x] ~~**[BLUR-3]** AppPrefs toggle + SettingsScreen~~ ✅ `KEY_FOLDER_BLUR` toggle Settings'te mevcut (2026-06-15 Döngü 46)
- [ ] **[BLUR-4]** Gerçek cihaz testi (blur performansı + API 26 uyumu)

---

### 🔒 Güvenlik (Önce Bu)
- [x] ~~🔒 Telegram bot token rotasyonu~~ ✅ Yeni token alındı, `.env` güncellendi, test edildi (2026-06-15)
- [x] ~~🔒 `.gitignore` doğrula~~ ✅ `.env`, `*.jks`, `keystore.properties`, `*.aab` korunuyor (2026-06-15)

### Play Store Yayını (Kritik)
- [x] ~~app-release.aab oluştur + imzala~~ ✅ v1.0.0 (6.3MB, `Desktop/AppOrganizer_PlayStore/`)
- [x] ~~Mapping dosyası~~ ✅ `mapping-v1.0.0.txt`
- [ ] Privacy Policy sayfası (GitHub Pages tek HTML) ⚠️ içerik onayı
- [x] ~~Store listing metni (TR + EN)~~ ✅ `docs/store_listing.md` (Döngü #22, 2026-06-15)
- [ ] Screenshots (Pixel 6 emülatörü, light + dark mode)
- [ ] Content rating anketi ⚠️
- [x] ~~ProGuard kuralları son kontrol~~ ✅ `@HiltAndroidApp` + `@AndroidEntryPoint` keep kuralları eklendi (Döngü #23, 2026-06-15)

### Otomasyon Tamamlama
- [x] ~~Döngü orchestrator scripti~~ ✅ `scripts/cycle.ps1`
- [x] ~~AppClassifier duplicate kontrol~~ ✅ `scripts/check_duplicates.py` + `dedup_classifier.py`
- [x] ~~Encoding fix scripti~~ ✅ `scripts/fix_encoding.py`
- [x] ~~Telegram bildirim helper~~ ✅ `scripts/telegram_notify.ps1`
- [ ] `cycle.ps1` yerel makinede uçtan uca test (build → push → Telegram)
- [x] ~~Git pre-commit hook~~ ✅ `.github/hooks/pre-commit` + `scripts/install_hooks.ps1` (2026-06-15)
- [x] ~~`scripts/update_notebooklm.py`~~ ✅ 68 Kotlin dosyası → 488KB, Masaüstü/notebooklm_apporganizer/ (2026-06-15)

---

## 📋 Backlog

### 🔴 KRİTİK — Arayüz + Ayarlar Yeniden Tasarlanacak (YENİ)
> **Kapsam:** Tüm UI katmanı sıfırdan yeniden yazılacak. Mevcut kod referans olarak kullanılacak ama her şey yeniden tasarlanacak.
> **Gerekçe:** Mevcut kodda `SettingsHomeScreenSection.kt` ve `SettingsHomeSection.kt` çakışıyor, aynı ayarlar 3 farklı dosyada tekrarlanmış. Sayfa kayması, klasör isimlerinin yarım kalması, widget/klasör sürükle-bırak eksikliği gibi sorunlar temelden çözülecek.

#### Yeni Tasarım Hedefleri
- [ ] **Tüm ayarlar açılıp kapanabilir olmalı** — her özellik için toggle (mevcut + yeniler)
- [ ] **Sayfa başına klasör sayısı ayarlanabilir** — kullanıcı seçimi veya "otomatik (ekran boyutuna göre)"
- [ ] **Klasör yeri değiştirilebilir** — sürükle-bırak ile (mevcut) + **sayı numarası girerek** (yeni)
- [ ] **Widget yer değişikliği** — sürükle-bırak ile widget'ların konumu değiştirilebilmeli
- [ ] **Sayfa kayması sorunu çözülecek** — tüm özellikler açıkken sayfa aşağı kayıyor; diğer sayfalara dağıtılacak
- [ ] **Klasör isimleri yarım kalmasın** — uzun isimler için tooltip/alt satıra geçme/kesme noktası
- [ ] **Masaüstü ve Tüm Uygulamalar için ayrı ayarlar** — Uygulama önerileri, Son kullanılanlar, Favoriler her iki alan için bağımsız toggle

#### Düzeltilecek Hatalar (Yeni Arayüzde)
- [ ] **Uygulama önerileri doğru çalışmıyor** — `suggestedApps` (en çok kullanılan) ile `recentApps` (son açılan) karışıyor, UI'da başlıklar yanlış
- [ ] **Son kullanılanlar doğru çalışmıyor** — `lastUsedTimestamp` sıfır olan sistem uygulamaları 1970 gösteriyor (sorun değil ama filtrelenmeli)
- [ ] **Favoriler doğru çalışmıyor** — SharedPrefs + StateFlow senkronizasyon sorunu, AllAppsDrawer'da ve ana ekranda ayrı ayrı test edilmeli
- [ ] **Sistem uygulamaları 1970 tarihi** — `lastUsedTimestamp == 0L` olanlar sıralamada en alta atılmalı (sorun değil, bilinçli)

### Yakın (Sonraki Sprint) — Kod Kalitesi
- [ ] Hilt DI kurulumu — manuel `new()` çağrılarını temizle (kısmen başladı: onboarding'de `hiltViewModel` kullanılıyor)
- [x] ~~StateFlow migrasyonu — kalan `LiveData` kullanımlarını tara ve geçir~~ ✅ LiveData kullanımı yok, tüm akışlar StateFlow (2026-06-15 Döngü 52)
- [ ] Unit test coverage — ViewModel'ler için MockK testleri 🔄 (başlandı, CI pipeline hazır)
- [x] ~~Compose UI: `LazyColumn`/`LazyVerticalGrid` `key` parametresi audit~~ ✅ 7 dosyada key eklendi (Döngü 51, 2026-06-15)
- [x] ~~Memory leak audit: Fragment binding null kontrolü~~ ✅ Proje tamamen Compose — Fragment yok, ViewBinding yok, leak riski minimal (Döngü 57, 2026-06-15)
- [ ] Dark mode tam uyum audit

### Akıllı Kategorizasyon Aşama 3
- [ ] **Kendi sunucu API'si** (`packageName → category` endpoint) — Play Store ToS uyumlu, DeepSeek fallback'e alternatif
  - Sunucu: Python Flask + SQLite, basit; bilinmeyen paket gönderilir, kategori döner
  - Mevcut 3116 paketlik `exactMatchMap` ile seed edilir
  - Avantaj: offline DB güncellemesi sunucudan push edilebilir (APK güncellemeden)

### CI/CD & Geliştirme Araçları
- [x] ~~GitHub Actions — lint + test + build pipeline~~ ✅ `.github/workflows/android.yml` (Döngü #28, 2026-06-15)
- [ ] Aider repo-map: CBM ile entegrasyon testi
- [ ] Greptile API denemesi — PR review otomasyonu

### Orta Vade — Ürün
- [ ] **Akıllı Uygulama Önerileri (30dk/1)** — Kullanıcının uygulama kullanım alışkanlığına göre (saat/gün/konum) her 30 dakikada bir değişen öneri satırı. Pixel Launcher'ın "App Predictions" API'si (hücre dışı) veya basit zaman-bazlı döngü: son 30dk'da en çok kullanılan 4 uygulama
- [ ] Multi-language support (TR/EN) — string resource ayrımı
- [ ] Backup/restore tam — Room export (kısmen: RESTORE_BACKUP onboarding + BackupWorker haftalık ✅, kalan: manuel export/import UI + bulut)

### Uzun Vade
- [ ] Wear OS companion app
- [ ] Tablet layout (large screen support)
- [ ] Widget ekranı genişletme (resize, çoklu sayfa)

---

## 🔍 Döngüden Gelen Yeni Görevler
_(Claude döngü sonunda buraya ekler — tarih + kaynak)_

- [ ] **Ayarlar — Kullanıcı Talep/Öneri Formu** — SettingsScreen'e "Talep Gönder" butonu: kullanıcı metin girer, GitHub Issue veya Telegram bot'a gönderilir (2026-06-16 — kullanıcı talebi)

- [x] ~~FolderTile reaktif AppPrefs pattern'ini standartlaştır~~ ✅ FolderTile parametre bazlı (HomeScreen'den geliyor), HomeScreen DisposableEffect+listener kullanıyor — pattern doğru (2026-06-15 Döngü 52)
- [ ] AppClassifier'ı ayrı veri dosyasına böl (2026-06-15) — 3717 paketlik `mapOf` tek Kotlin dosyasında şişiyor; `assets/app_categories.json` + runtime parse düşün
- [ ] Firebase Crashlytics API kurulumu (2026-06-15) — `google-services.json` + service account credentials `.env`'ye, crash kontrol otomasyonu
- [x] ~~`AppDatabaseService` 404 uyarısı~~ ✅ Network hata logu Timber.w→d indirildi, assets dosyası mevcut (dummy v2) — sessiz fallback (Döngü 55, 2026-06-15)
- [ ] `AppNotificationListenerService` ilk açılışta restart (2026-06-15 — Döngü 43) — emülatörde `Scheduling restart of crashed service` görüldü; race condition Döngü 44'te kısmen giderildi, gerçek cihazda test gerekiyor
- [ ] AllApps double-tap emülatörde doğrulanamadı (2026-06-15 — Döngü 43) — Compose içi state değişimi loglanmıyor; gerçek cihaz testi gerekiyor
- [ ] Üretici kategorileri gerçek cihaz testi (2026-06-15 — Döngü 41) — 9 yeni kategori eklendi (CAT_GOOGLE vb.), onboarding'den "üreticiye göre" seçip klasörlerin doğru oluştuğunu doğrula

### 🔴 Yüksek Öncelik — Döngü 69 Analiz (2026-06-16)
- [ ] 🔴 **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir
- [x] ~~🔴 **`git config core.hooksPath .githooks`**~~ ✅ `.githooks/pre-commit` oluşturuldu, `git config core.hooksPath .githooks` ayarlandı (Döngü 72, 2026-06-16)
- [x] ~~🔴 **Android 15 Edge-to-Edge**~~ ✅ `WindowCompat.setDecorFitsSystemWindows(false)` MainActivity + LauncherActivity'de; Compose Scaffold ekranları Material3 otomatik handle ediyor (Döngü 73, 2026-06-16)
- [x] ~~🔴 **Room `schemas/` klasörü**~~ ✅ `room.schemaLocation` kapt argumentine eklendi, `app/schemas/` git'e alındı (Döngü 71, 2026-06-16)
- [x] ~~🟡 **Predictive Back Gesture**~~ ✅ Manifest satır 49'da zaten mevcut; HomeScreen BackHandler doğru (Döngü 74, 2026-06-16)
- [x] ~~🟡 **LeakCanary**~~ ✅ `debugImplementation leakcanary-android:2.14` eklendi (Döngü 75, 2026-06-16)
- [ ] 🟡 **Android 14 NotificationListenerService** — gerçek cihazda test (Android 14 kısıtlamaları)
- [x] ~~🟢 **Themed monochrome icon**~~ ✅ `ic_launcher_monochrome.xml` oluşturuldu, ic_launcher + ic_launcher_round'a eklendi (Döngü 77, 2026-06-16)
- [x] ~~🟢 **`android:dataExtractionRules` XML**~~ ✅ Zaten mevcut; crash_log + deepseek_prefs exclude eklendi (Döngü 76, 2026-06-16)
- [x] ~~🟢 **Splash Screen API**~~ ✅ Dependency + installSplashScreen() zaten entegre; splash ikonu ic_launcher_foreground olarak güncellendi (Döngü 81, 2026-06-16)

---

## ✅ Tamamlananlar

### Altyapı & Config
- [x] CLAUDE.md v1 — Token optimizasyon stack
- [x] CLAUDE.md v2 — Aider, Android standartları, döngü öğrenme sistemi
- [x] CLAUDE.md v3 — Döngü logları HISTORY.md'ye taşındı, otomasyon scriptleri, CLAUDE.md ~%37 küçüldü, döngü logları artık HISTORY.md'ye gidiyor (her konuşmada şişmiyor) (2026-06-15)
- [x] Multi-agent mimari — subagent bölümü + 3 agent (`code-reviewer`/`android-builder`/`deepseek-analyst`)
- [x] CBM ignore konfigürasyonu (`.cbm-ignore`)
- [x] LEARNINGS.md doğru yapılandırıldı — 11 promote öğrenme (2026-06-15)
- [x] HISTORY.md arşivi oluşturuldu — 76 döngü logu (2026-06-15)

### Akıllı Kategorizasyon
- [x] Aşama 1: Offline veritabanı — **3116+ benzersiz paket** (479'dan)
- [x] Aşama 2: "Diğer" klasörü DeepSeek LLM fallback (`CategoryLLMFallback.kt`)
- [x] KeywordDatabase duplicate kategori bug fix (Döngü 79)
- [x] AppClassifier duplicate temizliği (350+ duplicate, çeşitli döngüler)

### Rakip Döngüsü (Rakiplerden Öne Geçme)
- [x] ~~Ana ekrana dönüş hızı~~ ✅ (Döngü 15,16,20,21 — Flow Eagerly + queryIntentActivities)
- [x] ~~Gesture navigation uyumsuzluk~~ ✅ (Döngü 11 — Xiaomi/Samsung)
- [x] ~~İkon pack desteği~~ ✅ (Döngü 22 — Nova/ADW/GO/Lawnchair/Tesla)
- [x] ~~Widget desteği~~ ✅ (Döngü 24 — AppWidgetHost)

### Özellikler (Geniş Liste)
- [x] App shortcuts (uzun bas, Döngü 28) · App Not (Döngü 46) · Uygulama önerileri/Son kullanılanlar (Döngü 29,36)
- [x] Klasör özelleştirme: ad + emoji + renk (Döngü 30,31)
- [x] Favoriler (Döngü 35,36) · Bildirim badge + metni (Döngü 8,18,27)
- [x] BackupWorker haftalık + RESTORE_BACKUP onboarding (Döngü 24,67)
- [x] Türkçe arama/sıralama (Locale tr, Döngü 25,84)
- [x] 12/12 Özellik Kontrol Listesi maddesi ✅

---

## 📊 Sprint Metrikleri

| Tarih | Tamamlanan | Eklenen | LEARNINGS | Build |
|-------|-----------|---------|-----------|-------|
| 2026-06-14 | Loop 84 (AppClassifier 3116) | — | FolderSheet TR fix | #17 (28.5MB) |
| 2026-06-15 | Config refactor (CLAUDE/HISTORY/LEARNINGS/ROADMAP + 6 script) | 5 yeni dosya | 11 promote | — |
| 2026-06-15 | Döngüler #22-30 — store listing, ProGuard, LLM fallback, AppInfo v8, widget, search, shortcut, CI | 3 yeni dosya | 4 yeni LEARNINGS | — |
| 2026-06-15 | Döngüler #39-45 — AllApps gesture fix, 9 üretici kategorisi, 0-warning build, DeepSeek 4 bug fix, emülatör testi, HISTORY/ROADMAP sistemi | — | L1 eklendi | #705 (msg) |
| 2026-06-16 | Döngüler #62-66 — 5 test fix, AppClassifier +191 paket (TR e-Devlet/ulaşım/LatAm/Browser/VPN/IoT/Çevre), 3680 benzersiz | — | — | #731 (29.3MB) |
| 2026-06-16 | Döngüler #67-80 — AllApps bug fix + performans opt + 0 uyarı build, 3717 benzersiz | 10+ özellik | DVM register limiti E13 | 24.8MB |
| 2026-06-16 | Döngüler #81-87 — SplashScreen + AppRepositoryTest 23 test + 0 uyarı build + recentApps fix | 8 dosya | — | BUILD #16 (24.8MB) |
| 2026-06-16 | MD Denetim + ROADMAP/LEARNINGS/CLAUDE düzeltme (K1/K

---

## ⚠️ Onay Bekleyen Kararlar (Claude → Kullanıcı)

| Karar | Bağlam | Durum |
|-------|--------|-------|
| Privacy Policy içeriği | Play Store şart, hangi veri toplandığı netleşmeli (NotificationListener, UsageStats) | Bekliyor |
| AppClassifier → JSON asset | Derleme süresi + duplicate riski azalır ama runtime parse maliyeti | Tartışma |
| Gemini API key | NotebookLM/LLM fallback için, sen sağlarsan eklenir | Bekliyor |

---

*Son güncelleme: 2026-06-16 — gerçek duruma senkronize edildi, dağılmış istekler + otomasyon + güvenlik görevleri eklendi.*
