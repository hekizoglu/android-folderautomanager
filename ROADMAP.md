# ROADMAP.md — AppOrganizer Yol Haritası

> Sprint yönetimi + Play Store yayını. Claude her döngü sonunda tamamlananları işaretler, yeni görevleri "Döngüden Gelen"e ekler.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 ile işaretlenir.

---

## 🎯 Hedef
Play Store yayını → Production AAB hazır ✅, kalan: Privacy Policy + görseller + content rating + son ProGuard kontrolü.

---

## 🔥 Aktif Sprint

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

### Yakın (Sonraki Sprint) — Kod Kalitesi
- [ ] Hilt DI kurulumu — manuel `new()` çağrılarını temizle (kısmen başladı: onboarding'de `hiltViewModel` kullanılıyor)
- [ ] StateFlow migrasyonu — kalan `LiveData` kullanımlarını tara ve geçir
- [ ] Unit test coverage — ViewModel'ler için MockK testleri 🔄 (başlandı, CI pipeline hazır)
- [ ] Compose UI: `LazyColumn`/`LazyVerticalGrid` `key` parametresi audit
- [ ] Memory leak audit: Fragment binding null kontrolü
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
- [ ] Multi-language support (TR/EN) — string resource ayrımı
- [ ] Backup/restore tam — Room export (kısmen: RESTORE_BACKUP onboarding + BackupWorker haftalık ✅, kalan: manuel export/import UI + bulut)

### Uzun Vade
- [ ] Wear OS companion app
- [ ] Tablet layout (large screen support)
- [ ] Widget ekranı genişletme (resize, çoklu sayfa)

---

## 🔍 Döngüden Gelen Yeni Görevler
_(Claude döngü sonunda buraya ekler — tarih + kaynak)_

- [ ] FolderTile reaktif AppPrefs pattern'ini standartlaştır (2026-06-13 — Döngü 26/27/36, LEARNINGS aday öğrenme) — tüm Compose ekranlarında `mutableStateOf + DisposableEffect` helper'ı oluştur, kopyala-yapıştır azalt
- [ ] AppClassifier'ı ayrı veri dosyasına böl (2026-06-15 — config refactor) — 3116 paketlik `mapOf` tek Kotlin dosyasında şişiyor; `assets/app_categories.json` + runtime parse düşün (derleme süresi + duplicate riski azalır)
- [ ] Firebase Crashlytics API kurulumu (2026-06-15) — `google-services.json` + service account credentials `.env`'ye, 6 saatlik crash kontrol otomasyonu

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

---

## ⚠️ Onay Bekleyen Kararlar (Claude → Kullanıcı)

| Karar | Bağlam | Durum |
|-------|--------|-------|
| 🔒 Telegram token rotasyonu | Token sızmış olabilir, BotFather erişimi sende | **Bekliyor** |
| Privacy Policy içeriği | Play Store şart, hangi veri toplandığı netleşmeli (NotificationListener, UsageStats) | Bekliyor |
| AppClassifier → JSON asset | Derleme süresi + duplicate riski azalır ama runtime parse maliyeti | Tartışma |
| Gemini API key | NotebookLM/LLM fallback için, sen sağlarsan eklenir | Bekliyor |

---

*Son güncelleme: 2026-06-15 — gerçek duruma senkronize edildi, dağılmış istekler + otomasyon + güvenlik görevleri eklendi.*
