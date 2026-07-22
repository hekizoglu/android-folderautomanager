# Archive — Teknik Detaylar ve Eski Öğrenmeler

> **Bu dosya:** LEARNINGS.md/HISTORY.md/CLAUDE.md'den arşivlenen detay notlar. Referans için saklanır ama düzenli okunmaz.
> Son güncelleme: 2026-07-22

---

## Mimari Kararlar (Referans Arşivi)

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

### Room DB Versiyon Geçmişi
- v1-v5: temel alanlar
- v6: `customNotes`, `notificationText` alanları eklendi
- v7: 18 yeni kategori (CAT_COMMUNICATION, CAT_MUSIC, CAT_VIDEO... vs.) — şema değişimi yok, MIGRATION_6_7 boş migration ile eklendi
- v8: `firstInstalledTime`, `lastUpdatedTime`, `targetSdkVersion`, `versionName` alanları eklendi (MIGRATION_7_8)
- v9: `search_documents` tablosu + FTS5 sanal tablo (birleşik arama)
- v10: `search_history` tablosu (arama geçmişi)
- v11: `apps` tablosuna `idx_apps_appName`, `idx_apps_categoryId`, `idx_apps_appName_categoryId` index'leri (CS13 performans fix'i, D198)
- v12: `notification_events` tablosu + index onarımları

> **UYARI:** `fallbackToDestructiveMigration()` — Döngü#19'da KALDIRILDI. Yeni versiyon eklerken mutlaka `MIGRATION_x_y` oluştur, boş bile olsa.

---

## Eski Buluşlar (D206, D202, D231 — Token Tasarrufu İçin Arşivlendi)

### D206 (2026-07-07) — SQLite ADD COLUMN İdempotent Değil (KRİTİK)
`ALTER TABLE ... ADD COLUMN` SQLite'ta "IF NOT EXISTS" desteklemez — sütun zaten varsa `duplicate column name` ile çöker.
- **Yaşanan:** `MIGRATION_5_6` cihazda customNotes zaten varken tekrar eklemeye çalıştı, crash.
- **Fix:** `addColumnIfNotExists()` helper — `PRAGMA table_info` ile kontrol edip yoksa ekler.
- **Kural:** Yeni ADD COLUMN migration'ı yazarken ham `execSQL("ALTER TABLE...")` YAZMA — her zaman `addColumnIfNotExists()` kullan.

### D202 (2026-07-07) — Room Migration Index Adı Tuzağı (KRİTİK)
- **Yaşanan:** MIGRATION_10_11 `idx_apps_appName` adıyla açıp; entity `index_apps_appName` bekliyordu → v11→v12 migration'ında `Migration didn't properly handle` fatal'ı.
- **Kural:** Index adını schema JSON'undan (app/schemas/) kopyala, uydurma.

### D231 — Reaktivite Tuzakları (Fable Analizi)
1. **Dock kararsızlık:** DB yazımı her akışı tetikliyor — `distinctUntilChanged { }` predicate kullan
2. **İkon cache anahtarı:** `pkg_px_lastUpdateTime(_iconPack)` formatı tutarlı olmalı
3. **Eagerly + initialValue=emptyList():** Sahte loading — `initialLoadDone` flag gerekli
4. **HOME BackHandler:** Her zaman aktif olmalı, sistem finish riski

---

## Eski Döngü Tanımları (SOP — Tekrarlayan Prosedürler)

### MD Denetim Raporu Kuralı (D210 taşındı)
Her döngüde proje kökünde `MD_DENETIM_*.md` dosyası varsa analiz et, çözülen maddeleri HISTORY'ye taşı, raporu sil.

### Denetim İyileştirme Kuralı (D191)
Her 0 sonuçlu denetim döngüsünde 1 yeni tespit yöntemi ekle (audit.ps1'a).

### Encoding Otomatik Tespit (D182)
`fix_encoding.py`, 3 denemede çözülmezse COZULEMEYEN_SORUNLAR.md'ye ekle.

---

## KeywordDatabase Substring Eşleşmesi (D-Sprint2 Bulgusu)
`KeywordDatabase` keyword listeleri substring `contains` ile eşleşiyor — "tool", "su", "edit" gibi kısa keyword'ler false-positive verir.
**Gelecek:** Word-boundary eşleşmesi veya min. keyword uzunluğu değerlendirilmeli.
