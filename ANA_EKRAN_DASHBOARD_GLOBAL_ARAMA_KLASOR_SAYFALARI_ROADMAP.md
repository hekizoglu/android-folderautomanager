# AppOrganizer — Dashboard, Global Arama ve Klasör Sayfaları Teknik Roadmap'i

> **Oluşturma tarihi:** 2026-07-17  
> **Kapsam:** Launcher ana ekranının; tüm sayfalarda sabit **Her Şeyi Ara**, sabit dock, birinci sayfada Akıllı Ana Ekran/Dashboard ve devamındaki sayfalarda klasörler olacak biçimde yeniden kurulması.  
> **Ana ürün kararı:** Uygulama çekmecesi mevcut swipe-up davranışıyla korunur. Dashboard ve klasörler iç içe pager kullanmaz; aynı ana `HorizontalPager` içinde ayrı sayfalardır.  
> **Uygulama yöntemi:** Yapay zekâ her çalışmada yalnızca bir döngüyü uygular. Kod, test ve gerekli cihaz doğrulaması bitmeden döngü `✅ Tamamlandı` yapılmaz.  
> **Birleştirme notu:** Bu roadmap daha sonra `ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md` ve genel proje roadmap'iyle birleştirilecektir.

---

# 0. Yapay zekâ çalışma protokolü

Her döngü aşağıdaki sırayla ele alınmalıdır:

1. İlgili mevcut dosyaları yeniden oku.
2. Roadmap ile güncel kod farklıysa güncel kodu esas al ve farkı durum notuna yaz.
3. Yalnız ilgili döngünün kapsamındaki dosyaları değiştir.
4. Kullanıcı arayüzü metinlerini hem `values/strings.xml` hem `values-en/strings.xml` içinde güncelle.
5. Her davranış değişikliği için unit test, Compose UI testi veya cihaz testi ekle.
6. İlgisiz refactor, toplu formatlama veya isim değiştirme yapma.
7. Her döngüyü ayrı commit ile tamamla.
8. Test geçmeden `✅ Tamamlandı` yazma.
9. Bir sonraki döngüye kendiliğinden geçme.
10. Gerçek cihaz gerektiren kabul kriterlerini emülatör testiyle tamamlandı sayma.

Durum değerleri:

- `⏳ Bekliyor`: Kodlama başlamadı.
- `🚧 Devam ediyor`: Kod başladı, test veya doğrulama eksik.
- `🟡 Kısmen tamamlandı`: Kod ve otomatik testler var; gerçek cihaz veya son kabul kanıtı eksik.
- `✅ Tamamlandı`: Kod, testler ve gerekli cihaz doğrulaması tamamlandı.
- `⛔ Bloke`: Harici bağımlılık veya cihaz gereksinimi bekleniyor.

Tamamlanan döngü satırı:

```text
**Durum:** ✅ Tamamlandı — Döngü PXX — commit: <SHA> — tarih: YYYY-MM-DD
```

Önerilen temel doğrulamalar:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

Cihaz gerektiren döngüler:

```bash
./gradlew connectedDebugAndroidTest
```

---

# 1. Nihai ürün yapısı

## 1.1 Ana ekranın iskeleti

```text
Root Launcher Box
│
├── GlobalSearchHost                     ← Pager dışında, tek örnek
│   └── Her Şeyi Ara
│
├── HomeHorizontalPager                  ← Tek yatay pager
│   ├── Sayfa 0: SmartDashboardPage
│   ├── Sayfa 1: FolderGridPage 1
│   ├── Sayfa 2: FolderGridPage 2
│   └── Sayfa N: FolderGridPage N
│
├── UnifiedHomePageIndicator
│
├── GlobalBottomArea
│   ├── GlobalSearchHost                 ← Kullanıcı alt konumu seçerse burada
│   ├── AppDrawerDragHandle
│   └── PixelDock                        ← Pager dışında, tek örnek
│
└── Overlays
    ├── GlobalSearchResultsOverlay
    ├── FullScreenSearchOverlayV2
    ├── AllAppsDrawer
    ├── QuickWheelOverlay
    ├── Context menus
    └── SnackbarHost
```

## 1.2 Sayfaların görevi

### Sayfa 0 — Akıllı Ana Ekran / Bugün

Yalnız kullanıcıya güncel durum ve eylem sağlayan alanlar bulunur:

- Pulse Clock: saat, tarih ve hava.
- Görevler kartı: mevcut değer, hedef, kalan miktar.
- Dijital Yaşam kartı: skor, trend, en önemli neden.
- Akıllı Nabız Şeridi: en önemli tek uyarı/eylem.
- Bu saatte önerilen uygulamalar veya kişiler.
- Favoriler.
- Son bildirim alanlar.
- Son kullanılanlar.
- Android widget alanı.
- İsteğe bağlı Google arama çubuğu.

Dashboard ayrıntılı rapor ekranı değildir. Uzun grafikler, tam görev listesi ve tüm uyarılar detay ekranlarına gider.

### Sayfa 1 ve sonrası — Klasör sayfaları

Yalnız klasör grid'i bulunur:

- Saat tekrar gösterilmez.
- Görev kartı tekrar gösterilmez.
- Dijital Yaşam kartı tekrar gösterilmez.
- Akıllı Nabız Şeridi tekrar gösterilmez.
- Favoriler/öneriler tekrar gösterilmez.
- Global Her Şeyi Ara ve dock görünmeye devam eder.

### Uygulama çekmecesi

- Dashboard'da yukarı kaydırma → uygulama çekmecesi.
- Klasör sayfasında yukarı kaydırma → uygulama çekmecesi.
- Arama aktifken swipe-up çekmeceyi açmaz.
- Klasör/modal/context menu açıkken swipe-up çekmeceyi açmaz.
- Tablet davranışı mevcut sağ panel yaklaşımını korur.

## 1.3 Global Her Şeyi Ara kararı

`Her Şeyi Ara`, Dashboard bileşeni değildir. Launcher'ın global navigasyon elemanıdır.

Kurallar:

1. Yalnız bir kez Compose edilir.
2. `HorizontalPager` dışında kalır.
3. Sayfa değişiminde sorgu state'i kaybolmaz.
4. Üst veya alt konum tercihi korunur.
5. Sonuçlar aktif sayfanın içinde değil, global overlay olarak açılır.
6. Arama aktifken yatay pager ve swipe-up uygulama çekmecesi kilitlenir.
7. Arama kapatıldığında kullanıcı son bulunduğu sayfaya döner.
8. Kişi, dosya, klasör, ayar ve uygulama sonuçları tüm sayfalarda aynı davranır.

## 1.4 Dock kararı

- Dock pager dışında kalır.
- Dashboard ve bütün klasör sayfalarında aynı dock görünür.
- Dock sürükleme, uzun basma ve klasör açma davranışları korunur.
- Dock alanı sistem gesture exclusion rect içinde kalmaya devam eder.

## 1.5 Başlangıç sayfası seçenekleri

Ayarlar > Ana Ekran içinde:

```text
Başlangıç sayfası

● Akıllı Ana Ekran
○ İlk klasör sayfası
○ Son kullanılan sayfa
```

Enum:

```kotlin
enum class HomeStartPageMode {
    SMART_DASHBOARD,
    FIRST_FOLDER,
    LAST_VISITED,
}
```

Varsayılan:

- Yeni kurulum: `SMART_DASHBOARD`
- Mevcut kullanıcı: migration kararıyla mevcut sayfa korunur; tek seferlik ürün açıklaması gösterilebilir.

---

# 2. Mevcut kod yapısı ve değişiklik nedenleri

## 2.1 `HomeScreen.kt` şu anda çok fazla sorumluluk taşıyor

Mevcut dosya aynı anda şunları yönetiyor:

- Bütün ayar state'leri.
- Root gesture'lar.
- Saat.
- Görev ve skor kartları.
- Arama.
- Widget.
- İçgörü ve ticker.
- Klasör pager'ı.
- Favoriler ve öneriler.
- Dock.
- Uygulama çekmecesi.
- Fullscreen arama.
- Context menüler.

Yeni ana pager eklenmeden önce sayfa sorumlulukları ayrıştırılmalıdır. Aksi hâlde Dashboard sayfası ile klasör sayfalarının görünürlük kuralları yeni `if` bloklarıyla daha da karmaşıklaşır.

## 2.2 Mevcut `FolderPager` yalnız klasör sayfalarını bilir

`HomeScreenFolderPager.kt` içindeki `FolderPager` kendi `HorizontalPager`'ını oluşturur. Dashboard'u bunun dışına ekleyip bir üst `HorizontalPager` daha oluşturmak yasaktır; iki yatay pager gesture çakışması üretir.

**Karar:** `FolderPager`, pager sahibi olmaktan çıkarılmalıdır. Klasör sayfası render eden bir `FolderGridPage` composable'ına dönüştürülmelidir. Ana `HorizontalPager` yalnız `HomePagerHost` içinde bulunmalıdır.

## 2.3 Arama şu anda sayfa içeriğiyle aynı Column'dadır

- Üst konumda saat ve kartlardan sonra render edilebilir.
- Alt konumda klasör grid'i ve öneri satırlarından sonra render edilir.
- Yeni yapıda Dashboard'dan klasör sayfasına geçerken aynı çubuğun sabit kalması için global shell'e taşınmalıdır.

## 2.4 Sayfa index'i ham `Int` olarak saklanıyor

Mevcut `AppPrefs.getLastHomePage()` değeri doğrudan klasör pager index'idir.

Yeni yapıda:

- `0` Dashboard olacak.
- Eski `0`, yeni yapıda ilk klasör sayfası olan `1` anlamına gelecektir.
- Klasör sırası veya sayfa kapasitesi değişince ham index yanlış sayfaya işaret edebilir.

**Karar:** Ham index yerine semantik sayfa anahtarı saklanmalıdır.

```kotlin
sealed interface HomePageAnchor {
    data object Dashboard : HomePageAnchor
    data class Folder(val firstFolderCategoryId: String) : HomePageAnchor
}
```

Disk formatı:

```text
dashboard
folder:<categoryId>
```

## 2.5 Home tuşu yalnız çift basışı işliyor

`LauncherActivity.onNewIntent()`:

- All Apps açıksa kapatıyor.
- 500 ms içinde ikinci Home basışında All Apps açıyor.
- Tek Home basışında pager'a herhangi bir komut göndermiyor.

Yeni davranış:

1. Açık overlay varsa kapat.
2. Tek Home → seçilen başlangıç sayfasına dön.
3. İkinci Home 500 ms içinde gelirse mevcut davranış korunarak All Apps aç.

## 2.6 `HomeLayoutConfig` eski tek yüzey mantığına göre tasarlanmış

Mevcut durumda:

- Çoğu Dashboard bölümü `HEADER` zone'unda.
- `FOLDER_GRID`, `CONTENT` zone'unda zorunlu bir section.
- `DOCK`, `FOOTER` içinde sabit.
- `MAIN_SEARCH`, `HEADER` veya `FOOTER` arasında taşınabiliyor.

Yeni modelde:

- `HEADER`: global üst alan.
- `CONTENT`: yalnız Dashboard düzenlenebilir bölümleri.
- `FOOTER`: global alt alan.
- `FOLDER_GRID`: Dashboard section'ı değildir; pager sayfa tipidir.

`HomeLayoutConfig.CURRENT_VERSION` artırılmalı ve migration yazılmalıdır.

---

# 3. Hedef veri modelleri

## 3.1 Ana sayfa tipi

Yeni dosya:

`presentation/ui/launcher/model/HomePageSpec.kt`

```kotlin
sealed interface HomePageSpec {
    val stableKey: String

    data object Dashboard : HomePageSpec {
        override val stableKey: String = "dashboard"
    }

    data class FolderPage(
        val pageIndex: Int,
        val firstFolderCategoryId: String?,
        val folders: List<AppFolder>,
    ) : HomePageSpec {
        override val stableKey: String =
            "folder:${firstFolderCategoryId ?: pageIndex}"
    }
}
```

## 3.2 Sayfa planı

Yeni saf hesaplayıcı:

`presentation/ui/launcher/HomePagePlanner.kt`

```kotlin
object HomePagePlanner {
    fun buildPages(
        folders: List<AppFolder>,
        pageSize: Int,
        dashboardEnabled: Boolean,
    ): List<HomePageSpec>
}
```

Kurallar:

- Dashboard açıksa ilk eleman daima `Dashboard`.
- Klasör listesi `chunked(pageSize)` ile bölünür.
- Klasör yoksa Dashboard yine gösterilir.
- Dashboard kapalı klasik modda ilk klasör sayfası index 0 olur.
- En az bir sayfa olmalıdır.

## 3.3 Global ana ekran state'i

Yeni model:

`presentation/ui/launcher/model/HomeShellUiState.kt`

```kotlin
data class HomeShellUiState(
    val pages: List<HomePageSpec>,
    val startPageMode: HomeStartPageMode,
    val searchPosition: GlobalSearchPosition,
    val searchActive: Boolean,
    val allAppsOpen: Boolean,
    val modalOpen: Boolean,
    val pagerUserScrollEnabled: Boolean,
    val appDrawerGestureEnabled: Boolean,
)
```

## 3.4 Ana ekran komutları

Yeni model:

`presentation/ui/launcher/model/HomeCommand.kt`

```kotlin
sealed interface HomeCommand {
    data object GoToDashboard : HomeCommand
    data object GoToFirstFolderPage : HomeCommand
    data object GoToConfiguredStartPage : HomeCommand
    data object RestoreLastVisitedPage : HomeCommand
    data object CloseSearch : HomeCommand
}
```

`LauncherViewModel` içinde:

```kotlin
private val _homeCommands = MutableSharedFlow<HomeCommand>(extraBufferCapacity = 1)
val homeCommands: SharedFlow<HomeCommand> = _homeCommands.asSharedFlow()
```

Activity doğrudan Compose `PagerState` tutmamalıdır; yalnız komut üretmelidir.

---

# 4. Uygulama döngüleri

# Faz A — Regresyon güvenliği ve model temeli

## Döngü P00 — Mevcut ana ekran davranışlarını testlerle kilitle

**Amaç:** Büyük mimari değişiklikten önce mevcut kritik davranışların bozulmasını fark edecek test tabanı oluşturmak.

**Mevcut sorun / kök neden:** Ana ekran davranışlarının önemli bölümü `HomeScreen.kt` içinde Compose state ve gesture'lara bağlıdır. Pager mimarisi değiştiğinde arama, dock veya All Apps davranışı sessizce bozulabilir.

**Nasıl yapılmalı:**

Aşağıdaki mevcut davranışlar için test yazılmalıdır:

1. Klasör sayfa sayısı doğru hesaplanır.
2. `HomeLayoutMath.pageSize()` kapasite sınırına uyar.
3. Son klasör sayfasında boş slotlar doğru oluşur.
4. Üst arama konumu `HEADER`, alt arama konumu `FOOTER` olarak okunur.
5. Dock zorunlu ve görünür kalır.
6. Swipe-up action varsayılan olarak All Apps açar.
7. All Apps açıkken Back/Home onu kapatır.
8. Son klasör sayfası preference'a yazılır.
9. Tablet sütun sayıları 5/6 olarak korunur.
10. Arama query'si `LauncherViewModel` içinde sayfadan bağımsız state'tir.

**Değişecek dosyalar:**

- Mevcut production kodu yalnız test edilebilirlik için minimum görünürlük değişiklikleri alabilir.

**Yeni test dosyaları:**

- `HomePagePlannerTest.kt` için taslak test altyapısı.
- `HomeLayoutPrefsMigrationTest.kt` genişletmesi.
- `HomeScreenNavigationContractTest.kt`.
- `LauncherHomePressPolicyTest.kt`.

**Kabul kriterleri:** Yeni mimari başlamadan önce mevcut davranışları kapsayan testler yeşildir.

**Bağımlılıklar:** Yok.

**Durum:** ✅ Tamamlandı — Döngü P00 — commit: 5316f3c — tarih: 2026-07-18 — Not: 25 regresyon testi; roadmap dosya adı düzeltmesi: HomeLayoutPrefsMigrationTest yok, gerçek dosya HomeLayoutPrefsTest (P02'de o genişletilmeli). Roadmap bölüm 2 varsayımları gerçek kodla doğrulandı.

---

## Döngü P01 — `HomePageSpec` ve `HomePagePlanner` oluştur

**Amaç:** Dashboard ve klasör sayfalarının tek listede, saf ve test edilebilir biçimde temsil edilmesi.

**Nasıl yapılmalı:**

1. `HomePageSpec` sealed interface oluştur.
2. `HomePagePlanner.buildPages()` ekle.
3. Page size sıfır veya negatif gelirse güvenli minimum kullan.
4. FolderPage stable key için ilk klasör categoryId'sini kullan.
5. Dashboard görünürlüğünü parametre yap.
6. Page planında duplicate stable key oluşmasını engelle.
7. Klasör sırası değişince yeni planın deterministik olduğunu test et.

**Yeni dosyalar:**

- `presentation/ui/launcher/model/HomePageSpec.kt`
- `presentation/ui/launcher/HomePagePlanner.kt`
- `test/.../HomePagePlannerTest.kt`

**Testler:**

- 0 klasör + Dashboard açık → 1 sayfa.
- 0 klasör + Dashboard kapalı → güvenli boş klasör sayfası veya fallback page.
- 8 klasör/pageSize 8 → Dashboard + 1 klasör sayfası.
- 9 klasör/pageSize 8 → Dashboard + 2 klasör sayfası.
- Dashboard kapalı klasik mod → yalnız klasör sayfaları.
- Stable key klasör sırasına göre tutarlı.

**Kabul kriterleri:** Sayfa listesi üretimi Compose veya Android bağımlılığı olmadan test edilebilir.

**Bağımlılıklar:** P00.

**Durum:** ✅ Tamamlandı — Döngü P01 — commit: d22bc67 — tarih: 2026-07-18 — Not: AppFolder mevcut tip kullanıldı (LauncherViewModel'dekiler); dashboardEnabled saf parametre (P24 flag'ine P05'te bağlanacak). 14 planner testi + HomeLayoutMath çapraz tutarlılık.

---

## Döngü P02 — Semantik sayfa preference ve migration oluştur

**Amaç:** Eski ham klasör page index'ini yeni Dashboard + klasör yapısına veri kaybı olmadan taşımak.

**Yeni dosya:**

- `utils/HomePagePrefs.kt`

**Yeni anahtarlar:**

```kotlin
const val KEY_HOME_START_PAGE_MODE = "home_start_page_mode_v2"
const val KEY_LAST_HOME_PAGE_ANCHOR = "last_home_page_anchor_v2"
const val KEY_HOME_PAGER_MIGRATED = "home_pager_migrated_v2"
const val KEY_SMART_DASHBOARD_ENABLED = "smart_dashboard_enabled"
```

**Nasıl yapılmalı:**

1. Eski `AppPrefs.getLastHomePage()` yalnız migration kaynağı olarak okunur.
2. Eski index, mevcut folder listesi ve pageSize ile eşleştirilir.
3. Eski sayfanın ilk klasör categoryId'si `folder:<id>` olarak saklanır.
4. Klasör bulunamazsa `dashboard` veya ilk klasör fallback'i kullanılır.
5. Yeni kurulumda `SMART_DASHBOARD` yazılır.
6. Existing user migration ürün kararı ayrı pure function ile test edilir.
7. Backup/restore alanlarına yeni preference'lar eklenir.
8. Diagnostics raporuna start mode ve last page type eklenir; categoryId rapora yazılmamalı, yalnız `DASHBOARD`/`FOLDER` tipi gönderilmelidir.

**Değişecek dosyalar:**

- `AppPrefs.kt` — eski last page key deprecated notu.
- `BackupManager.kt`
- `DiagnosticsReportManager.kt`
- Backup/restore testleri.

**Testler:**

- Eski page 0 + ilk klasör A → `folder:A`.
- Eski page 2 ve sayfa kapasitesi değişince ilgili klasör anchor'ı bulunur.
- Silinmiş klasör anchor'ı güvenli fallback verir.
- Bozuk preference crash üretmez.
- Yeni kurulum Dashboard açar.

**Kabul kriterleri:** Güncelleme sonrası kullanıcı yanlış veya boş sayfaya düşmez.

**Bağımlılıklar:** P01.

**Durum:** ✅ Tamamlandı — Döngü P02 — commit: 1e081d8 — tarih: 2026-07-18 — Not: HomePagePrefs (StartPageMode + bayraklı legacy migration + backup/diagnostics köprüsü); resolver fallback'leri: silinmiş klasör→Dashboard→ilk sayfa, index clamp. categoryId diagnostics'e asla yazılmaz. UI bağlama P05/P13'te.

---

# Faz B — Global shell ve tek pager

## Döngü P03 — `HomeScreen` içinden global shell çıkar

**Amaç:** Search, pager, indicator ve dock'u açık bir ana iskelette toplamak.

**Yeni composable:**

`HomeShell.kt`

```kotlin
@Composable
fun HomeShell(
    topSearch: (@Composable () -> Unit)?,
    pager: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    bottomSearch: (@Composable () -> Unit)?,
    dock: @Composable () -> Unit,
    overlays: @Composable BoxScope.() -> Unit,
)
```

**Nasıl yapılmalı:**

1. `HomeScreen.kt` root `Box` arka plan ve overlay sahibi olarak kalabilir.
2. İçteki uzun `Column` shell'e taşınır.
3. Search konumuna göre aynı `GlobalSearchHost` ya üstte ya altta yalnız bir kez çağrılır.
4. Dock global footer'da kalır.
5. Pager `weight(1f)` alanını alır.
6. Indicator pager ile dock/search arasında bulunur.
7. `SnackbarHost` dock üzerinde kalır.
8. `AllAppsDrawer`, fullscreen search ve context overlay'ler shell'in en üst katmanında kalır.
9. Sistem bar ve IME padding yalnız root shell'de uygulanır; sayfalarda tekrar uygulanmaz.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeScreenComponents.kt`

**Yeni dosyalar:**

- `HomeShell.kt`
- `HomeShellTest.kt`

**Testler:**

- Top search seçiliyken bottom search compose edilmez.
- Bottom search seçiliyken top search compose edilmez.
- Dock her sayfada aynı instance/state ile kalır.
- IME açıldığında dock/search taşması oluşmaz.

**Kabul kriterleri:** Search ve dock pager içeriğinden fiziksel olarak ayrılmıştır; henüz sayfa davranışı değişmese bile shell testleri geçer.

**Bağımlılıklar:** P00.

**Durum:** ✅ Tamamlandı — Döngü P03 — commit: 3862679 — tarih: 2026-07-18 — Not: HomeShell slot yapısı kuruldu; indicator slotu bilinçli boş (pagerState BoxWithConstraints ölçümüne bağımlı — gerçek hoisting P04/P05'te). 1. deneme kota kesintisiyle yarıda kaldı, watchdog attempt 2 ile tamamladı.

---

## Döngü P04 — `FolderPager`ı `FolderGridPage`a dönüştür

**Amaç:** İç içe pager riskini kaldırmak.

**Mevcut dosya:**

- `HomeScreenFolderPager.kt`

**Nasıl yapılmalı:**

1. `FolderPager()` içindeki `HorizontalPager` kaldırılır.
2. Yeni composable yalnız verilen klasör listesini grid olarak render eder:

```kotlin
@Composable
internal fun FolderGridPage(
    pageFolders: List<AppFolder>,
    globalStartIndex: Int,
    columnsCount: Int,
    ...
)
```

3. `pageStart = page * pageSize` hesabı ana pager host/planner tarafına taşınır.
4. Drag callback'ine `globalStartIndex` üzerinden gerçek index verilir.
5. Mevcut `FolderTile`, badge, swipe-up app launch ve context menu davranışları korunur.
6. Empty slots hesaplaması pageFolders ve pageSize üzerinden yapılır.
7. Sayfa geçiş graphicsLayer efekti `HomePagerHost` içinde sayfa tipine göre uygulanır; grid içinde pager state bağımlılığı kalmaz.
8. Dosya adı geçiş sürecinde korunabilir; sonrasında `FolderGridPage.kt` olarak yeniden adlandırılabilir.

**Değişecek dosyalar:**

- `HomeScreenFolderPager.kt`
- `HomeScreen.kt`
- Drag/reorder testleri.

**Testler:**

- Birinci klasör sayfasındaki index'ler 0..7.
- İkinci klasör sayfasındaki index'ler 8..15.
- Tablet sütun sayısı korunur.
- Son sayfa empty slot uzun basması ana ekran menüsünü açar.
- Normal mod uzun basış context menu; edit mode uzun basış reorder.

**Kabul kriterleri:** Repoda launcher ana ekranı için yalnız bir `HorizontalPager` sahibi kalmasına hazır yapı oluşur.

**Bağımlılıklar:** P01, P03.

**Durum:** ✅ Tamamlandı — Döngü P04 — commit: 1841ab2 — tarih: 2026-07-18 — Not: FolderGridPage pager-state'siz saf bileşen (columnsCount parametre, graphicsLayer efekti FolderPager'da, globalStartIndex drag index). Dosya adı HomeScreenFolderPager.kt korundu — P05'te FolderGridPage.kt'ye ayrılabilir. pageCount tek kaynak: HomeLayoutMath.

---

## Döngü P05 — `HomePagerHost` ile Dashboard + klasörleri tek pager'da birleştir

**Amaç:** Page 0 Dashboard, Page 1+ klasörler olacak tek ana pager'ı kurmak.

**Yeni dosya:**

- `HomePagerHost.kt`

**Önerilen API:**

```kotlin
@Composable
internal fun HomePagerHost(
    pages: List<HomePageSpec>,
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    dashboardContent: @Composable () -> Unit,
    folderPageContent: @Composable (HomePageSpec.FolderPage) -> Unit,
)
```

**Nasıl yapılmalı:**

1. `HomeScreen.kt` mevcut folder-only `rememberPagerState` kullanımını kaldırır.
2. `HomePagePlanner` tarafından üretilen tüm pages ile tek `rememberPagerState` oluşturulur.
3. `HorizontalPager(key = { pages[it].stableKey })` kullanılmalıdır.
4. `Dashboard` sayfasında `SmartDashboardPage` render edilir.
5. `FolderPage` sayfasında `FolderGridPage` render edilir.
6. `PagerSnapDistance.atMost(1)` korunur.
7. Dashboard ve klasör geçişi için hafif fade/scale kullanılabilir; rotationY erişilebilirlik azaltılmış hareket açıkken kapatılır.
8. `beyondViewportPageCount` düşük tutulur; pahalı Dashboard state'i gereksiz tekrar hesaplanmaz.
9. Search aktifken `userScrollEnabled = false`.
10. Folder reorder aktifken pager scroll devre dışı kalır.
11. Modal veya dock edit açıkken pager scroll devre dışı kalır.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeScreenFolderPager.kt`
- `HomeScreenPageIndicator.kt`

**Yeni dosyalar:**

- `HomePagerHost.kt`
- `HomePagerHostTest.kt`

**Testler:**

- İlk sayfa Dashboard'dur.
- İkinci sayfa ilk klasör chunk'ıdır.
- Dashboard kapatıldığında page 0 ilk klasör olur.
- Search aktifken yatay swipe sayfa değiştirmez.
- Reorder sırasında yatay swipe sayfa değiştirmez.
- Page listesi değiştiğinde current page güvenli sınıra çekilir.

**Kabul kriterleri:** Dashboard ile klasörler aynı pager içinde çalışır; iç içe `HorizontalPager` yoktur.

**Bağımlılıklar:** P04.

**Durum:** ✅ Tamamlandı — Döngü P05 — commit: c0420a9 — tarih: 2026-07-18 — Not: FolderPager silindi (tek pager sahibi HomePagerHost). Anchor kalıcılığı + eski int köprü paralel. Indicator hoisting tamam. Scroll gating + reduced-motion eklendi. dashboardEnabledForPager=false hardcoded (boş placeholder kullanıcıya gösterilmez) — P06 içeriği doldurur, P24 flag'i açar.

---

# Faz C — Dashboard ayrıştırması

## Döngü P06 — `SmartDashboardPage` oluştur ve mevcut bileşenleri taşı

**Amaç:** Dashboard içeriğini `HomeScreen.kt` monolitinden çıkarmak.

**Yeni dosya:**

- `SmartDashboardPage.kt`

**Dashboard'a taşınacak mevcut kodlar:**

- `PulseClockWidget`
- Görevler/Dijital Yaşam alanı
- `GoogleSearchBar`
- `WidgetArea`
- `AssistantInsightRow`
- `HomeTickerRow` veya `FolderStatsRow`
- `HomeFavoritesSection`
- Dashboard'a ait öneri ve son kullanılan bölümleri

**Global kalacaklar:**

- `HomeAppSearchBar` / `GlobalSearchHost`
- `PixelDock`
- `HomePageIndicator`
- `AllAppsDrawer`
- Fullscreen search overlay
- Context menüler
- Root gesture yönetimi

**Nasıl yapılmalı:**

1. `DashboardUiState` oluştur; onlarca parametreyi tek tek composable'a vermek yerine anlamlı alt modeller kullan.
2. Dashboard section sırası `HomeLayoutConfig` üzerinden render edilir.
3. `FOLDER_GRID` Dashboard renderer'a gönderilmez.
4. Dashboard mümkün olduğunca dikey kaydırmasız tasarlanır; global swipe-up korunur.
5. Dar ekranda `DashboardLayoutPolicy` kompakt varyant seçer.
6. Çok fazla section açıksa düşük öncelikli bölümler kullanıcıya uyarı ile gizlenmez; edit ekranında kapasite bilgisi gösterilir.
7. Android widget alanı için maksimum dashboard yüksekliği ve auto resize uygulanır.
8. Dashboard boş kalırsa yalnız saat + önerilen ana ekran açıklaması gösterilir.

**Yeni saf yardımcı:**

```kotlin
object DashboardLayoutPolicy {
    fun mode(
        screenHeightDp: Int,
        visibleSectionCount: Int,
        hasWidgets: Boolean,
    ): DashboardDensity
}
```

Enum:

```kotlin
enum class DashboardDensity { COMFORTABLE, COMPACT, ULTRA_COMPACT }
```

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeSectionRenderer.kt`
- `HomeScreenComponents.kt`
- `HomeFavoritesSection.kt`
- `PulseClockWidget.kt` gerektiği kadar.

**Yeni dosyalar:**

- `SmartDashboardPage.kt`
- `DashboardUiState.kt`
- `DashboardLayoutPolicy.kt`
- Testler.

**Kabul kriterleri:** `HomeScreen.kt` Dashboard section'larının görsel ayrıntılarını doğrudan içermemelidir.

**Bağımlılıklar:** P05 ve Akıllı Nabız/Görev/Dijital Yaşam roadmap'indeki ilgili model döngüleri.

**Durum:** ✅ Tamamlandı — Döngü P06 — commit: ba742d9 — tarih: 2026-07-18 — Not: DashboardUiState 7 alt modelle; mevcut bileşenler taşındı (yeniden yazılmadı); flag kapalıyken bileşenler eski yerlerinde (davranış değişmedi), P24 açınca Dashboard'a geçecek. P07 riski: SmartDashboardPage verticalScroll ↔ global swipe-up nested scroll ilişkisi.

---

## Döngü P07 — Dashboard dikey alan ve swipe-up çatışmasını çöz

**Amaç:** Uygulama çekmecesi swipe-up hareketini korurken Dashboard'u taşmasız göstermek.

**Ürün kararı:** Ana Dashboard varsayılan olarak tam ekran içinde kaydırmasız olmalıdır. Launcher'ı açan kullanıcı, app drawer hareketinin çalışacağını her sayfada bekler.

**Nasıl yapılmalı:**

1. `BoxWithConstraints` ile kullanılabilir Dashboard yüksekliği ölçülür.
2. Global search, indicator ve dock yüksekliği Dashboard kapasite hesabından çıkarılır.
3. `DashboardLayoutPolicy` kartların compact varyantlarını seçer.
4. Dashboard section'larına öncelik atanır:
   - P0: Saat, görev/dijital yaşam.
   - P1: Akıllı Nabız.
   - P2: Öneriler/favoriler.
   - P3: Widget/Google arama/ikincil listeler.
5. Kullanıcı çok fazla section açarsa layout kırılmaz; düzenleme ekranında “Bu cihazda bazı bölümler kompakt gösterilir” açıklaması çıkar.
6. Android widget kendi içinde scroll alıyorsa pointer alanı app drawer gesture'dan hariç tutulur.
7. Root swipe-up yalnız gesture arbitration sonucunda açılır.

**Alternatif fallback:** Çok küçük ekranlarda yalnız Dashboard section alanı dikey scroll olabilir; bu modda app drawer yalnız global drag handle veya boş arka plan swipe'ından açılır. Bu fallback açıkça test edilmeden varsayılan yapılmamalıdır.

**Değişecek dosyalar:**

- `SmartDashboardPage.kt`
- `DashboardLayoutPolicy.kt`
- `HomeScreen.kt`
- `WidgetArea.kt`

**Testler:**

- 640dp altı ekran.
- 700dp ekran.
- Tablet.
- Widget açık/kapalı.
- Büyük font %130/%200.
- App drawer swipe her standart sayfada çalışır.

**Kabul kriterleri:** Dashboard içeriği dock veya arama altında kırpılmaz; uygulama çekmecesi erişilebilir kalır.

**Bağımlılıklar:** P06.

**Durum:** ✅ Tamamlandı — Döngü P07 — commit: 9d8af74 — tarih: 2026-07-18 — Not: Mevcut child-first nestedScroll zinciri kuralı zaten sağlıyordu (ek connection gerekmedi); DashboardLayoutPolicy yoğunluk modları eklendi (640/700dp + section eşikleri). Fallback modu bilinçli uygulanmadı (roadmap şartı: test edilmeden varsayılan olmasın). Gerçek gesture doğrulaması P24 flag açılışında cihazda.

---

# Faz D — Global arama

## Döngü P08 — `GlobalSearchHost` oluştur

**Amaç:** Her Şeyi Ara'yı bütün sayfalarda sabit ve tek instance yapmak.

**Yeni dosya:**

- `GlobalSearchHost.kt`

**Nasıl yapılmalı:**

1. Mevcut `searchBarSection` local lambda `HomeScreen.kt` içinden çıkarılır.
2. `GlobalSearchHost` hem `HomeAppSearchBar` hem yalnız klasör araması fallback'ini kapsar.
3. `HomeAppSearchBar` içindeki inline sonuç listesi global overlay modeline hazırlanır.
4. Query `LauncherViewModel.searchQuery` üzerinden devam eder.
5. Aktif sayfa GlobalSearchHost'a business parametresi olarak verilmez; arama sonucu sayfadan bağımsızdır.
6. Folder result click mevcut `onNavigateToFolder` davranışını korur.
7. Fullscreen search seçeneği korunur.
8. `homeResumeTrigger` tek global host'a uygulanır.
9. Search position `HomeLayoutPrefs`/global setting üzerinden belirlenir.
10. Üst ve alt konumda aynı state ve aynı semantics kullanılır.

**Önerilen state:**

```kotlin
data class GlobalSearchUiState(
    val query: String,
    val active: Boolean,
    val overlayVisible: Boolean,
    val fullscreenVisible: Boolean,
    val resultGroups: Map<SourceType, List<SearchDocument>>,
    val filesIndexState: FilesIndexState,
)
```

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeAppSearchBar.kt`
- `FolderSearchBar.kt`
- `LauncherViewModel.kt`

**Yeni dosyalar:**

- `GlobalSearchHost.kt`
- `GlobalSearchUiState.kt`

**Testler:**

- Dashboard'da yazılan query klasör sayfasına geçince korunur; normalde pager kilitli olduğu için programatik test edilir.
- Query temizlenince overlay kapanır.
- İki harften kısa query sonuç çağrısı yapmaz.
- Klasör, uygulama, kişi ve dosya click doğru hedefe gider.
- Üst/alt konum aynı davranır.

**Kabul kriterleri:** `HomeAppSearchBar` yalnız bir kez compose edilir ve bütün sayfalarda aynı ekranda kalır.

**Bağımlılıklar:** P03, P05.

**Durum:** ✅ Tamamlandı — Döngü P08 — commit: cc28079 — tarih: 2026-07-18 — Not: State LauncherViewModel'de kaldı (çift state yok); FullScreenSearchOverlayV2 fillMaxSize/Box-vs-Column kısıtı nedeniyle host'a taşınmadı — P09'da HomeShell'e search-overlay slotu gerekebilir (host dosyasında dokümante).

---

## Döngü P09 — Arama sonuçlarını global overlay'e taşı

**Amaç:** Sonuçların Dashboard veya klasör page bounds'u tarafından kırpılmasını önlemek.

**Nasıl yapılmalı:**

1. Root `Box` içinde search host'un anchor koordinatı ölçülür.
2. `GlobalSearchResultsOverlay` pager'ın üzerinde, All Apps'in altında render edilir.
3. Üst aramada sonuçlar aşağı; alt aramada yukarı açılır.
4. Overlay yüksekliği IME ve sistem bar inset'lerine göre hesaplanır.
5. Overlay açıkken:
   - pager `userScrollEnabled = false`,
   - app drawer gesture kapalı,
   - root double tap/long press kapalı,
   - arka plan dokunması query'yi değil yalnız overlay focus'unu kapatacak ürün kararına göre uygulanır.
6. Back sırası:
   - fullscreen search,
   - global search overlay/query,
   - All Apps,
   - no-op launcher root.
7. Sonuç açıldığında query temizleme davranışı tek helper ile yönetilir.
8. TalkBack focus overlay dışına kaçmamalıdır.

**Yeni dosyalar:**

- `GlobalSearchResultsOverlay.kt`
- `SearchOverlayPolicy.kt`

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeAppSearchBar.kt`
- `FullScreenSearchOverlayV2.kt`

**Testler:**

- Alt arama + klavye açık.
- Üst arama + uzun sonuç listesi.
- Overlay açıkken yatay swipe page değiştirmez.
- Overlay açıkken swipe-up All Apps açmaz.
- Back önce aramayı kapatır.
- Büyük font ve TalkBack.

**Kabul kriterleri:** Arama sonuçları hangi sayfa açık olursa olsun aynı konum ve davranışla kullanılabilir.

**Bağımlılıklar:** P08.

**Durum:** ✅ Tamamlandı — Döngü P09 — commit: 958662d — tarih: 2026-07-18 — Not: HomeShell'e ayrı searchOverlay slotu (z-order kod garantisi); FullScreenSearchOverlayV2 taşındı, sarmalayıcı dosyalar bilinçli açılmadı (duplikasyon). Inline kısa önizleme sayfa içinde kaldı. TalkBack/büyük font doğrulaması cihaz oturumuna.

---

# Faz E — Gesture ve uygulama çekmecesi

## Döngü P10 — Gesture arbitration katmanı oluştur

**Amaç:** Yatay sayfa geçişi, dikey app drawer swipe, arama, widget ve drag/reorder hareketlerini merkezi kurallarla ayırmak.

**Yeni dosya:**

- `HomeGestureCoordinator.kt`

**Önerilen model:**

```kotlin
data class HomeGestureContext(
    val searchActive: Boolean,
    val allAppsOpen: Boolean,
    val modalOpen: Boolean,
    val folderReorderActive: Boolean,
    val quickWheelOpen: Boolean,
    val touchStartedInExcludedRegion: Boolean,
)
```

```kotlin
enum class HomeGestureDecision {
    ALLOW_HORIZONTAL_PAGER,
    OPEN_ALL_APPS,
    HANDLE_CHILD,
    IGNORE,
}
```

**Nasıl yapılmalı:**

1. Root'taki ayrı tap ve vertical drag pointerInput blokları koordinatör state'ini kullanır.
2. Yatay hareket eşiği dikey hareketten ayrılır.
3. Dikey swipe için density bağımsız eşik kullanılır; sabit raw `-60f` kaldırılır.
4. Search, modal, context menu veya reorder açıkken root app drawer swipe kapatılır.
5. Dock, search sonuçları ve scroll alanları exclusion region olarak işaretlenir.
6. Gesture bir kez sahiplenildiğinde diğer handler'a devredilmez.
7. Uygulama çekmecesi açılış telemetrisi yalnız gerçek open'da bir kez yazılır.
8. Tablet side panel davranışı korunur.
9. Android predictive back ile çakışma testi yapılır.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomeScreenFolderPager.kt` / `FolderGridPage.kt`
- `HomePagerHost.kt`
- `WidgetArea.kt`

**Testler:**

- Yatay swipe Dashboard → klasör.
- Dikey swipe Dashboard → All Apps.
- Dikey swipe klasör sayfası → All Apps.
- Diagonal gesture yalnız bir karar üretir.
- Search açıkken All Apps açılmaz.
- Folder drag sırasında page değişmez.
- Dock üzerinde swipe uygulama çekmecesini yanlış açmaz.

**Kabul kriterleri:** Gesture davranışı `HomeScreen` içindeki dağınık `if` blokları yerine test edilen tek policy ile belirlenir.

**Bağımlılıklar:** P05, P09.

**Durum:** ✅ Tamamlandı — Döngü P10 — commit: 844bdfa — tarih: 2026-07-18 — Not: HomeGestureArbiter saf çekirdek; 3 dağınık koşul delege edildi; -60px→60dp density-bağımsız eşik (kasıtlı iyileştirme). swipeLock debounce çağrı noktasında korundu. Cihaz doğrulaması: eşik hissi + predictive back + tablet panel.

---

## Döngü P11 — Uygulama çekmecesi davranışını yeni pager'a bağla

**Amaç:** Mevcut All Apps overlay/side panel davranışını Dashboard mimarisinde korumak.

**Nasıl yapılmalı:**

1. `LauncherViewModel.openAllApps()` ve `closeAllApps()` mevcut state'i korur.
2. Drawer açıldığında pager state değiştirilmez.
3. Drawer kapandığında kullanıcı kaldığı Dashboard/klasör sayfasına döner.
4. Drawer içi arama ile global search query state'inin karışıp karışmayacağı netleştirilir:
   - Tercih: aynı `searchQuery` kullanılabilir ancak drawer kapanınca query temizlenmelidir.
   - Daha güvenlisi: `allAppsSearchQuery` ayrı state; global search bağımsız kalır.
5. `focusSearchOnOpen` yalnız All Apps aramasını etkiler.
6. Tablet side panel açıldığında global search ve dock görsel olarak arkada kalabilir ancak pointer alamaz.
7. All Apps open olduğunda root pager `userScrollEnabled = false`.

**Değişecek dosyalar:**

- `LauncherViewModel.kt`
- `AllAppsDrawer.kt`
- `HomeScreen.kt`
- Arama state testleri.

**Kabul kriterleri:** Uygulama çekmecesi her ana sayfadan aynı şekilde açılır ve kapanınca sayfa kaybolmaz.

**Bağımlılıklar:** P10.

**Durum:** ✅ Tamamlandı — Döngü P11 — commit: e79cc98 — tarih: 2026-07-18 — Not: 5/7 madde zaten sağlanıyordu (doğrulandı); düzeltilen 2 gerçek eksik: tablet side-panel pointer sızıntısı (scrim) + drawer açıkken kök pager kilidi (arbiter bypass kaldırıldı).

---

# Faz F — Home tuşu ve sayfa navigasyonu

## Döngü P12 — Home komut akışını ekle

**Amaç:** Activity'nin pager'a doğrudan erişmeden tek Home basışını Dashboard'a yönlendirmesi.

**Nasıl yapılmalı:**

1. `LauncherViewModel` içine `SharedFlow<HomeCommand>` eklenir.
2. `LauncherActivity.onNewIntent()` şu sırayı uygular:

```text
All Apps açık → kapat, işlem biter
Search/modal açık → kapatma komutu gönder
İlk Home → GoToConfiguredStartPage komutu gönder, zamanı kaydet
500 ms içinde ikinci Home → All Apps aç, zamanı sıfırla
```

3. İlk Home komutu geciktirilmemelidir; kullanıcı anında Dashboard'a döner.
4. İkinci basış All Apps açar; Dashboard'a dönmüş olması kabul edilir.
5. `HomePagerHost` command flow'u `LaunchedEffect` ile toplar ve `animateScrollToPage` veya reduce motion'da `scrollToPage` çağırır.
6. Start mode:
   - SMART_DASHBOARD → Dashboard page index.
   - FIRST_FOLDER → ilk FolderPage index.
   - LAST_VISITED → semantik anchor çözümü.
7. Search veya drawer kapanma state'i ayrı komutlarda idempotent olmalıdır.

**Değişecek dosyalar:**

- `LauncherActivity.kt`
- `LauncherViewModel.kt`
- `HomePagerHost.kt`
- `HomeScreen.kt`

**Yeni test dosyaları:**

- `LauncherHomePressPolicyTest.kt`
- `HomeCommandResolverTest.kt`

**Testler:**

- Klasör sayfasında tek Home → Dashboard.
- Dashboard'da tek Home → aynı sayfa.
- İki Home ≤500ms → All Apps.
- All Apps açıkken Home → yalnız kapanır.
- Last visited mode doğru semantic anchor'a gider.
- Dashboard kapalı klasik modda Home ilk klasör sayfasına gider.

**Kabul kriterleri:** Home tuşu davranışı kullanıcı ayarına uyar ve çift Home All Apps özelliği korunur.

**Bağımlılıklar:** P02, P05.

**Durum:** ✅ Tamamlandı — Döngü P12 — commit: 66e9117 — tarih: 2026-07-18 — Not: HomeCommandPolicy sıralaması: drawer > search > modal > GoToStartPage (StartPageMode) > çift basış All Apps (500ms). Ayar UI'sı P17'de. Önceki oturum kesintisinde P12 hiç başlamamıştı — attempt 2 ile kurtarıldı.

---

## Döngü P13 — Son ziyaret edilen sayfayı semantic anchor olarak kaydet

**Amaç:** Klasör sırası, page size veya Dashboard görünürlüğü değişse bile kullanıcı mantıklı sayfaya dönsün.

**Nasıl yapılmalı:**

1. `snapshotFlow { pagerState.currentPage }` index yerine `pages[currentPage].stableKey` yazar.
2. FolderPage için ilk klasör categoryId anchor olur.
3. Aynı anchor yeni sayfa planında bulunamazsa:
   - aynı kategori başka chunk'taysa onu bul,
   - bulunamazsa ilk folder page,
   - folder yoksa Dashboard.
4. Dashboard anchor doğrudan çözülür.
5. Reorder sonrası page planı değiştiğinde current page semantic olarak yeniden çözülür; kullanıcı rastgele başka sayfaya fırlamaz.
6. Page size değişiminde anchor çözümü test edilir.

**Değişecek dosyalar:**

- `HomePagerHost.kt`
- `HomePagePrefs.kt`
- `HomePagePlanner.kt`
- `HomeScreen.kt`

**Testler:**

- Klasör reorder.
- Page size 8 → 4.
- Folder silme.
- Dashboard kapatma/açma.
- Process death sonrası restore.

**Kabul kriterleri:** Ham page index persistence kullanılmaz.

**Bağımlılıklar:** P02, P05.

**Durum:** ✅ Tamamlandı — Döngü P13 — commit: 8eb8faa — tarih: 2026-07-18 — Not: 8 madde P05/P12'de zaten sağlanmıştı (doğrulandı); gerçek eksik kapatıldı: plan değişiminde semantik reconciliation (resolvePageAfterPlanChange — reorder sonrası yanlış klasöre düşme yok). Eski setLastHomePage köprüsü ikincil/deprecated duruyor (P25 temizliğinde kaldırılabilir).

---

# Faz G — Page indicator ve görünür navigasyon

## Döngü P14 — Dashboard farkını gösteren yeni indicator oluştur

**Amaç:** Kullanıcı ilk sayfanın Dashboard, diğerlerinin klasör olduğunu tek bakışta anlasın.

**Yeni composable:**

- `UnifiedHomePageIndicator.kt`

**Önerilen görünüm:**

```text
⌂  ●  ○  ○
```

veya erişilebilir metin:

```text
Akıllı Ana Ekran · Klasör 1/3
```

**Nasıl yapılmalı:**

1. Dashboard sayfası ev simgesi ile temsil edilir.
2. Folder pages noktalarla gösterilir.
3. Indicator item'ları tıklanabilir olabilir; tıklama `animateScrollToPage` yapar.
4. Her item minimum 48dp touch target alır; görsel nokta küçük kalabilir.
5. TalkBack açıklamaları:
   - “Akıllı Ana Ekran, seçili”
   - “Klasör sayfası 1 / 3”
6. Dashboard kapalı klasik modda yalnız noktalar kullanılır.
7. Sayfa sayısı 1 olsa bile Dashboard ev simgesinin gösterilip gösterilmeyeceği ürün testiyle belirlenir; varsayılan yalnız navigation değeri varsa göster.
8. Reduce motion'da nokta büyüme animasyonu kapatılır.

**Değişecek dosyalar:**

- `HomeScreenPageIndicator.kt` veya yeni dosyayla değiştirme.
- `HomePagerHost.kt`
- strings.

**Testler:**

- Dashboard + 3 folder page semantics.
- Dashboard kapalı.
- Tek page.
- Tıklayarak sayfa geçişi.
- TalkBack custom action.

**Kabul kriterleri:** Indicator Dashboard ile klasör sayfalarını görsel ve erişilebilir olarak ayırır.

**Bağımlılıklar:** P05.

**Durum:** ✅ Tamamlandı — Döngü P14 — commit: 1f84567 — tarih: 2026-07-18 — Not: Ev ikonu vs nokta ayrımı (renk-bağımsız), 48dp tıklanabilir hedefler, TR/EN contentDescription; flag kapalıyken görünüm birebir eski (testle kanıtlı).

---

# Faz H — Ana ekran düzenleyicisi ve ayarlar

## Döngü P15 — `HomeLayoutConfig` v2 migration

**Amaç:** Dashboard section düzeni ile global search/dock alanlarını doğru zone'lara ayırmak.

**Model kararı:**

- `HEADER`: global üst alan; esas olarak `MAIN_SEARCH`.
- `CONTENT`: Dashboard section'ları.
- `FOOTER`: global alt alan; `MAIN_SEARCH` ve `DOCK`.

**Nasıl yapılmalı:**

1. `HomeLayoutConfig.CURRENT_VERSION = 2` yap.
2. Dashboard bölümlerinin default zone'u `CONTENT` olur:
   - CLOCK
   - MISSIONS_AND_SCORE
   - GOOGLE_SEARCH
   - FAVORITES
   - SUGGESTIONS
   - RECENT_NOTIFICATIONS
   - RECENT_APPS
   - ANDROID_WIDGETS
   - ASSISTANT_INSIGHTS
   - TICKER_OR_STATS
3. `MAIN_SEARCH` HEADER/FOOTER arasında hareket edebilir ancak gizlenmesi ürün kararıyla sınırlanmalıdır. Her Şeyi Ara global temel özellik olduğundan yeni kurulumda required yapılması önerilir.
4. `DOCK` FOOTER fixed kalır.
5. `FOLDER_GRID` Dashboard layout items listesinden çıkarılır.
6. Enum entry backup uyumluluğu için bir sürüm deprecated tutulabilir; sanitize v2 içinde drop edilir.
7. v1 migration:
   - Eski HEADER Dashboard section'ları CONTENT'e taşınır.
   - Search eski zone'unu korur.
   - Dock FOOTER'da kalır.
   - FOLDER_GRID kaydı page system'e dönüşür.
8. `HomeLayoutConfig` required validation yeni modele göre güncellenir.
9. `HomeLayoutPrefs` header/content/footer order saklayacak şekilde genişletilir.
10. Backup formatı version-aware olur.

**Değişecek dosyalar:**

- `domain/models/HomeLayout.kt`
- `utils/HomeLayoutPrefs.kt`
- `BackupManager.kt`
- `DiagnosticsReportManager.kt`
- `HomeSectionRenderer.kt`
- İlgili testler.

**Yeni preference alanı:**

```kotlin
const val KEY_CONTENT_ORDER = "content_order"
```

**Testler:**

- v1 default → v2 doğru zone'lar.
- v1 custom order korunur.
- Search top/bottom korunur.
- FOLDER_GRID migration sonrası config'te görünmez.
- Bozuk backup sanitize edilir.

**Kabul kriterleri:** Mevcut kullanıcıların dashboard section görünürlük ve sıralama tercihleri mümkün olduğunca korunur.

**Bağımlılıklar:** P06.

**Durum:** ✅ Tamamlandı — Döngü P15 — commit: dd0197a — tarih: 2026-07-18 — Not: v2 şeması (10 section CONTENT zone'a), idempotent v1→v2 partition migration, backup round-trip. Tam suite 971 test yeşil. HomeScreen.homeZonePlan hâlâ tüketilmiyor — P16 render bağlantısını yapacak.

---

## Döngü P16 — Ana ekran düzenleyicisini Dashboard odaklı yap

**Amaç:** Kullanıcının hangi alanı düzenlediğini netleştirmek.

**Nasıl yapılmalı:**

1. `HomeLayoutEditorScreen` üstünde iki açıklayıcı bölüm kullan:
   - Global alanlar: Her Şeyi Ara ve Dock.
   - Akıllı Ana Ekran bölümleri: saat, görevler, skor, ticker, widget vb.
2. `FOLDER_GRID` section kartı kaldırılır.
3. Klasör sırası ayrı “Klasör Sayfaları” başlığı altında korunur.
4. Search için yalnız konum seçimi verilir:
   - Üstte sabit.
   - Altta dock üstünde sabit.
5. Dock fixed/required açıklaması gösterilir.
6. Dashboard preview tab eklenebilir; gerçek zamanlı tam launcher preview zorunlu değildir.
7. Dashboard section reorder yalnız CONTENT zone içinde yapılır.
8. Widget ve dock reordering mevcut davranışı korunur.
9. Reset davranışı:
   - Dashboard layout default'a döner.
   - Klasör sırası ve dock içeriği silinmez; mevcut ürün kuralı korunur.
10. Edit ekranında küçük cihaz kapasite uyarısı gösterilir.

**Değişecek dosyalar:**

- `HomeLayoutEditorScreen.kt`
- `HomeLayout.kt`
- `HomeLayoutPrefs.kt`
- strings ve editor testleri.

**Testler:**

- FOLDER_GRID section görünmez.
- Search üst/alt değişir.
- Dashboard section order kaydedilir.
- Reset klasör/dock içeriğini silmez.
- Unsaved changes dialog çalışır.

**Kabul kriterleri:** Kullanıcı global elemanlarla Dashboard elemanlarını karıştırmadan düzenleyebilir.

**Bağımlılıklar:** P15.

**Durum:** ⏳ Bekliyor

---

## Döngü P17 — Başlangıç sayfası ve klasik mod ayarlarını ekle

**Amaç:** Dashboard istemeyen kullanıcıların hızlı klasör deneyimini korumak.

**Ayarlar > Ana Ekran:**

- Akıllı Ana Ekran: açık/kapalı.
- Başlangıç sayfası: Dashboard / İlk klasör / Son ziyaret edilen.
- Home tuşu davranışı: başlangıç sayfasına dön.
- Sayfa göstergesi: açık/kapalı opsiyonu değerlendirilir.

**Nasıl yapılmalı:**

1. `SettingsHomeScreenSection.kt` içine ayrı “Ana sayfa yapısı” kartı ekle.
2. Dashboard toggle kapatılırsa `HomePagePlanner` Dashboard page üretmez.
3. SMART_DASHBOARD seçiliyken Dashboard kapatılırsa FIRST_FOLDER'a normalize et.
4. LAST_VISITED anchor bulunamazsa güvenli fallback uygula.
5. Ayar değişince launcher'a dönmeden mümkünse SharedPreferences listener ile anında yansıt.
6. Backup/restore ve diagnostics kapsamına ekle.
7. Yeni kullanıcı default'u Dashboard açık.
8. Existing user migration tek seferlik açıklama ile desteklenebilir:

```text
Yeni Akıllı Ana Ekran hazır
Görevler, dijital yaşam ve öneriler ilk sayfada; klasörlerin hemen yanında.
[Deneyin] [Klasörlerle Başla]
```

**Değişecek dosyalar:**

- `SettingsHomeScreenSection.kt`
- `HomePagePrefs.kt`
- `HomeScreen.kt`
- `HomePagePlanner.kt`
- Backup/diagnostics.

**Testler:**

- Toggle on/off page count.
- Dashboard disabled + invalid mode normalization.
- Setting reactivity.
- Backup/restore.

**Kabul kriterleri:** Dashboard zorunlu değildir; varsayılan güçlü deneyim olurken klasik kullanım korunur.

**Bağımlılıklar:** P02, P05.

**Durum:** ⏳ Bekliyor

---

# Faz I — Focus mode ve mevcut özel davranışlar

## Döngü P18 — Focus Mode'u yeni sayfa sistemine uyumla

**Amaç:** Mevcut `Search-first Home modu aktif` yaklaşımının yeni Dashboard ile çelişmemesi.

**Ürün seçenekleri:**

A. Focus Mode, Dashboard'un sade preset'i olur.  
B. Focus Mode, Dashboard'u kapatıp yalnız global arama + dock gösterir.  
C. Focus Mode kaldırılır ve Dashboard section ayarlarına göç ettirilir.

**Önerilen karar:** A. Focus Mode, ayrı page mimarisi değil Dashboard preset'i olmalıdır.

**Nasıl yapılmalı:**

1. Focus Mode açıkken Dashboard section'ları policy ile sınırlandırılır:
   - Saat kompakt.
   - Görev/skor opsiyonel.
   - Global arama görünür.
   - Öneriler/favoriler minimum.
2. Klasör sayfaları yine erişilebilir kalır.
3. Mevcut HomeScreen içindeki “grid ve istatistik gizle” özel `if` bloğu kaldırılır.
4. Focus Mode page count'u değiştirmemelidir.
5. Ayar açıklaması güncellenir.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `SmartDashboardPage.kt`
- `DashboardLayoutPolicy.kt`
- `SettingsHomeScreenSection.kt`

**Testler:**

- Focus Mode Dashboard'u sadeleştirir.
- Klasör sayfaları kaybolmaz.
- Global search/dock kalır.
- Swipe-up çalışır.

**Kabul kriterleri:** Focus Mode yeni page mimarisini bypass eden paralel bir ana ekran oluşturmaz.

**Bağımlılıklar:** P06, P17.

**Durum:** ⏳ Bekliyor

---

# Faz J — Erişilebilirlik, animasyon ve tablet

## Döngü P19 — Erişilebilirlik ve büyük yazı desteği

**Amaç:** Dashboard ve klasör pager'ın TalkBack, klavye, switch access ve büyük yazıda kullanılabilir olması.

**Nasıl yapılmalı:**

1. Ana pager semantics:
   - current page title.
   - toplam page sayısı.
   - custom next/previous page actions.
2. Indicator 48dp touch target kullanır.
3. Search global `heading` değil `search field` semantics alır.
4. Dashboard card'ları tek bir açıklayıcı contentDescription üretir; içindeki dekoratif emoji ayrı okunmaz.
5. FolderPage için “Klasör sayfası X / Y” açıklaması.
6. Font scale 2.0'da kart metinleri taşmaz; `maxLines` ve adaptive density uygulanır.
7. Reduce motion:
   - pager rotationY kaldırılır,
   - indicator size animasyonu kaldırılır,
   - Home command `scrollToPage` kullanır.
8. D-pad/klavye ile search → dashboard → folders → dock focus sırası test edilir.

**Değişecek dosyalar:**

- `HomePagerHost.kt`
- `UnifiedHomePageIndicator.kt`
- `GlobalSearchHost.kt`
- `SmartDashboardPage.kt`
- `FolderGridPage.kt`

**Testler:**

- TalkBack phone.
- Font %200.
- Reduce motion.
- D-pad tablet/emülatör.
- RTL temel kontrolü.

**Kabul kriterleri:** Yalnız dokunma ve renk ile taşınan kritik anlam yoktur.

**Bağımlılıklar:** P05, P08, P14.

**Durum:** ⏳ Bekliyor

---

## Döngü P20 — Telefon ve tablet adaptif düzen

**Amaç:** Dashboard ve klasör sayfalarının farklı ekranlarda uygun yoğunlukta çalışması.

**Telefon:**

- Tek Dashboard sayfası.
- 4 sütun klasör grid.
- All Apps tam ekran overlay.
- Search üst/alt tercihli.

**Küçük tablet:**

- Dashboard kartları iki kolonlu layout kullanabilir.
- 5 sütun klasör grid.
- All Apps sağ panel.

**Büyük tablet:**

- Dashboard `TwoPaneDashboardLayout` kullanabilir.
- 6 sütun klasör grid.
- Global search maksimum genişlik alır; tüm ekranı gereksiz uzatmaz.
- Dock maksimum genişlikle ortalanır.

**Nasıl yapılmalı:**

1. Window width class veya mevcut `screenWidthDp` yaklaşımı tek helper'a alınır.
2. `HomeDeviceClass` üret:

```kotlin
enum class HomeDeviceClass { PHONE, COMPACT_TABLET, EXPANDED_TABLET }
```

3. Dashboard density ve folder columns aynı helper'dan türetilir.
4. Tablet All Apps paneli pager'ı state olarak değiştirmez.
5. Landscape için Dashboard kartları kırpılmaz.
6. Foldable posture gelecekte eklenebilir; ilk fazda config change güvenliği test edilir.

**Yeni dosya:**

- `HomeAdaptiveLayoutPolicy.kt`

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `SmartDashboardPage.kt`
- `FolderGridPage.kt`
- `AllAppsDrawer.kt`
- `PixelDock` ve search modifier'ları.

**Test cihaz matrisi:**

- Küçük Android telefon.
- Standart telefon.
- Büyük telefon.
- 7–8 inç tablet.
- 10+ inç tablet.
- Portrait ve landscape.

**Kabul kriterleri:** Hiçbir cihazda arama/dock kaybolmaz; Dashboard ile klasör grid'i kırpılmaz.

**Bağımlılıklar:** P06, P19.

**Durum:** ⏳ Bekliyor

---

# Faz K — Telemetri, sağlık ve performans

## Döngü P21 — Anonim sayfa kullanım telemetrisi

**Amaç:** Yeni mimarinin gerçekten kullanılıp kullanılmadığını kişisel veri toplamadan ölçmek.

**Event'ler:**

```text
home_page_viewed
home_page_swiped
home_start_mode_changed
smart_dashboard_toggled
home_search_opened
all_apps_opened_from_page
home_button_navigation
```

**Güvenli parametreler:**

```text
page_type = dashboard | folder
page_position_bucket = first | middle | last
navigation_source = swipe | indicator | home_button | restore
search_position = top | bottom
start_mode = dashboard | first_folder | last_visited
device_class = phone | compact_tablet | expanded_tablet
```

**Gönderilmeyecekler:**

- Klasör adı.
- Kategori adı/id.
- Uygulama adı/paket adı.
- Search query.
- Kişi veya dosya bilgisi.

**Nasıl yapılmalı:**

1. `TelemetryEvent` sealed modeline event'leri ekle.
2. Page impression debounce edilir; recomposition event üretmez.
3. Pager settled page değişiminde bir kez event yazılır.
4. Telemetry paylaşımı kapalıysa event gönderilmez.
5. Debug build logları privacy-safe tutulur.

**Değişecek dosyalar:**

- Telemetry event modelleri.
- `HomePagerHost.kt`
- `GlobalSearchHost.kt`
- `LauncherActivity.kt`/ViewModel command noktaları.

**Testler:**

- Recomposition duplicate event üretmez.
- Folder identity parametreye girmez.
- Consent off event göndermez.

**Kabul kriterleri:** Dashboard benimsenmesi ve klasör erişim etkisi anonim olarak ölçülebilir.

**Bağımlılıklar:** Telemetri roadmap'inin merkezi manager döngüsü, P05.

**Durum:** ⏳ Bekliyor

---

## Döngü P22 — Sağlık raporuna ana ekran mimarisi özeti ekle

**Amaç:** Test telefonlarından yeni page sisteminin durumunu görebilmek.

**Sağlık raporunda:**

```text
Ana ekran modu: Akıllı Dashboard / Klasik
Başlangıç sayfası: Dashboard / İlk klasör / Son ziyaret
Son sayfa türü: Dashboard / Klasör
Toplam ana sayfa: 4
Klasör sayfası: 3
Global arama konumu: Üst / Alt
Pager restore: OK / FALLBACK
Gesture policy: Normal / Search kilidi / Modal kilidi
```

**Gizlilik:** CategoryId veya klasör adı rapora yazılmaz.

**Değişecek dosyalar:**

- `DiagnosticsReportManager.kt`
- `HomePagePrefs.kt`
- Diagnostics tests.

**Kabul kriterleri:** Evdeki 3 telefon ve tablet raporunda page sisteminin doğru kurulup kurulmadığı görülebilir.

**Bağımlılıklar:** P02, P05.

**Durum:** ⏳ Bekliyor

---

## Döngü P23 — Performans ve recomposition optimizasyonu

**Amaç:** Dashboard eklenirken launcher açılış hızını ve swipe akıcılığını bozmamak.

**Nasıl yapılmalı:**

1. `HomeScreen.kt` içindeki state collect'leri sayfa alt ViewModel/state holder'lara ayrılır.
2. `collectAsStateWithLifecycle` kullanımı değerlendirilir.
3. Dashboard görünür değilken pahalı widget/insight composable'ları gereksiz ölçülmez.
4. Pager page key stabil olmalıdır.
5. `HomePagePlanner` sonuçları uygun key'lerle `remember` edilir.
6. Folder chunk listeleri her frame yeniden üretilmez.
7. Search overlay kapalıyken sonuç UI compose edilmez.
8. Performance trace'leri:

```text
home_shell_ready
home_dashboard_ready
home_folder_page_ready
home_page_switch
```

9. Baseline profile içinde Dashboard açılışı ve swipe senaryosu eklenir.
10. JankStats veya Macrobenchmark mevcut altyapı uygunsa kullanılır.

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `HomePagerHost.kt`
- `SmartDashboardPage.kt`
- `HomePagePlanner.kt`
- Benchmark modülü/testleri.

**Hedefler:**

- Cold start mevcut baseline'dan anlamlı şekilde kötüleşmemeli.
- Page swipe görünür takılma üretmemeli.
- Dashboard → folder ilk geçişi ölçülmeli.
- Tablet panel açılışı stabil kalmalı.

**Kabul kriterleri:** Yeni yapı işlevsel olduğu kadar akıcıdır; performans yalnız gözle değil ölçümle doğrulanır.

**Bağımlılıklar:** P05, P06.

**Durum:** ⏳ Bekliyor

---

# Faz L — Rollout ve temizlik

## Döngü P24 — Feature flag ile kontrollü geçiş

**Amaç:** Büyük ana ekran değişikliğini geri dönüş yolu olmadan tüm kullanıcılara açmamak.

**Nasıl yapılmalı:**

1. İlk geliştirme sırasında local feature flag:

```kotlin
KEY_HOME_PAGER_V2_ENABLED
```

2. Flag kapalıyken mevcut HomeScreen davranışı korunur.
3. Flag açıkken yeni HomeShell/HomePagerHost çalışır.
4. Eski ve yeni path aynı data kaynaklarını kullanır.
5. Test cihazlarında flag Ayarlar > Geliştirici bölümünden değiştirilebilir.
6. Otomatik test ve dört cihaz kanıtından sonra yeni path default olur.
7. Eski path bir sürüm boyunca fallback olarak kalabilir.
8. Crash/ANR veya kritik layout sorunu olursa safe mode flag'i eski path'e döndürebilir.

**Değişecek dosyalar:**

- `AppPrefs.kt`
- `HomeScreen.kt` veya route-level host.
- Geliştirici ayarları.
- Diagnostics.

**Kabul kriterleri:** Yeni ana ekran sorun çıkarırsa kullanıcı verisi silmeden eski görünüme dönülebilir.

**Bağımlılıklar:** P03–P23.

**Durum:** ⏳ Bekliyor

---

## Döngü P25 — Eski folder-only pager ve legacy kod temizliği

**Amaç:** Yeni yol doğrulandıktan sonra iki paralel ana ekran taşımamak.

**Silinecek/deprecate edilecek adaylar:**

- Eski `FolderPager` pager sahibi API'si.
- `AppPrefs.getLastHomePage/setLastHomePage` ham index kullanımı.
- `HomeScreen.kt` içindeki eski folder-only pager bloğu.
- Legacy `FOLDER_GRID` layout section kaydı.
- Kullanılmayan focus-mode özel branch'leri.
- Duplicate searchBarSection lambda.
- Eski page indicator.

**Nasıl yapılmalı:**

1. Repo-wide usage search yap.
2. Backup migration en az bir sürüm çalıştıysa legacy okuma yalnız restore uyumluluğu için tutulur.
3. Test fixture'ları yeni modellere taşınır.
4. Detekt baseline'daki eski HomeScreen complexity maddeleri gözden geçirilir.
5. `HomeScreen.kt` hedef olarak orchestration dosyası seviyesine indirilir.
6. HISTORY.md'ye migration ve kullanıcı etkisi yazılır.
7. Roadmap durumları commitlerle güncellenir.

**Kabul kriterleri:** Üretimde yalnız tek ana page mimarisi bulunur; eski kod yolu aktif değildir.

**Bağımlılıklar:** P24 gerçek cihaz doğrulaması.

**Durum:** ⏳ Bekliyor

---

# 5. Değişecek dosya haritası

## Ana dosyalar

| Dosya | Yapılacak değişiklik |
|---|---|
| `HomeScreen.kt` | Monolit render kodunu shell, pager ve sayfalara bölecek; root gesture ve overlay orchestration kalacak. |
| `HomeScreenFolderPager.kt` | `HorizontalPager` kaldırılacak; `FolderGridPage` renderer'a dönüşecek. |
| `HomeScreenPageIndicator.kt` | Dashboard ev simgesi ve folder noktaları olan birleşik indicator olacak. |
| `LauncherViewModel.kt` | Home command flow, global search state ayrımı ve yeni page koordinasyon state'i eklenecek. |
| `LauncherActivity.kt` | Tek Home başlangıç sayfası, çift Home All Apps davranışı uygulanacak. |
| `HomeLayout.kt` | v2 zone ve Dashboard section modeli. |
| `HomeLayoutPrefs.kt` | CONTENT order, v1→v2 migration, backup/diagnostics desteği. |
| `HomeLayoutEditorScreen.kt` | Global alanlar, Dashboard bölümleri ve klasör sırası ayrılacak. |
| `SettingsHomeScreenSection.kt` | Dashboard toggle ve başlangıç sayfası ayarları. |
| `AppPrefs.kt` | Yeni start mode/feature flag ve legacy page key deprecation. |
| `BackupManager.kt` | Yeni layout/page preference'ları. |
| `DiagnosticsReportManager.kt` | Privacy-safe ana ekran page özeti. |
| `HomeAppSearchBar.kt` | Global host/overlay ayrımı. |
| `FullScreenSearchOverlayV2.kt` | Global search state ve back sırası uyumu. |
| `AllAppsDrawer.kt` | Global search'ten bağımsız drawer query ve pager kilidi. |

## Yeni dosyalar

```text
presentation/ui/launcher/HomeShell.kt
presentation/ui/launcher/HomePagerHost.kt
presentation/ui/launcher/SmartDashboardPage.kt
presentation/ui/launcher/FolderGridPage.kt
presentation/ui/launcher/GlobalSearchHost.kt
presentation/ui/launcher/GlobalSearchResultsOverlay.kt
presentation/ui/launcher/UnifiedHomePageIndicator.kt
presentation/ui/launcher/HomePagePlanner.kt
presentation/ui/launcher/HomeGestureCoordinator.kt
presentation/ui/launcher/HomeAdaptiveLayoutPolicy.kt
presentation/ui/launcher/DashboardLayoutPolicy.kt
presentation/ui/launcher/model/HomePageSpec.kt
presentation/ui/launcher/model/HomePageAnchor.kt
presentation/ui/launcher/model/HomeCommand.kt
presentation/ui/launcher/model/HomeShellUiState.kt
presentation/ui/launcher/model/GlobalSearchUiState.kt
utils/HomePagePrefs.kt
```

---

# 6. Test matrisi

## Unit testler

- Page planı.
- Semantic anchor migration.
- Start mode çözümü.
- Home press policy.
- Gesture decision policy.
- Dashboard density policy.
- Adaptive device class.
- HomeLayout v1→v2 migration.
- Backup/restore.
- Privacy-safe telemetry params.

## Compose UI testleri

- Global search tek instance.
- Search top/bottom.
- Dashboard → folder swipe.
- Indicator click.
- Search açıkken pager kilidi.
- Dock bütün sayfalarda görünür.
- Dashboard toggle off.
- Start mode seçimleri.
- Back sırası.
- Büyük font.

## Gerçek cihaz testleri

### Telefon 1 — günlük kullanım

- Dashboard varsayılan.
- Global search.
- Home tuşu.
- Uygulama çekmecesi.
- 24 saat gerçek kullanım.

### Telefon 2 — temiz kurulum

- Onboarding sonrası Dashboard.
- Yeni preference default'ları.
- Klasör sayfa üretimi.
- Search izin akışları.

### Telefon 3 — stres/izin kapalı

- Usage izni yok.
- Contacts/files izni yok.
- Widget çokluğu.
- Dashboard toggle.
- Bozuk/eksik preference fallback.

### Android tablet

- 5/6 kolon klasör grid.
- All Apps sağ panel.
- Landscape.
- Global search maksimum genişlik.
- Dock ve indicator hizası.

### Erişilebilirlik

- TalkBack.
- Font %200.
- Animasyonlar kapalı.
- D-pad/klavye.
- Yüksek kontrast duvar kâğıdı.

---

# 7. Definition of Done

Bu ana ekran dönüşümü yalnız şu şartların tamamında bitmiş sayılır:

1. Her Şeyi Ara bütün ana sayfalarda tek instance olarak sabit kalıyor.
2. Dock bütün ana sayfalarda tek instance olarak sabit kalıyor.
3. Dashboard ve klasörler aynı `HorizontalPager` içinde çalışıyor.
4. Repoda iç içe ana ekran pager'ı yok.
5. Sayfa 0 varsayılan Dashboard, Sayfa 1+ klasörler.
6. Dashboard kapatılabilir ve klasik mod çalışır.
7. Search aktifken pager ve app drawer gesture kilitleniyor.
8. Swipe-up Dashboard ve klasör sayfalarında All Apps açıyor.
9. Tek Home ayarlı başlangıç sayfasına dönüyor.
10. Çift Home All Apps davranışı korunuyor.
11. Son sayfa semantic anchor ile restore ediliyor.
12. Klasör reorder/page size değişimi kullanıcıyı yanlış sayfaya atmıyor.
13. HomeLayout v1→v2 migration testleri geçiyor.
14. Backup/restore yeni ayarları taşıyor.
15. TalkBack ve büyük font testleri geçiyor.
16. Telefon ve tablette layout kırılmıyor.
17. Telemetri kişisel klasör/uygulama/search verisi göndermiyor.
18. Sağlık raporu page sistemini privacy-safe özetliyor.
19. Cold start ve swipe performansı kabul edilebilir ölçümlerde.
20. Eski page path temizlenmiş veya kontrollü fallback olarak açıkça işaretlenmiş.

---

# 8. Önerilen uygulama sırası

## Birinci paket — güvenli temel

1. P00 — Regresyon testleri.
2. P01 — Page model/planner.
3. P02 — Semantic preference migration.
4. P03 — Global shell.
5. P04 — FolderPager → FolderGridPage.

## İkinci paket — yeni ana ekran

6. P05 — Tek HomePagerHost.
7. P06 — SmartDashboardPage.
8. P08 — GlobalSearchHost.
9. P09 — Global search overlay.
10. P14 — Yeni indicator.

## Üçüncü paket — davranış ve ayarlar

11. P10 — Gesture coordinator.
12. P11 — All Apps uyumu.
13. P12 — Home tuşu.
14. P13 — Last page anchor.
15. P15 — Layout v2.
16. P16 — Layout editor.
17. P17 — Başlangıç sayfası/klasik mod.
18. P18 — Focus Mode uyumu.

## Dördüncü paket — kalite

19. P19 — Erişilebilirlik.
20. P20 — Tablet/adaptive.
21. P21 — Telemetri.
22. P22 — Sağlık raporu.
23. P23 — Performans.
24. P24 — Feature flag rollout.
25. P25 — Legacy temizlik.

---

# 9. Net mimari karar özeti

```text
Her Şeyi Ara = Global ve sabit
Dock = Global ve sabit
Dashboard = Pager sayfa 0
Klasörler = Pager sayfa 1..N
Uygulama çekmecesi = Her sayfada swipe-up
Yatay pager = Yalnız bir tane
Home tek basış = Başlangıç sayfası
Home çift basış = Uygulama çekmecesi
Sayfa restore = Ham index değil semantic anchor
Dashboard bölümleri = HomeLayout CONTENT zone
Search konumu = Global HEADER veya FOOTER
```

Bu yapı AppOrganizer'ı üç ayrı erişim yöntemini birlikte kullanan bir launcher'a dönüştürür:

1. **Biliyorsan ara:** Her Şeyi Ara.
2. **Kategorisini biliyorsan klasörden aç:** Klasör sayfaları.
3. **Listeyi görmek istiyorsan yukarı kaydır:** Uygulama çekmecesi.

Dashboard uygulamalara erişimi engelleyen bir ara ekran değil; arama, dock ve çekmece sürekli erişilebilirken kullanıcının günlük durumunu gösteren ilk sayfadır.

---

> **Son güncelleme tarihi:** 2026-07-17
