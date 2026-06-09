# AppOrganizer

> Telefonundaki uygulamaları yapay zeka ile otomatik kategorilere ayıran Android yönetim uygulaması.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-0.1.0--beta-orange.svg)](https://github.com/hekizoglu/android-folderautomanager/releases)
[![Build](https://github.com/hekizoglu/android-folderautomanager/actions/workflows/build.yml/badge.svg)](https://github.com/hekizoglu/android-folderautomanager/actions/workflows/build.yml)

---

## Ne Yapar?

Telefonuna 50'den fazla uygulama kurdun ama hepsi ana ekranda dağınık duruyor. AppOrganizer bu sorunu çözer:

1. **Tara** — Cihazındaki tüm kurulu uygulamaları algılar
2. **Sınıflandır** — Yapay zeka ile her uygulamayı doğru kategoriye atar
3. **Organize Et** — Her kategori için ana ekrana kısayol oluşturur

Sonuç: Sosyal medya, oyunlar, finans, sağlık — hepsi yerli yerinde.

---

## Özellikler

### 🤖 Yapay Zeka Sınıflandırma
- **Online veritabanı** — GitHub'da tutulan `app_database.json` ile 1000+ uygulamayı tanır
- **Exact match** — Instagram → Sosyal Medya, ChatGPT → Üretkenlik gibi kesin eşleşmeler
- **Keyword analizi** — Uygulama adı ve paket adından kategori çıkarımı
- Yanlış sınıflandırılanlar için manuel düzeltme

### 📂 10 Varsayılan Kategori
| Kategori | Örnekler |
|---|---|
| 👥 Sosyal Medya | Instagram, WhatsApp, Telegram, Twitter |
| 📝 Üretkenlik | ChatGPT, Microsoft Office, Notion, DeepSeek |
| 🎮 Oyunlar | PUBG Mobile, Clash of Clans, Steam Link |
| 🛍️ Alışveriş | Trendyol, Amazon, Hepsiburada |
| 📰 Haber | Haberler, RSS okuyucular |
| ❤️ Sağlık | Spor, fitness, meditasyon uygulamaları |
| 💰 Finans | Bankacılık, kripto, yatırım |
| 🎓 Eğitim | Duolingo, Khan Academy, Udemy |
| 🔧 Araçlar | Dosya yöneticisi, VPN, temizleyici |
| 📦 Diğer | Yukarıdakilere girmeyen her şey |

### 📋 Uygulama Listesi & Yönetimi
- Kategoriye göre filtreleme ve fuzzy search
- Ada, kurulum tarihine göre sıralama
- Toplu kategori atama (uzun basış → seç → ata)
- Sistem uygulamalarını gizle / göster (varsayılan: gizli)
- Kategorileri sıfırla ve yeniden sınıflandır
- Yeni kurulan uygulamaları otomatik algıla

### 🔗 Launcher Organizasyonu (ShortcutManager)
- Her kategori için ana ekrana **pinlenebilir kısayol** oluşturur
- Tüm launcher'larda çalışır (Samsung, MIUI, Pixel vb.)
- Kısayola basınca doğrudan o kategorinin uygulamaları açılır
- Kullanıcı onayıyla gerçekleşir — zorla hiçbir şey değiştirilmez

### ♿ Erişilebilirlik Servisi (Gelişmiş)
- AOSP tabanlı launcher'larda otomatik ikon taşıma desteği
- Her adımı gösteren detaylı debug log sistemi
- MIUI/HyperOS için kurulum rehberi ve uyarılar
- Android 17 Advanced Protection Mode uyumlu (`isAccessibilityTool="true"`)
- Dinamik gesture zamanlaması — cihaz ayarına göre otomatik

---

## Mimari

```
app/src/main/java/com/armutlu/apporganizer/
├── data/
│   ├── local/           # Room DB (AppDao, CategoryDao, AppDatabase)
│   ├── remote/          # GitHub app veritabanı (AppDatabaseService)
│   └── repository/      # AppRepository — tek veri kaynağı
├── domain/
│   ├── models/          # AppInfo, Category
│   └── usecase/         # AppClassifier, KeywordDatabase
├── presentation/
│   ├── navigation/      # Compose Navigation grafiği
│   ├── receivers/       # PackageChangeReceiver
│   ├── ui/
│   │   ├── screens/     # AppListScreen, SettingsScreen, CategoryEditorScreen
│   │   └── theme/       # Material 3 tema
│   └── viewmodel/       # AppListViewModel
├── service/             # LauncherAccessibilityService
└── utils/               # PackageManagerHelper, LauncherOrganizer, PermissionHelper
```

**Teknoloji yığını:**

| Katman | Teknoloji |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Mimari | MVVM + StateFlow |
| DI | Hilt |
| Veritabanı | Room |
| Asenkron | Kotlin Coroutines + Flow |
| Log | Timber |
| Test | JUnit 4 + MockK |

---

## Kurulum & Build

### Gereksinimler
- Android Studio Hedgehog veya üzeri
- JDK 17
- Android SDK (API 26–34)

```bash
# Projeyi klonla
git clone https://github.com/hekizoglu/android-folderautomanager.git
cd android-folderautomanager

# Birim testleri çalıştır
./gradlew testDebugUnitTest

# Debug APK derle
./gradlew assembleDebug

# APK'yı bağlı cihaza kur
./gradlew installDebug
```

APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

---

## Kullanım

### İlk Açılış
1. Uygulamayı aç → Onboarding ekranını tamamla
2. İzinleri ver → Uygulama listesi otomatik yüklenir

### Yapay Zeka Sınıflandırma
1. Ana ekranda **🤖 AI** butonuna bas
2. Sınıflandırılmamış uygulamalar otomatik kategorilere atanır
3. Yanlış atananları uzun basış → Kategori Seç ile düzelt

### Launcher'da Organize Et
1. **FAB (+)** → "Launcher'da Organize Et"
2. "Başlat" → Her kategori için kısayol oluşturulur
3. Her kısayolu onayladıkça ana ekrana eklenir

### Erişilebilirlik Servisi
```
Ayarlar > Erişilebilirlik > AppOrganizer > Etkinleştir
```
> **MIUI/HyperOS:** Uygulama Bilgisi → Otomatik Başlatma'yı da açın ve pil optimizasyonunu devre dışı bırakın.

---

## Testler

```bash
# Tüm birim testleri
./gradlew testDebugUnitTest

# Kapsam raporu (JaCoCo)
./gradlew jacocoTestReport
# → app/build/reports/jacoco/jacocoTestReport/html/index.html

# Enstrümanlı testler (emülatör/cihaz gerekir)
./gradlew connectedDebugAndroidTest
```

**Test dosyaları:**
- `AppClassifierTest` — AI sınıflandırma mantığı
- `AppInfoTest` — Model doğrulamaları  
- `CategoryTest` — Kategori operasyonları
- `KeywordDatabaseTest` — Keyword eşleşme algoritması

---

## Sürüm Geçmişi

| Sürüm | Tarih | Notlar |
|---|---|---|
| 0.1.0-beta | Haziran 2026 | İlk beta — AI sınıflandırma, kısayol organizasyonu, erişilebilirlik servisi |

---

## Katkıda Bulunma

1. Fork'la
2. Branch oluştur: `git checkout -b feature/ozellik-adi`
3. Commit et ve push yap
4. Pull Request aç

---

## Lisans

MIT © [hekizoglu](https://github.com/hekizoglu/android-folderautomanager)
