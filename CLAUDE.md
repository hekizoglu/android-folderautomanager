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

### Her görev sonunda otomatik olarak:
1. **Build al** — `.\gradlew assembleDebug`
2. **Emülatörde test et** — `Pixel6_AOSP33` veya `Pixel7_API33` üzerinde
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

- **Paralel agent'lar kullan** — bağımsız görevler için aynı anda birden fazla agent başlat
- **DeepSeek V4-Flash** — kod review, analiz, öneri için (`sk-1e1c3788040f4ac7b72a51964e99760c`)
- **Gemini** — Play Store analizi, UX önerileri (kota dolunca yeni key iste)
- **Explore agent** — dosya arama ve codebase taraması için
- **Plan agent** — büyük mimari kararlar için

### Agent görevi tamamlandığında:
- Sonuçları analiz et → uygula → build al → test et → gönder
- Sadece "analiz edildi" deme, değişikliği KOD'a yansıt

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
cd "c:\Users\hekizoglu\Documents\AppOrganizer"
.\gradlew assembleDebug

# Emülatör başlat
$em = "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe"
Start-Process $em -ArgumentList "-avd","Pixel6_AOSP33","-no-snapshot-save"

# APK yükle
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity"
```

### Telegram Bot:
- Token: `TELEGRAM_TOKEN_REMOVED`
- Chat ID: `937179261`
- APK gönderimi: `C:\Users\hekizoglu\Desktop\ai_agents\` klasöründeki script
- Bot adı: `@claudetestbotibm_bot`

---

## Limit Takılmaları ve Süreklilik

Sistem rate limit'e takıldığında veya context kesildiğinde:
- Bu CLAUDE.md dosyası durumu özetler
- `C:\Users\hekizoglu\.claude\projects\...\memory\` klasöründeki memory'ler devam için rehber
- Masaüstünde `CLAUDE.md` yedeği her güncellemede oluşturulur

---

## DeepSeek Bulgularından Bekleyen Düzeltmeler

Sıradaki iterasyonda uygulanacaklar (öncelik sırasıyla):
1. Icon loading → Coil kütüphanesi ile async yükleme (UI thread bloke ediyor)
2. AppListScreen → küçük composable'lara böl (dosya çok büyük)
3. Haptic feedback → klasör açma/kapama, uygulama başlatma
4. Dock → sabit 4 uygulama yerine kullanıcı seçimi
5. Ayarlar ekranı → launcher'ı varsayılan yapma butonu ekle

---

## Play Store Hazırlık (Bekleyen)

- [ ] Privacy policy URL
- [ ] Store listing görselleri (screenshot)
- [ ] Content rating anketi
- [ ] Release keystore: `release.jks` (şifre: `AppOrganizer2024!`)
- [ ] ProGuard kuralları

---

*Son güncelleme: 2026-06-07*
