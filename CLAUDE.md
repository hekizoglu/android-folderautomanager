# AppOrganizer — Claude Çalışma Talimatları

> **Meta:** ~390 satır · Son güncelleme: 2026-07-07 (D210 sadeleştirme) · Döngü logları → HISTORY.md · Mimari kararlar/SOP → LEARNINGS.md · Görevler → FİKİRLER.md · Yol haritası → ROADMAP.md (15+ puan)

---

## 1. Hüseyin Kimdir?

- **Vizyon odaklı girişimci** — büyük resmi görür, detay takibini Claude'a bırakır
- **Az token, çok iş** — verimliliği önemser, gereksiz tekrardan nefret eder
- **Söylemek istemez, anlamamızı bekler** — "neden söylemek zorundayım?" tepkisi vermemeli
- **Telegram üzerinden takip** — her önemli adımda bildirim + APK gönder
- **Türkçe iletişim** — tüm yanıtlar Türkçe

---

## 2. Konuşma Açılışında Yap

1. `git pull origin main` — yerel/uzak senkronize et
2. LEARNINGS.md oku — mimari kararlar ve tuzaklar
3. FİKİRLER.md oku — aktif görevler ve fikir havuzu; ROADMAP.md oku — 15+ puan yüksek öncelikli yol haritası
4. `git log --oneline -10` — son değişiklikler
5. Özellik Kontrol Listesi'ni (§8) koda karşı doğrula, ❌ varsa o konuşmada düzelt

---

## 3. Temel Çalışma Kuralları

### Hata Çözüm Kuralı
- 1. denemede çözemezsen → HEMEN agent görevlendir
- Sıra: WebSearch → DeepSeek → Gemini → Claude Opus
- Minimum 3 deneme, her seferinde farklı kaynak

### Araştırma Önceliği
**ZORUNLU WebSearch:** Yeni API/kütüphane, versiyon uyumluluğu, derleme hatası, daha önce yapılmamış işlemler.
**OPSİYONEL:** Mevcut kodda bug fix, pure Kotlin/Compose değişikliği, refactor.

### Yeni Özellik = Ayarlar Kuralı
Her yeni UI özelliği SettingsScreen'den toggle ile kapatılabilir olmalı:
1. `AppPrefs.kt` → `KEY_xxx` + getter/setter (varsayılan: açık)
2. `SettingsScreen.kt` → ilgili bölüme toggle
3. Özellik kodunda `AppPrefs.getXxx(context)` ile oku

### Git Kuralları
- Tüm değişiklikler `main` branch — yeni branch açma
- Her döngü sonunda: commit + push + Telegram
- Her 6 döngüde: build + APK Telegram'a
- Her 18 döngüde: emülatörde tam test (HomeScreen klasör açılışı → AllAppsDrawer → Ayarlar → geri dön → bildirim badge görünümü)
- Rollback: kötü commit → `git revert HEAD~1 --no-edit && git push`; APK kaybı → Telegram bot geçmişinden son APK al
- Semantic versioning: MAJOR.MINOR.PATCH — `versionCode` ve `versionName` her release'te artır (`app/build.gradle.kts`)
- **Her build'de versiyon güncelle:** `versionCode` +1, `versionName` PATCH +1 — commit öncesi `app/build.gradle.kts` güncellenmeli
- Git hook aktifleştirme: `git config core.hooksPath .githooks` — ilk kurulumda çalıştır (pre-commit check_duplicates.py için)
- Rebase standardı: repo local `pull.rebase=true` ayarlı (Döngü 222) — akış her zaman `git fetch` → `git rebase origin/main` → `git push`; merge commit'i tercih etme

### Paralel Agent Kullanımı
Bağımsız araştırma/analiz görevlerinde tek mesajda birden fazla Agent çağrısı yap — bekleme süresini yarıya indirir.
Örnek: WebSearch + Explore aynı anda; DeepSeek + code-reviewer aynı anda.

### Sıralı (Ard Arda) Döngü Kuralları
- Ard arda çalışan döngülerde build sadece **son döngüde** yapılır
- Her hata/uyarıda anında agent ile online araştırma başlatılır (beklemeden)
- Her döngü sonunda HISTORY.md güncellenir; yeni görevler FİKİRLER.md'ye, 15+ puan alanlar ROADMAP.md'ye
- Her döngüde kullanıcıya detaylı değişiklik raporu verilir (dosya:satır + eski→yeni)

### Loop Context Optimizasyonu (Maliyet Azaltma)
- **Her 6 döngüde `/compact`** — context 150k'ya ulaşmadan önce sıkıştır; loop devam eder
- **Karmaşık görevlerde agent spawn** — loop iterasyonunda sadece analiz/oku; kod yazan ağır işi Sonnet agent'a devret (hafif iterasyonlar Haiku düzeyinde context tutar)
- **Gece döngüsü (00:00–07:00):** ScheduleWakeup interval 3300s (55dk) — gündüz 2580s (43dk)
- **Boşta döngü:** Yeni görev yoksa ScheduleWakeup 3600s — gereksiz tam iterasyon atla
- **Loop model:** Rutin iterasyonlarda Haiku (`--model haiku`) kullan; kod yazma gerektiren işlerde Sonnet'e geç
- **Compact eşiği:** Her 4 döngüde veya context 100k'yı geçince `/compact` — 6 döngü çok gevşek
- Mevcut kullanım: %76 loop, %20'si 150k+ context → model düşürme + sıkı compact ile maliyet yarıya iner

### Her Görev Sonunda
1. Build al (`.\gradlew assembleDebug`)
2. APK boyutunu logla: `(Get-Item app\build\outputs\apk\debug\app-debug.apk).length / 1MB` → harcananvakit.md BUILD satırına ekle
3. Hata varsa düzelt
4. Commit + push
5. Telegram'a rapor (bug fix: sebep+fix+sonuç / yeni özellik: hangi dosya / refactor: eski→yeni)
6. HISTORY.md'ye döngü özeti ekle (3 satır — zorunlu)
7. CLAUDE.md veya LEARNINGS.md güncelle (yeni tuzak/kural öğrenildiyse)
8. **harcananvakit.md** — döngü için başlangıç/bitiş saati ve kategori logu ekle

### Dosya Güncelleme Kuralları

| Dosya | Ne zaman | İçerik |
|-------|----------|--------|
| **HISTORY.md** | **Her döngü sonunda** (zorunlu) | Yapılanlar / Bug / Sonraki — 3 satır |
| **ROADMAP.md** | **Her 6 döngüde** (build döngüsü) | Tamamlananları işaretle, testlerden gelen yeni görev ekle |
| **LEARNINGS.md** | DeepSeek/test/hata sonrası yeni bulgu | Tuzak, race condition, mimari karar — kayda değer |
| **CLAUDE.md** | 3+ tekrar veya ÖNCELİK:YÜKSEK | LEARNINGS'den promote — kalıcı kural |
| **FİKİRLER.md** | Her döngü sonunda yeni fikir/görev varsa | Madde ekle; **15+ puan alanları ROADMAP ⭐ bölümüne de yaz** |
| **ROADMAP.md** | Görev tamamlandığında | ✅ olan satırı **ROADMAP'tan sil** → HISTORY.md'nin en üstüne `## Döngü N — [tarih] [başlık]` formatında ekle (zorunlu) |

### FİKİRLER.md Puanlama Kuralı
Her döngü sonunda yeni fikir/görev FİKİRLER.md'ye eklenince puanla (agent ile veya manuel):
- **Kriterler (her biri 1-5):** Kullanıcı Değeri · Uygulanabilirlik · Bağımlılık Riski · Etki Alanı
- **15+ puan** → ROADMAP.md ⭐ Yüksek Puanlı bölümüne de ekle (puan + görev + durum)
- **10-14 puan** → FİKİRLER.md 🟡 Değerlendir olarak kalır
- **9-** → FİKİRLER.md ⏸ Beklet
- **Yeniden Puanlama:** Hüseyin istediğinde veya 3+ döngü sonra bekleyen fikirleri yeniden puanla — rekabet ve tamamlanan fikirler puanı değiştirir

### AppOrganizer Dashboard Kuralı
- Dashboard (`AppOrganizerDashboardScreen.kt`) mevcut verileri aggregate eder: `UsageStatsHelper` + `InsightEngine` + Room applar + kategoriler
- Settings > "AppOrganizer Dashboard" → `Routes.DASHBOARD` yönlendirmesi
- Yeni istatistik eklemek için: `DashboardStats.compute()` içine alan ekle → `EfficiencyCard` veya yeni `@Composable` bileşen

### Görev Zorluk Puanı Kuralı
Her döngü başında yapılacak görev için **1-10** arası zorluk puanı ver, ardından çalış:
- **1-3 (Kolay):** Tek dosya, bilinen pattern — doğrudan yap
- **4-6 (Orta):** Çok dosya veya yeni mantık — dikkatli yap, yan etki kontrol et
- **7-8 (Yüksek):** Yeni sistem/API veya mimari değişiklik → **önce 2+ kaynaktan agent araştırması başlat, 2+ seçenek sun, kullanıcı onayı al**
- **9-10 (Kritik):** Veri kaybı/güvenlik riski → **3+ kaynak, Plan aşaması zorunlu, commit öncesi onay**

Puan yüksekse araştırma tamamlanmadan kod yazılmaz.

**CLAUDE.md'ye eklenir:** kalıcı kural, build hatası+çözümü, kritik mimari karar.
**CLAUDE.md'ye eklenmez:** döngü özeti (→ HISTORY.md), tek seferlik not, geçici durum.

### MD Denetim Raporu Kuralı ve Denetim İyileştirme Kuralı
Detaylı SOP → **LEARNINGS.md** "SOP — Nadiren Tetiklenen Prosedürler". Kısa özet: `MD_DENETIM_*.md` varsa maddeleri tek tek çöz, rapordan sil, HISTORY.md'ye taşı; 0 sonuçlu denetim döngüsünde audit.ps1'a yeni tespit kuralı ekle.

### Döngü Sonu Özet Formatı
```
## Döngü [N] — [SAAT]
**Yapılanlar:** [dosya, ne değişti, neden]
**Agent:** [çalıştıysa: tür + görev + sonuç]
**CLAUDE.md/LEARNINGS.md:** [güncellendiyse ne eklendi]
**Sonraki:** [en öncelikli görev]
```

### Encoding Bozukluğu Tespit Edilirse
`scripts/fix_encoding.py` çalıştır, 3 denemede çözülmezse `COZULEMEYEN_SORUNLAR.md`'ye ekle. Detaylı adımlar → LEARNINGS.md SOP bölümü.

### Asla Yapma
- Küçük değişiklik için "onaylıyor musun?" deme — yap, test et, bildir
- Yarım bırakma — başlanan her görev aynı döngüde tamamlanmalı; multi-dosya işlemler bölünmez
- Bir görevi "devam edeyim mi?" diye sorarak bırakma — tüm ilgili dosyaları aynı anda bitir
- Encoding bozukluğu — her zaman UTF-8 kaydet
- Onboarding sırası MUTLAKA WELCOME → THEME_SELECT → QUICK_SETTINGS → SET_LAUNCHER → DONE olmalı — hiçbir değişiklik bu sırayı bozamaz. (D201 radikal kesme; D233: Hüseyin'in talebiyle SET_LAUNCHER en sona alındı — tüm ayarlar bitmeden kalıcı launcher kararı dayatılmaz)
- **Yan etki yaratma:** Bir dosyayı değiştirirken ilgili tüm dosyaları (ViewModel, Repository, Model, UI) önce oku. Yeni sabit/fonksiyon eklenince tüm kullanım noktaları güncellenmeli. Değişiklik sonrası build al — kırmızı alan kalmamalı.

### Değişiklik Güvenlik Protokolü
Her agent görevi sonunda (build almadan önce): import doğrula, yeni sabit/fonksiyonun tüm kullanım noktalarını güncelle, silinen/yeniden adlandırılan şeyi grep ile ara. Detaylı adımlar → LEARNINGS.md SOP bölümü.

---

## 4. Araçlar ve Servisler

### Build Komutları
> Proje dizini: `c:\Users\huseyinekizoglu\android-folderautomanager`. `local.properties` yoksa `sdk.dir=` ekle.
> **google-services.json yokken:** `-PskipGoogleServices` flag'i ile build al.
```powershell
cd "c:\Users\huseyinekizoglu\android-folderautomanager"
.\gradlew assembleDebug        # Debug APK
.\gradlew bundleRelease        # Play Store AAB (imzalı — keystore.properties gerekli)

# Emülatör (AVD: Pixel6_API33, Xiaomi_HyperOS_API34)
$emu = "C:\Android\Sdk\emulator\emulator.exe"; & $emu -avd Pixel6_API33 -no-snapshot-save

# APK yükle
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.MainActivity"
```

### Telegram / AI Servisleri
- Telegram Bot: token/chat ID → `.env` (`TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`). Gönderim: `curl.exe -X POST "https://api.telegram.org/bot$t/sendDocument" -F "chat_id=$c" -F "document=@<apk>"`
- Lokal AI Gateway (öncelikli, ucuz): `http://localhost:20128/v1`, `.env` → `LOCAL_AI_KEY`, model `all99`. Offline ise DeepSeek API'ye (`deepseek-chat`, `.env` → `DEEPSEEK_API_KEY`) geç.
- Windows Defender exclusion (build kilidi önlemi, Admin gerekir): `app\build`, `.gradle`, `.android` klasörlerini ekle.

### Zaman Loglama
`harcananvakit.md` — her döngüde başlangıç/bitiş saati ve kategori (BUILD/KOD/ORTAM/GİT/DÖKÜMAN).

---

## 5. Kritik Mimari Tuzaklar (LEARNINGS'den Promote)

### Kotlin Smart Cast (`by` delegate)
`by produceState(...)` ile üretilen property `if (x != null)` bloğunda bile smart cast yapamaz.
- ❌ `bitmap = icon`
- ✅ `icon?.let { bmp -> Image(bitmap = bmp) }`

### Bağımlılık Uyumluluk Matrisi
| Güncelleme | Kontrol |
|-----------|---------|
| Compose BOM yükselt | BOM 2025+ → Kotlin 2.x gerekir |
| AGP yükselt | AGP 8.6 → Gradle 8.7+ |
| Kotlin 2.x | kapt çalışmaz → KSP geçişi yap |
| Coil 3.x | compileSdk 36 gerekir (AGP 8.6 max 35) |
| compileSdk yükselt | SDK 35: applicationInfo nullable |

### AppClassifier Duplicate
D115'ten itibaren paket→kategori haritası `assets/app_categories.json`'da (3702 paket). `AppClassifier.kt` artık bu JSON'u yükler.
- JSON'da duplicate key: JSON parser sessizce son entry'i kullanır, önceki kaybolur — aynı risk.
- Her commit öncesi: `python scripts/check_duplicates.py assets/app_categories.json`
- Temizlik: `python scripts/dedup_classifier.py`
- Pre-commit hook: `.githooks/pre-commit` otomatik çalıştırır (`git config core.hooksPath .githooks` ile aktifleştirilir)
- **Merge conflict durumunda:** `python scripts/dedup_classifier.py` ile remote+local birleştir, set dedup uygula (4+ kez tekrarlandı)

### KeywordDatabase Duplicate Kategori
`mapOf()` içinde `CAT_x` iki kez tanımlanırsa ilk (daha kapsamlı) liste kaybolur.
- Her `CAT_x` tek `to` satırı olmalı, yeni keyword'leri mevcut listeye ekle.

### Encoding (Curly Quote + Bozuk UTF-8)
Edit tool bazen `"` `"` (0x201c/201d) yazar → "Expecting an expression" hatası.
- Fix: `python scripts/fix_encoding.py <dosya>`
- Kontrol: `xxd dosya | grep e280`
- **KiloCode/audit.ps1 tuzağı (D182):** KiloCode audit.ps1'e otomatik kural eklerken description alanında curly quote veya em dash (`—`) kullanabilir → PS5.1 syntax hatası. Description alanlarında daima ASCII-safe string kullan. Test: `.\scripts\audit.ps1 -DryRun`.

### Unit Test — Türkçe Proje Yolu ClassNotFoundException
Proje yolu Türkçe karakter içeriyorsa (`Github Klasörleri`) `testDebugUnitTest` çalıştırmak ClassNotFoundException verir.
- **Neden:** Java test worker `@argfile` classpath dosyasını Windows platform encoding (CP1252) ile okur; UTF-8 `ö` (`c3 b6`) bozulur, path bulunamaz.
- **Fix:** Junction oluştur, testleri oradan çalıştır:
  ```powershell
  cmd /c "mklink /J C:\AppOrg `"C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager`""
  cd C:\AppOrg && .\gradlew testDebugUnitTest
  ```
- **Kalıcı çözüm:** Projeyi ASCII-only path'e taşı veya junction'ı kalıcı bırak.
- `android.overridePathCheck=true` bu sorunu ÇÖZMEZ — sadece AGP uyarısını kapatır.
- Hilt 2.52 ve `android.hilt.enableTransformForLocalTests=false` bu sorunu ÇÖZMEZ — farklı semptom.

### Türkçe Locale
Arama/sıralama: `lowercase(Locale("tr"))` — `contains(ignoreCase=true)` Türkçe'de güvenilmez (I/İ/ı sorunu).

### Flow Sıcaklığı
Launcher kök akışları (`folders`/`allApps`) `SharingStarted.Eagerly` — `WhileSubscribed` ile dönüşte "Yükleniyor..." flaşı oluşur.

### `derivedStateOf` Pattern
`derivedStateOf` ile türetilen değerleri doğrudan UI bağımlılığı olarak kullan; scroll ve offset gibi sık değişen girdilerde gereksiz recomposition'ı azaltır.

### `installSplashScreen()` Sırası
`installSplashScreen()` çağrısı `super.onCreate()` sonrası, `setContentView()` öncesi olmalı; splash yaşam döngüsü bu sıraya bağlıdır.

### Async İkon Yükleme
`produceState<ImageBitmap?>` + IO thread + ortak `iconCacheInternal` (LRU-200).
Cache key: `"${pkg}_${px}"` (ikon paketi varsa `+"_${iconPackPkg}"`). `initialValue = cache[key]` ile cache hit'te anında göster.

### Reaktif AppPrefs (Settings → Launcher)
`remember {}` ile okunan AppPrefs değerleri Settings'ten dönünce güncellenmiyor.
Pattern: `mutableStateOf` + `DisposableEffect(context)` + `OnSharedPreferenceChangeListener`.
Conditional `remember` kullanma — değeri composable dışına al.

### Build Cache Kilidi (Windows)
Gradle build dizini kilitlenince: `Get-Process java | Stop-Process -Force` → `Remove-Item -Recurse -Force app\build` → yeniden build.
`robocopy /MIR` ile `app\build` silinebilir (locked file'lar dahil). Tekrar: 3+ — kalıcı kural.

### Room Migration Şablonu
DB versiyonu artışında zorunlu adımlar:
1. `app/build.gradle.kts`'de `room.schemaLocation` tanımlı olmalı → `schemas/` klasörü git'e alınır
2. `Migration(vOld, vNew) { db -> db.execSQL("...") }` class oluştur (boş bile olsa)
3. `addMigrations(MIGRATION_x_y)` — `fallbackToDestructiveMigration()` kullanma (veri siler)
4. `MigrationTestHelper` ile test yaz

### Android Platform Uyumluluk Kuralları
- **Edge-to-Edge (Android 15 zorunlu):** `WindowCompat.setDecorFitsSystemWindows(window, false)` + tüm ekranlarda `WindowInsets` padding
- **Predictive Back (Android 13+):** `AndroidManifest.xml`'de `android:enableOnBackInvokedCallback="true"` + `BackHandler` gözden geçir
- **QUERY_ALL_PACKAGES:** Play Store'a göndermeden önce Google'a "uygulama keşif/arama" beyan formu zorunlu — eksikse APK reddedilir
- **NotificationListenerService (Android 14+):** `foregroundServiceType` ve runtime izin değişimleri — gerçek cihaz testi şart
- **Android 16 Dosya Erişimi:** `getExternalFilesDir` veya `Environment.getExternalStorageDirectory` kullanma — sadece `context.filesDir` (internal) veya SAF (`OpenDocumentTree`) kullan. Audit: `grep -r "getExternalFilesDir\|getExternalStorageDirectory" app/src/` ile tarandı, bu projede kullanılmıyor (D187).

---

## 6. Agent Stratejisi

### Orkestra Şefi Kuralı (ZORUNLU — her çalışmada uygulanır)
Ana Claude oturumu **orkestra şefidir** — kodu bizzat yazmak yerine ROADMAP.md'deki açık işleri paralel agent'lara böler, sonuçları merge eder, build/test doğrular, commit+push+Telegram ile kapatır:
1. Konuşma açılışında veya "roadmap devam et" gibi bir istekte ROADMAP.md'yi oku, cihaz/hesap gerektirmeyen en öncelikli 1-3 maddeyi seç.
2. Bağımsız maddeleri **paralel** Agent çağrılarıyla worktree izolasyonunda başlat (`isolation: "worktree"`); UX/ürün kararı gerektiren zor maddelerde `model: "fable"` (Fable 5) kullan, mekanik/orta işlerde Sonnet.
3. Agent'lar bittikçe worktree dallarını ana `main`'e merge et, conflict'leri çöz, `assembleDebug` + ilgili unit testleri çalıştır.
4. ROADMAP.md'den kanıtlanan maddeleri sil, HISTORY.md'ye Döngü girdisi ekle, versionCode/versionName bump et, commit+push, Telegram'a Türkçe özet gönder.
5. Cihaz/Play Console gerektiren maddelerde durma — bir sonraki uygun maddeye geç, engelleri COZULEMEYEN_SORUNLAR.md'ye not düş.

| Tür | Ne Zaman |
|-----|---------|
| WebSearch | Yeni API, versiyon uyumu, derleme hatası |
| Explore | Dosya/sembol arama |
| Plan | Mimari karar |
| DeepSeek | Kod review, analiz |

### Otomatik Model Seçimi Kuralı (ZORUNLU — 2026-07-07)
Her talimat/görev için zorluk puanına göre model OTOMATİK seçilir — Fable 5 en kıymetli kaynak, mekanik işe harcanmaz:

| Model | Tanım | Ne Zaman Kullan |
|-------|-------|-----------------|
| **Fable 5** (`claude-fable-5`) | EN GÜÇLÜ model (Mythos sınıfı, Opus'un üstü). Pahalı — kıymetli. | SADECE: orkestrasyon, mimari karar, çok-dosyalı kritik entegrasyon, zor debugging, plan/review |
| **Opus** (`claude-opus-4-8`) | Güçlü genel model | Karmaşık analiz, Fable gerektirmeyen zor görevler |
| **Sonnet** (`claude-sonnet-5`) | Dengeli kod modeli (agent varsayılanı) | Ekran/ViewModel yazımı, refactor, boilerplate — Agent tool `model: "sonnet"` ile spec verilerek devret |
| **Haiku** (`claude-haiku-4-5`) | Hızlı/ucuz | Lint, basit düzeltme, dosya tarama, rutin loop iterasyonu |
| **Lokal AI / DeepSeek** | `localhost:20128` (`all99`) / `deepseek-chat` | Araştırma, ikinci görüş, doküman analizi — ÖNCE lokal |

Uygulama: Zorluk 1-3 → Haiku/lokal · 4-6 → Sonnet agent'a spec'le devret · 7-10 → Fable planlar, Sonnet uygular, Fable entegre eder.
Örnek (D202): NotificationReportScreen+VM Sonnet agent'a yazdırıldı (~65k token Sonnet'te), Fable sadece navigasyon/DI entegrasyonu yaptı.

### Kurulu Agent'lar (`.claude/agents/`)
| Agent | Model | Görev |
|-------|-------|-------|
| `code-reviewer` | Sonnet 4.6 | Kotlin/Compose review |
| `android-builder` | Haiku 4.5 | assembleDebug + hata raporu |
| `deepseek-analyst` | Sonnet 4.6 | DeepSeek API analizi |

---

## 7. Proje

### Ne?
Android launcher — uygulamaları otomatik kategorilere göre klasörlere böler. "Invisible launcher" prensibi: kullanıcı Pixel Launcher'dan fark etmeden geçiş yapabilmeli.

### Temel Prensipler
- Turkuaz tema: primary `#00897B` (Teal 600), secondary `#26C6DA` (Cyan)
- Pixel Launcher klonu: transparent bg, frosted dock, Google clock widget
- İlk açılışta launcher dialog (RoleManager)
- Büyük dosyaları böl, tek sorumluluk

### Proje Yapısı
```
app/src/main/java/com/armutlu/apporganizer/
├── presentation/ui/
│   ├── launcher/    # HomeScreen, FolderTile, FolderScreen, AllAppsDrawer, HomeScreenComponents
│   ├── screens/     # AppListScreen, SettingsScreen, OnboardingScreen
│   └── theme/       # Theme.kt (turkuaz, DataStore reaktif)
├── domain/
│   ├── models/      # AppInfo, Category, AppFolder
│   └── usecase/classify/  # AppClassifier (3702 paket), KeywordDatabase (32 kategori)
├── data/
│   ├── local/       # AppDao, AppDatabase (Room v12)
│   ├── remote/      # BackupSyncService
│   └── repository/  # AppRepository, SearchRepository
└── utils/           # AppPrefs, IconPackManager, ShortcutHelper, WidgetPrefs, WidgetHostManager
```

### Önemli Mimari Notlar
- **AppClassifier:** 3702 benzersiz paket, `assets/app_categories.json` + `KeywordDatabase` (32 kategori). Bilinmeyen → `CAT_OTHER` → DeepSeek LLM fallback (`CategoryLLMFallback.kt`)
- **Room DB:** v12 (v11→v12: `notification_events` tablosu — Bildirim Analiz Raporu + eski `idx_apps_*` index adları `index_apps_*` olarak onarıldı, D202)
- **Bildirim Analizi:** `AppNotificationListenerService` her bildirimi loglar (paket+zaman, içerik YOK) → `NotificationAnalyzer` (çok konuşan/rahatsız eden/dikkat dağıtan) → `NotificationReportScreen` (Routes.NOTIFICATION_REPORT)
- **Haber Şeridi:** `HomeTickerRow` + `LauncherViewModel.tickerItems` (klasör istatistikleri + içgörüler + bildirim özeti); dokun→hedef açılır, kaydır→sonraki; `KEY_TICKER_ENABLED`
- **Tema:** Material You `DYNAMIC` Android 12+ default (`AppTheme.default()`); build'e `-PskipGoogleServices` verilirse Firebase null-guard'lı çalışır (çökmez)
- **Onboarding:** 5 adım (WELCOME → THEME_SELECT → QUICK_SETTINGS → SET_LAUNCHER → DONE), `AppPrefs.PREFS_NAME` + `KEY_ONBOARDING_DONE` (D233: launcher sorusu en sonda)
- **HomeScreen sayfalama:** 8 klasör/sayfa, `HorizontalPager`
- **Firebase:** Gerçek proje bağlı (`com-armutlu-apporganizer`) — Analytics + Crashlytics aktif, emülatörde doğrulandı (D205)
- **Birleşik arama:** Ana ekran tek arama çubuğu 4 kaynak grubunda sonuç gösterir (Uygulamalar/Klasörler/Kişiler/Dosyalar) — `SearchRepository` FTS5 (D207)

### Özellik Durum Özeti
| Özellik | Durum |
|---------|-------|
| AppClassifier 3702 paket | ✅ |
| DeepSeek LLM fallback | ✅ |
| İkon pack desteği | ✅ |
| Widget desteği | ✅ |
| App shortcuts | ✅ |
| Favoriler + Son kullanılanlar | ✅ |
| Bildirim badge + metin | ✅ |
| Klasör özelleştirme (ad+emoji+renk) | ✅ |
| BackupWorker haftalık | ✅ |
| Firebase Analytics + Crashlytics | ✅ gerçek proje bağlı (D205) |
| FCM Push (uzaktan DB güncelleme) | ✅ `AppFirebaseMessagingService.kt` (2026-06-18) |
| DeepSeek API | ✅ `.env`'de |
| Telegram Bot | ✅ yeni token |
| Play Store AAB | ✅ v1.0.0 hazır |

### Play Store Bekleyenler
- [ ] Privacy Policy URL (GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli)
- [ ] Store listing görselleri
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks`
- [ ] 🔴 **QUERY_ALL_PACKAGES beyan formu** — Play Store'a göndermeden önce Google'a "uygulama keşif/arama" kategoryasını beyan et (reject sebebi)
- [x] Android 15 Edge-to-Edge geçişi — `enableEdgeToEdge()` + `WindowCompat.setDecorFitsSystemWindows` MainActivity+LauncherActivity (D175)
- [x] Room `schemas/` klasörü git'te — `schemaLocation` tanımlı, `app/schemas/8.json` commit'li (D143)

---

## 8. Özellik Kontrol Listesi

| # | Özellik | Dosya | Son Durum |
|---|---------|-------|-----------|
| 1 | Turkuaz tema `#00897B`+`#26C6DA` | `Theme.kt` | ✅ 2026-06-13 |
| 2 | Launcher HOME+DEFAULT manifest | `AndroidManifest.xml` | ✅ satır 71-72 |
| 3 | RoleManager launcher dialog | `MainActivity.kt`, `SettingsScreen.kt` | ✅ |
| 4 | AllAppsDrawer blur(20.dp) | `AllAppsDrawer.kt` | ✅ |
| 5 | Icon async produceState + LRU-200 | `AppIconView.kt`, `FolderTile.kt` | ✅ |
| 6 | Haptic long-press | `HomeScreen.kt`, `FolderScreen.kt` | ✅ |
| 7 | Haptic tap (uygulama başlatma) | `HomeScreen.kt`, `AllAppsDrawer.kt` | ✅ |
| 8 | DockEditSheet | `DockEditSheet.kt` | ✅ |
| 9 | SettingsScreen varsayılan launcher butonu | `SettingsScreen.kt` | ✅ |
| 10 | Bildirim badge UI | `AppIconView.kt`, `FolderTile.kt` | ✅ |
| 11 | NotificationListenerService | `AppNotificationListenerService.kt` | ✅ |
| 12 | AppListScreen max 300 satır | `AppListScreen.kt` | ✅ 244 satır |
| 13 | Firebase Analytics + Crashlytics | `AppOrganizerApp.kt` | ✅ D205 |

---

## 9. Güvenlik

- `.env`, `*.jks`, `keystore.properties`, `*.aab` asla git'e commit edilmez
- Telegram token, DeepSeek key plaintext CLAUDE.md'ye yazılmaz — sadece `.env`
- `google-services.json` repo'ya commit edilebilir (Firebase tasarımı)

---

*Son güncelleme: 2026-07-07 — CLAUDE.md v6.0 (D210): time_token_analysis önerisiyle sadeleştirildi (441→391 satır) — nadiren tetiklenen SOP'lar (MD Denetim, Denetim İyileştirme, Encoding detayları, Değişiklik Güvenlik Protokolü) LEARNINGS.md'ye taşındı; bayat bilgiler düzeltildi (FolderSheet→FolderScreen, Room v8→v12, build yolları, Firebase Analytics/Crashlytics aktif durumu).*
