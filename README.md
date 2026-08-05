# AppOrganizer — Android Launcher

Uygulamalarını otomatik klasörlere bölen, kendi kimliğiyle öne çıkan akıllı Android launcher'ı.

---

## Uygulama Nasıl Çalışır?

### Genel Akış

```
Cihaz başlatılır
    └── LauncherActivity açılır
         ├── İlk kez: OnboardingScreen
         │    ├── Karşılama
         │    ├── İzin isteme (bildirim, kullanım istatistikleri)
         │    ├── Launcher seçimi (varsayılan yap)
         │    ├── Tema & font seçimi
         │    └── Bitti → HomeScreen
         └── Sonraki açılışlar: HomeScreen
```

### HomeScreen (Ana Ekran)

- **Saat widget** — Pixel tarzı büyük saat + tarih (Türkçe)
- **Klasör grid** — 4 sütun, uygulamalar kategoriye göre otomatik gruplanmış
- **İzin banner** — eksik izinler varsa uyarı gösterir (kapatılabilir)
- **Swipe up** — Tüm Uygulamalar çekmecesini açar
- **Long press (zemin)** — Yönetim ekranını açar
- **Dock** — Alt kısımda frosted pill, kullanıcı seçimli 5 uygulama
- **Klasör sürükleme** — Düzenleme modunda aktif (iyileştirme planlı)

### Klasörler Nasıl Oluşur?

1. Launcher ilk açıldığında `PackageManagerHelper` cihazda yüklü tüm uygulamaları tarar
2. Her uygulama `AppClassifier` tarafından kategorize edilir:
   - 3702 uygulama tam eşleşme haritası (paket adı → kategori)
   - Anahtar kelime analizi (sosyal medya, oyun, verimlilik vb.)
   - DeepSeek LLM fallback (bilinmeyen uygulamalar)
   - Bilinmeyen uygulamalar → "Kategorisiz"
3. Sonuçlar Room veritabanına kaydedilir
4. `LauncherViewModel` veritabanındaki uygulamaları klasörlere dönüştürür
5. Her klasör bir `FolderTile` olarak grid'de gösterilir

### Tüm Uygulamalar (Niagara Stili)

Swipe up veya "Tümü" ile açılır:

| Özellik | Detay |
|---------|-------|
| **Düzen** | Tek sütun liste — isim baskın, ikon sol |
| **Alfabetik gruplar** | Büyük Teal harf başlıkları (34sp Bold) |
| **A-Z Sidebar** | Sağ kenarda — sürükle → anında scroll + dokunsal geri bildirim |
| **Arama** | Anlık filtre, uygulama adı ve paket adına göre |
| **Sıralama** | A-Z / Kullanım Sayısı / Son Açılan |
| **Bildirim rozetleri** | Kırmızı (acil) / Yeşil (mesaj) / Sarı (güncelleme) |
| **Async ikonlar** | UI thread bloke edilmez — `produceState(IO)` + Accompanist |

### FolderScreen (Klasör Detayı)

Klasöre tıklanınca alt sayfa açılır:
- Klasördeki tüm uygulamalar grid'de
- Uygulama ismine tıkla → direkt başlat
- Kategori adı/emoji/renk özelleştirilebilir

---

## Teknik Mimari

```
app/
└── src/main/java/com/armutlu/apporganizer/
    ├── data/
    │   ├── local/          # Room DB (AppDatabase, AppDao, CategoryDao)
    │   └── repository/     # AppRepository — DB <-> ViewModel köprüsü
    ├── di/                 # Hilt modülleri (AppModule)
    ├── domain/
    │   ├── models/         # AppInfo, Category (Room entity'leri)
    │   └── usecase/
    │       └── classify/   # AppClassifier, KeywordDatabase, AppCategoryRepository
    ├── presentation/
    │   ├── ui/
    │   │   ├── launcher/   # HomeScreen, AllAppsDrawer, FolderTile, FolderScreen, HomeScreenPageIndicator, HomeContentWidthTokens
    │   │   ├── screens/    # AppListScreen, SettingsScreen, OnboardingScreen, CategoryEditorScreen
    │   │   └── theme/      # Theme.kt (DataStore — Material You + custom temalar)
    │   └── viewmodel/      # LauncherViewModel, AppListViewModel
    └── utils/              # PackageManagerHelper, UsageStatsHelper, DockPrefs, BadgeColorEngine, IconPackManager
```

### Kullanılan Teknolojiler

| Teknoloji | Versiyon | Amaç |
|-----------|----------|------|
| Jetpack Compose | BOM 2024.12.01 | UI |
| Room | 2.6.1 | Yerel veritabanı |
| Hilt | 2.52 | Dependency Injection |
| DataStore | 1.0.0 | Tema/font tercihleri |
| Coil | 2.7.0 | Async uygulama ikonu |
| Coroutines + Flow | 1.7.3 | Async işlemler |
| Timber | 5.0.1 | Loglama |

### AppInfo Modeli (Veritabanı)

| Alan | Tür | Açıklama |
|------|-----|----------|
| packageName | String | Birincil anahtar |
| appName | String | Görünen ad |
| categoryId | String | Klasör kategorisi |
| usageCount | Long | Kaç kez açıldı |
| lastUsedTimestamp | Long | Son açılış zamanı |
| notificationCount | Int | Bekleyen bildirim sayısı |
| notificationImportance | Int | Bildirim önceliği (renk için) |
| isSystemApp | Boolean | Sistem uygulaması mı? |

---

## Temalar

| Tema | Primary | Secondary |
|------|---------|-----------|
| Turkuaz (varsayılan) | #00897B | #26C6DA |
| Mor | #7B1FA2 | #CE93D8 |
| Okyanus | #1565C0 | #4FC3F7 |
| Gün Batımı | #E64A19 | #FFCA28 |
| Mono | #424242 | #9E9E9E |

---

## Build & Çalıştırma

```powershell
# Build (Google Services atlanarak - yerel test için)
.\gradlew assembleDebug -PskipGoogleServices

# Cihaza yükle
# Not: adb.exe yolunun PATH'de olduğunu varsayar.
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n "com.armutlu.apporganizer/com.armutlu.apporganizer.presentation.ui.launcher.LauncherActivity"
```

---

## Gelecek Özellikler

- [ ] Online uygulama havuzu (2M+ uygulama kategori DB)
- [ ] Contextual dock (zaman bazlı ikon değişimi — sabah/öğle/gece)
- [ ] Klasöre swipe-up → en sık kullanılan uygulamayı direkt aç
- [ ] Ayarlar: dock düzenleme, ikon boyutu, grid sütun sayısı
- [ ] Play Store yayını
