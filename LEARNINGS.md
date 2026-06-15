# LEARNINGS.md — AppOrganizer Öğrenme Kaydı

> Claude her anlamlı döngü sonunda günceller. Promote eşiği: 3 tekrar veya ÖNCELİK:YÜKSEK → CLAUDE.md §5'e taşınır.
> Döngü logları → HISTORY.md. Burası **tekrar eden öğrenme, tuzak, mimari karar** içindir.

---

## 📊 Metrik Hedefler (Firebase ile İzlenecek)

| Metrik | Hedef | Nasıl Ölçülür |
|--------|-------|---------------|
| Launcher açılış süresi | < 300ms | Firebase Performance |
| AllApps drawer açılışı | < 150ms | `folder_opened` event latency |
| Kategori doğruluğu | > %90 | `category_classified` event + kullanıcı düzeltme oranı |
| Crash-free oturum | > %99 | Crashlytics |
| Özellik kullanım oranı | — | Her event sayısı |

### İzlenecek Firebase Events
```kotlin
// folder_opened     — klasör açılışı (category_id parametreli)
// app_launched      — uygulama başlatma
// all_apps_opened   — AllAppsDrawer açılışı
// category_reclassified — kullanıcı kategori değiştirdi (öğrenme sinyali)
// shortcut_used     — app shortcuts kullanıldı
```

---

## 🔼 Promote Edilmiş Kayıtlar (CLAUDE.md §5'e Taşınanlar)

| # | Öğrenme | CLAUDE.md | Tekrar |
|---|---------|-----------|--------|
| P1 | Kotlin smart cast (`by` delegate) | §5 | 5+ |
| P2 | Bağımlılık uyumluluk matrisi | §5 | 4 |
| P3 | AppClassifier `mapOf()` duplicate | §5 | 8+ |
| P4 | KeywordDatabase duplicate kategori | §5 | 2 |
| P5 | Encoding (curly quote + bozuk UTF-8) | §5 | 4+ |
| P6 | Türkçe `Locale("tr")` | §5 | 3 |
| P7 | Flow `SharingStarted.Eagerly` | §5 | 3 |
| P8 | Async ikon `produceState`+LRU | §5 | 5+ |
| P9 | Reaktif AppPrefs (DisposableEffect) | §5 | 3 |
| P10 | `fallbackToDestructiveMigration()` kaldırıldı — production'da veri kaybı riski | §5 | 2 |
| P11 | `derivedStateOf` pattern — scroll sırasında gereksiz recomposition önler | §5 | 2 |
| P12 | `installSplashScreen()` sırası: `super.onCreate()` sonrası, `setContentView()` öncesi | §5 | 1 |
| P13 | Build cache kilidi: Java process'leri öldür + `app\build` sil — tekrarlayan sorun | §5 | 3+ |

---

## 🏗️ Mimari Kararlar (Referans)

### Bildirim Sistemi
- `AppNotificationListenerService` → `StateFlow<Map<String, Int>>` (badge) + `StateFlow<Map<String, String>>` (metin)
- `onNotificationRemoved`: aktif bildirim yoksa map'ten kaldır
- `onListenerDisconnected`: her iki map temizlenir (stale badge önlenir)
- DB'ye yazma: `if (counts.isNotEmpty())` guard KALDIRILDI — boş map temizleme için gerekli

### favoriteApps Mimarisi
- `_favoritePkgs: MutableStateFlow<Set<String>>` + `combine` ile `favoriteApps: StateFlow<List<AppInfo>>`
- `initFavorites(context)` → `LauncherActivity.onCreate` + `onResume`'da çağır
- `getFavoriteApps(context)` KALDIRILDI — `viewModel.favoriteApps` kullan
- `PackageChangeReceiver.onPackageRemoved` → `AppPrefs.removeFavorite()` otomatik

### Dock In-Memory Yönetimi
- `dockLoaded` flag: SharedPrefs sadece ilk yüklemede okunur
- `_dockPackages.value` her zaman güncel — `addToDock`/`removeFromDock` disk IO yapmaz
- `@Volatile` → `AtomicBoolean` (bileşik operasyon güvenliği)

### LauncherActivity onResume Optimizasyonu
- `gestureNavEnabled: Boolean by lazy { }` — `resources.getIdentifier()` bir kez çalışır
- `receiverRegistered` bayrağı — çift kayıt önlenir
- `PACKAGE_FILTER` companion object sabiti — her `onResume`'da nesne oluşturulmaz

### AppClassifier Mimarisi
- `exactMatchMap`: 3375 benzersiz paket (paketAdi → kategori)
- `KeywordDatabase`: 32 kategori, 20-50 keyword her biri
- Bilinmeyen → `CAT_OTHER` → `CategoryLLMFallback.kt` (DeepSeek batch 15)
- Pre-commit hook: her AppClassifier commit'inde `check_duplicates.py` otomatik

### Onboarding Adım Sırası (v2026-06-13)
WELCOME → RESTORE_BACKUP → QUERY_PACKAGES → NOTIFICATIONS → UNUSED_GREY → AUTO_BACKUP → NOTIF_TEXT → NOTIF_ACCESS → SWIPE_HINT → NEW_BADGE → FOLDER_COUNT → NAV_HIDE → THEME_SELECT → SET_LAUNCHER → DONE (14 adım)
Toggle chip adımları: AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE

### Room DB Versiyon Geçmişi
- v1-v5: temel alanlar
- v6: `customNotes`, `notificationText` alanları eklendi
- v7: 18 yeni kategori (CAT_COMMUNICATION, CAT_MUSIC, CAT_VIDEO... vs.) — şema değişimi yok, MIGRATION_6_7 boş migration ile eklendi

> **UYARI:** `fallbackToDestructiveMigration()` — Döngü#19'da KALDIRILDI. Bu metod eksik migration varsa tüm DB'yi siler. Yeni versiyon eklerken mutlaka MIGRATION_x_y oluştur, boş bile olsa.

---

## 🐛 Hata Kataloğu (Aynı Hatayı İki Kez Yaşamama)

| # | Hata | Sebep | Fix |
|---|------|-------|-----|
| E1 | "Smart cast impossible" | `by` delegate nullable | `icon?.let { bmp -> }` |
| E2 | Curly quote derleme hatası | Edit tool curly quote yazar | `scripts/fix_encoding.py` |
| E3 | mapOf() duplicate sessiz kayıp | Kotlin spec — son entry kazanır | `check_duplicates.py` pre-commit |
| E4 | KeywordDatabase kategori kaybı | mapOf() içinde aynı key iki kez | Mevcut listeye ekle, yeni satır açma |
| E5 | Türkçe arama bulamıyor | `ignoreCase=true` I/İ hataları | `lowercase(Locale("tr"))` |
| E6 | Settings'ten dönünce UI eski | `remember {}` bir kez hesaplar | `DisposableEffect` + SharedPrefs listener |
| E7 | Onboarding her açılışta tekrar | Yanlış prefs key kullanımı | `AppPrefs.PREFS_NAME` + `KEY_ONBOARDING_DONE` |
| E8 | Badge silinince kalmaya devam | `if (counts.isNotEmpty())` guard | Guard kaldırıldı, boş map temizleme sağlandı |
| E9 | `isLoadingApps` race condition | `@Volatile` bileşik operasyon korumaz | `AtomicBoolean.compareAndSet()` |
| E10 | Git push non-fast-forward | Remote ahead | `git pull --rebase` önce |
| E11 | Merge conflict AppClassifier | Remote + local aynı döngü | Python ile birleştir, set dedup |
| E12 | Firebase Crashlytics API erişimi yok | `google-services.json` + credentials yapılandırılmamış | `.env`'de Firebase service account JSON ekle |

---

## 📌 Aktif Öğrenmeler (Promote Bekleyenler)

### [2026-06-13] araç-kullanımı [ÖNCELİK: ORTA] — Tekrar: 4+
**Merge conflict AppClassifier:** Remote ve local aynı döngüde yazınca çakışıyor.
**Kural:** `scripts/dedup_classifier.py` mantığı — iki tarafı birleştir, set ile dedup.
**Promote:** Aday (4+ tekrar, CLAUDE.md §5'e eklenecek)

### [2026-06-15] araç-kullanımı [ÖNCELİK: ORTA] — Tekrar: 1
**Firebase öğrenme döngüsü:** Metrik → gözlem → karar → uygulama zinciri.
**Gözlem:** Veri olmadan "iyi/kötü" kararı verilemez. Firebase'den gelen crash stack trace + event sayıları somut öğrenme sinyali.
**Kural:** Her sprint başında önceki dönemin Firebase metriklerini LEARNINGS'e yaz. Neyin ne kadar kullanıldığını görmeden özellik önceliği verme.
**Promote:** İzleniyor

---

## 🆕 İzlenen Gözlemler
_(3 tekrara ulaşınca yukarı taşınır)_

- [2026-06-15] CLAUDE.md büyüyünce konuşma başında gereksiz token harcanıyor — her bölüm büyüyünce ilgili MD'ye taşı kuralı eklendi.

---

*Son güncelleme: 2026-06-15 — v3: Metrik Hedefler + Firebase Events + Hata Kataloğu eklendi. Mimari kararlar bölümü genişletildi. CLAUDE.md'den taşınan döngü logları HISTORY.md'de.*
