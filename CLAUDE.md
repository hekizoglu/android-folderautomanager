# AppOrganizer — Claude Çalışma Talimatları

> **Meta:** ~380 satır · Son güncelleme: 2026-06-15 · Döngü logları → HISTORY.md · Mimari kararlar → LEARNINGS.md · Görevler → ROADMAP.md

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
- Her 18 döngüde: emülatörde tam test

### Her Görev Sonunda
1. Build al (`.\gradlew assembleDebug`)
2. Hata varsa düzelt
3. Commit + push
4. `python scripts/update_notebooklm.py`
5. Telegram'a rapor (bug fix: sebep+fix+sonuç / yeni özellik: hangi dosya / refactor: eski→yeni)
6. CLAUDE.md veya LEARNINGS.md güncelle (yeni tuzak/kural öğrenildiyse)

### CLAUDE.md Güncelleme Kuralı
Buraya eklenir: kalıcı kural, build hatası+çözümü, kritik mimari karar.
Buraya EKLENMEZ: döngü özeti (→ HISTORY.md), tek seferlik not, geçici durum.

### Döngü Sonu Özet Formatı
```
## Döngü [N] — [SAAT]
**Yapılanlar:** [dosya, ne değişti, neden]
**Agent:** [çalıştıysa: tür + görev + sonuç]
**CLAUDE.md/LEARNINGS.md:** [güncellendiyse ne eklendi]
**Sonraki:** [en öncelikli görev]
```

### Asla Yapma
- Küçük değişiklik için "onaylıyor musun?" deme — yap, test et, bildir
- Yarım bırakma
- Encoding bozukluğu — her zaman UTF-8 kaydet

---

## 4. Araçlar ve Servisler

### Build Komutları
```powershell
cd "c:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug

# Emülatör
$em = "C:\Android\Sdk\emulator\emulator.exe"
Start-Process $em -ArgumentList "-avd","Pixel6_API33","-no-snapshot-save"
Start-Process $em -ArgumentList "-avd","Xiaomi_HyperOS_API34","-no-snapshot-save"

# APK yükle
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
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
- Pre-commit hook: `.github/hooks/pre-commit` otomatik çalıştırır

### KeywordDatabase Duplicate Kategori
`mapOf()` içinde `CAT_x` iki kez tanımlanırsa ilk (daha kapsamlı) liste kaybolur.
- Her `CAT_x` tek `to` satırı olmalı, yeni keyword'leri mevcut listeye ekle.

### Encoding (Curly Quote + Bozuk UTF-8)
Edit tool bazen `"` `"` (0x201c/201d) yazar → "Expecting an expression" hatası.
- Fix: `python scripts/fix_encoding.py <dosya>`
- Kontrol: `xxd dosya | grep e280`

### Türkçe Locale
Arama/sıralama: `lowercase(Locale("tr"))` — `contains(ignoreCase=true)` Türkçe'de güvenilmez (I/İ/ı sorunu).

### Flow Sıcaklığı
Launcher kök akışları (`folders`/`allApps`) `SharingStarted.Eagerly` — `WhileSubscribed` ile dönüşte "Yükleniyor..." flaşı oluşur.

### Async İkon Yükleme
`produceState<ImageBitmap?>` + IO thread + ortak `iconCacheInternal` (LRU-200).
Cache key: `"${pkg}_${px}"` (ikon paketi varsa `+"_${iconPackPkg}"`). `initialValue = cache[key]` ile cache hit'te anında göster.

### Reaktif AppPrefs (Settings → Launcher)
`remember {}` ile okunan AppPrefs değerleri Settings'ten dönünce güncellenmiyor.
Pattern: `mutableStateOf` + `DisposableEffect(context)` + `OnSharedPreferenceChangeListener`.
Conditional `remember` kullanma — değeri composable dışına al.

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
│   └── usecase/classify/  # AppClassifier (3375 paket), KeywordDatabase (32 kategori)
├── data/
│   ├── local/       # AppDao, AppDatabase (Room v7)
│   ├── remote/      # BackupSyncService
│   └── repository/  # AppRepository
└── utils/           # AppPrefs, IconPackManager, ShortcutHelper, WidgetPrefs, WidgetHostManager
```

### Önemli Mimari Notlar
- **AppClassifier:** 3375 benzersiz paket, `exactMatchMap` + `KeywordDatabase` (32 kategori). Bilinmeyen → `CAT_OTHER` → DeepSeek LLM fallback (`CategoryLLMFallback.kt`)
- **Room DB:** v7 (18 yeni kategori eklendi)
- **Onboarding:** 14 adım (WELCOME→DONE), `AppPrefs.PREFS_NAME` + `KEY_ONBOARDING_DONE`
- **HomeScreen sayfalama:** 8 klasör/sayfa, `HorizontalPager`
- **Firebase Analytics:** Entegrasyon planlanıyor — `google-services.json` bekleniyor

### Özellik Durum Özeti
| Özellik | Durum |
|---------|-------|
| AppClassifier 3375 paket | ✅ |
| DeepSeek LLM fallback | ✅ |
| İkon pack desteği | ✅ |
| Widget desteği | ✅ |
| App shortcuts | ✅ |
| Favoriler + Son kullanılanlar | ✅ |
| Bildirim badge + metin | ✅ |
| Klasör özelleştirme (ad+emoji+renk) | ✅ |
| BackupWorker haftalık | ✅ |
| Firebase Analytics | ❌ bekliyor |
| DeepSeek API | ✅ `.env`'de |
| NotebookLM MCP | ✅ auth tamam |
| Telegram Bot | ✅ yeni token |
| Play Store AAB | ✅ v1.0.0 hazır |

### Play Store Bekleyenler
- [ ] Privacy Policy URL (GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli)
- [ ] Store listing görselleri
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks`

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

*Son güncelleme: 2026-06-15 — CLAUDE.md v4: ~%70 küçüldü, döngü logları HISTORY.md'ye, mimari notlar LEARNINGS.md'ye taşındı. Firebase Analytics planı eklendi.*
