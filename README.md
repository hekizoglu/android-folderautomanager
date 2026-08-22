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

### Home V2 (Ana Ekran)

Ana ekran tek yatay pager'da üç bölgeden oluşur: `[Hero Dashboard] → [Widget?] → [Klasörler…]`

- **Hero Dashboard (sayfa 0)** — büyük saat + Türkçe tarih, dijital nabız skoru, görev ilerlemesi ve Smart Access
- **Widget sayfası** — ekli Android widget'ları (varsa)
- **Klasör grid** — adaptif sütunlu sayfalı grid; uygulamalar kategoriye göre otomatik gruplanmış
- **Klasör kartları** — emoji + ad + uygulama sayısı, en sık kullanılan 4 uygulamanın önizleme ikonları, bildirim rozeti (acil bildirimde renkli halka)
- **Hızlı başlat** — klasör kartında hızlı yukarı kaydırma → klasörün en sık açılan uygulaması başlar
- **Sürükle-sırala** — klasör kartına uzun bas + sürükle → kalıcı sıralama (hedef kart vurgulanır)
- **Bağlam menüsü** — dock/önizleme/çekmece ikonuna uzun basma → başlat, dock'a ekle/çıkar, kategori değiştir, gizle, not, favori
- **Swipe up** — Tüm Uygulamalar çekmecesini açar
- **Long press (boş alan)** — yönetim menüsü: duvar kağıdı, ayarlar, dock düzenleme, widget/klasör ekleme, Ana Ekranı Düzenle
- **Dock** — buzlu-cam pill; sabitlenmiş uygulamalar + bağlamsal öneriler (saat dilimi ve kullanım alışkanlığı). Akıllı slot oranı ayarlanabilir (Ayarlar → Dock, 0-3)
- **Öneri Merkezi** — klasör birleştirme, bekleyen sınıflandırmalar, düzeltmeler ve eksik izinler tek kartta (uyarı yoksa gizlenir)
- **Görünüm** — arka plan (duvar kağıdı/düz renk/gradyan) ve metin saydamlığı ayarları anlık yansır
- **Nabız şeridi + banner'lar** — pulse skoru/görev çipleri saat altında; izin ve sınıflandırma uyarıları kapatılabilir şeritte

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
    │   │   ├── launcher/   # HomeShell, AllAppsDrawer, FolderScreen, Hero Dashboard, widget/dock/bağlam menüsü bileşenleri
    │   │   │   └── homev2/ # HomeV2Screen (kompozisyon kökü), saf assembler/state, FolderPageV2, FolderTileV2, DockBarV2, ClockHeaderV2
    │   │   ├── screens/    # AppListScreen, SettingsScreen, OnboardingScreen, CategoryEditorScreen, FolderMergeScreen
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

## Test

- **1449 birim testi** (`./gradlew :app:testDebugUnitTest -PskipGoogleServices=true`): domain mantığı, repository sözleşmeleri, sınıflandırma, dock motoru ve migration'lar.
- **Robolectric görsel UI testleri** (`HomeV2VisualUiTest`): bileşenler gerçek ölçüleriyle render edilir; semantics ağacı yürünerek her düğümün ekran sınırları içinde kaldığı doğrulanır (taşma dedektörü). Dar ekran ve büyük font ölçeği kombinasyonları taranır.
- **Maestro akışları** (`.maestro/`): cihazda uçtan uca davranış ve görsel kanıt (özellikle `07_home_v2_visual_check.yaml` ekran görüntüsü üretir).

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
- [x] Contextual dock (saat dilimi + kullanım bazlı öneriler; akıllı slot oranı ayarı)
- [x] Klasöre swipe-up → en sık kullanılan uygulamayı direkt aç (hızlı başlat)
- [x] Klasör sürükle-sırala (uzun bas + sürükle, kalıcı sıralama)
- [x] Bağlamsal ana ekran: Hero Dashboard + widget sayfası + öneri merkezi
- [ ] Ayarlar: ikon boyutu, grid sütun sayısı
- [ ] Play Store yayını
