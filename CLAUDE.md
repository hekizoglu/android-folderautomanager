# AppOrganizer — Claude Çalışma Talimatları (v4)

> **Son güncelleme: 2026-06-10** | Bu dosya her konuşmanın başında okunur.

---

## 1. HÜSEYİN KİMDİR?

| Özellik | Açıklama |
|---|---|
| Çalışma stili | Vizyon odaklı, büyük resmi görür — detay takibini Claude'a bırakır |
| Beklenti | "Söylemek zorunda kalmamalıyım, anlamalısın" prensibi |
| Hız | Az token, çok iş — gereksiz tekrardan nefret eder |
| İletişim | **Türkçe** — tüm yanıtlar Türkçe |
| Takip | Telegram üzerinden — her önemli adımda bildirim + APK |

---

## 2. TEMEL ÇALIŞMA KURALLARI

### Her görev sonunda otomatik olarak yap:
1. **Build al** → `.\gradlew assembleDebug`
2. **Emülatörde test et** → `Pixel6_AOSP33` veya `Pixel7_API33`
3. **Hata varsa düzelt** → DeepSeek ile analiz et, düzelt, tekrar build
4. **Test geçtiyse commit + push** → açıklayıcı commit mesajı
5. **Telegram'a gönder** → APK + kısa durum raporu

### Asla yapma:
- Küçük değişiklik için "onaylıyor musun?" sorma → yap, test et, bildir
- Yarım bırakma → başladığın işi sonuna kadar götür
- Gereksiz açıklama → ne yaptığını değil, **ne değiştiğini** söyle
- Encoding bozukluğu → her zaman **UTF-8** kaydet
- "Analiz edildi" deyip kodu değiştirmemek → değişikliği KOD'a yansıt

---

## 3. PROJE: AppOrganizer

### Ne?
Android launcher uygulaması. Uygulamaları otomatik kategorilere göre klasörlere böler. Kullanıcı fark etmeden Pixel Launcher'dan geçiş yapabilmeli — **"invisible launcher" prensibi**.

### Temel Prensipler:
- **Kullanıcı dostu** — her özellik sezgisel, açıklama gerektirmemeli
- **Turkuaz tema** → primary: `#00897B` (Teal 600), secondary: `#26C6DA` (Cyan)
- **Pixel Launcher klonu** → transparent background, frosted dock, Google clock widget stili
- **Launcher dialog** → ilk açılışta "Ana ekran uygulaması olarak ayarla?" sor
- **Temiz kod** → büyük dosyaları böl, tek sorumluluk prensibi

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
cd "c:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug

# Emülatör başlat
$em = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
Start-Process $em -ArgumentList "-avd","Pixel6_AOSP33","-no-snapshot-save"

# APK yükle & başlat
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity"
```

### Telegram Bot:
- Token: `.env` → `TELEGRAM_BOT_TOKEN`
- Chat ID: `.env` → `TELEGRAM_CHAT_ID`
- Script: `C:\Users\hekizoglu\Desktop\ai_agents\`
- Bot: `@claudetestbotibm_bot`

---

## 4. BEKLEYEN DÜZELTMELER (Öncelik Sırasıyla)

| # | Görev | Durum |
|---|---|---|
| 1 | Icon loading → Coil ile async yükleme (UI thread bloke ediyor) | ✅ v0.9 |
| 2 | AppListScreen → küçük composable'lara böl | ⏳ |
| 3 | Haptic feedback → klasör açma/kapama, uygulama başlatma | ✅ v0.9 |
| 4 | Dock → sabit 4 uygulama yerine kullanıcı seçimi | ✅ v0.9 |
| 5 | Ayarlar ekranı → launcher'ı varsayılan yapma butonu | ✅ v0.9 |
| 6 | Onboarding akışı → SET_LAUNCHER adımını sona taşı | ⏳ |

---

## 5. AGENT KULLANIM STRATEJİSİ

### Paralel Agent Kullanımı:
- **Bağımsız görevler** için aynı anda birden fazla agent başlat
- **DeepSeek V4-Flash** → kod review, analiz, öneri (key: `.env` → `DEEPSEEK_API_KEY`)
- **Gemini** → Play Store analizi, UX önerileri (kota dolunca yeni key iste)
- **Explore agent** → dosya arama ve codebase taraması
- **Plan agent** → büyük mimari kararlar

### Mevcut Alt-Agentlar:
| Agent | Uzmanlık | MCP Araçları |
|---|---|---|
| `shadcn-ui-expert` | Bileşen tasarımı, layout planı | shadcn/ui MCP, Twix design MCP |
| `vercel-ai-expert` | Vercel AI SDK v5 entegrasyonu | Context7 MCP |
| `stripe-expert` | Ödeme & usage-based pricing | Context7 MCP |
| `supabase-expert` | DB şema, auth, realtime | Supabase MCP |

> **Yeni agent eklendiğinde bu tabloya yaz.**

---

## 6. SUBAGENT MİMARİSİ — DOĞRU TASARIM

### Neden Subagent Kullanıyoruz?
Subagent olmadan `read` tool'u her kullanımda dosya içeriğini **ana konuşma geçmişine** ekler. Büyük bir codebase'de implementasyona başlamadan context'in %80'i dolabilir → `auto-compact` tetiklenir → performans düşer.

**Subagent çözümü:** Alt-agent kendi izole context penceresinde çalışır. Ara adımlar (dosya okuma, arama) ana agenta görünmez. Ana agent yalnızca birkaç yüz token'lık **özet** görür.

### ✅ DOĞRU KULLANIM — "Araştırmacı/Planlamacı" Modeli

```
Ana Agent (Parent)
    │
    ├── context-session.md oluştur (proje bilgisi, kaynak of truth)
    │
    ├── Subagent: shadcn-expert
    │       ├── context-session.md oku
    │       ├── MCP araçlarıyla bileşen araştır
    │       ├── docs/task/ui-plan.md yaz
    │       └── "Planı yazdım, önce onu oku" → Parent'a döner
    │
    ├── ui-plan.md oku → implementasyonu kendisi yaz
    │
    ├── Subagent: vercel-ai-expert
    │       ├── context-session.md oku
    │       ├── Codebase'i tara, implementasyon planı yap
    │       ├── docs/task/vercel-plan.md yaz
    │       └── context-session.md güncelle → Parent'a döner
    │
    └── vercel-plan.md oku → entegrasyonu kendisi yaz
```

**Kural:** Subagent **ASLA** kod yazmaz. Sadece araştırır, planlar, `.md` dosyasına yazar.  
**Kural:** Tüm kod yazımını **tek bir ana (parent) agent** yapar — tam context'e sahip olduğu için bug fix de sağlam çalışır.

### ❌ YANLIŞ KULLANIM — Kaçın

```
# YANLIŞ — subagent'a implementasyon yaptırma
frontend-agent: UI kodu yaz
backend-agent: API kodu yaz
parent-agent: sadece orkestre et

# SORUN: Her subagent izole oturum = önceki oturumu bilmez
# Bug çıktığında hiçbir tarafta tam resim yok → fix felç olur
```

### Context Paylaşımı — Dosya Sistemi Yaklaşımı (Manus Deseni):

```
docs/
└── task/
    ├── context-session.md    # Ana "source of truth" — proje durumu
    ├── ui-plan.md            # shadcn subagent çıktısı
    ├── vercel-plan.md        # vercel subagent çıktısı
    └── [feature]-plan.md    # Her yeni feature için
```

**Kurallar:**
- Parent agent her göreve başlarken `context-session.md` oluşturur
- Her subagent işe başlamadan `context-session.md` okur
- Her subagent bitişte `context-session.md` günceller
- Büyük tool çıktıları (log, scrape) → dosyaya yaz, context'e sadece referans koy

---

## 7. KV-CACHE OPTİMİZASYONU (Maliyet Tasarrufu)

Cache'li token ~10× daha ucuz. Cache hit rate'ini artırmak için:

| ✅ Yap | ❌ Yapma |
|---|---|
| Sistem promptu başını sabit tut | Başa timestamp / değişken veri koyma |
| Append-only context kullan | Eski adımları silme (model aynı hatayı tekrarlar) |
| JSON anahtar sırasını sabit tut | Dinamik tool yükleme (cache'i bozar) |
| Subagent sistem promptuna dok gömülü tut | Araç tanımlarıyla context'i şişirme |
| Hata mesajlarını context'te bırak | "Tool masking" yerine tool ekleme/çıkarma yapma |

**Önemli:** Sistem promptu başına tek karakter değişimi bile KV-cache'i geçersiz kılar.

---

## 8. YAYGIN YANILGILAR — BUNLARDAN KAÇIN

1. **"Daha büyük context penceresi daha iyi"** → Yanlış. Context rot oluşur, maliyet artar. İlke: *"retrieve less, but better."*
2. **Saf embedding/vector-RAG'a güvenmek** → Büyük codebase'de bozulur. Grep + AST + rerank kombinasyonu daha sağlam.
3. **CLAUDE.md / her zaman açık kuralları şişirmek** → "Token tax" — always-apply kurallarını ~200 kelime altında tut.
4. **Çok sayıda MCP sunucusunu sürekli açık tutmak** → Araç tanımı token israfı.
5. **Subagent'a implementasyon yaptırmak** → İzole oturumlar birbirini bilmez, bug fix felç olur.
6. **"Daha çok agent = daha iyi"** → Her çağrı yeni izole oturum + gecikme. Asıl kazanç paralel keşif ve context temizliğinde.
7. **LLM'e kendi context dosyasını yazdırıp körlemesine güvenmek** → Bazı çalışmalarda performansı düşürüyor, gözden geçir.

---

## 9. PLAY STORE HAZIRLIK (Bekleyen)

- [ ] Privacy policy URL
- [ ] Store listing görselleri (screenshot)
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks` (şifre: `AppOrganizer2024!`)
- [ ] ProGuard kuralları

---

## 10. SÜREKLİLİK & BAĞLAM KESİLMESİ

Rate limit veya context kesilmesinde:
- Bu `CLAUDE.md` dosyası durumu özetler
- `docs/task/context-session.md` → devam için source of truth
- `C:\Users\hekizoglu\.claude\projects\...\memory\` → memory yedekleri
- Masaüstünde `CLAUDE.md` yedeği her güncellemede oluştur

---

## 11. SUBAGENT SİSTEM PROMPTU — ŞABLON

Her yeni subagent oluşturulurken bu şablonu temel al:

```markdown
## GÖREV
[Servis/teknoloji adı] uzmanısın. Amacın araştırma yapmak ve
bir implementasyon planı oluşturmaktır. ASLA kod yazma.

## ÇALIŞMA SÜRECİ
1. docs/task/context-session.md dosyasını oku
2. [MCP araçları veya dökümanlar] kullanarak araştır
3. Planı docs/task/[feature]-plan.md'ye yaz
4. context-session.md'yi güncelle

## ÇIKTI FORMATI
Son mesajında şunu söyle:
"Planı `docs/task/[feature]-plan.md` dosyasına yazdım.
Devam etmeden önce bu dosyayı oku."

## KURALLAR
- Implementasyon yapma, sadece planla
- claude MCP client'ı çağırma (kendini çağırma)
- Her adımda context-session.md'yi referans al
```

---

*Bu dosya AppOrganizer projesi için Claude'un tek referans kaynağıdır.*
*Hüseyin Ekizoğlu — Kemalpaşa /*