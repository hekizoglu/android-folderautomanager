# LEARNINGS.md — AppOrganizer Öğrenme Kaydı

> Claude her anlamlı döngü sonunda günceller. Promote eşiği: 3 tekrar veya ÖNCELİK:YÜKSEK → CLAUDE.md §5'e taşınır.
> Döngü logları → HISTORY.md. Burası **tekrar eden öğrenme, tuzak, mimari karar** içindir.

---

## 🧭 VİZYON KARARI — Pixel klonu değiliz (2026-07-14, Hüseyin — ÖNCELİK:YÜKSEK)

AppOrganizer artık "invisible launcher / Pixel Launcher klonu" DEĞİL. Hedef: kendi kimliği olan, kendi başına harika bir uygulama. Sonuçları:
- Tasarım/UX kararlarında ölçüt "Pixel ne yapıyor?" değil, "kullanıcımız için en iyisi ne?"
- Pixel'den ayrışmak artık hata değil, tercih olabilir — "Pixel'de böyle" tek başına gerekçe sayılmaz
- Mevcut görsel öğeler (frosted dock, transparent bg, Pulse Clock) Pixel taklidi olarak değil, kendi kimliğimizin parçası olarak evrilir
- CLAUDE.md §7 ve README bu vizyonla güncellendi (D255 sonrası)

---

## 🔎 KeywordDatabase Agresif Substring Eşleşmesi (D-Sprint2 P0.7 bulgusu)

`KeywordDatabase` keyword listeleri substring `contains` ile eşleşiyor — "tool", "su", "edit" gibi kısa keyword'ler alakasız uygulama/paket adlarına false-positive verir (örn. "samsung" içindeki "su"). CategorySuggestionEngine testlerinde fixture adları bu yüzden çakışmasız seçilmek zorunda kaldı. Gelecek classifier işlerinde: kelime sınırı (word-boundary) eşleşmesi veya min. keyword uzunluğu değerlendirilmeli.

---

## 🔒 Build Kilidi Kök Nedenleri (D235 — 6 kilit vakasının otopsisi)

### D259 ÇÖZÜLDÜ: Defender exclusion'ları ESKİ proje yolundaydı
D255-258 kilitlerinin kök nedeni: D235 exclusion'ları `Github Klasörleri\android-folderautomanager` yoluna ekliydi; repo `Documents\AppOrganizer`'a taşınınca kapsam dışı kaldı. 2026-07-14'te UAC onaylı script ile eklendi ve `Get-MpPreference` çıktısıyla doğrulandı: `AppOrganizer\app\build`, `AppOrganizer\.gradle`, `AppOrganizer\.claude\worktrees`, `~\.gradle`, `~\.android`, `java.exe` (process). **Ders: proje taşınırsa Defender exclusion'ları da taşınmalı.** Kilit yine görülürse önce `Get-MpPreference | Select ExclusionPath` (admin) ile yol kapsamını doğrula.

### VSCode redhat.java dil sunucusu R.jar'ı kilitler
`Get-Process java` çıktısında `redhat.java-*/jre/*/bin/java.exe` görünüyorsa VSCode Java LS, Gradle çıktılarını (özellikle R.jar) açık tutuyor olabilir — Gradle daemon'ları durdurmak YETMEZ. Çözüm sırası:
1. `Get-Process java | Stop-Process -Force` (VSCode LS dahil hepsi — LS kendini yeniden başlatır, zararsız)
2. 3 sn bekle → `Remove-Item -Recurse -Force app\build`
3. Defender exclusion'ları ayrıca şart (Hüseyin D235'te ekledi) ama tek başına yeterli değil

### app\build altında SEÇİCİ silme yapma
Sadece `generated`/tek klasör silmek Gradle incremental state'ini bozar → `Cannot access output property ... does not exist` (compileDebugKotlin MD5 hatası). Kural: app\build ya komple silinir ya hiç dokunulmaz.

---

## 🔄 Reaktivite ve Akış Tuzakları (D231 teşhisi, Fable)

### Dock kararsızlık zinciri — DB'ye her yazım tüm türev akışları tetikler
Bildirim badge'i `apps` tablosuna yazılınca (`updateNotificationCount`) `getAllAppsFlow()` yeniden emit eder; `distinctUntilChanged` liste alanı değiştiği için işe yaramaz. Öneri/dock gibi türev akışlar SADECE ilgilendikleri alanlara `distinctUntilChanged { }` predicate'i ile bağlanmalı — aksi halde her bildirimde dock/öneri sıralaması değişir. Genel kural: bir StateFlow zincirine kaynak eklerken "bu kaynak hangi sıklıkta emit eder?" sorusu sorulmalı.

### İkon cache anahtarı tutarlılığı — lastUpdateTime her yerde olmalı
`DockIcon` cache anahtarına `getPackageInfo().lastUpdateTime` ekliyor ama `MiniAppIcon` (FolderTile önizleme) eklemiyordu → uygulama güncellenince önizleme ikonu bayat kalıyordu. Kural: `produceState` ikon cache anahtarı HER ZAMAN `pkg_px_lastUpdateTime(_iconPack)` formatında olmalı; yeni ikon composable'ı eklerken mevcutlardan kopyala.

### Eagerly + initialValue=emptyList() = cold resume'da sahte "yükleniyor"
Process LMK ile öldükten sonra dönüşte StateFlow'lar ilk Room emit'ine kadar boş — `isEmpty()` tabanlı loading koşulu sahte loading ekranı gösterir. Kural: loading UI koşulu `!initialLoadDone && list.isEmpty()` olmalı; `initialLoadDone` Room'un ilk emisyonuyla true'ya döner.

### Home kökünde koşullu BackHandler = OEM'de Activity finish riski (D272)
`HomeScreen.kt`'de `BackHandler(enabled = allAppsOpen)` yalnızca AllApps açıkken aktifti; ana ekran kökünde (`allAppsOpen=false`) sistemin `OnBackPressedDispatcher`'ında hiçbir enabled callback kalmıyordu. Android 13+ predictive-back ve bazı OEM'lerde (MIUI/HyperOS) bu durumda sistem varsayılan davranışı devreye giriyor ve `LauncherActivity`'yi (HOME activity) finish edebiliyor — Activity `singleTask` olsa bile bir sonraki HOME basışında sıfırdan `onCreate` tetiklenip "her seferinde yeniden yükleniyor" hissi yaratıyor. Kural: HOME/launcher rolündeki bir Activity'nin kök ekranında **her zaman aktif** (`enabled = true`) bir `BackHandler` bulunmalı ve en azından no-op tüketmeli — geri tuşu launcher'ı asla finish etmemeli.

### Cold start borç listesi (D231, henüz yapılmadı)
`AppOrganizerApp.onCreate` main thread'de senkron: Firebase init + 3 worker schedule + FCM token. Baseline profile YOK (macrobenchmark modülü yok) — cold start iyileştirmesinin en yüksek getirili adımı. 7+ `Eagerly` StateFlow ViewModel init'te aynı anda ayağa kalkıyor; kritik olmayanlar (`widgetSuggestions`, `tickerItems`) `WhileSubscribed`'e adaydır (folders/allApps DEĞİL — bkz. Flow Sıcaklığı kuralı).

---

## 📋 SOP — Nadiren Tetiklenen Prosedürler (Detaylı → docs/internal/archive_technical_details.md)

Gereksiz teknik detaylar arşivlendi. Kod yazılırken gerekli adımlar CLAUDE.md §3'te aktif.

---

## 🆕 D206 (2026-07-07) Tuzakları

### SQLite ADD COLUMN İdempotent Değil (KRİTİK)
`ALTER TABLE ... ADD COLUMN` SQLite'ta "IF NOT EXISTS" desteklemez — sütun zaten varsa `duplicate column name` ile çöker.
- **Yaşanan:** `MIGRATION_5_6` cihazda customNotes zaten varken tekrar eklemeye çalıştı, crash. Kök neden muhtemelen backup/restore veya DB dosyası kopyalama sırasında `user_version` ile gerçek şema arasında oluşan uyuşmazlık.
- **Fix:** `AppDatabase.kt` içinde `SupportSQLiteDatabase.addColumnIfNotExists(table, column, definition)` — `PRAGMA table_info` ile kontrol edip yoksa ekler. Tüm ADD COLUMN migration'ları (1_2, 2_3, 3_4, 4_5, 5_6, 7_8) buna geçirildi.
- **Kural:** Yeni bir ADD COLUMN migration'ı yazarken ham `execSQL("ALTER TABLE...")` YAZMA — her zaman `addColumnIfNotExists()` kullan.

## 🆕 D202 (2026-07-07) Tuzakları

### Room Migration Index Adı Tuzağı (KRİTİK)
Migration'da elle `CREATE INDEX` yazarken ad, Room'un entity'den ürettiği adla (`index_<tablo>_<kolonlar>`) BİREBİR aynı olmalı.
- **Yaşanan:** MIGRATION_10_11 `idx_apps_appName` adıyla index açtı; entity `index_apps_appName` bekliyordu. v11'de sorun görünmedi (validation tetiklenmedi), v11→v12 migration'ı çalışınca `Migration didn't properly handle: apps` fatal'ı geldi.
- **Fix:** MIGRATION_11_12 içinde `DROP INDEX IF EXISTS idx_apps_*` + doğru adla yeniden oluşturma (onarım migration'ı).
- **Kural:** Yeni index migration'ı yazarken adı schema JSON'undan (app/schemas/) kopyala, uydurma.

### Firebase Null-Guard (skipGoogleServices build'leri)
`google-services.json` yokken `FirebaseApp.initializeApp()` null döner; sonrasındaki HER `getInstance()` çağrısı IllegalStateException ile ÇÖKER.
- **Yaşanan:** Açılışta `FirebaseCrashlytics.getInstance()` (AppOrganizerApp) + her UI etkileşiminde `Firebase.analytics` (AppAnalytics) crash.
- **Fix:** `AppOrganizerApp` firebaseApp null kontrolü; `AppAnalytics` nullable lazy + tüm event'ler `runCatching` no-op.
- **Kural:** Firebase'e dokunan HER yeni kod `FirebaseApp.getApps(ctx).isEmpty()` guard'ı ile yazılır.

### Windows KAPT Kilit Döngüsü
`kaptGenerateStubsDebugKotlin` "Unable to delete directory" hatası tek dizin temizliğiyle çözülmüyor — kilit `app\build` altında geziniyor (snapshot → tmp\kapt3 → kotlin\cacheable).
- **Çalışan çözüm:** `gradlew --stop` + java kill + **app\build ve build dizinlerini robocopy /MIR ile KOMPLE sil** + sıfırdan build.
- **Kalıcı çözüm adayı:** KAPT→KSP geçişi (ROADMAP K1) + Defender exclusion'ları.

---

## 📊 Metrik Hedefler (Kontrol → docs/PLAY_RELEASE_EVIDENCE_CHECKLIST.md)

Ürün metrikleri arşivlendi. Firebase event tanımları kod comentlerinde.

---

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

### Onboarding Adım Sırası (CLAUDE.md §7'de — 6 adım)
WELCOME → SET_LAUNCHER → THEME_SELECT → QUICK_SETTINGS → BROWSER_SELECT → DONE

### Room DB Versiyon
Mevcut v12. Migration şablonları ve geçmiş → docs/internal/archive_technical_details.md
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

## Compose "measure is called on a deactivated node" (EX03, 2026-07-19)
- Belirti: HorizontalPager + LazyVerticalGrid, rotasyon + hizli swipe kombinasyonunda IllegalArgumentException crash.
- Kok neden: Compose 1.7.x (BOM 2024.09.03) framework race — pager sayfayi deactivate ederken bekleyen remeasure sonradan tetikleniyor.
- COZUM: BOM 2024.12.01 (Kotlin 1.9.25 ile uyumlu, Kotlin 2.x GEREKMEZ). Kod workaround'lari (graphicsLayer icinde deferred state read, beyondViewportPageCount=1) tek basina YETMEZ ama frekansi dusurur — kalici birakildi.
- Gelecek BOM yukseltmesinde bu repro (rotasyon+swipe 5+ tekrar, gercek cihaz) yeniden kosulmali.
