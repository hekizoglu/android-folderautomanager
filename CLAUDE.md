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

### Git Kuralları
- **Tüm değişiklikler `main` branch üzerinde** — yeni branch oluşturma
- `git add` + `git commit` + `git push origin main` — her build sonrası

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

## DeepSeek Bulgularından Bekleyen Düzeltmeler

Sıradaki iterasyonda uygulanacaklar (öncelik sırasıyla):
1. ~~**Icon loading** → async yükleme~~ ✅ `produceState(IO)` + accompanist ile tamamlandı
2. **AppListScreen** → küçük composable'lara böl (dosya çok büyük)
3. **Haptic feedback** → klasör açma/kapama ✅ (kısmen var), uygulama başlatma
4. **Dock** → sabit 4 uygulama yerine kullanıcı seçimi UI (DockPrefs var, ekran yok)
5. **Ayarlar ekranı** → launcher'ı varsayılan yapma butonu ekle

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
| Agent Teams / Swarm | 🧪 Experimental | `CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` |
| VS Code Agent Sessions View | ✅ Stabil | 1.109+ |
| Custom Agents + Handoffs | ✅ GA | `.vscode/agents/` |
| NotebookLM MCP (npx) | ✅ Aktif | Çerez 2–4 hf'da bir yenilenir |
| NotebookLM MCP (uv) | ✅ Aktif | Daha hafif kurulum |
| Nested Agent Teams | ❌ Yok | v2.1.69'da düzeltildi ama devre dışı |
| Session Resumption (Teams) | ❌ Yok | Roadmap'te |

---

*Son güncelleme: 2026-06-10*