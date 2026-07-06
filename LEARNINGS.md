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
| Cold start süresi | < 1.5s | Firebase Performance (trace: `cold_start`) |
| Bellek kullanımı | < 120MB baseline | MemoryStats + Crashlytics OOM |
| Batarya tüketimi | < %4/saat (aktif kullanım) | Android Battery Historian |
| APK boyutu | < 30MB | `assembleDebug` çıktısı |
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

## 🧠 Aktif Öğrenmeler

### [L1] AppClassifier — exactMatchMap vs MANUFACTURER_PREFIX_MAP Çakışması
**Tarih:** 2026-06-15 | **Öncelik:** ORTA | **Tekrar:** 1

`exactMatchMap` (3702 paket, D115 sonrası — `assets/app_categories.json`'da) ile `MANUFACTURER_PREFIX_MAP` (prefix bazlı) aynı paketi farklı kategoriye atayabilir.

**Örnek:** `com.whatsapp`
- `exactMatchMap["com.whatsapp"] = CAT_COMMUNICATION` ✅
- `MANUFACTURER_PREFIX_MAP["com.facebook"] = CAT_META` → prefix `com.` ile başlayan her şey değil, ama `com.whatsapp` da `com.` ile başlıyor

**Neden şimdi sorun değil:** `classifyApp()` içinde `exactMatchMap` kontrolü **önce** yapılıyor. Exact match bulunursa manufacturer prefix'e hiç bakılmıyor.

**Risk:** Yeni paket eklerken `exactMatchMap`'e koymadan sadece prefix map'e güvenilirse yanlış kategori atanır.

**Kural:** Meta/Facebook ekosistemi paketleri (`com.whatsapp`, `com.instagram`, `com.facebook.*`) `exactMatchMap`'te kesin tanımlanmış olmalı — prefix map'e bırakılmamalı.

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
- D115'ten itibaren: `assets/app_categories.json` (**3702** paket) — `AppClassifier.kt` bu dosyayı yükler
- `AppClassifierAssets.kt`: singleton, thread-safe double-check lazy init, JSONObject ile 122 KB parse; `AppClassifier.kt`'nin `exactMatchMap` kaynağı
- `KeywordDatabase`: 32 kategori, 20-50 keyword her biri
- Bilinmeyen → `CAT_OTHER` → `CategoryLLMFallback.kt` (DeepSeek batch 15)
- Pre-commit hook: her AppClassifier commit'inde `check_duplicates.py` otomatik

**Güncelleme Prosedürü (yeni paket eklerken — D115 sonrası):**
1. `assets/app_categories.json`'da doğru kategoriye paketi ekle (alfabetik)
2. `python scripts/check_duplicates.py assets/app_categories.json` çalıştır
3. Duplicate varsa `python scripts/dedup_classifier.py` ile temizle
4. Build + commit + push

### Onboarding Adım Sırası (D201 güncel — 6 adım, 19⭐ radikal kesme)
WELCOME → SET_LAUNCHER → THEME_SELECT → QUICK_SETTINGS → BROWSER_SELECT → DONE
~~17 adım~~ → 6 adıma indirildi (kullanıcı kaybı %72'den düşürmek için — bkz. FİKİRLER.md 19⭐). CLASSIFY_MODE, RESTORE_BACKUP, QUERY_PACKAGES, NOTIFICATIONS, UNUSED_GREY, AUTO_BACKUP, NOTIF_TEXT, NOTIF_ACCESS, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE adımları kaldırıldı — ilgili izinler/ayarlar artık özellik ilk kullanıldığında contextual olarak soruluyor. BROWSER_SELECT (RoleManager.ROLE_BROWSER) yeni eklendi.

### Room DB Versiyon Geçmişi
- v1-v5: temel alanlar
- v6: `customNotes`, `notificationText` alanları eklendi
- v7: 18 yeni kategori (CAT_COMMUNICATION, CAT_MUSIC, CAT_VIDEO... vs.) — şema değişimi yok, MIGRATION_6_7 boş migration ile eklendi
- v8: `firstInstalledTime`, `lastUpdatedTime`, `targetSdkVersion`, `versionName` alanları eklendi (MIGRATION_7_8)
- v9: `search_documents` tablosu + FTS5 sanal tablo (birleşik arama)
- v10: `search_history` tablosu (arama geçmişi)
- v11: `apps` tablosuna `idx_apps_appName`, `idx_apps_categoryId`, `idx_apps_appName_categoryId` index'leri (CS13 performans fix'i, D198)

> **UYARI:** `fallbackToDestructiveMigration()` — Döngü#19'da KALDIRILDI. Yeni versiyon eklerken mutlaka `MIGRATION_x_y` oluştur, boş bile olsa.

**Migration Şablonu (v8 için örnek):**
```kotlin
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Şema değişimi yoksa boş bırak
        // db.execSQL("ALTER TABLE apps ADD COLUMN newField TEXT")
    }
}
// AppDatabase.kt → addMigrations(MIGRATION_7_8)
```
**room.schemaLocation** → `app/build.gradle.kts`'e ekle, `schemas/` klasörünü git'e al.

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
| E12 | PowerShell heredoc `<<'EOF'` syntax hatası | PS 5.1'de bash heredoc çalışmaz | `@'...'@` kullan — kapatan `'@` sıfır indent olmalı |
| E13 | VerifyError / DVM register limit | Büyük `@Composable` (300+ satır) → register limiti aşılıyor | Fonksiyonlara böl, composable'ı küçült |
| E14 | `derivedStateOf` + plain String reaktif değil | `searchQuery: String` Compose State değil, `derivedStateOf` izleyemiyor | `remember(searchQuery) { ... }` — key-based invalidation kullan |
| E15 | `fix_encoding.py` MOJIBAKE dict curly-quote SyntaxError | Edit tool dict key delimiters'ı curly quote yaptı; içerik de `\x9d`/`\x94` içeriyordu — blanket replace content'i de bozdu | Dict'i `_mb(*bs)` fonksiyonu ile byte bazlı oluştur (D156) |
| E16 | `fix_encoding.py` terminal cp1254 emoji hatası | Windows Türkçe terminal (cp1254) emoji print'i redediyor | `sys.stdout.reconfigure(encoding='utf-8')` ile başta set et (D159) |
| E17 | Kotlin Internal Compiler Error (JvmValueClassAbstractLowering) | Gradle cache bazen bozuk obje kachlar — ilk build fail, ikinci `--rerun-tasks` ile geçer | `.\gradlew :app:compileDebugKotlin --rerun-tasks` ile yeniden derle; kalıcıysa `.\gradlew clean assembleDebug` |

---

## 📌 Promote Bekleyenler + Gözlemler
_(3 tekrara ulaşınca 🔼 tablosuna ve CLAUDE.md §5'e taşınır)_

### [2026-06-15] Firebase öğrenme döngüsü — Tekrar: 1 | Öncelik: ORTA
**Kural:** Her sprint başında Firebase metriklerini LEARNINGS'e yaz. Veri olmadan özellik önceliği verme.
→ Firebase entegrasyonu aktif olunca izlemeye başla

### [2026-06-15] Gözlemler
- CLAUDE.md büyüyünce gereksiz token — her bölüm büyüyünce ilgili MD'ye taşı kuralı eklendi.
- AppClassifier yeni kategori: `CAT_PHOTO` sabiti yok, doğrusu `Category.CAT_PHOTOGRAPHY` (Category.kt satır 44).
- Firebase Crashlytics erişimi: `google-services.json` + `.env`'de service account → ROADMAP görevi, çözülmemiş hata değil.

---

### [2026-06-28] KiloCode audit.ps1 Encoding Tuzağı — Tekrar: 1 | Öncelik: YÜKSEK → **PROMOTE EDİLDİ (D191)**
**Sorun:** KiloCode otomatik olarak audit.ps1'e kural ekliyor. Eklenen kuralların description alanında curly quote (`'` U+2019) veya double-encoded em dash (`â€"`) olunca PowerShell 5.1 syntax parser patlatıyor — script tamamen çalışmıyor.
**Fix:** Description alanlarında daima ASCII-safe string kullan (tek tırnak → `'`, em dash → `-`). Mevcut kuralları kaldırarak temiz versiyon yaz.
**Test:** `.\scripts\audit.ps1 -DryRun` — çıktı görünüyorsa syntax OK.
**Bağlantı:** audit.ps1'deki K1-Y8 kurallarının çoğu artık yanlış alarm (D144-D151'de çözüldü). Yeni gerçek kurallar eklenecekse ASCII-safe olmalı.

---

*Son güncelleme: 2026-06-29 — v8: MD denetim N1-N9 kapatildi (D191). KiloCode CLAUDE.md §5'e promote edildi. Onboarding 17 adim guncellendi.*
