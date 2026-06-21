# AppOrganizer — Claude Çalışma Talimatları

> **Meta:** ~390 satır · Son güncelleme: 2026-06-20 · Döngü logları → HISTORY.md · Mimari kararlar → LEARNINGS.md · Görevler → FİKİRLER.md (ROADMAP.md donduruldu)

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
3. ROADMAP.md oku — aktif sprint ve bekleyen görevler
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
- Git hook aktifleştirme: `git config core.hooksPath .githooks` — ilk kurulumda çalıştır (pre-commit check_duplicates.py için)

### Paralel Agent Kullanımı
Bağımsız araştırma/analiz görevlerinde tek mesajda birden fazla Agent çağrısı yap — bekleme süresini yarıya indirir.
Örnek: WebSearch + Explore aynı anda; DeepSeek + code-reviewer aynı anda.

### Sıralı (Ard Arda) Döngü Kuralları
- Ard arda çalışan döngülerde build sadece **son döngüde** yapılır
- Her hata/uyarıda anında agent ile online araştırma başlatılır (beklemeden)
- Her döngü sonunda HISTORY.md + ROADMAP.md güncellenir
- Her döngüde kullanıcıya detaylı değişiklik raporu verilir (dosya:satır + eski→yeni)

### Her Görev Sonunda
1. Build al (`.\gradlew assembleDebug`)
2. APK boyutunu logla: `(Get-Item app\build\outputs\apk\debug\app-debug.apk).length / 1MB` → harcananvakit.md BUILD satırına ekle
3. Hata varsa düzelt
4. Commit + push
5. `python scripts/update_notebooklm.py`
6. Telegram'a rapor (bug fix: sebep+fix+sonuç / yeni özellik: hangi dosya / refactor: eski→yeni)
7. HISTORY.md'ye döngü özeti ekle (3 satır — zorunlu)
8. CLAUDE.md veya LEARNINGS.md güncelle (yeni tuzak/kural öğrenildiyse)
9. **harcananvakit.md** — döngü için başlangıç/bitiş saati ve kategori logu ekle

### Dosya Güncelleme Kuralları

| Dosya | Ne zaman | İçerik |
|-------|----------|--------|
| **HISTORY.md** | **Her döngü sonunda** (zorunlu) | Yapılanlar / Bug / Sonraki — 3 satır |
| **ROADMAP.md** | **Her 6 döngüde** (build döngüsü) | Tamamlananları işaretle, testlerden gelen yeni görev ekle |
| **LEARNINGS.md** | DeepSeek/test/hata sonrası yeni bulgu | Tuzak, race condition, mimari karar — kayda değer |
| **CLAUDE.md** | 3+ tekrar veya ÖNCELİK:YÜKSEK | LEARNINGS'den promote — kalıcı kural |
| **FİKİRLER.md** | Her döngü sonunda yeni fikir/görev varsa | Madde ekle; **15+ puan alanları ROADMAP ⭐ bölümüne de yaz** |
| **ROADMAP.md** | Görev tamamlandığında | ✅ olan satırı **sil** → HISTORY.md Tamamlananlar Arşivi'ne taşı |

### FİKİRLER.md Puanlama Kuralı
Her döngü sonunda yeni fikir/görev FİKİRLER.md'ye eklenince puanla (agent ile veya manuel):
- **Kriterler (her biri 1-5):** Kullanıcı Değeri · Uygulanabilirlik · Bağımlılık Riski · Etki Alanı
- **15+ puan** → ROADMAP.md ⭐ Yüksek Puanlı bölümüne de ekle (puan + görev + durum)
- **10-14 puan** → FİKİRLER.md 🟡 Değerlendir olarak kalır
- **9-** → FİKİRLER.md ⏸ Beklet

### Görev Zorluk Puanı Kuralı
Her döngü başında yapılacak görev için **1-10** arası zorluk puanı ver, ardından çalış:
- **1-3 (Kolay):** Tek dosya, bilinen pattern — doğrudan yap
- **4-6 (Orta):** Çok dosya veya yeni mantık — dikkatli yap, yan etki kontrol et
- **7-8 (Yüksek):** Yeni sistem/API veya mimari değişiklik → **önce 2+ kaynaktan agent araştırması başlat, 2+ seçenek sun, kullanıcı onayı al**
- **9-10 (Kritik):** Veri kaybı/güvenlik riski → **3+ kaynak, Plan aşaması zorunlu, commit öncesi onay**

Puan yüksekse araştırma tamamlanmadan kod yazılmaz.

**CLAUDE.md'ye eklenir:** kalıcı kural, build hatası+çözümü, kritik mimari karar.
**CLAUDE.md'ye eklenmez:** döngü özeti (→ HISTORY.md), tek seferlik not, geçici durum.

### Döngü Sonu Özet Formatı
```
## Döngü [N] — [SAAT]
**Yapılanlar:** [dosya, ne değişti, neden]
**Agent:** [çalıştıysa: tür + görev + sonuç]
**CLAUDE.md/LEARNINGS.md:** [güncellendiyse ne eklendi]
**Sonraki:** [en öncelikli görev]
```

### Encoding Otomatik Tespit ve Düzeltme Kuralı
Bir dosyada Türkçe karakter bozukluğu (`Ã¶`, `Ä±`, `ÅŸ` vb.) tespit edilirse:
1. `PYTHONIOENCODING=utf-8 python scripts/fix_encoding.py <dosya>` — çalıştır
2. Bozukluk devam ediyorsa 2. deneme: `python -c "..."` ile TURKISH_DOUBLE_ENCODED tablosunu uygula
3. 3. denemede de çözülemediyse `COZULEMEYEN_SORUNLAR.md`'ye ekle ve kullanıcıya bildir
- PowerShell'de `Add-Content` kullanma (double-encode üretir) — her zaman `Write-Output | Out-File -Encoding utf8` veya Python `write_text(encoding='utf-8')` kullan
- PowerShell heredoc: `@'...'@` — kapatan `'@` mutlaka sıfır indent (satır başı) olmalı, `<<'EOF'` bash syntax PS5'te çalışmaz

### Asla Yapma
- Küçük değişiklik için "onaylıyor musun?" deme — yap, test et, bildir
- Yarım bırakma — başlanan her görev aynı döngüde tamamlanmalı; multi-dosya işlemler bölünmez
- Bir görevi "devam edeyim mi?" diye sorarak bırakma — tüm ilgili dosyaları aynı anda bitir
- Encoding bozukluğu — her zaman UTF-8 kaydet
- Onboarding'in son 3 adımı MUTLAKA CLASSIFY_MODE → SET_LAUNCHER → DONE sırasında olmalı — hiçbir değişiklik bu sırayı bozamaz. (D120'de SET_LAUNCHER en sona alındı — kullanıcı talebi)
- **Yan etki yaratma:** Bir dosyayı değiştirirken ilgili tüm dosyaları (ViewModel, Repository, Model, UI) önce oku. Yeni sabit/fonksiyon eklenince tüm kullanım noktaları güncellenmeli. Değişiklik sonrası build al — kırmızı alan kalmamalı.

### Değişiklik Güvenlik Protokolü
Her agent görevi sonunda (build almadan önce):
1. Değiştirilen dosyalardaki tüm import'ları doğrula (unresolved referans yok)
2. Eklenen her yeni sabit/fonksiyon — tüm kullanım noktaları güncellendi mi?
3. Silinen/yeniden adlandırılan her şey — eski adı kullanan başka dosya var mı? (`grep` ile kontrol)
4. Her görev sonunda `.\gradlew assembleDebug` — build hataları commit öncesi düzeltilmeli

---

## 4. Araçlar ve Servisler

### Build Komutları
```powershell
cd "c:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug        # Debug APK (veya: .\build.ps1)
.\gradlew bundleRelease        # Play Store AAB (imzalı — keystore.properties gerekli)
.\build.ps1                    # Akıllı build: cache+parallel, otomatik hata retry
.\build.ps1 -Clean             # Kilitli build dizinlerini temizleyip yeniden build

# Emülatör
$em = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\emulator\emulator.exe"
Start-Process $em -ArgumentList "-avd","Pixel6_AOSP33","-no-snapshot-save"

# APK yükle
$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity"
```

### Telegram Bot
- Token: `.env` → `TELEGRAM_BOT_TOKEN`
- Chat ID: `.env` → `TELEGRAM_CHAT_ID` (937179261)
- Bot: `@claudetestbotibm_bot`
```powershell
$t = $env:TELEGRAM_BOT_TOKEN; $c = $env:TELEGRAM_CHAT_ID
curl.exe -s -X POST "https://api.telegram.org/bot$t/sendDocument" -F "chat_id=$c" -F "caption=<mesaj>" -F "document=@app\build\outputs\apk\debug\app-debug.apk"
```

### NotebookLM MCP
- Auth: `npx notebooklm-mcp@latest auth` (2-4 haftada bir yenile)
- 2 notebook yüklü, `apporganizer` aktif, 20 tool
- Günde ~50 sorgu limiti

### DeepSeek API
- Model: `deepseek-chat` · Key: `.env` → `DEEPSEEK_API_KEY`

### Remote Ortam Notu
`dl.google.com` ve `api.telegram.org` bu ortamda engelli — build ve Telegram gönderimi yerel makinede yapılmalı.

### Zaman Loglama
`harcananvakit.md` — her döngüde başlangıç/bitiş saati ve kategori (BUILD/KOD/ORTAM/GİT/DÖKÜMAN) loglanır.
Her 6 saatte bir otomatik MD denetimi yapılır → Telegram'a rapor → **onay gelmeden değişiklik yapılmaz**.
Windows Defender exclusion (Admin PowerShell gerekli):
```powershell
Add-MpPreference -ExclusionPath "C:\Users\hekizoglu\Documents\AppOrganizer\app\build"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.android"
```

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
`mapOf()` duplicate key'de sessizce son entry'i kullanır, önceki kaybolur.
- Her commit öncesi: `python scripts/check_duplicates.py AppClassifier.kt`
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

---

## 6. Agent Stratejisi

| Tür | Ne Zaman |
|-----|---------|
| WebSearch | Yeni API, versiyon uyumu, derleme hatası |
| Explore | Dosya/sembol arama |
| Plan | Mimari karar |
| DeepSeek | Kod review, analiz |

### Model Katmanlama
| Görev | Model |
|-------|-------|
| Mimari, karmaşık analiz | Opus 4.6 |
| Kodlama, refactor | Sonnet 4.6 (varsayılan) |
| Lint, basit görevler | Haiku 4.5 |
| Doküman araştırması | Gemini via NotebookLM |

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
│   ├── launcher/    # HomeScreen, FolderTile, AllAppsDrawer, FolderSheet, HomeScreenComponents
│   ├── screens/     # AppListScreen, SettingsScreen, OnboardingScreen
│   └── theme/       # Theme.kt (turkuaz, DataStore reaktif)
├── domain/
│   ├── models/      # AppInfo, Category, AppFolder
│   └── usecase/classify/  # AppClassifier (3717 paket), KeywordDatabase (32 kategori)
├── data/
│   ├── local/       # AppDao, AppDatabase (Room v8)
│   ├── remote/      # BackupSyncService
│   └── repository/  # AppRepository
└── utils/           # AppPrefs, IconPackManager, ShortcutHelper, WidgetPrefs, WidgetHostManager
```

### Önemli Mimari Notlar
- **AppClassifier:** 3717 benzersiz paket, `exactMatchMap` + `KeywordDatabase` (32 kategori). Bilinmeyen → `CAT_OTHER` → DeepSeek LLM fallback (`CategoryLLMFallback.kt`)
- **Room DB:** v8 (v7→v8 boş migration, 2026-06-16)
- **Onboarding:** 16 adım (WELCOME → ... → THEME_SELECT → SET_LAUNCHER → CLASSIFY_MODE → DONE), `AppPrefs.PREFS_NAME` + `KEY_ONBOARDING_DONE`
- **HomeScreen sayfalama:** 8 klasör/sayfa, `HorizontalPager`
- **Firebase Analytics:** Entegrasyon planlanıyor — `google-services.json` bekleniyor

### Özellik Durum Özeti
| Özellik | Durum |
|---------|-------|
| AppClassifier 3717 paket | ✅ |
| DeepSeek LLM fallback | ✅ |
| İkon pack desteği | ✅ |
| Widget desteği | ✅ |
| App shortcuts | ✅ |
| Favoriler + Son kullanılanlar | ✅ |
| Bildirim badge + metin | ✅ |
| Klasör özelleştirme (ad+emoji+renk) | ✅ |
| BackupWorker haftalık | ✅ |
| Firebase Analytics | ❌ bekliyor |
| FCM Push (uzaktan DB güncelleme) | ✅ `AppFirebaseMessagingService.kt` (2026-06-18) |
| DeepSeek API | ✅ `.env`'de |
| NotebookLM MCP | ✅ auth tamam |
| Telegram Bot | ✅ yeni token |
| Play Store AAB | ✅ v1.0.0 hazır |

### Play Store Bekleyenler
- [ ] Privacy Policy URL (GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli)
- [ ] Store listing görselleri
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks`
- [ ] 🔴 **QUERY_ALL_PACKAGES beyan formu** — Play Store'a göndermeden önce Google'a "uygulama keşif/arama" kategoryasını beyan et (reject sebebi)
- [ ] Android 15 Edge-to-Edge geçişi — `WindowInsets` tüm ekranlarda
- [ ] Room `schemas/` klasörü git'e ekle (`room.schemaLocation` gradle'da tanımla)

---

## 8. Özellik Kontrol Listesi

| # | Özellik | Dosya | Son Durum |
|---|---------|-------|-----------|
| 1 | Turkuaz tema `#00897B`+`#26C6DA` | `Theme.kt` | ✅ 2026-06-13 |
| 2 | Launcher HOME+DEFAULT manifest | `AndroidManifest.xml` | ✅ satır 71-72 |
| 3 | RoleManager launcher dialog | `MainActivity.kt`, `SettingsScreen.kt` | ✅ |
| 4 | AllAppsDrawer blur(20.dp) | `AllAppsDrawer.kt` | ✅ |
| 5 | Icon async produceState + LRU-200 | `AppIconView.kt`, `FolderTile.kt` | ✅ |
| 6 | Haptic long-press | `HomeScreen.kt`, `FolderSheet.kt` | ✅ |
| 7 | Haptic tap (uygulama başlatma) | `HomeScreen.kt`, `AllAppsDrawer.kt` | ✅ |
| 8 | DockEditSheet | `DockEditSheet.kt` | ✅ |
| 9 | SettingsScreen varsayılan launcher butonu | `SettingsScreen.kt` | ✅ |
| 10 | Bildirim badge UI | `AppIconView.kt`, `FolderTile.kt` | ✅ |
| 11 | NotificationListenerService | `AppNotificationListenerService.kt` | ✅ |
| 12 | AppListScreen max 300 satır | `AppListScreen.kt` | ✅ 244 satır |
| 13 | Firebase Analytics | `LauncherApplication.kt` | ❌ bekliyor |

---

## 9. Güvenlik

- `.env`, `*.jks`, `keystore.properties`, `*.aab` asla git'e commit edilmez
- Telegram token, DeepSeek key plaintext CLAUDE.md'ye yazılmaz — sadece `.env`
- `google-services.json` repo'ya commit edilebilir (Firebase tasarımı)

---

*Son güncelleme: 2026-06-20 — CLAUDE.md v5: FCM push özelliği eklendi, AppClassifier merge conflict kuralı §5'e promote edildi, FİKİRLER.md sistemi oluşturuldu (ROADMAP.md donduruldu), Meta satırı güncellendi.*
