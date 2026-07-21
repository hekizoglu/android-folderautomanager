# YENİ HERO DASHBOARD — BİREBİR UYGULAMA ROADMAP

**Durum:** Uygulanacak kesin ürün kararı  
**Hedef depo:** `hekizoglu/android-folderautomanager`  
**Hedef dal:** `main`  
**Tarih:** 21 Temmuz 2026  
**Referans tasarım:** Klasör bulunmayan yeni cam görünümlü ana sayfa  

---

## 0. KESİN KARARLAR

Bu roadmap bir alternatif veya deney değildir. Yeni dashboard tamamlandığında eski dashboard kaldırılacaktır.

### Kesin ürün kararları

1. Sayfa 0 yalnızca yeni Hero Dashboard olacaktır.
2. Eski dashboard görünümü, ayarı, feature flag'i ve geri dönüş seçeneği olmayacaktır.
3. Uygulama sıfırdan yazılmayacaktır.
4. Mevcut ViewModel, repository, Room, arama, kullanım istatistikleri, bildirim dinleyici, Dijital Yaşam motoru, pager ve klasör sistemi korunacaktır.
5. Mevcut sistemin UI katmanı ve dashboard kompozisyonu yerinde dönüştürülecektir.
6. Sayfa 0'da hiçbir klasör, klasör istatistiği veya klasör dock öğesi görünmeyecektir.
7. Klasörler Sayfa 1 ve sonraki pager sayfalarında yaşamaya devam edecektir.
8. Sabit dock yalnız uygulamalardan oluşacaktır.
9. Dinamik öneriler dock içine karıştırılmayacak; yalnız Akıllı Erişim kartında gösterilecektir.
10. Hero Dashboard tamamlandıktan sonra legacy dashboard kodu, state alanları, ayarları ve kullanılmayan testleri temizlenecektir.

### Yeni Sayfa 0 yapısı

```text
Büyük Saat ve Tarih
Dijital Yaşam Kartı
Her Şeyi Ara Kartı
Akıllı Erişim
  - Şimdi
  - Son Açılanlar
  - Bildirimler
Sayfa Göstergesi
Sabit Uygulama Dock'u
```

### Sayfa 0'da olmayacaklar

- Klasör grid'i
- FolderStatsRow
- Klasör satırı
- Klasör önerisi
- Dock klasörü
- AssistantInsightRow
- HomeTickerRow
- RecentInstalls kartı
- Android widget alanı
- WidgetFreeGrid
- Ayrı favoriler satırı
- Ayrı öneriler satırı
- Ayrı son kullanılanlar satırı
- Ayrı bildirim satırı
- İçerik sıralama editörü
- Eski dashboard yoğunluk/section mantığı

---

# 1. MEVCUT MİMARİDEN KORUNACAKLAR

Aşağıdaki sistemler yeniden yazılmayacaktır:

- `LauncherViewModel`
- Uygulama kataloğu ve Room repository
- `UsageStatsHelper`
- `NotificationEventDao`
- `AppNotificationListenerService`
- `HomeIntelligenceCoordinator`
- `DigitalPulseEngine` ve `HomePulseSummary`
- Global arama repository'si
- `FullScreenSearchOverlayV2`
- `HomeShell`
- `HomePagerHost`
- `HomePagePlanner`
- Semantic page anchor sistemi
- Klasör sayfaları
- Klasör düzenleme ve taşıma
- Uygulama açma ve son kullanım timestamp'i
- Firebase Analytics / Crashlytics / Performance
- Kullanıcı yedekleme sistemi
- İkon cache sistemi

## Dönüştürülecek katman

```text
Mevcut veri motorları
        ↓
Yeni Hero Dashboard state adaptörü
        ↓
Yeni Hero UI bileşenleri
```

Bu yaklaşım sıfırdan yazma değildir. Mevcut çalışan sistemin görünüm ve sunum katmanı değiştirilecektir.

---

# 2. ESKİ DASHBOARD'IN KALDIRILMA STRATEJİSİ

## 2.1 Geçici paralel dashboard oluşturulmayacak

Yanlış yaklaşım:

```kotlin
if (heroEnabled) HeroDashboard() else OldDashboard()
```

Bu proje kararında kullanılmayacaktır.

Doğru yaklaşım:

1. `SmartDashboardPage` yeni tasarıma dönüştürülecek.
2. Eski alt bileşen çağrıları tek tek sökülecek.
3. Yeni Hero bileşenleri aynı gerçek state'e bağlanacak.
4. Testler geçince kullanılmayan eski kod silinecek.
5. Kullanıcıya eski dashboard seçeneği sunulmayacak.

## 2.2 Silinecek legacy dashboard davranışları

`SmartDashboardPage.kt` içinden kaldırılacak:

- `countVisibleSections()` kullanımı
- `DashboardLayoutPolicy.mode()` ile section yoğunluğu seçimi
- `dashboardGroupOrder()`
- `DashboardContentGroup`
- `HomeSectionRenderer` ile dashboard section sıralama
- Dashboard içindeki `FolderStatsRow`
- Dashboard içindeki `HomeTickerRow`
- Dashboard içindeki `AssistantInsightRow`
- Dashboard içindeki `WidgetArea`
- Dashboard içindeki `WidgetFreeGrid`
- Dashboard içindeki `HomeFavoritesSection`
- Dashboard içindeki `RecentInstalls`
- `hasAnyContent` eski boş durum hesabı

## 2.3 Legacy state temizliği

Hero Dashboard bağlandıktan sonra `DashboardUiState.kt` içinden artık dashboard tarafından kullanılmayan alanlar kaldırılacaktır:

- `DashboardRecentInstallsState`
- `DashboardInsightsState`
- `DashboardTickerState`
- `contentOrder`
- widget alanına ait dashboard state'i
- eski favorites/section görünürlük boolean'ları

Bu veriler başka ekranlarda kullanılıyorsa ilgili ekranın kendi state modeline taşınacaktır; körlemesine silinmeyecektir.

---

# 3. REFERANS TASARIM VE ÖLÇÜ SİSTEMİ

## 3.1 Referans artboard

```text
Görsel: 941 × 1672 px
Normalize Compose artboard: 360 × 640 dp
Oran: 9:16
```

Kod doğrudan piksel kullanmayacaktır. Tüm ölçüler dp/sp token'larından üretilecektir.

## 3.2 360 × 640 dp referans yerleşimi

| Bileşen | X | Y | Genişlik | Yükseklik |
|---|---:|---:|---:|---:|
| Saat kartı | 54 | 20 | 252 | 114 |
| Dijital Yaşam | 28 | 152 | 304 | 96 |
| Her Şeyi Ara | 28 | 256 | 304 | 74 |
| Akıllı Erişim | 28 | 338 | 304 | 162 |
| Sayfa göstergesi | merkez | 512 | 20 | 8 |
| Dock | 10 | 528 | 340 | 64 |

Not: `HomeShell` sistem bar padding'ini uyguladığı için Y koordinatları güvenli içerik alanının başlangıcına göredir.

## 3.3 Dikey boşluklar

```text
Saat → Dijital Yaşam: 18 dp
Dijital Yaşam → Arama: 8 dp
Arama → Akıllı Erişim: 8 dp
Akıllı Erişim → Noktalar: 12 dp
Noktalar → Dock: 8 dp
```

## 3.4 Yatay boşluklar

```text
Ana kartlar: 28 dp sağ/sol
Saat kartı: ortalanmış 252 dp
Dock: 10 dp sağ/sol
Kart iç padding: 14–16 dp
```

---

# 4. YENİ DOSYA MİMARİSİ

Oluşturulacak dosyalar:

```text
app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/hero/
├── HeroDashboardPage.kt
├── HeroClockCard.kt
├── HeroDigitalLifeCard.kt
├── HeroSearchCard.kt
├── SmartAccessCard.kt
├── SmartAccessAppItem.kt
├── HeroDock.kt
├── PremiumGlassSurface.kt
├── HomeHeroTokens.kt
├── HomeHeroProfile.kt
└── HomeHeroLayoutPolicy.kt
```

Domain katmanı:

```text
app/src/main/java/com/armutlu/apporganizer/domain/home/smartaccess/
├── SmartAccessModels.kt
├── SmartAccessCoordinator.kt
├── SmartAccessRanker.kt
└── SmartAccessDedupePolicy.kt
```

Testler:

```text
app/src/test/.../HomeHeroLayoutPolicyTest.kt
app/src/test/.../SmartAccessRankerTest.kt
app/src/test/.../SmartAccessDedupePolicyTest.kt
app/src/androidTest/.../HeroDashboardBoundsTest.kt
app/src/androidTest/.../HeroDashboardInteractionTest.kt
app/src/androidTest/.../HeroDashboardScreenshotTest.kt
```

---

# 5. TASARIM TOKEN'LARI

`HomeHeroTokens.kt` tek doğruluk kaynağı olacaktır.

## 5.1 Geometri

```kotlin
internal object HomeHeroTokens {
    val ReferenceWidth = 360.dp
    val ReferenceHeight = 640.dp

    val ContentHorizontalPadding = 28.dp

    val ClockWidth = 252.dp
    val ClockHeight = 114.dp
    val ClockCorner = 32.dp

    val DigitalLifeHeight = 96.dp
    val SearchHeight = 74.dp
    val SmartAccessHeight = 162.dp

    val CardCorner = 24.dp
    val SearchCorner = 22.dp

    val DockHorizontalPadding = 10.dp
    val DockHeight = 64.dp
    val DockCorner = 28.dp

    val SectionGap = 8.dp
    val ClockToLifeGap = 18.dp
}
```

Hiçbir Hero composable içinde aynı ölçüler yeniden yazılmayacaktır.

## 5.2 Renkler

```text
Arka plan üst: #020914
Arka plan orta: #07182D
Arka plan alt: #06101F
Sağ sıcak bloom: #FFAD68 / %16
Sol mavi bloom: #167CD5 / %14

Cam dolgu: beyaz / %6.5
Cam soğuk katman: #7CB7FF / %3.5
Cam border: beyaz / %28
Üst parlama: beyaz / %10

Ana metin: beyaz / %96
İkincil metin: beyaz / %65
Pasif metin: beyaz / %46

Aktif tab: #3579E8 / %40
Aktif tab border: #85BCFF / %72
Dijital Yaşam yeşili: #54E67C
İkinci yeşil: #9CEA64
Bildirim rozeti: #FF3B30
```

## 5.3 Tipografi

| Alan | Boyut | Ağırlık |
|---|---:|---|
| Saat | 76 sp | Thin |
| Tarih | 15 sp | Normal |
| Kart başlıkları | 14–15 sp | SemiBold |
| Dijital Yaşam ana mesaj | 18 sp | SemiBold |
| Alt açıklama | 12 sp | Normal |
| Arama placeholder | 13 sp | Normal |
| Tab metni | 12 sp | Medium |
| Uygulama adı | 11 sp | Normal |
| Rozet | 10 sp | Bold |

---

# 6. PREMIUM CAM YÜZEY

Mevcut `GlassCard` global olarak değiştirilmemelidir. Yeni bileşen oluşturulacaktır:

```text
PremiumGlassSurface.kt
```

Katman sırası:

1. Gölge
2. Yarı saydam ana dolgu
3. Soğuk lineer gradient
4. Sıcak ışık yansıması
5. İnce border
6. Üst-sol highlight

API 26 uyumluluğu nedeniyle gerçek arka plan blur'u zorunlu olmayacaktır. API 31+ blur daha sonra opsiyonel optimizasyon olabilir; temel görünüm gradient ve saydam katmanlarla aynı kalmalıdır.

---

# 7. HERO SAAT KARTI

`HeroClockCard.kt`

## Ölçü

```text
252 × 114 dp
Köşe: 32 dp
```

## İçerik

```text
Saat: 76 sp Thin, ortalı
Tarih: 15 sp, saatten 2 dp sonra
Üst padding: 17 dp
Alt padding: 14 dp
```

## Davranış

- Tek dokunma: haftalık rapor
- Uzun basma: launcher yönetim menüsü
- Dakikada bir yenileme
- Her saniye recomposition yok
- Sistem 12/24 saat tercihi desteklenmeli
- Tarih locale'a göre üretilmeli

Mevcut `PulseClockWidget` veri ve action davranışları ayrıştırılarak yeni karta aktarılacaktır. Eski görünüm tamamlandığında silinecektir.

---

# 8. HERO DİJİTAL YAŞAM KARTI

`HeroDigitalLifeCard.kt`

## Ölçü

```text
Genişlik: ekran - 56 dp
Yükseklik: 96 dp
Köşe: 24 dp
```

## İç düzen

```text
Başlık: Dijital Yaşam
Yeşil nokta: 8 dp
Skor halkası: 56 dp
Halka kalınlığı: 7 dp
Skor: 24 sp
Ana mesaj: 18 sp SemiBold
Alt mesaj: 12 sp
Sağ ok dokunma alanı: 48 × 48 dp
```

## Veri

Yeni skor motoru yazılmayacaktır. Kaynak:

```text
HomePulseSummary
```

Durumlar:

- Hazır veri
- Düşük güven
- Veri birikiyor
- Erişim gerekli
- Bayat veri

Kartın dış yüksekliği durum değişince zıplamayacaktır.

---

# 9. HERO ARAMA KARTI

`HeroSearchCard.kt`

## Ölçü

```text
304 × 74 dp
Köşe: 22 dp
İç arama pill yüksekliği: 38 dp
```

## İçerik

```text
Başlık: Her Şeyi Ara
Placeholder: Uygulama, kişi, dosya, ayar ara
Sol ikon: arama
Sağ ikon: filtre/kaynak ayarları
```

## Davranış

Hero kart gerçek text field olmayacaktır. Kartın herhangi bir yerine dokununca:

```kotlin
fullScreenSearchOpen = true
```

Mevcut `FullScreenSearchOverlayV2` açılacaktır. Böylece ana sayfada IME açılıp layout bozulmayacaktır.

Global arama motoru korunacaktır. Sadece giriş yüzeyi yeni tasarıma göre değişecektir.

Klasör sayfalarında arama erişimi devam edecek; oralarda daha kompakt bir arama pill'i kullanılacaktır.

---

# 10. AKILLI ERİŞİM STATE MODELİ

```kotlin
enum class SmartAccessTab {
    NOW,
    RECENT,
    NOTIFICATIONS
}
```

```kotlin
data class SmartAccessUiState(
    val selectedTab: SmartAccessTab,
    val nowApps: List<AppInfo>,
    val recentApps: List<AppInfo>,
    val notificationApps: List<NotificationAccessItem>,
    val notificationTotal: Int,
    val usagePermissionGranted: Boolean,
    val notificationPermissionGranted: Boolean,
    val loading: Boolean
)
```

Sekme Türkçe karşılıkları:

```text
NOW → Şimdi
RECENT → Son Açılanlar
NOTIFICATIONS → Bildirimler
```

Varsayılan sekme her yeni launcher oturumunda `Şimdi` olacaktır. Yeni bildirim geldiğinde sekme otomatik değişmeyecektir.

---

# 11. ŞİMDİ ALGORİTMASI

Mevcut `suggestedApps` ve `UsageStatsHelper` altyapısı kullanılacaktır.

Puanlama:

```text
%45 aynı saat dilimi kullanımı
%25 son kullanım yakınlığı
%20 son 28 günlük kullanım sıklığı
%10 hafta içi / hafta sonu bağlamı
```

Saat dilimleri:

```text
05:00–09:59 Sabah
10:00–13:59 Öğle
14:00–17:59 Öğleden sonra
18:00–21:59 Akşam
22:00–04:59 Gece
```

Çıkarılacaklar:

- Gizli uygulamalar
- Kaldırılmış uygulamalar
- Launch intent'i olmayanlar
- Launcher'ın kendi paketi
- Kullanıcının bu saat için gizlediği uygulamalar
- Tekrar eden packageName'ler

Beş slot doldurma sırası:

1. Saat bazlı adaylar
2. Genel ağırlıklı adaylar
3. Son kullanılanlar
4. Favoriler
5. En yakın kullanım fallback'i

---

# 12. SON AÇILANLAR ALGORİTMASI

Bu sekme kullanım sayısına göre değil, gerçek son açılma zamanına göre sıralanacaktır.

```kotlin
sortedByDescending { it.lastUsedTimestamp }
```

Kurallar:

- Maksimum 5 uygulama
- Timestamp sıfır olanlar dışarıda
- Gizli ve kaldırılmış uygulamalar dışarıda
- Aynı paket bir kez
- Launcher üzerinden açılanlar anında güncellenmeli
- Usage Access varsa launcher dışından açılanlar da hesaba katılmalı

---

# 13. BİLDİRİMLER ALGORİTMASI

Mevcut count bazlı sıralama Hero ana sayfa için kullanılmayacaktır.

Yeni model:

```kotlin
data class PackageNotificationSummary(
    val packageName: String,
    val count: Int,
    val lastPostedAt: Long
)
```

`NotificationEventDao.kt` yeni sorgu:

```sql
SELECT
    packageName,
    COUNT(*) AS count,
    MAX(postedAt) AS lastPostedAt
FROM notification_events
WHERE postedAt >= :since
GROUP BY packageName
ORDER BY lastPostedAt DESC
LIMIT :limit
```

Eski rapor sorguları korunacaktır; bu sorgu ayrıca eklenecektir.

Bildirim sekmesi:

- Son bildirimin zamanına göre sıralanır
- Maksimum 5 uygulama
- İkon üstünde okunmamış sayı rozeti
- Footer'da toplam sayı
- Yeni bildirim gelince sekme otomatik açılmaz

---

# 14. AKILLI ERİŞİM KARTI UI

`SmartAccessCard.kt`

## Dış ölçü

```text
304 × 162 dp
Köşe: 24 dp
```

## Başlık

```text
Sparkle ikon: 18 dp
Başlık: 15 sp SemiBold
Üç nokta menü dokunma alanı: 48 dp
```

## Tab satırı

```text
Görsel yükseklik: 32 dp
Her tab eşit ağırlık
Aktif tab mavi pill
Aktif border: açık mavi
Minimum dokunma hedefi: 48 dp
```

## Uygulama satırı

```text
5 slot
İkon: 48 dp
Etiket: 11 sp
Slot genişliği: 52 dp
Slot aralığı: 5 dp
```

Hesap:

```text
Kart iç genişliği: 304 - 24 = 280 dp
5 × 52 = 260 dp
4 × 5 = 20 dp
Toplam = 280 dp
```

Yardımcı metinler:

```text
Şimdi → Bu saat için önerilenler
Son Açılanlar → En son açtıkların
Bildirimler → Son bildirimler
```

Animasyon:

```text
Tab pill: 180 ms
İçerik fade out: 90 ms
İçerik fade in: 150 ms
```

Reduce Motion durumunda animasyon yapılmayacaktır.

---

# 15. SABİT HERO DOCK

`HeroDock.kt`

## Ölçü

```text
Genişlik: ekran - 20 dp
Yükseklik: 64 dp
Köşe: 28 dp
İç padding: 8 dp
İkon: 48 dp
Slot: 5
```

## Kurallar

- Dock yalnız uygulama içerecek
- Klasör kabul etmeyecek
- Dinamik öneri içermeyecek
- Uygulama isimleri görünmeyecek
- TalkBack açıklamaları korunacak
- Uzun basma ile dock düzenleme açılacak

Örnek varsayılan:

```text
Telefon
WhatsApp
Kamera
Chrome
Mesajlar
```

## Mevcut kullanıcı migration'ı

Dock içinde klasör varsa:

1. Eski dock verisi migration öncesi yedeklenir.
2. Klasör öğeleri Hero dock listesinden çıkarılır.
3. Klasör verisi silinmez.
4. Boş slotlar uygun uygulamalarla doldurulur.
5. Kullanıcının uygulama sırası mümkün olduğunca korunur.
6. Migration tek sefer çalışır.

Hero görünümde `contextualDockPackages` kullanılmayacaktır. Sabit `dockPackages` kullanılacaktır.

---

# 16. KLASÖRLERİN SAYFA 0'DAN ÇIKARILMASI

Sayfa planı:

```text
Sayfa 0: Hero Dashboard
Sayfa 1: Klasörler 1–8
Sayfa 2: Klasörler 9–16
Sayfa 3: Klasörler 17–24
...
```

`HomePagePlanner` ve semantic anchor sistemi korunacaktır.

Kontrol edilmesi gereken kritik nokta:

- Ticker kapalıyken pager seviyesinde çizilen `FolderStatsRow`, Dashboard sayfasında görünmemelidir.
- Bu satır yalnız `HomePageSpec.FolderPage` aktifken render edilmelidir.
- Tercihen `FolderStatsRow` doğrudan klasör sayfası composable'ına taşınmalıdır.

---

# 17. SMARTDASHBOARDPAGE YENİ İÇERİĞİ

`SmartDashboardPage.kt` sonunda yalnız şu kompozisyonu yapmalıdır:

```kotlin
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    HeroDashboardPage(
        clock = state.clock,
        pulse = state.pulse,
        smartAccess = state.smartAccess,
        iconPackPkg = state.iconPackPkg,
        actions = actions,
        modifier = modifier,
    )
}
```

Eski section render mantığı tamamen kaldırılacaktır.

Hero ana sayfada scroll:

- Reference/Tall/Large phone: scroll yok
- Compact phone ve fontScale ≥1.30: kontrollü verticalScroll
- Dock her zaman `HomeShell` seviyesinde sabit

---

# 18. ADAPTİF CİHAZ PROFİLLERİ

```kotlin
enum class HomeHeroProfile {
    COMPACT_PHONE,
    REFERENCE_PHONE,
    TALL_PHONE,
    LARGE_PHONE,
    COMPACT_TABLET,
    EXPANDED_TABLET,
    LANDSCAPE_PHONE,
    ACCESSIBLE
}
```

## Profil seçimi

```text
COMPACT_PHONE:
width < 360 dp veya kullanılabilir height < 620 dp

REFERENCE_PHONE:
width 360–392 dp, height 620–759 dp

TALL_PHONE:
width 360–392 dp, height ≥760 dp

LARGE_PHONE:
width 393–599 dp

COMPACT_TABLET:
width 600–839 dp

EXPANDED_TABLET:
width ≥840 dp

LANDSCAPE_PHONE:
width > height ve width <600 dp

ACCESSIBLE:
fontScale ≥1.30
```

Telefon model adına göre özel kod yazılmayacaktır. Karar yalnız gerçek kullanılabilir dp, orientation ve fontScale üzerinden verilecektir.

## Profil ölçüleri

| Profil | Saat | Dijital Yaşam | Arama | Akıllı Erişim | Dock |
|---|---|---|---|---|---|
| Compact | 222×92 | h84 | h66 | h146 | h58 |
| Reference | 252×114 | h96 | h74 | h162 | h64 |
| Tall | 252×114 | h96 | h74 | h162 | h64 |
| Large phone | 280×124 | h100 | h78 | h170 | h68 |
| Compact tablet | 296×132 | h104 | h80 | h176 | h68 |
| Expanded tablet | 304×136 | h108 | h82 | h180 | h72 |

Tabletlerde dashboard tam ekran genişliğine yayılmayacaktır:

```text
Compact tablet içerik max: 420 dp
Expanded tablet içerik max: 440 dp
Expanded tablet dock max: 520 dp
```

Landscape telefonda iki kolon kullanılacaktır:

```text
Sol: Saat + Dijital Yaşam
Sağ: Arama + Akıllı Erişim
Alt: Sabit dock
```

---

# 19. FONT SCALE VE ERİŞİLEBİLİRLİK

## fontScale 1.00–1.15

Referans görünüm korunur.

## fontScale 1.16–1.29

- Alt mesaj tek satır ellipsis olabilir
- Uygulama etiketleri tek satır
- Kart dış ölçüleri korunur

## fontScale ≥1.30

ACCESSIBLE profile:

- Saat 64–70 sp
- Dijital Yaşam 112 dp
- Arama 82 dp
- Akıllı Erişim 184 dp
- Dashboard scroll edilebilir
- Dock sabit
- Dokunma hedefi minimum 48 dp

Font scale zorla 1.0 yapılmayacaktır.

---

# 20. SİSTEM BARLARI VE CUTOUT

`HomeShell` sistem bar ve IME padding için tek doğruluk kaynağı olmaya devam edecektir.

Hero alt composable'ları tekrar:

- `statusBarsPadding()`
- `navigationBarsPadding()`
- `imePadding()`

uygulamayacaktır.

Gesture ve üç tuşlu navigasyon ayrı test edilecektir.

---

# 21. PERMISSION DURUMLARI

## Usage Access yok

`Şimdi` sekmesi:

- Son kullanılanlar/favoriler fallback'i
- Yardımcı metin: `Daha doğru öneriler için kullanım erişimi ver`
- Kart yüksekliği değişmez

## Notification Access yok

Bildirimler sekmesi:

- Erişim açıklaması
- `Erişim ver` aksiyonu
- Kart yüksekliği değişmez

Aynı oturumda sistem izin penceresi tekrar tekrar açılmayacaktır.

---

# 22. PERFORMANS KURALLARI

1. Saat saniyede bir güncellenmeyecek.
2. İkon sorguları recomposition içinde yapılmayacak.
3. Smart Access sıralaması UI katmanında yapılmayacak.
4. Listeler ViewModel/domain katmanında hazırlanacak.
5. Gradient ve shape nesneleri `remember` edilecek.
6. Tab değişiminde yalnız Smart Access içeriği recompose olacak.
7. Bütün dashboard yeniden ölçülmeyecek.
8. Beş uygulama için mevcut ikon cache kullanılacak.
9. Gerçek blur zorunlu olmayacak.
10. Hero açılış akışı Baseline Profile'a eklenecek.

---

# 23. TELEMETRİ

Yeni event'ler:

```text
home_hero_viewed
home_hero_profile_applied
hero_clock_opened
hero_digital_life_opened
hero_search_opened
smart_access_tab_selected
smart_access_app_launched
smart_access_permission_cta_clicked
hero_dock_app_launched
hero_layout_scroll_fallback_used
```

Gönderilmeyecek hassas alanlar:

- Package name
- Kişi adı
- Telefon numarası
- Dosya adı
- Arama sorgusu

---

# 24. TEST MATRİSİ

## Ekranlar

```text
320×568
360×640
360×720
360×800
393×852
412×915
600×960
840×1280
915×412 landscape
```

## API

```text
26
29
31
33
35
```

## Font scale

```text
1.00
1.15
1.30
1.50
```

## Permission

```text
Usage + Notification açık
Yalnız Usage açık
Yalnız Notification açık
İkisi de kapalı
İzin geri alınmış
```

---

# 25. BOUNDS VE SCREENSHOT TESTLERİ

Test tag'leri:

```text
hero_clock
hero_digital_life
hero_search
smart_access
home_page_indicator
hero_dock
```

360×640 referans kabul sınırları:

```text
Saat: 252×114 dp ±1
Dijital Yaşam: 304×96 dp ±1
Arama: 304×74 dp ±1
Akıllı Erişim: 304×162 dp ±1
Dock: 340×64 dp ±1
```

Golden görüntüler:

```text
hero_reference_now.png
hero_reference_recent.png
hero_reference_notifications.png
hero_compact.png
hero_tall.png
hero_large_phone.png
hero_compact_tablet.png
hero_expanded_tablet.png
hero_accessible.png
hero_permission_missing.png
```

Kabul:

- Ana bounds farkı maksimum 1–2 dp
- Görsel fark hedefi %2.5 altı
- Metin taşması yok
- İkon kırpılması yok
- Kart zıplaması yok

---

# 26. UYGULAMA SIRASI VE COMMIT PLANI

## Commit 1

```text
refactor(home): simplify dashboard state for hero migration
```

## Commit 2

```text
feat(home): add hero tokens and adaptive layout policy
```

## Commit 3

```text
feat(home): add premium glass surface
```

## Commit 4

```text
feat(home): replace pulse clock UI with hero clock card
```

## Commit 5

```text
feat(home): replace digital life UI with hero card
```

## Commit 6

```text
refactor(search): split search launch surface from overlay
```

## Commit 7

```text
feat(home): add hero search card
```

## Commit 8

```text
feat(home): add smart access models and ranking
```

## Commit 9

```text
feat(notifications): add latest notification summaries
```

## Commit 10

```text
feat(home): add smart access tabs and app row
```

## Commit 11

```text
refactor(dock): migrate to fixed app-only hero dock
```

## Commit 12

```text
refactor(home): remove folders and legacy sections from dashboard
```

## Commit 13

```text
refactor(home): replace SmartDashboardPage with hero composition
```

## Commit 14

```text
refactor(home): remove legacy dashboard code and settings
```

## Commit 15

```text
feat(home): add compact tablet landscape and accessible profiles
```

## Commit 16

```text
test(home): add hero policy ranking bounds and screenshot tests
```

## Commit 17

```text
perf(home): optimize hero startup and baseline profile
```

---

# 27. LEGACY TEMİZLİK KONTROL LİSTESİ

Yeni dashboard testleri geçtikten sonra:

- [ ] Eski `DashboardContentGroup` silindi
- [ ] `dashboardGroupOrder()` silindi
- [ ] `countVisibleSections()` silindi
- [ ] Dashboard section-order ayarları silindi
- [ ] Dashboard içindeki widget dalları silindi
- [ ] Dashboard içindeki ticker dalları silindi
- [ ] Dashboard içindeki FolderStatsRow silindi
- [ ] Dashboard içindeki favorites/suggestions satırı silindi
- [ ] Eski dashboard density yalnız başka yerde kullanılmıyorsa silindi
- [ ] Eski Dashboard state alanları silindi
- [ ] Kullanılmayan string kaynakları silindi
- [ ] Kullanılmayan testler silindi veya Hero testlerine çevrildi
- [ ] Feature flag veya eski görünüm seçeneği bırakılmadı
- [ ] Ayarlarda eski dashboard seçimi bulunmuyor
- [ ] Backup migration eski anahtarları güvenli biçimde yok sayıyor

---

# 28. DEFINITION OF DONE

Yeni dashboard tamamlanmış sayılabilmesi için:

- [ ] Sayfa 0 yalnız Hero Dashboard
- [ ] Eski dashboard kod yolu yok
- [ ] Eski dashboard ayarı yok
- [ ] Sayfa 0'da klasör yok
- [ ] Dock içinde klasör yok
- [ ] Saat referans ölçülerinde
- [ ] Dijital Yaşam referans ölçülerinde
- [ ] Arama referans ölçülerinde
- [ ] Akıllı Erişim referans ölçülerinde
- [ ] Şimdi sekmesi çalışıyor
- [ ] Son Açılanlar timestamp'e göre çalışıyor
- [ ] Bildirimler son bildirim zamanına göre çalışıyor
- [ ] Beş uygulama slotu hizalı
- [ ] Dock beş sabit uygulama gösteriyor
- [ ] Klasörler Sayfa 1 ve sonrasında çalışıyor
- [ ] Global arama her sayfadan erişilebilir
- [ ] 320×568 cihazda kırpılma yok
- [ ] 360×640 referans görüntüye uyuyor
- [ ] 412×915 cihazda gereksiz büyüme yok
- [ ] Tabletlerde içerik ortalanıyor
- [ ] Landscape kırılmıyor
- [ ] fontScale 1.5 okunabilir
- [ ] TalkBack doğru
- [ ] Reduce Motion doğru
- [ ] API 26 ve API 35 testleri geçiyor
- [ ] R8 release build geçiyor
- [ ] Kullanıcı klasör ve dock verisi kaybolmuyor
- [ ] Golden screenshot testleri geçiyor
- [ ] Legacy dashboard tamamen kaldırılmış

---

# 29. NİHAİ MİMARİ

```text
HomeScreen
└── HomeShell
    ├── HomePagerHost
    │   ├── Page 0
    │   │   └── SmartDashboardPage
    │   │       └── HeroDashboardPage
    │   │           ├── HeroClockCard
    │   │           ├── HeroDigitalLifeCard
    │   │           ├── HeroSearchCard
    │   │           └── SmartAccessCard
    │   ├── Page 1
    │   │   └── FolderGridPage
    │   ├── Page 2
    │   │   └── FolderGridPage
    │   └── ...
    ├── GlobalSearchOverlay
    ├── HomePageIndicator
    ├── HeroDock
    └── AllApps / Menüler / Snackbar
```

## Son teknik ilke

Yeni Hero Dashboard, mevcut uygulamanın veri ve davranış motorlarının üstünde çalışacaktır. Uygulama yeniden yazılmayacak; ancak eski dashboard görünümü ve ona özel artık gereksiz UI mimarisi tamamen kaldırılacaktır.
