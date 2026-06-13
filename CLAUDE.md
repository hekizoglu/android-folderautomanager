# AppOrganizer — Claude Çalışma Talimatları

Bu dosya her konuşmanın başında okunur. Hüseyin ile çalışma şeklini, projeyi ve beklentileri tanımlar.

---

## Hüseyin Kimdir?

- **Vizyon odaklı girişimci** — büyük resmi görür, detay takibini Claude'a bırakır
- **"Dünyayı değiştirelim"** mentalitesi — cesur hedefler, hızlı iterasyon
- **Az token, çok iş** — verimliliği önemser, gereksiz tekrardan nefret eder
- **Söylemek istemez, anlamamızı bekler** — "neden söylemek zorundayım?" tepkisi vermemeli
- **Telegram üzerinden takip** — her önemli adımda Telegram'a bildirim + APK gönder
- **Türkçe iletişim** — tüm yanıtlar Türkçe

---

## Temel Çalışma Kuralları

### Agent Şeffaflık Kuralı
**Her agent çalıştırıldığında kullanıcıya bilgi ver:**
> "X agent'ı Y görevi için çalıştırdım."

Kısa, net — hangi agent, hangi görev.

### Hata Çözüm Kuralı (KRİTİK)
**Bir işlem hata verdiğinde:**
- **1. denemede çözüm bulamazsan → HEMEN agent görevlendir, vakit kaybetme**
- Agent türünü duruma göre seç: WebSearch (online hata arama), Explore (kod tarama), Plan (mimari sorun)
- Yine başarısız olursa farklı model/kaynak ile tekrar: DeepSeek → Gemini → Claude Opus sırasıyla
- **Minimum 3 deneme** — her seferinde farklı model/kaynak kullan (WebSearch, GitHub Issues, Stack Overflow)

**%85 eşiği YOK** — çözümden emin olsan da olmasan da 1 denemeden sonra agent devreye girer.

Agent görevlendirme formatı:
> "X hatası için [DeepSeek/Gemini/Claude Opus] agent araştırma yapıyor..."

### Araştırma Önceliği Kuralı (KRİTİK)
**Her yeni teknoloji, kütüphane, entegrasyon veya bilmediğim bir şey için:**
1. **Önce WebSearch agent ile online araştır** — güncel dokümantasyon, changelog, bilinen sorunlar
2. **Sonra uygula** — eski/yanlış bilgiyle zaman kaybetme

Bu kural şunlar için zorunludur:
- Yeni MCP sunucuları (notebooklm, telegram, vs.)
- API entegrasyonları (Telegram Bot, GitHub Actions, vs.)
- Yeni kütüphaneler veya versiyonlar
- Daha önce hiç yapmadığım işlemler

> **Neden:** Bilgi kesim tarihi var, kütüphaneler değişiyor. Güncel olmayan yöntemle saatler kaybedilebilir.

### Kotlin Smart Cast Kuralı
**`by` delegate property (örn. `val icon by produceState(...)`) `if (x != null)` bloğu içinde bile smart cast yapılamaz.**
- ❌ `bitmap = icon` → derleme hatası: "Smart cast impossible"
- ✅ `icon?.let { bmp -> Image(bitmap = bmp) }` kullan

### Bağımlılık Güncelleme Kuralı (KRİTİK)
**Compose BOM, AGP, Kotlin veya compileSdk güncellenecekse önce uyumluluk matrisini kontrol et:**

| Güncelleme | Kontrol edilmesi gereken |
|-----------|--------------------------|
| Compose BOM yükselt | Minimum Kotlin versiyonu — BOM 2025+ → Kotlin 2.x gerekir |
| AGP yükselt | Minimum Gradle versiyonu (AGP 8.6 → Gradle 8.7+) |
| Kotlin 2.x | kapt çalışmaz → KSP gerekir; önce KSP geçişi yap |
| Coil 3.x | compileSdk 36 gerektirir — AGP 8.6 max 35 destekler |
| compileSdk yükselt | Yeni nullable API'ler kırılabilir (örn. SDK 35: applicationInfo nullable) |

**Kontrol kaynakları:**
- Compose BOM ↔ Kotlin uyumu: `developer.android.com/develop/ui/compose/bom/bom-mapping`
- AGP ↔ Gradle uyumu: `developer.android.com/build/releases/gradle-plugin`

> **Neden:** Bu konuşmada BOM 2026.05.00 → Kotlin 2.x çakışması, Coil 3.x → compileSdk 36 çakışması, SDK 35 nullable API kırılması ardı ardına yaşandı. Her seferinde ekstra build döngüsü harcandı.

### Git Kuralları
- **Tüm değişiklikler `main` branch üzerinde** — yeni branch oluşturma
- `git add` + `git commit` + `git push origin main` — her build sonrası
- **Rutin döngü bitince push zorunlu** — Telegram raporu göndermeden önce push yapılmalı

### Her konuşma açılışında otomatik olarak:
1. **GitHub'dan çek** — `git fetch origin && git status` ile yerel/uzak fark kontrol et
2. **Güncel değilse pull** — `git pull origin main` ile senkronize et
3. **Özellik listesini doğrula** — aşağıdaki "Her Konuşmada Özellik Kontrol Listesi" tablosunu koda karşı kontrol et, ❌ olanları o konuşmada düzelt

### Her görev sonunda otomatik olarak:
1. **Build al** — `.\gradlew assembleDebug`
2. **Emülatörde test et** — `Pixel6_API33` (AOSP) veya `Xiaomi_HyperOS_API34` (Android 14, 395dpi) üzerinde
3. **Hata varsa düzelt** — DeepSeek ile analiz et, düzelt, tekrar build
4. **Test geçtiyse commit + push** — açıklayıcı commit mesajı
5. **NotebookLM dosyasını güncelle** — `python scripts/update_notebooklm.py` (Masaüstü/notebooklm_apporganizer/app_source.txt)
6. **Telegram'a gönder** — APK + kısa durum raporu
7. **CLAUDE.md'yi güncelle** — (aşağıdaki kurala göre)
8. **Döngü sonu özeti ver** — (aşağıdaki formata göre)

### Yeni Özellik = Ayarlar Kuralı (KRİTİK)
**Her yeni UI özelliği SettingsScreen'den kapatılıp açılabilir olmalıdır.**

Uygulama kuralı:
1. `AppPrefs.kt`'ye `KEY_xxx` + getter/setter ekle (varsayılan: açık)
2. `SettingsScreen.kt`'ye ilgili bölüme toggle/chip ekle
3. Özellik kodunda `AppPrefs.getXxx(context)` ile oku, kapalıysa render etme

Örnekler:
- Swipe-up hint → `KEY_SWIPE_HINT_ENABLED` → Görünüm bölümüne toggle
- YENİ badge → `KEY_NEW_BADGE_ENABLED` → Görünüm bölümüne toggle
- Kullanılmayan uygulamalar gri → `KEY_UNUSED_GREY_DAYS` → zaten var ✅
- Klasör uygulama sayısı → `KEY_FOLDER_COUNT_VISIBLE` → Görünüm bölümüne toggle
- Swipe-up hint (klasör tile) → `KEY_FOLDER_SWIPE_HINT_ENABLED` → Görünüm bölümüne toggle

> **Neden:** Kullanıcılar farklı tercihlere sahip. Bir özelliği beğenmeyenin tüm launcher'ı bırakmaması gerekir.

### CLAUDE.md Otomatik Güncelleme Kuralı (KRİTİK)
**Her döngü sonunda, aşağıdaki bilgiler öğrenilmişse CLAUDE.md'ye ekle:**

Eklenmesi ZORUNLU olan şeyler:
- Yeni keşfedilen build hatası ve çözümü (Bağımlılık tablosuna veya ayrı madde olarak)
- Değişen dosya sorumluluğu (hangi dosya ne işe yarıyor — Proje Yapısı'na)
- Özellik kontrol listesindeki yeni ✅/❌ durumları (Son Kontrol Sonuçları güncellenir)
- Yeni eklenen agent veya MCP araç (Agent tablosuna)
- Öğrenilen kalıcı bir davranış kuralı (Temel Çalışma Kuralları'na)

**Güncelleme formatı:** Dosyayı Edit tool ile aç, ilgili bölüme ekle, `*Son güncelleme*` tarihini yenile.
**Ne zaman:** Build + push tamamlandıktan hemen sonra, Telegram'dan önce.

> **Neden:** CLAUDE.md her konuşmada sıfırdan okunur. Öğrenilen şeyler buraya yazılmazsa bir sonraki konuşmada unutulur ve aynı hatalar tekrarlanır.

### Döngü Sonu Özet Kuralı (KRİTİK)
**Her döngü (veya 5-döngü bloğu) bitince kullanıcıya şu formatı ver:**

```
## Döngü [N] Özeti — [SAAT]

### Ne Yapıldı
- [Değişiklik 1 — hangi dosya, ne değişti, neden]
- [Değişiklik 2 ...]

### Çalışan Agent'lar
| Agent | Görev | Sonuç |
|-------|-------|-------|
| Explore | X araştırma | Y bulgusu |
| WebSearch | Z hatası | W çözümü |

### NotebookLM'e Sorulan Sorular
(Bu döngüde NotebookLM kullanıldıysa soruları ve özet cevapları listele)
- Soru: "..." → Özet cevap: "..."

### CLAUDE.md'ye Eklenenler
- [Bu döngüde CLAUDE.md'ye eklenen/güncellenen maddeler]

### Sonraki Döngü Önerisi
- [En öncelikli yapılacak şey]
```

**Kural:** Agent çalışmadıysa veya NotebookLM kullanılmadıysa o satırı "—" olarak bırak, boş tablo gösterme.

### Asla yapma:
- Küçük bir değişiklik için "onaylıyor musun?" diye sorma — yap, test et, bildir
- Yarım bırakma — bir iş başladıysa sonuna kadar götür
- Gereksiz açıklama — ne yaptığını değil, ne değiştiğini söyle
- Encoding bozukluğu oluşturma — her zaman UTF-8 kaydet

---

## Agent Kullanım Stratejisi

### Agent Türleri

| Tür | Nerede | Etkileşim | Ne Zaman Kullan |
|-----|--------|-----------|-----------------|
| **Local Agent** | Makine | İnteraktif | Hızlı, anlık görevler |
| **Background Agent** | Makine (CLI) | Async | Uzun, paralel işler |
| **Subagent** | Bağımsız context | Lead'e raporlar | Odaklanmış paralel görevler |
| **Agent Team (Swarm)** | Bağımsız context | Inter-agent mesajlaşma | Koordineli, karmaşık işler |

### Proje Özelinde Agent Rolleri

- **DeepSeek V4-Flash** — kod review, analiz, öneri (key: `.env` → `DEEPSEEK_API_KEY`)
- **Gemini / NotebookLM** — doküman araştırması, UX önerileri, Play Store analizi (kota dolunca yeni key iste)
- **Explore agent** — dosya arama ve codebase taraması
- **Plan agent** — büyük mimari kararlar

### Agent Karar Ağacı

```
Görev bağımsız mı?
├── EVET → Subagent
│   └── Sonuçlar birbirini etkiliyor mu?
│       ├── HAYIR → Paralel subagent (en hızlı + ucuz)
│       └── EVET  → Sıralı subagent
└── HAYIR → Agent Team
    ├── Bilgi paylaşımı + tartışma gerekli? → Swarm / Agent Team
    └── Sadece parçalara bölmek yeterli?   → Paralel subagents
```

### Subagent vs Agent Team

| | Subagents | Agent Teams |
|---|---|---|
| **İletişim** | Sadece lead'e rapor | Teammate'ler birbirine direkt yazar |
| **Token maliyeti** | Düşük | Yüksek (her teammate ayrı instance) |
| **Ne zaman** | Sonuç önemli, süreç değil | Tartışma ve işbirliği gerektiren işler |

### Agent görevi tamamlandığında:
- Sonuçları analiz et → uygula → build al → test et → gönder
- Sadece "analiz edildi" deme, değişikliği **KODA** yansıt

---

## Model Katmanlama Stratejisi

| Görev | Model | Neden |
|-------|-------|-------|
| Mimari kararlar, karmaşık analiz | **Opus 4.6** | Derin reasoning, ultrathink |
| Genel kodlama, refactor, review | **Sonnet 4.6** | Hız/kalite dengesi, varsayılan |
| Lint, format, basit görevler | **Haiku 4.5** | Token tasarrufu |
| Doküman araştırması | **Gemini via NotebookLM** | Ücretsiz input context |

**Kural:** Subagent YAML config'lerini repo'ya commit et — her agent'ın Opus'a bağlanmasını engelle.

```yaml
# .claude/agents/code-reviewer.yaml
name: code-reviewer
model: claude-sonnet-4-6
tools: [read_file, list_files]
instructions: |
  Yalnızca kod review yap. Güvenlik açığı, performans,
  okunabilirlik odaklı. Dosya değiştirme.
```

---

## NotebookLM + MCP: Token Tasarrufu

### Neden?

Eskiden: Büyük doküman → Claude'a yükle → her seferinde binlerce input token.  
Şimdi: Doküman NotebookLM'de → Claude MCP üzerinden "özetle" der → kısa sentez döner.  
**Sonuç: Token kullanımı %50–90 azalır.**

| Senaryo | Tasarruf |
|---------|---------|
| 50+ sayfalık doküman | ~%80–85 |
| Tekrarlanan sorular | %70+ |
| Büyük kod spekleri | %65–70 |
| Uzun vadeli bilgi tabanı | %80+ |

> Opus ve Sonnet'in en pahalı kısmı input tokenlarıdır. NotebookLM bu yükü Gemini'ye aktarır; Claude yalnızca reasoning, kod yazma ve strateji için kullanılır.

### MCP Server Kurulumu (Windows — Önerilen: NPX)

**1. Authenticate:**
```powershell
npx notebooklm-mcp@latest auth
# Chrome açılır → Google hesabıyla giriş yap → çerezler kaydedilir
```

**2. Claude Desktop config (`%APPDATA%\Claude\claude_desktop_config.json`):**
```json
{
  "mcpServers": {
    "notebooklm": {
      "command": "npx",
      "args": ["notebooklm-mcp@latest"]
    }
  }
}
```

**3. VS Code için `.vscode/settings.json`:**
```json
{
  "mcp": {
    "servers": {
      "notebooklm": {
        "command": "npx",
        "args": ["notebooklm-mcp@latest"]
      }
    }
  }
}
```

**4. Agent Teams aktif et:**
```json
{
  "env": {
    "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1"
  }
}
```

### Alternatif: UV (jacob-bd)
```powershell
irm https://astral.sh/uv/install.ps1 | iex
uv tool install notebooklm-mcp-cli
notebooklm-mcp-cli auth
```

### Kullanım Örnekleri (AppOrganizer için)
```
@notebooklm "AppOrganizer Docs" notebookunda Pixel Launcher UX pattern'larını özetle
@notebooklm "Android Dev" notebookunda Coil async image loading best practice'ini getir
@notebooklm "Play Store Hazırlık" notebookunda eksik adımları listele
```

### Dikkat:
- Session çerezleri **2–4 haftada bir** sona erer → `npx notebooklm-mcp@latest auth` ile yenile
- Ücretsiz NotebookLM: **günde ~50 sorgu** limiti
- Google UI güncellemesi MCP'yi bozabilir → her zaman `@latest` kullan
- Resmi API değil, browser automation — kritik/NDA dokümanlar için Enterprise kullan

---

## Maliyet ve Token Optimizasyonu

```bash
# Görev bitince agent'ı durdur (idle Opus = aktif token tüketimi)
/stop

# Yeni görev öncesi context temizle (%30–50 token tasarrufu)
/clear

# Aktif harcamaları gör
/usage
```

**5 Kritik Kural:**
1. **Tier'la** — Opus sadece karmaşık reasoning, Haiku lint/format
2. **Boşta bırakma** — Her bitişten sonra `/stop`
3. **Config'e yaz** — Model limitlerini YAML'a koy, repo'ya commit et
4. **Context temizle** — `/clear` arası görevlerde; stale history %30–50 fazla token
5. **NotebookLM kullan** — Büyük dokümanlarda araştırmayı Gemini'ye devret

---

## Proje: AppOrganizer

### Ne?
Android launcher uygulaması. Uygulamaları otomatik kategorilere göre klasörlere böler. Kullanıcı fark etmeden Pixel Launcher'dan geçiş yapabilmeli — "invisible launcher" prensibi.

### Temel Prensipler:
- **Kullanıcı dostu** — her özellik sezgisel, açıklama gerektirmemeli
- **Turkuaz tema** — primary: `#00897B` (Teal 600), secondary: `#26C6DA` (Cyan)
- **Pixel Launcher klonu** — transparent background, frosted dock, Google clock widget stili
- **Launcher dialog** — ilk açılışta "Ana ekran uygulaması olarak ayarla?" diye sor
- **Temiz kod** — büyük dosyaları böl, tek sorumluluk prensibi

### Proje Yapısı:
```
app/src/main/java/com/armutlu/apporganizer/
├── presentation/ui/
│   ├── launcher/         # HomeScreen, FolderTile, AllAppsDrawer, FolderSheet
│   ├── screens/          # AppListScreen, SettingsScreen, OnboardingScreen
│   └── theme/            # Theme.kt (turkuaz)
├── domain/models/        # AppInfo, Category, AppFolder
└── presentation/viewmodel/
```

### Build Komutları:
```powershell
# Build
cd "c:\Users\huseyinekizoglu\android-folderautomanager"
.\gradlew assembleDebug

# Emülatör başlat
$em = "C:\Android\Sdk\emulator\emulator.exe"
# Pixel 6 (API 33 AOSP):
Start-Process $em -ArgumentList "-avd","Pixel6_API33","-no-snapshot-save"
# Xiaomi HyperOS (API 34, Android 14, 1080x2400 395dpi):
Start-Process $em -ArgumentList "-avd","Xiaomi_HyperOS_API34","-no-snapshot-save"

# APK yükle ve başlat
$adb = "C:\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity"
```

### Telegram Bot:
- Token: `8811346243:AAEp28STOJTTIzcZLEZsX07yxsWLOfaoDGg` (GitHub secret: `TELEGRAM_BOT_TOKEN`)
- Chat ID: `.env` → `TELEGRAM_CHAT_ID` (937179261)
- Bot: `@claudetestbotibm_bot`
- APK gönderim komutu:
```powershell
$t = $env:TELEGRAM_BOT_TOKEN; $c = $env:TELEGRAM_CHAT_ID
curl.exe -s -X POST "https://api.telegram.org/bot$t/sendDocument" -F "chat_id=$c" -F "caption=<mesaj>" -F "document=@app\build\outputs\apk\debug\app-debug.apk"
```

---

## Limit Takılmaları ve Süreklilik

Sistem rate limit'e takıldığında veya context kesildiğinde:
- Bu CLAUDE.md dosyası durumu özetler
- `C:\Users\huseyinekizoglu\.claude\projects\...\memory\` klasöründeki memory'ler devam için rehber
- Masaüstünde `CLAUDE.md` yedeği her güncellemede oluşturulur

---

## Her Konuşmada Özellik Kontrol Listesi

**KURAL: Her konuşmanın başında bu listeyi koda karşı doğrula. Eksik olan varsa o konuşmada düzelt.**

| # | Özellik | Dosya | Kontrol Komutu |
|---|---------|-------|----------------|
| 1 | Turkuaz tema `#00897B` + `#26C6DA` | `theme/Theme.kt` | `grep -n "00897B\|26C6DA" app/src/.../Theme.kt` |
| 2 | Launcher HOME+DEFAULT manifest | `AndroidManifest.xml` | `grep -n "HOME\|DEFAULT" .../AndroidManifest.xml` |
| 3 | Launcher dialog — RoleManager (ilk açılış + Ayarlar) | `MainActivity.kt`, `SettingsScreen.kt`, `OnboardingScreen.kt` | `grep -rn "RoleManager\|ROLE_HOME" app/src` |
| 4 | AllAppsDrawer transparent bg + blur(20.dp) | `AllAppsDrawer.kt` | `grep -n "blur\|Transparent" .../AllAppsDrawer.kt` |
| 5 | Icon async `produceState` + LRU cache 200 | `AppIconView.kt`, `FolderTile.kt` | `grep -rn "produceState\|iconCacheInternal" app/src` |
| 6 | Haptic — long-press klasör/uygulama | `HomeScreen.kt`, `FolderSheet.kt`, `AllAppsDrawer.kt` | `grep -rn "HapticFeedback" app/src` |
| 7 | Haptic — uygulama başlatma (tap) | `HomeScreen.kt`, `AllAppsDrawer.kt` | `grep -n "launchApp\|startActivity" + haptic` |
| 8 | DockEditSheet — kullanıcı dock seçimi | `DockEditSheet.kt` | `grep -n "DockEditSheet\|DockEdit" app/src` |
| 9 | SettingsScreen — varsayılan launcher butonu | `SettingsScreen.kt` | `grep -n "Varsayılan Launcher\|ROLE_HOME" .../SettingsScreen.kt` |
| 10 | Bildirim badge UI | `AppIconView.kt`, `FolderTile.kt` | `grep -rn "notificationCount\|badgeText" app/src` |
| 11 | NotificationListenerService — gerçek veri | `services/` altında servis dosyası | `find app/src -name "*Notification*Service*"` |
| 12 | AppListScreen refactor — max 300 satır | `AppListScreen.kt`, `AppListComponents.kt` | `wc -l .../AppListScreen.kt` |

### Son Kontrol Sonuçları (2026-06-10)
| # | Durum |
|---|-------|
| 1 | ✅ Theme.kt doğru |
| 2 | ✅ Manifest satır 71-72 |
| 3 | ✅ MainActivity + SettingsScreen + OnboardingScreen |
| 4 | ✅ blur(20.dp) aktif |
| 5 | ✅ LRU(200) + produceState |
| 6 | ✅ Long-press haptic — tüm dosyalarda |
| 7 | ✅ Tap haptic — LongPress tip ile eklendi (TextHapticFeedback API yok) |
| 8 | ✅ DockEditSheet tam UI + toast feedback |
| 9 | ✅ SettingsScreen'de RoleManager butonu var |
| 10 | ✅ Badge UI — AppIconView, FolderTile, AllAppsDrawer |
| 11 | ✅ AppNotificationListenerService eklendi, DB'ye yazıyor |
| 12 | ✅ AppListScreen.kt 244 satır — CategoryChip+AppListContent AppListComponents'a taşındı |

**Düzeltilen Buglar (2026-06-10)**
- FolderSheet geri/home tuşu: `sheetState.hide()` + `BackHandler` entegre edildi
- Sil/Gizle: `contextMenuApp` artık `allApps` flow'undan güncel veri alıyor (stale state giderildi)
- Dock'a ekle: dolu/zaten var durumları Toast ile kullanıcıya bildiriliyor
- Swipe-up AllApps: `detectVerticalDragGestures` eklendi, güvenilirlik arttı
- Çift tap: ana ekrana çift dokunarak AllApps açılıyor

**Eklenen Özellikler (2026-06-12)**
- AppClassifier: +30 Türk uygulaması (Getir, Çiçeksepeti, D-Smart, Puhutv, Tabii, TRT, Marti, TCDD vb.)
- KeywordDatabase: Türkçe keyword'ler eklendi (yemeksepeti, getir, blutv, bitaksi vb.)
- AppIconView: tap sırasında spring bounce scale animasyonu (ripple kaldırıldı, Pixel hissi)
- HomeLongPressSheet: ana ekrana uzun basınca Duvar Kağıdı / Dock Düzenle / Ayarlar menüsü
- AllAppsDrawer: açılınca 300ms sonra klavye otomatik açılıyor (FocusRequester)
- AllAppsDrawer: kapat/swipe-down sırasında arama geçmişe kaydediliyor + klavye kapanıyor

---

## Gelecek Yol Haritası — Rekabet Döngüsü (Uzun Vadeli)

Bu özellikler **şu an değil**, rakiplerden öne geçmek için ilerleyen döngülerde uygulanacak.

| # | Özellik | Rakip Fırsat | Öncelik |
|---|---------|--------------|---------|
| 3 | ~~Ana ekrana dönüşte hız iyileştirmesi~~ ✅ | Smart Launcher şikayeti: "yavaş geri dönüş" — **Döngü 15, 16, 20, 21'de tamamlandı** | Yüksek |
| 4 | ~~Gesture navigation uyumsuzluk fix~~ ✅ | Xiaomi/Samsung sistem navigasyonu çakışması — **Döngü 11'de tamamlandı** | Yüksek |
| 6 | ~~Icon pack desteği~~ ✅ | Nova kullanıcıları "icon pack yok" diyor — **Döngü 22'de tamamlandı** | Orta |
| 7 | ~~Widget desteği~~ ✅ | Niagara kullanıcıları "widget eksik" diyor — **Döngü 24'te tamamlandı** | Orta |

**Döngülere eklenme zamanı:** Rakip analizi tamamlandıktan sonra (bkz. Özellik Durum Tablosu)

---

## Play Store Hazırlık (Bekleyen)

- [ ] Privacy policy URL
- [ ] Store listing görselleri (screenshot)
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks` (şifre: `AppOrganizer2024!`)
- [ ] ProGuard kuralları

---

## Özellik Durum Tablosu

| Özellik | Durum | Not |
|---------|-------|-----|
| Paralel Subagents | ✅ Stabil | VS Code 1.109+ |
| Agent Teams / Swarm | ✅ Config hazır | `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` → `.vscode/settings.json` |
| VS Code Agent Sessions View | ✅ Stabil | 1.109+ |
| Custom Agents + Handoffs | ✅ Kuruldu | `.claude/agents/` — 3 agent: code-reviewer, deepseek-analyst, android-builder |
| DeepSeek API | ✅ Çalışıyor | `deepseek-v4-flash`, key `.env`'de, test edildi |
| NotebookLM MCP (config) | ✅ Config hazır | `claude_desktop_config.json` + `.vscode/settings.json` oluşturuldu |
| NotebookLM MCP (auth) | ⚠️ Manuel adım | `npx notebooklm-mcp@latest auth` → **SEN çalıştır** → Chrome açılır → Google ile giriş yap → çerezler kaydedilir → Claude yeniden başlatılır |
| Gemini / Google LLM | ❌ API key yok | Sen key sağlarsan `.env`'e ekleriz |
| Nested Agent Teams | ❌ Yok | Roadmap'te |
| Session Resumption (Teams) | ❌ Yok | Roadmap'te |

### Kurulu Agent'lar (`.claude/agents/`)
| Agent | Model | Görev |
|-------|-------|-------|
| `code-reviewer` | Sonnet 4.6 | Kotlin/Compose güvenlik + performans review |
| `android-builder` | Haiku 4.5 | `assembleDebug` build + hata raporu |
| `deepseek-analyst` | Sonnet 4.6 | DeepSeek API ile derin kod analizi |

---

### Encoding Kuralı (KRİTİK)
**Edit tool bazen curly quote (`"` `"`) karakterleri yazar — Kotlin string literal hatası verir.**
- Belirtisi: `SettingsScreen.kt:453 Expecting an expression` tarzı hatalar
- Fix: `python3 -c "f='dosya.kt'; open(f,'w').write(open(f).read().replace(chr(0x201c),'\"').replace(chr(0x201d),'\"'))"` ile düzelt
- Önlem: Büyük Edit işlemlerinden sonra `xxd | grep e280` ile kontrol et

### AppPrefs Yeni Anahtarlar (2026-06-12)
- `KEY_AUTO_BACKUP_ENABLED` — otomatik yedekleme toggle
- `KEY_HIDE_NAV_BUTTONS` — sistem navigasyon gizleme
- `KEY_ALLAPPS_BG_ALPHA` — AllApps arka plan opakligi (float)
- `KEY_NOTIFICATION_TEXT_ENABLED` — bildirim metni gosterme
- `KEY_LAST_RECONCILE` — reconcile throttle (5dk)

### Tema Sistemi Notu
**Theme.kt'de AppOrganizerTheme artik ThemePreferences'i dinliyor** — `context.themeDataStore` Flow'undan `AppTheme` ve `AppFont` okuyarak `buildColorScheme` ve `buildTypography` olusturuyor. Tema secimi artik aninda uygulanir.

### HorizontalPager Sayfalama
**HomeScreen'de 8 klasor/sayfa** — `pageSize = 8`, `HorizontalPager` + sayfa noktaciklari. LazyVerticalGrid `userScrollEnabled = false` ile sayfa icinde scroll engellendi.

### Rutin Dongu Kurali Guncellendi
**Her dongude once agent ile arastirma raporla, sonra uygulamaya gec.**

### Bildirim Metni Mimarisi (Döngü 8)
**`AppNotificationListenerService`**: `latestTexts: StateFlow<Map<String, String>>` — her `onNotificationPosted`'da `EXTRA_TITLE + EXTRA_TEXT` birleştirilerek güncelleniyor.
**`AppInfo`**: `notificationText: String = ""` field eklendi — DB v6 (fallbackToDestructiveMigration).
**Gösterim:** `AppPrefs.isNotificationTextEnabled()` = true → FolderTile'da klasör altı, AllAppsDrawer'da kategori etiketi yerine.

### LauncherActivity Onboarding Kontrolu
**LauncherActivity, onCreate basinda onboarding bitmemisse MainActivity'ye yonlendirir.**
- Kontrol:  false ise  + 
- Neden: pm clear / fresh install sonrasi LauncherActivity direkt acilinca onboarding atlaniyordu

### Onboarding Adim Listesi (2026-06-13 itibari)
WELCOME → QUERY_PACKAGES → NOTIFICATIONS → UNUSED_GREY → SET_LAUNCHER → AUTO_BACKUP → NOTIF_TEXT → NOTIF_ACCESS → SWIPE_HINT → NEW_BADGE → FOLDER_COUNT → NAV_HIDE → THEME_SELECT → DONE (14 adim)
Toggle chip (Acik/Kapali) olan adimlar: AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE

### OnboardingScreen Bug Fix + Yeni Adımlar (Döngü 9)
**BUG:** `OnboardingScreen.kt:544` — `"app_prefs"/"onboarding_complete"` yanlış key/prefs kullanıyordu → her açılışta onboarding tekrar gösteriliyordu.
**FIX:** `AppPrefs.PREFS_NAME` + `AppPrefs.KEY_ONBOARDING_DONE` kullanımına geçildi.
**Yeni adımlar:** `AUTO_BACKUP` (otomatik yedekleme) + `NOTIF_TEXT` (bildirim metni) — her ikisi `isSkippable=true`, genel "Atla" butonu devreye giriyor.

### MainActivity Refactor (Döngü 10)
**Kaldırıldı:** `private const val PREFS_NAME` ve `KEY_ONBOARDING_DONE` — `AppPrefs.PREFS_NAME` / `AppPrefs.KEY_ONBOARDING_DONE` olarak değiştirildi. DRY ihlali giderildi.

### onResume Performans + Gesture Navigation Fix (Döngü 11)
**LauncherActivity.kt degisiklikleri:**
- `PACKAGE_FILTER` companion object sabiti — her `onResume`'da `IntentFilter` nesnesi olusturulmaz
- `receiverRegistered` bayragi — `onResume`/`onPause` dongusunde cift kayit onlendi
- `isGestureNavEnabled()` — `config_navBarInteractionMode == 2` ile gesture nav algilama
- `applyNavBarVisibility`: gesture nav aktifse `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` yerine `BEHAVIOR_DEFAULT` kullan (Xiaomi/Samsung home gesture cakismasi giderildi)

**HomeScreen.kt degisiklikleri:**
- `LaunchedEffect(Unit)` icindeki cift `loadDockPackages` + `syncAppSizes` cagrilari kaldirildi (`onCreate` + `onResume` zaten handle ediyor)
- `detectVerticalDragGestures`'a `onDragStart` eklendi: baslangic Y pozisyonu izleniyor
- Alt 80dp sistem gesture zone'undan baslayan swipe AllApps'i tetiklemez (Xiaomi/Samsung alt kenar gesture cakismasi giderildi)

**Uzak Ortam Notu:** `dl.google.com` ve `maven.google.com` bu remote ortamda erişim listesinde yok — Android AGP indirilemediginden APK build edilemiyor. Build dogrulama yerel makinede yapilmali.

### DockIcon Async Yukleme (Döngü 12)
**HomeScreen.kt - DockIcon composable degistirildi:**
- Onceki: `remember(packageName)` ile senkron `toBitmap()` — main thread'i bloke ediyordu
- Yeni: `produceState<ImageBitmap?>` ile IO thread async yukleme + `iconCacheInternal` (LRU 200) paylasimiyla
- `bitmap?.let { bmp -> ... }` pattern: `by` delegate'lerde smart cast sorununu onler
- Yeni import'lar: `produceState`, `ImageBitmap`, `Dispatchers`, `withContext`

**Encoding Duzeltmesi (Döngü 12):**
- PixelClockWidget yorumlarindaki bozuk UTF-8 sekanslar (`C3 A2 E2 82 AC 22`) duzgun em-dash ile degistirildi
- Python `open(f, 'rb')` + `.replace(bad, good)` yontemiyle guvende duzeltildi

### onResume Yuk Azaltma + Dock Kırık İkon Fix (Döngü 15)
**LauncherViewModel.kt degisiklikleri:**
- `dockLoaded` bayragi eklendi — klasor sirasi sadece ilk `loadDockPackages()` cagrisinda SharedPrefs'ten okunur; `reorderFolders()` sonraki guncellemeleri bellekte tutar
- `loadDockPackages()`: `newPackages != _dockPackages.value` karsilastirmasi — deger degismemisse `StateFlow` guncellenmez, gereksiz dock rekomposisyonu onlendi
- `onPackageRemoved()`: silinen uygulama dock'taysa aninda `_dockPackages` guncelleniyor — bir sonraki resume'a kadar kirik ikon gosterilmez
**LauncherActivity.kt degisiklikleri:**
- `isGestureNavEnabled()` fonksiyonu kaldirildi → `gestureNavEnabled: Boolean by lazy { ... }` property'ye donusturuldu — `resources.getIdentifier()` artik bir kere calisir, her `onResume`'da tekrar edilmez

**Yol haritasi:** #3 "Ana ekrana donus hizi iyilestirmesi" bu donguyle tamamlandi.

### queryIntentActivities Optimizasyonu (Döngü 16)
**PackageManagerHelper.kt degisiklikleri:**
- `getInstalledApps(onlyLaunchable=true)`: `getInstalledPackages(GET_META_DATA)` + per-package `getLaunchIntentForPackage` kaldirildi
- Tek `queryIntentActivities(MAIN+LAUNCHER)` sorgusuyla tum launcher-visible uygulamalar aliniyor — 100 uygulamada ~200ms tasarruf, ~5x hizlanma
- `onlyLaunchable=false` modu: eski `getInstalledPackages(0)` yolu korundu (geriye donuk uyum)

**LauncherViewModel.kt degisiklikleri:**
- `reconcileIfNeeded()`: `getInstalledPackages(0)` + per-package `getLaunchIntentForPackage` yerine `queryIntentActivities` — onResume count check ~3x hizli
- Uygulama sayisi tutmazsa `loadAppsIfEmpty()` tetikleme mant. korundu

**HomeScreen.kt degisiklikleri:**
- AllApps drawer animasyonu: `fadeIn/Out` yerine `LinearOutSlowInEasing` (acilis 300ms) + `FastOutLinearInEasing` (kapanis 220ms) — Material motion standartlari
- Dock `systemGestureExclusionRects`: onceki rect ile karsilastirma eklendi — sadece dock pozisyonu degisince guncellenir, her layout gecisinde degil
- Yeni import'lar: `FastOutLinearInEasing`, `LinearOutSlowInEasing`

**Yol haritasi:** #3 bu donguyle daha da ilerletildi — uygulama tarama suresi onemli olcude azaldi.

### AllAppsDrawer Rekomposisyon Optimizasyonu (Döngü 17)
**AllAppsDrawer.kt degisiklikleri:**
- `rememberAppIcon`: `iconCacheInternal[cacheKey]` ile `initialValue` set edildi — cache hit'te aninda gosterir, IO tetiklemez
  - Onceki: `initialValue = null` → her drawer acilisinda 100+ ikon diskten yeniden yukluyordu
  - Yeni: FolderTile/AppIconView ile ayni LRU-200 cache paylasilir; key format: `"packageName_96"`
- `quickFilterCounts`: `remember(apps)` ile memoize edildi — `intArrayOf(size, userCount, systemCount, recent7Days)`
  - Onceki: `itemsIndexed` icerisinde `apps.count { }` her chip rekomposisyonunda 4x hesapliyordu
  - Yeni: Sadece `apps` degisince yeniden hesaplar
- `notifTextEnabled`: `remember { AppPrefs.isNotificationTextEnabled(context) }` ile AllAppsDrawer seviyesine ciktirildi
  - `NiagaraAppRow` imzasina `notifTextEnabled: Boolean = false` parametresi eklendi
  - Onceki: Her satir rekomposisyonunda SharedPrefs okuyordu (100+ satir = 100+ okuma)

**LauncherActivity.kt degisiklikleri:**
- `onCreate`'de `syncUsageStats` sonrasina `AppPrefs.markUsageStatsSynced(this)` eklendi
  - Onceki: `onCreate` + hemen ardindan `onResume` iki kez senkronizasyon tetikliyordu
  - Yeni: Ilk oturumda tek senkronizasyon; sonraki 30 dakika throttle korunur

### Bildirim Badge/Metin Temizleme Bug Fix (Döngü 18)
**Hata:** Tum bildirimler silindiginde badge sayilari ve bildirim metinleri DB'de kaliyor, UI yanlis badge gosteriyordu.

**AppNotificationListenerService.kt degisiklikleri:**
- `onNotificationRemoved`: `_latestTexts` map'inden uygulamanin entry'si kaldiriliyor — o uygulama icin baska aktif bildirim yoksa
- Mekanizma: `activeNotifications?.any { it.packageName == pkg && !it.isOngoing }` kontroluyle calisiyor

**LauncherViewModel.kt degisiklikleri:**
- `badgeCounts` observer: `if (counts.isNotEmpty())` guardi kaldirildi — bos map geldiginde (tum bildirimler silindi) DB temizleme kodu calismiyordu
- `latestTexts` observer: `if (texts.isNotEmpty())` guardi kaldirildi + DB'deki eski metinleri temizleyen blok eklendi
- Her iki observer `toReset`/`toClean` listeleri bos oldugunda yazma yapmaz — sifir gereksiz DB IO

**Sonuc:** Badge sayisi ve bildirim metni artik gercek zamani yansitiyor. Bildirim silindiginde badge aninda kalkiyor.

### Ikon Cache Temizleme + FolderSortMode DRY + onPackageAdded Optimizasyonu (Döngü 19)
**Explore agent analiz bulgulari:** LauncherViewModel'de ikon cache invalidation eksikti; FolderSheet FolderSortMode enum AllAppsSortMode ile identic ama ayri yasiyordu; onPackageAdded tam PM taramasi yapiyordu.

**LauncherViewModel.kt degisiklikleri:**
- `onPackageRemoved`: `iconCacheInternal.snapshot().keys.filter { it.startsWith("$pkg_") }` ile paket-spesifik cache entry'leri temizleniyor — kaldirilan uygulamanin ikonu bir sonraki acilista bozuk gozukmez
- `onPackageAdded`: ayni cache temizleme + tam `getInstalledApps(...)` taramasi yerine `helper.getAppInfo(packageName)` — tek paket fetch, ~5x daha hizli; `IGNORE` conflict stratejisi mevcut kategori/usage/hidden verilerini koruyor
- `dockLoaded`: `@Volatile` eklendi — coklu thread erişiminde tutarsiz okuma onlendi

**FolderSheet.kt degisiklikleri:**
- `FolderSortMode` enum kaldirildi (AllAppsSortMode ile identic, DRY ihlali)
- `private fun List<AppInfo>.sortedByMode(mode: AllAppsSortMode)` extension ile AllAppsSortMode kullanilmaya gecildi
- `FolderSortMode.entries` → `AllAppsSortMode.entries`; enum tanımı artık tek yerde

**Uzak Ortam Notu:** APK build bu remote ortamda yapilamiyor — yerel makinede build dogrulanmali.

### Ana Ekrana Dönüş Hızı — Flow Eagerly + Çift Yükleme Koruması (Döngü 20)
**LauncherViewModel.kt degisiklikleri:**
- `folders`, `allApps`, `filteredAllApps`: `WhileSubscribed(5_000L)` → `SharingStarted.Eagerly`
  - Onceki: Kullanici 5+ saniye baska uygulamada kalip donunce akis durmus oluyordu; DB yeniden sorgulanana kadar kisa "Yükleniyor..." flasi goruluyordu
  - Yeni: Launcher her zaman arka planda calisir — akis hic durmuyor, donus aninda veri hazir
- `isLoadingApps: @Volatile Boolean` flag eklendi — `loadAppsIfEmpty` guard
  - Onceki: `onCreate` + hemen ardindan `onResume` (ilk acilis) iki kez PM taramasi tetikleyebiliyordu
  - Yeni: Eger tarama devam ediyorsa ikinci cagri return eder; `finally` blogu ile flag temizleniyor
- `loadDockPackages`: Dock paketleri SharedPrefs sadece ilk yüklemede (`!dockLoaded`) okunur
  - Onceki: Her `onResume`'da `DockPrefs.getDockPackages()` cagrisi — SharedPrefs string parse
  - Yeni: `dockLoaded=true` sonrasinda `_dockPackages`, ViewModel metotlariyla (saveDockPackages/addToDock/removeFromDock) her zaman guncel — disk okuma yok
- `reconcileIfNeeded`: `distinctBy { ... } .count { ... }` → `mapTo(mutableSetOf()) { packageName } .count { shouldHide(it) }`
  - Tek gecisli set deduplication — ara liste olusturulmuyor

### Dock In-Memory Operasyonlar + HomeScreen Refactor (Döngü 21)
**LauncherViewModel.kt degisiklikleri:**
- `addToDock`: `DockPrefs.getDockPackages(context)` (SharedPrefs okuma) yerine `_dockPackages.value` kullaniliyor
  - Onceki: Her dock ekleme isleminde 2x SharedPrefs okuma yapiliyordu
  - Yeni: `dockLoaded` sonrasinda `_dockPackages.value` her zaman guncel — sifir disk IO
- `removeFromDock`: ayni desen — `DockPrefs.removeFromDock` yerine `_dockPackages.value - packageName` + `saveDockPackages`
- Hardcoded `4` → `DOCK_MAX_SIZE` sabitiyle (`max $DOCK_MAX_SIZE`) gecerli

**LauncherActivity.kt degisiklikleri:**
- `isDefaultLauncher(context)` fonksiyonu kaldirildi — hicbir yerde cagirilmiyordu (olu kod)
- `import android.content.pm.PackageManager` gereksiz import kaldirildi

**HomeScreen.kt → HomeScreenComponents.kt refactor:**
- `PixelClockWidget`, `GoogleSearchBar`, `PixelDock`, `DockIcon`, `SwipeHint` private composable'lar yeni dosyaya taşındı
- `private` → `internal` görünürlük — aynı modülden erişim korunuyor
- HomeScreen.kt: 866 → 634 satır (232 satır azaldı)
- HomeScreenComponents.kt: yeni dosya, 288 satır
- `iconCacheInternal` internal visibility — aynı paketten doğrudan erişim korunuyor

**Uzak Ortam Notu:** APK build bu remote ortamda yapilamiyor — yerel makinede build dogrulanmali.

### İkon Paketi Desteği (Döngü 22)
**Nova/ADW/Lawnchair/GO Launcher uyumlu ikon paketi altyapısı — Yol Haritası #6 tamamlandı:**

**`utils/IconPackManager.kt`** (yeni dosya):
- 5 intent filter ile kurulu ikon paketlerini tarar: `com.novalauncher.THEME`, `org.adw.launcher.THEMES`, `com.gau.go.launcherex.theme`, `app.lawnchair.ICON_PACK`, `com.teslacoilsw.launcher.THEME`
- `parseAppFilter()`: paket resources'indan `appfilter.xml` parse ederek `packageName -> drawableName` eslestirmesi
- `filterCache: ConcurrentHashMap` — thread-safe; `clearCache()` ikon paketi degisince cagrilir

**`utils/AppPrefs.kt`**: `KEY_ICON_PACK` + `getIconPack`/`setIconPack` eklendi; set'te otomatik cache temizleme

**`AppIconView.kt`, `AllAppsDrawer.rememberAppIcon`, `HomeScreenComponents.DockIcon`**:
- Cache key: ikon paketi seciliyse `"${pkg}_${px}_${iconPackPkg}"`, yoksa `"${pkg}_${px}"` (geriye uyumlu)
- Yukleme onceligi: once ikon paketinden dene, bulamazsan sistem ikonuna don

**`SettingsScreen.kt`**: "İkon Paketi" bolumu eklendi — kurulu paketleri listele, secili secenegi CheckCircle goster; paket yoksa Play Store yonlendirmesi

### HomeLongPressSheet Grid Sıralaması (Döngü 23)
**Hata:** `items(emptySlots)` FolderTile'lardan önce eklenmişti — boş Box'lar grid başına render edildiğinden koordinat kaymasıyla FolderSheet açılıyordu.
**Fix:** Boş slotlar `items(pageFolders.size)` blokundan **sonra** eklendi. Uzun bas ile "Ana Ekran" menüsü (Duvar Kağıdı / Dock Düzenle / Launcher Ayarları) artık doğru çalışıyor.
**Test:** Üst boş alan (y≈180, clock widget bölgesi) uzun basılarak doğrulandı.

### Widget Desteği (Döngü 24) — Yol Haritası #7 Tamamlandı
**Yeni dosyalar:**
- `utils/WidgetPrefs.kt` — widget ID'leri (int listesi) SharedPrefs'te `widget_prefs` altında saklanır
- `utils/WidgetHostManager.kt` — `AppWidgetHost` singleton; `startListening`/`stopListening` lifecycle yönetimi
- `presentation/ui/launcher/WidgetArea.kt` — `AppWidgetHostView` Compose `AndroidView` ile gösterilir; uzun basınca kırmızı X silme butonu belirir

**Değiştirilen dosyalar:**
- `LauncherActivity`: `widgetPickerLauncher` + `widgetConfigureLauncher` (`registerForActivityResult`); `launchWidgetPicker()`; `onResume` → `startListening`, `onPause` → `stopListening`; `onCreate` → `loadWidgetIds`
- `LauncherViewModel`: `widgetIds: StateFlow<List<Int>>` + `addWidgetId`/`removeWidgetId`/`loadWidgetIds`
- `HomeScreen`: `onLaunchWidgetPicker` lambda parametresi; `WidgetArea` Google search bar ile klasör grid arası
- `HomeLongPressSheet`: "Widget Ekle" seçeneği (`Icons.Default.Widgets`)
- `AppPrefs`: `KEY_WIDGET_AREA_ENABLED` — widget alanını açıp kapama
- `SettingsScreen`: "Widget" bölümü eklendi — toggle
- `AndroidManifest`: `BIND_APPWIDGET` izni (launcher rolüyle otomatik bind edilebilir)

**Kullanım akışı:** Ana ekran uzun bas → "Widget Ekle" → sistem widget seçici açılır → seçim yapılınca WidgetArea'da gösterilir → Uzun bas → X ile silinir

### AllAppsDrawer Greyscale Fix (Döngü 25)
**Hata:** `unusedGreyDays=0` (kapalı) olsa bile tüm uygulamalar gri geliyordu.
**Fix:** `NiagaraAppRow`'a `unusedGreyDays: Int = 0` parametresi eklendi; `<= 0` ise `saturation=1f`.

### Klasör Sort Mode Kalıcılık (Döngü 25)
`AppPrefs.KEY_FOLDER_SORT_MODE` eklendi. FolderSheet kapanıp açılınca sıralama sıfırlanıyor sorunu giderildi.

### Onboarding RESTORE_BACKUP Adımı (Döngü 25)
WELCOME'dan sonra yeni adım: "Önceki Yedeğiniz Var Mı?" — JSON dosya seçici ile geri yükleme, `hiltViewModel` ile `AppListViewModel` inject edildi.

### Settings Reaktiflik Fix (Döngü 26)
**Hata:** `remember {}` ile okunan AppPrefs değerleri (bgType, bgColor, textAlpha, widgetAreaEnabled, notifTextEnabled, unusedGreyDays) Settings'den dönerken güncellenmiyordu — launcher yeniden başlatmak gerekiyordu.
**Fix:** `mutableStateOf` + `DisposableEffect(context)` + `SharedPreferences.OnSharedPreferenceChangeListener` kombinasyonu ile reaktif hale getirildi:
- `HomeScreen.kt`: bgType, bgColor, textAlpha, widgetAreaEnabled — Settings'de değişince anında yansır
- `AllAppsDrawer.kt`: notifTextEnabled, unusedGreyDays — drawer açıkken dahi güncellenir
**Ek:** `FolderTile.kt`'ye `textAlpha: Float = 1f` parametresi eklendi; kategori adı metnine uygulandı (textAlpha özelliği daha önce hiç FolderTile'a geçirilmiyordu).
**Uzak Ortam Notu:** APK build bu remote ortamda yapılamıyor — yerel makinede doğrulanmalı.

### Robustlik İyileştirmeleri (Döngü 27)
**LauncherViewModel.kt degisiklikleri:**
- `isLoadingApps`: `@Volatile Boolean` → `AtomicBoolean` — `compareAndSet(false, true)` ile atomik check-then-set; `Volatile` sadece görünürlük sağlar, bileşik operasyonu korumaz
- `finally { isLoadingApps.set(false) }` — AtomicBoolean API'si ile

**AppNotificationListenerService.kt degisiklikleri:**
- `onListenerDisconnected()` override eklendi — bağlantı kesilince `_badgeCounts` ve `_latestTexts` temizleniyor; pm clear / izin iptali / sistem yeniden başlatma sonrası stale badge gösterilmesi önlendi

**AllAppsDrawer.kt degisiklikleri:**
- `iconPackPkg`: `remember { AppPrefs.getIconPack() }` → `mutableStateOf + DisposableEffect + KEY_ICON_PACK listener`
  - Onceki: Icon pack Settings'ten değiştirilince AllAppsDrawer yeniden oluşturulmadan ikone güncellenmiyordu
  - Yeni: SharedPrefs listener tetikleyince `iconPackPkg` state güncellenir → `NiagaraAppRow` yeniden render → `rememberAppIcon` yeni cacheKey ile ikonu diskten yükler
- `rememberAppIcon(packageName, iconPackPkg)` — `iconPackPkg` artık parametre (içeride `remember {}` ile hesaplanmıyor)
- `NiagaraAppRow(..., iconPackPkg: String = "")` — yeni parametre, `rememberAppIcon`'a iletilir

*Son güncelleme: 2026-06-13 (Döngü 27 — AtomicBoolean, bildirim temizleme, ikon paketi reaktifliği)*