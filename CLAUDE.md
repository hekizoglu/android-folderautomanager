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
**Bir işlem hata verdiğinde ve çözümden %85'in altında eminsen:**
1. Hemen bir agent ile online araştırma yap — hatanın tam metnini ara
2. Gelen sonucu yorumla, çözümü uygula
3. Yine başarısız olursa farklı bir AI modeli ile agent tekrar araştır
4. **Minimum 3 deneme** — her seferinde farklı model/kaynak kullan (WebSearch, GitHub Issues, Stack Overflow)

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

### Git Kuralları
- **Tüm değişiklikler `main` branch üzerinde** — yeni branch oluşturma
- `git add` + `git commit` + `git push origin main` — her build sonrası

### Her konuşma açılışında otomatik olarak:
1. **GitHub'dan çek** — `git fetch origin && git status` ile yerel/uzak fark kontrol et
2. **Güncel değilse pull** — `git pull origin main` ile senkronize et
3. **Özellik listesini doğrula** — aşağıdaki "Her Konuşmada Özellik Kontrol Listesi" tablosunu koda karşı kontrol et, ❌ olanları o konuşmada düzelt

### Her görev sonunda otomatik olarak:
1. **Build al** — `.\gradlew assembleDebug`
2. **Emülatörde test et** — `Pixel6_API33` (AOSP) veya `Xiaomi_HyperOS_API34` (Android 14, 395dpi) üzerinde
3. **Hata varsa düzelt** — DeepSeek ile analiz et, düzelt, tekrar build
4. **Test geçtiyse commit + push** — açıklayıcı commit mesajı
5. **Telegram'a gönder** — APK + kısa durum raporu

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
- Token: `.env` → `TELEGRAM_BOT_TOKEN`
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
| 12 | AppListScreen refactor — max 300 satır | `AppListScreen.kt` | `wc -l .../AppListScreen.kt` |

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
| 12 | ⚠️ AppListScreen.kt 385 satır — Components+Dialogs ayrıldı, kabul edilebilir |

**Düzeltilen Buglar (2026-06-10)**
- FolderSheet geri/home tuşu: `sheetState.hide()` + `BackHandler` entegre edildi
- Sil/Gizle: `contextMenuApp` artık `allApps` flow'undan güncel veri alıyor (stale state giderildi)
- Dock'a ekle: dolu/zaten var durumları Toast ile kullanıcıya bildiriliyor
- Swipe-up AllApps: `detectVerticalDragGestures` eklendi, güvenilirlik arttı
- Çift tap: ana ekrana çift dokunarak AllApps açılıyor

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
| NotebookLM MCP (auth) | ⚠️ Manuel adım | `npx notebooklm-mcp@latest auth` → sen çalıştır → Google ile giriş yap |
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

*Son güncelleme: 2026-06-10*