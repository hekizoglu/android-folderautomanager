# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-06-21 (D103 sonrası). Yeni görevler → **FİKİRLER.md**. Bu dosya aktif durumu gösterir.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅  
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 🔥 Şu An Ne Yapılıyor (D104+)

Gerçek cihaz testleri ve Play Store ön hazırlıkları.

---

## 📋 Bekleyen Görevler

### 🔴 Kritik (Engel)

| Görev | Neden Kritik | Durum |
|-------|-------------|-------|
| **QUERY_ALL_PACKAGES Play Store beyanı** | Göndermeden önce zorunlu — eksikse APK reddedilir | ⚠️ Bekliyor |
| **Privacy Policy sayfası** | Play Store şart — GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli | ⚠️ Onay gerekli |
| **Content rating anketi** | Play Store — göndermeden önce doldurulmalı | ⚠️ Bekliyor |
| **Screenshots** | Play Store — Pixel 6 emülatörü, light + dark mode | Bekliyor |

### 🟡 Orta Öncelik (Sonraki Sprint)

| Görev | Dosya / Alan | Durum |
|-------|-------------|-------|
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |
| **Dark mode tam uyum audit** | Kalan hardcode renkler | Bekliyor |
| **Firebase Crashlytics kurulumu** | `google-services.json` + service account | Bekliyor |
| **`cycle.ps1` uçtan uca test** | build → push → Telegram yerel | Bekliyor |
| **AppNotificationListenerService ilk açılışta restart** | gerçek cihaz test gerekli | Bekliyor |

### 🟢 Düşük Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Unit test coverage** | LauncherViewModel MockK testleri | Bekliyor |
| **Multi-language support (TR/EN)** | string resources | Bekliyor |
| **Hilt DI kurulumu** | manuel `new()` çağrılarını temizle | Bekliyor |
| **AppClassifier → JSON asset** | `assets/app_categories.json` + runtime parse | Tartışma ⚠️ |
| **AllApps double-tap gerçek cihaz testi** | emülatörde doğrulanamadı | Bekliyor |
| **Üretici kategorileri gerçek cihaz testi** | 9 yeni kategori (CAT_GOOGLE vb.) | Bekliyor |

### 🔵 Uzun Vade

- Kendi sunucu API'si (`packageName → category` endpoint) — DeepSeek fallback'e alternatif
- Backup/restore: manuel export/import UI + bulut senkron
- Akıllı Uygulama Önerileri (30dk) — kullanım alışkanlığına göre
- Wear OS companion app
- Tablet layout (large screen)
- Widget ekranı genişletme (resize, çoklu sayfa)
- Aider CBM entegrasyon testi
- Greptile API PR review otomasyonu

---

## ⚠️ Onay Bekleyen Kararlar

| Karar | Bağlam | Durum |
|-------|--------|-------|
| Privacy Policy içeriği | Hangi veri toplandığı netleşmeli (NotificationListener, UsageStats) | Bekliyor |
| AppClassifier → JSON asset | Derleme süresi + duplicate riski azalır, runtime parse maliyeti artar | Tartışma |
| Gemini API key | LLM fallback için, kullanıcı sağlarsa eklenir | Bekliyor |

---

## ✅ Tamamlananlar

### Altyapı & Config
- [x] CLAUDE.md v1-v5, LEARNINGS.md, HISTORY.md sistemi
- [x] Multi-agent mimari (code-reviewer / android-builder / deepseek-analyst)
- [x] `scripts/`: cycle.ps1, check_duplicates.py, dedup_classifier.py, fix_encoding.py, telegram_notify.ps1, update_notebooklm.py
- [x] `.githooks/pre-commit` — AppClassifier duplicate otomatik kontrol
- [x] GitHub Actions CI/CD pipeline
- [x] Room `schemas/` git'e alındı, `room.schemaLocation` gradle'da tanımlı
- [x] 🔒 `.gitignore` → `.env`, `*.jks`, `keystore.properties`, `*.aab` korunuyor
- [x] 🔒 Telegram bot token rotasyonu

### Play Store Hazırlık
- [x] app-release.aab v1.0.0 (6.3MB) + mapping dosyası
- [x] Store listing metni (TR + EN) — `docs/store_listing.md`
- [x] ProGuard kuralları son kontrol
- [x] Android 15 Edge-to-Edge — `WindowCompat.setDecorFitsSystemWindows(false)`
- [x] Predictive Back Gesture — Manifest + BackHandler
- [x] Themed monochrome icon (`ic_launcher_monochrome.xml`)
- [x] `android:dataExtractionRules` XML — crash_log + deepseek_prefs exclude
- [x] Splash Screen API — `installSplashScreen()` + ic_launcher_foreground

### Akıllı Kategorizasyon
- [x] Aşama 1: Offline veritabanı — **3717 benzersiz paket**
- [x] Aşama 2: DeepSeek LLM fallback (`CategoryLLMFallback.kt`)
- [x] KeywordDatabase duplicate bug fix
- [x] AppClassifier duplicate temizliği (350+, pre-commit hook ile korunuyor)

### Launcher Özellikleri
- [x] HyperOS blur (AllAppsDrawer `Modifier.blur(20.dp)` + FolderSheet frosted tint)
- [x] İkon pack desteği (Nova/ADW/GO/Lawnchair/Tesla)
- [x] Widget desteği + drag-drop sıralama (D101)
- [x] App shortcuts (uzun bas)
- [x] Klasör özelleştirme: ad + emoji + renk
- [x] Favoriler + Son Kullanılanlar (race condition fix D102)
- [x] Bildirim badge + metin
- [x] BackupWorker haftalık
- [x] FCM push ile AppDatabase uzaktan güncelleme (`AppFirebaseMessagingService.kt`)

### UI & Ayarlar
- [x] Masaüstü ve Tüm Uygulamalar için bağımsız Favoriler + Son Kullanılanlar toggle'ları (D99)
- [x] Sayfa başına klasör sayısı ayarı — slider 4/6/8/12 (D100)
- [x] Sayfa kayması fix — `Arrangement.SpaceBetween` → `Arrangement.Top` (D100)
- [x] FolderTile uzun isim sarma — `maxLines=2` (D98)
- [x] AppSuggestionsRow başlık: "Önerilenler" → "Sık Kullanılanlar" (D98)
- [x] SettingsScreen duplicate "Görünüm" başlığı temizliği (D97)
- [x] SettingsHomeScreenSection 20+ Türkçe string fix (D97)
- [x] SettingsAppearanceSection 12+ Türkçe string fix (D103)
- [x] Geri Bildirim — Telegram API → e-posta Intent (huseyinekizoglu@gmail.com) (D103)
- [x] Dark mode — hardcode renk → MaterialTheme.colorScheme (D91)
- [x] LeakCanary debugImplementation eklendi
- [x] AllApps arama kritik bug fix (D88)
- [x] AppRepositoryTest + LauncherViewModelTest (D89)

### Kod Kalitesi
- [x] StateFlow migrasyonu — LiveData kullanımı yok
- [x] `LazyColumn`/`LazyVerticalGrid` `key` parametresi audit (7 dosya)
- [x] Memory leak audit — Fragment/ViewBinding yok, Compose tamamen
- [x] 0-warning build

---

## 📊 Sprint Özeti

| Tarih | Döngüler | Özet |
|-------|---------|------|
| 2026-06-14 | D84 | AppClassifier 3116 paket |
| 2026-06-15 | D22-D57 | Config refactor, store listing, LLM fallback, widget, CI, AllApps |
| 2026-06-16 | D62-D87 | AppClassifier 3717, 0-uyarı build, SplashScreen, 23 test, FCM push |
| 2026-06-18 | D88-D92 | AllApps arama fix, LauncherViewModelTest, BUILD #17, dark mode |
| 2026-06-20 | D93-D95 | MD denetim + senkronizasyon düzeltmeleri |
| 2026-06-21 | D96-D103 | FolderSheet Türkçe, Settings audit, widget drag-drop, favoriler race condition, e-posta geri bildirim |
