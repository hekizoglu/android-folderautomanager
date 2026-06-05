# AppOrganizer — Akıllı Launcher

> Uygulamalarını yapay zeka ile kategorilere ayıran ve kendi ana ekranını sunan Android başlatıcısı.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
[![Version](https://img.shields.io/badge/Version-0.1.0--beta-orange.svg)](https://github.com/hekizoglu/android-folderautomanager/releases)
[![Build](https://github.com/hekizoglu/android-folderautomanager/actions/workflows/build.yml/badge.svg)](https://github.com/hekizoglu/android-folderautomanager/actions/workflows/build.yml)

---

## Ne Yapar?

AppOrganizer iki şeyi birden yapar:

**1. Yönetim Uygulaması** — Kurulu uygulamalarını AI ile tarar, sınıflandırır, düzenler.

**2. Akıllı Launcher** — Telefondaki varsayılan ana ekranın (MIUI Home, Samsung One UI vb.) **yerini alır**. Uygulamaların artık açık bir ekranda değil, kategoriye göre hazırlanmış **klasörlerde** durur.

```
Ana Ekran (Launcher)
├── 📁 Sosyal Medya   (Instagram, WhatsApp, Telegram...)
├── 📁 Oyunlar        (PUBG, Clash of Clans...)
├── 📁 Finans         (Banka, kripto...)
├── 📁 Üretkenlik     (ChatGPT, Office...)
└── 📁 + daha fazlası
```

---

## Launcher Özellikleri

### 🏠 Ana Ekran
- Sistem **duvar kağıdı** arka planda görünür (`windowShowWallpaper`)
- **Büyük dijital saat** ve tarih widget'ı
- 4 sütunlu **klasör grid** — her kategori bir karo
- Her klasörde **uygulama sayısı** rozeti
- Yukarı kaydır → tüm uygulamalar çekmecesi açılır
- ⚙️ FAB butonu → yönetim ekranına geç

### 📁 Klasör Sistemi
- Her kategori otomatik bir klasöre dönüşür
- Boş kategoriler ana ekranda görünmez
- Klasöre dokun → kategori içeriği bottom sheet olarak açılır
- Bottom sheet içinde 4 sütunlu uygulama grid
- Uygulamaya dokun → direkt başlatılır

### 🔍 Tüm Uygulamalar Çekmecesi
- Yukarı kaydırma hareketi ile açılır
- **Anlık arama** — uygulama adı veya paket adına göre filtreler
- Tüm kategorilerden uygulamalar alfabetik sırada
- Geri tuşu / aşağı kaydırma ile kapanır

### 🎨 Görsel Tasarım
- Her kategori için **özel renk** ve **emoji ikon**
- Glassmorphism tarzı klasör karoları
- Duvar kağıdı üzerinde okunabilirlik için koyu yarı-şeffaf katman
- Edge-to-edge görünüm — durum çubuğu ve gezinme çubuğu şeffaf

---

## Tüm Özellikler (Launcher + Yönetim)

### 🤖 Yapay Zeka Sınıflandırma
- **Online veritabanı** — GitHub'da `app_database.json` ile 1000+ uygulamayı tanır
- **Exact match** — popüler uygulamalar için garantili sınıflandırma
- **Keyword analizi** — uygulama adı ve paket adından kategori tahmini
- Yanlış sınıflandırılanlar için tek dokunuşla düzeltme

### 📂 10 Varsayılan Kategori
| Kategori | Emoji | Renk | Örnekler |
|---|---|---|---|
| Sosyal Medya | 👥 | Mavi | Instagram, WhatsApp, Telegram, Twitter |
| Üretkenlik | 📝 | Yeşil | ChatGPT, DeepSeek, Office, Notion |
| Oyunlar | 🎮 | Turuncu | PUBG Mobile, Clash of Clans |
| Alışveriş | 🛍️ | Pembe | Trendyol, Amazon, Hepsiburada |
| Haber | 📰 | Mavi | Haberler, RSS okuyucular |
| Sağlık | ❤️ | Kırmızı | Spor, fitness, meditasyon |
| Finans | 💰 | Teal | Bankacılık, kripto, yatırım |
| Eğitim | 🎓 | Mor | Duolingo, Khan Academy |
| Araçlar | 🔧 | Camgöbeği | Dosya yöneticisi, VPN |
| Diğer | 📦 | Mor | Sınıflandırılamayan uygulamalar |

### 📋 Yönetim Ekranı
- Tüm uygulamaları listele, filtrele, ara
- Ada ve kurulum tarihine göre sırala
- Uzun basış ile toplu kategori atama
- Kategorileri sıfırla ve yeniden sınıflandır
- Sistem uygulamalarını gizle (varsayılan: gizli)
- Yeni kurulan uygulamaları otomatik algıla

### ♿ Erişilebilirlik Servisi (Gelişmiş)
- AOSP launcher'larında ikon taşıma desteği
- Her adımı gösteren pro debug log sistemi
- MIUI/HyperOS tespiti ve kurulum rehberi
- Android 17 Advanced Protection Mode uyumlu

---

## Mimari

```
app/src/main/java/com/armutlu/apporganizer/
├── data/
│   ├── local/               # Room DB (AppDao, CategoryDao, AppDatabase)
│   ├── remote/              # AppDatabaseService (GitHub'dan JSON indir)
│   └── repository/          # AppRepository — tek veri kaynağı
├── domain/
│   ├── models/              # AppInfo, Category
│   └── usecase/             # AppClassifier, KeywordDatabase
├── presentation/
│   ├── navigation/          # Compose Navigation
│   ├── receivers/           # PackageChangeReceiver
│   ├── ui/
│   │   ├── launcher/        # ← YENİ: Launcher modülü
│   │   │   ├── LauncherActivity.kt   # HOME category activity
│   │   │   ├── LauncherViewModel.kt  # Klasör mantığı + uygulama başlatma
│   │   │   ├── HomeScreen.kt         # Saat, grid, swipe-up drawer
│   │   │   ├── FolderSheet.kt        # Klasör içeriği bottom sheet
│   │   │   └── AppIconView.kt        # Yeniden kullanılabilir ikon bileşeni
│   │   ├── screens/         # AppListScreen, SettingsScreen, CategoryEditorScreen
│   │   └── theme/           # Material 3 tema
│   └── viewmodel/           # AppListViewModel
├── service/                 # LauncherAccessibilityService
└── utils/                   # PackageManagerHelper, LauncherOrganizer
```

**Teknoloji yığını:**

| Katman | Teknoloji |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Mimari | MVVM + StateFlow |
| DI | Hilt |
| Veritabanı | Room |
| Asenkron | Kotlin Coroutines + Flow |
| Navigasyon | Compose Navigation |
| Log | Timber |
| Test | JUnit 4 + MockK |

---

## Launcher Nasıl Çalışır?

### Teknik Detaylar
`LauncherActivity`, Android'in `android.intent.category.HOME` intent filtresiyle kayıtlıdır. Bu sayede Android, Ana Ekran tuşuna basıldığında bu Activity'yi açar.

```xml
<activity android:name=".presentation.ui.launcher.LauncherActivity"
    android:launchMode="singleTask"
    android:stateNotNeeded="true"
    android:theme="@style/Theme.AppOrganizer.Launcher">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

`Theme.AppOrganizer.Launcher` teması:
- `android:windowShowWallpaper="true"` — sistem duvar kağıdını arka planda gösterir
- `android:windowBackground="@android:color/transparent"` — pencere arka planı şeffaf

`LauncherViewModel.buildFolders()` fonksiyonu:
- Veritabanından tüm uygulamaları okur
- Kategorilere göre gruplar (`AppFolder` listesi)
- Boş kategorileri filtreler
- Klasörleri `displayOrder`'a göre sıralar

### Veri Akışı
```
Room DB (AppInfo)
    ↓ Flow<List<AppInfo>>
LauncherViewModel.buildFolders()
    ↓ StateFlow<List<AppFolder>>
HomeScreen (Compose)
    ↓ LazyVerticalGrid
FolderTile → FolderSheet → AppIconView → launchApp()
```

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

## Launcher'ı Etkinleştirme

APK'yı telefonuna kurduktan sonra:

**Yöntem 1 — Ana Ekran Tuşu:**
1. Telefonda Ana Ekran tuşuna bas
2. Açılan "Başlatıcı seç" ekranında **AppOrganizer**'ı seç
3. "Her zaman" seçeneğini işaretle

**Yöntem 2 — Ayarlar:**
```
Ayarlar > Uygulamalar > Varsayılan uygulamalar > Başlatıcı > AppOrganizer
```

**MIUI/HyperOS için ek adımlar:**
```
Uygulama Bilgisi > Otomatik Başlatma: AÇ
Uygulama Bilgisi > Diğer İzinler > Arka planda açılır pencere: İZİN VER
Pil > Pil optimizasyonu: KISITLAMA YOK
```

---

## Testler

```bash
# Tüm birim testleri
./gradlew testDebugUnitTest

# Sadece launcher testleri
./gradlew testDebugUnitTest --tests "com.armutlu.apporganizer.launcher.*"

# Kapsam raporu (JaCoCo)
./gradlew jacocoTestReport
# → app/build/reports/jacoco/jacocoTestReport/html/index.html
```

**Test dosyaları:**
| Dosya | Test Sayısı | Kapsam |
|---|---|---|
| `AppClassifierTest` | 15+ | AI sınıflandırma mantığı |
| `AppInfoTest` | 5+ | Model doğrulamaları |
| `CategoryTest` | 5+ | Kategori operasyonları |
| `KeywordDatabaseTest` | 10+ | Keyword eşleşme algoritması |
| `LauncherViewModelLogicTest` | 13 | `buildFolders` + `buildAllApps` |

`LauncherViewModelLogicTest` özellikle dikkat çekicidir: Android bağımlılığı **sıfır**, saf JVM'de çalışır çünkü `buildFolders()` ve `buildAllApps()` fonksiyonları bağımsız (pure) yazılmıştır.

---

## Yol Haritası

- [ ] Dock (sabitlenmiş uygulamalar satırı)
- [ ] Klasör yeniden adlandırma ve sıralama (düzenleme modu)
- [ ] Çoklu sayfa desteği (pager)
- [ ] Widget desteği
- [ ] Hava durumu widget'ı
- [ ] Uygulama simgesi özelleştirme
- [ ] Yedekleme / geri yükleme (Google Drive)
- [ ] Koyu / açık tema otomatik geçiş

---

## Sürüm Geçmişi

| Sürüm | Tarih | Değişiklikler |
|---|---|---|
| 0.1.0-beta | Haziran 2026 | İlk beta — AI sınıflandırma + ShortcutManager organizasyonu + Launcher |

---

## Katkıda Bulunma

1. Fork'la
2. Branch oluştur: `git checkout -b feature/ozellik-adi`
3. Testleri yaz ve çalıştır: `./gradlew testDebugUnitTest`
4. Commit et ve push yap
5. Pull Request aç

---

## Lisans

MIT © [hekizoglu](https://github.com/hekizoglu/android-folderautomanager)
