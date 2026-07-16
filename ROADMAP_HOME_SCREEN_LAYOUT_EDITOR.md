# AppOrganizer — Modüler Ana Ekran Düzenleme Modu Roadmap'i

**Tarih:** 2026-07-15  
**Ürün kararı:** Tam serbest Pixel/Samsung tipi hücre gridi yapılmayacak. İlk sürümde kontrollü, güvenli ve geri alınabilir bir **Ana Ekranı Düzenle** modu geliştirilecek.  
**Genel zorluk:** **5.5 / 10**  
**Durum:** Hüseyin tarafından uygulanması onaylandı; geliştirme backlog'una alındı.

---

## 1. Ürün Kararı

Kullanıcı ana ekranı uzun basarak düzenleme moduna girecek ve şu işlemleri yapabilecek:

- Ana ekran bölümlerinin sırasını değiştirmek.
- İsteğe bağlı bölümleri göstermek veya gizlemek.
- Klasörlerin sırasını sürükleyerek değiştirmek.
- Android uygulama widget'larının sırasını değiştirmek.
- Dock içindeki uygulama ve klasörlerin sırasını değiştirmek.
- Yapılan değişiklikleri kaydetmek veya iptal etmek.
- Tek dokunuşla önerilen varsayılan düzene dönmek.

Bu sistem **tam serbest koordinat yerleşimi değildir**. Kullanıcı öğeleri boş bir `x/y` gridinde istediği hücreye bırakamaz. Bunun yerine ürünün otomatik düzenleme kimliğini koruyan, çakışma üretmeyen ve telefon/tablet boyutlarında güvenli çalışan modüler bölüm sıralaması sunulur.

---

## 2. Neden Tam Serbest Grid Yapılmıyor?

Tam serbest grid aşağıdaki ek sistemleri gerektirir:

- Her öğe için sayfa, hücre, satır, sütun ve span koordinatları.
- Çakışma çözme ve otomatik yer açma algoritması.
- Widget yeniden boyutlandırma tutamaçları.
- Çoklu ana ekran sayfaları arasında sürükleme.
- Ekran boyutu değişince koordinat migration'ı.
- Telefon, tablet, foldable ve yatay ekran için ayrı yerleşim doğrulaması.
- Uygulama ikonunu klasör üstüne bırakarak klasör oluşturma/ekleme.
- Çok daha karmaşık erişilebilirlik ve geri alma sistemi.

Bunların maliyeti ürünün mevcut ihtiyacına göre gereksiz yüksektir. Kontrollü düzenleme modu, kullanıcı değerinin büyük kısmını daha düşük hata ve bakım riskiyle sağlar.

---

## 3. Mevcut Kod Tabanındaki Hazır Altyapı

### 3.1 Klasör sıralama

`HomeScreen.kt` ve `FolderPager` içinde klasörler için uzun basma + sürükleme state'i zaten bulunuyor:

- `dragFromIndex`
- `dragToIndex`
- `draggingFolders`
- `viewModel.reorderFolders(...)`

Bu altyapı korunacak fakat mümkünse normal kullanım sırasında yanlışlıkla tetiklenmemesi için **düzenleme modu aktifken** kullanılacak.

### 3.2 Widget sıralama

`WidgetArea.kt` içinde Android `AppWidgetHostView` öğeleri dikey olarak uzun basıp sürüklenebiliyor ve sıralama `onReorderWidgets` üzerinden kaydediliyor.

Bu mekanizma yeniden yazılmayacak; ortak düzenleme moduna bağlanacak.

### 3.3 Dock düzenleme

`DockEditSheet.kt` şu anda uygulama/klasör ekleme ve çıkarma yapıyor. Mevcut dock öğeleri yatay gösteriliyor ancak kullanıcı sıralarını değiştiremiyor.

Dock sıralaması bu roadmap'in doğrudan kapsamındadır.

### 3.4 Ana ekran bölümleri

`HomeScreen.kt` içinde bölüm sırası doğrudan kodla sabitlenmiş durumda. Mevcut genel sıra:

1. Pulse Clock
2. Görevler + Dijital Yaşam Skoru
3. Üst konumlu arama çubuğu
4. Google arama çubuğu
5. Favoriler / öneriler / son bildirim alanlar / son kullanılanlar
6. Android widget alanı
7. Assistant içgörüleri
8. Ticker veya istatistik bandı
9. Klasör sayfaları
10. Alt konumlu arama çubuğu
11. Dock

Bu sabit sıra, sürüm kontrollü bir kullanıcı yerleşim modeline dönüştürülecek.

---

## 4. MVP Kapsamı

### 4.1 Taşınabilir ana bölümler

İlk sürümde aşağıdaki bölüm kimlikleri tanımlanacak:

```kotlin
enum class HomeSectionId {
    CLOCK,
    MISSIONS_AND_SCORE,
    MAIN_SEARCH,
    GOOGLE_SEARCH,
    FAVORITES,
    SUGGESTIONS,
    RECENT_NOTIFICATIONS,
    RECENT_APPS,
    ANDROID_WIDGETS,
    ASSISTANT_INSIGHTS,
    TICKER_OR_STATS,
    FOLDER_GRID,
    DOCK,
}
```

### 4.2 Zorunlu ve isteğe bağlı bölümler

| Bölüm | Taşınabilir | Gizlenebilir | Açıklama |
|---|---:|---:|---|
| Saat | Evet | Evet | Kullanıcı saatsiz minimal düzen seçebilir |
| Görevler + Skor | Evet | Evet | Tek birleşik modül olarak başlar |
| Ana arama | Evet | Evet | Mevcut üst/alt ayarı migration ile yeni sıraya çevrilir |
| Google arama | Evet | Evet | İsteğe bağlı dış arama modülü |
| Favoriler | Evet | Evet | `HomeFavoritesSection` içinden ayrıştırılacak |
| Öneriler | Evet | Evet | Favorilerden bağımsız sıralanabilir olacak |
| Son bildirim alanlar | Evet | Evet | Mevcut ayar korunacak |
| Son kullanılanlar | Evet | Evet | Mevcut ayar korunacak |
| Android widget'ları | Evet | Evet | Widget'ların kendi iç sırası ayrıca değişebilir |
| Assistant içgörüleri | Evet | Evet | Ticker ile çakışma kuralları korunur |
| Ticker / istatistik | Evet | Evet | Aynı veri alanının tek görünür versiyonu |
| Klasör gridi | Kısıtlı | Hayır | Ürünün temel işlevi; gizlenemez |
| Dock | Hayır | İlk sürümde hayır | Ekranın altında sabit kalır; iç öğeleri sıralanabilir |

---

## 5. Kontrollü Yerleşim Bölgeleri

Ana ekran tamamen tek bir sınırsız liste yapılmayacak. Mevcut klasör pager'ının `weight(1f)` davranışını ve dock'un alt sabitliğini korumak için üç bölge kullanılacak:

```kotlin
enum class HomeLayoutZone {
    HEADER,
    CONTENT,
    FOOTER,
}
```

### HEADER

Taşınabilir modüllerin çoğu burada bulunur:

- Saat
- Görevler + Skor
- Arama
- Google arama
- Favoriler
- Öneriler
- Son bildirim alanlar
- Son kullanılanlar
- Android widget'ları
- Assistant içgörüleri
- Ticker / istatistik

### CONTENT

- Klasör gridi ve klasör sayfa göstergesi
- İlk sürümde bu bölge zorunludur ve tek ana içerik alanıdır

### FOOTER

- Kullanıcı arama bölümünü alta taşımışsa ana arama
- Dock
- Dock ekranın en altında sabit kalır

Bu bölgelendirme, kullanıcıya gerçek kontrol verirken klasör gridinin ekran dışına itilmesini ve dock/IME çakışmasını engeller.

---

## 6. Veri Modeli

### 6.1 Yerleşim öğesi

```kotlin
data class HomeLayoutItem(
    val sectionId: HomeSectionId,
    val zone: HomeLayoutZone,
    val order: Int,
    val visible: Boolean,
    val locked: Boolean = false,
)
```

### 6.2 Yerleşim belgesi

```kotlin
data class HomeLayoutConfig(
    val version: Int,
    val items: List<HomeLayoutItem>,
)
```

### 6.3 Saklama stratejisi

İlk sürümde Room migration açmak yerine ayrı ve sürüm kontrollü bir preference deposu kullanılacak:

```text
HomeLayoutPrefs
├── KEY_HOME_LAYOUT_VERSION
├── KEY_HOME_HEADER_ORDER
├── KEY_HOME_FOOTER_ORDER
├── KEY_HOME_HIDDEN_SECTIONS
└── KEY_HOME_LAYOUT_CUSTOMIZED
```

Önerilen saklama:

- Sıralar: virgülle ayrılmış stabil enum ID listesi.
- Gizlenenler: `StringSet`.
- Layout sürümü: `Int`.
- Kullanıcı düzenledi mi: `Boolean`.

Yerleşim tek bir büyük JSON'a mahkûm edilmeyecek. Bilinmeyen veya yeni eklenen bölüm kimlikleri sanitize edilerek varsayılan konuma eklenir.

### 6.4 Neden ayrı `HomeLayoutPrefs`?

- `AppPrefs.kt` zaten çok büyük ve çok sayıda ilgisiz ayar taşıyor.
- Yerleşim migration kuralları tek dosyada tutulabilir.
- Unit test yazmak kolaylaşır.
- İleride backup ve cihazlar arası aktarım için bağımsız export edilebilir.

---

## 7. Varsayılan Düzen

Yeni kurulum için önerilen sıra:

### Header

1. `CLOCK`
2. `MAIN_SEARCH`
3. `MISSIONS_AND_SCORE`
4. `FAVORITES`
5. `SUGGESTIONS`
6. `RECENT_NOTIFICATIONS`
7. `RECENT_APPS`
8. `ANDROID_WIDGETS`
9. `ASSISTANT_INSIGHTS`
10. `TICKER_OR_STATS`
11. `GOOGLE_SEARCH`

### Content

1. `FOLDER_GRID`

### Footer

1. `DOCK`

Bu sıra, ana ekranın temel değerini şu hiyerarşiyle anlatır:

1. Hızlı bilgi
2. Hızlı arama
3. Bağlamsal erişim
4. Otomatik klasörler
5. Dock

---

## 8. Eski Ayarların Migration'ı

### 8.1 Arama çubuğu üst/alt ayarı

Mevcut `KEY_SEARCH_BAR_POSITION` kaybedilmeyecek.

İlk `HomeLayoutConfig` oluşturulurken:

- Eski değer `TOP` ise `MAIN_SEARCH`, header varsayılan konumuna eklenir.
- Eski değer `BOTTOM` ise `MAIN_SEARCH`, footer'da dock'un hemen üstüne eklenir.
- Kullanıcı yeni düzenleme modunda ilk kez kaydettiğinde yeni layout ana kaynak olur.
- Yeni layout aktifleştikten sonra eski ayar sadece backward compatibility için okunur, UI'da tekrar ayrı kontrol olarak gösterilmez.

Aynı işlevi iki farklı ayarın yönetmesine izin verilmez.

### 8.2 Mevcut görünürlük toggle'ları

Aşağıdaki eski ayarlar ilk migration sırasında `visible` alanına çevrilir:

- Widget alanı
- Favoriler
- Öneriler
- Son bildirim alanlar
- Son kullanılanlar
- Assistant kartları
- Ticker
- Görevler
- Ana arama
- Google arama

İlk sürümde eski toggle setter'ları kaldırılmayabilir; ancak `HomeLayoutPrefs` ile tek karar kaynağına geçiş planı hazırlanmalıdır. İki ayrı state'in birbirini ezmesi engellenmelidir.

---

## 9. Düzenleme Moduna Giriş

### 9.1 Giriş noktası

Ana ekrana uzun basıldığında açılan `HomeLongPressSheet` içine yeni bir üst seviye aksiyon eklenecek:

```text
Ana Ekranı Düzenle
Bölümleri taşı, gizle ve sırala
```

Önerilen ikon:

- `Icons.Default.DashboardCustomize`
- veya `Icons.Default.DragIndicator`

### 9.2 Alternatif giriş

Ayarlar > Ana Ekran bölümüne de aynı editörü açan bir satır eklenebilir:

```text
Ana ekran düzeni
Bölümlerin sırasını ve görünürlüğünü değiştir
```

İlk teslim için uzun basma giriş noktası zorunlu, Ayarlar girişi önerilir.

---

## 10. Düzenleme Modu UX'i

### 10.1 Tam ekran editör

Editör bottom sheet içinde sıkıştırılmayacak. Tam ekran overlay veya ayrı Compose route kullanılacak.

Üst bar:

- `İptal`
- Başlık: `Ana Ekranı Düzenle`
- `Bitti`

Alt aksiyonlar:

- `Varsayılana dön`
- `Gizlenen bölümler`
- Opsiyonel: `Son değişikliği geri al`

### 10.2 Bölüm kartı

Her taşınabilir bölüm edit modunda şu çerçeveye alınır:

```kotlin
@Composable
fun EditableHomeSection(
    item: HomeLayoutItem,
    isDragging: Boolean,
    onMove: (...),
    onVisibilityChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
)
```

Görsel öğeler:

- Sol veya sağ drag handle.
- Bölüm adı etiketi.
- Göz/gizle butonu.
- Kilitli bölümlerde kilit simgesi.
- Taşıma sırasında scale + elevation + alpha animasyonu.

### 10.3 Etkileşim güvenliği

Edit modundayken:

- Uygulamalar açılmaz.
- Klasörler açılmaz.
- Widget içindeki butonlar çalıştırılmaz.
- Arama alanı klavye açmaz.
- Ticker linkleri açılmaz.
- All Apps swipe jesti devre dışı kalır.
- Double tap ve Quick Wheel devre dışı kalır.

Editör yalnız yerleşim değiştirir.

### 10.4 Kaydetme modeli

- Editör açılınca kayıtlı config'in taslak kopyası oluşturulur.
- Sürükleme ve gizleme yalnız taslak state'i değiştirir.
- `Bitti` ile atomik olarak kaydedilir.
- `İptal` veya geri tuşu değişiklikleri atar.
- Kaydedilmemiş değişiklik varken geri tuşunda kısa onay gösterilir.

---

## 11. Sürükle-Bırak Teknik Yaklaşımı

Bu kullanım, uygulamalar arasında veri aktaran genel Android drag-and-drop değil, uygulama içi **pick up and move / reorder** modelidir.

Önerilen teknik yapı:

- Uzun basma sonrası `detectDragGesturesAfterLongPress`.
- Kararlı ve benzersiz `key = sectionId.name`.
- Taşınan öğe için geçici offset.
- Hedef indeks, görünür item merkezleri üzerinden hesaplanır.
- Liste değişince `Modifier.animateItem()` ile placement animasyonu.
- İlk sürümde yeni bir üçüncü taraf reorder bağımlılığı eklenmez.

Mevcut klasör ve widget sürükleme kodu ortak yardımcı sınıfa taşınabilir:

```text
ReorderState<T>
├── draggedKey
├── sourceIndex
├── targetIndex
├── dragOffset
├── move(from, to)
├── commit()
└── cancel()
```

Aynı davranışın Home bölümleri, widget listesi ve dock için üç farklı şekilde kopyalanması engellenmelidir.

---

## 12. HomeScreen Refactor Planı

### 12.1 Büyük `HomeScreen.kt` parçalanacak

Mevcut `HomeScreen.kt` çok fazla state, ayar listener'ı, gesture, overlay ve bölüm render'ını aynı fonksiyonda taşıyor. Layout editörü doğrudan bu dosyaya eklenirse bakım maliyeti artar.

Yeni yapı:

```text
presentation/ui/launcher/home/
├── HomeScreen.kt
├── HomeContent.kt
├── HomeSectionRenderer.kt
├── HomeLayoutEditorScreen.kt
├── EditableHomeSection.kt
├── HomeLayoutTopBar.kt
├── HomeLayoutHiddenSectionsSheet.kt
└── ReorderState.kt
```

Zorunlu birebir klasör taşıması şart değildir; ancak sorumluluklar ayrı Kotlin dosyalarına çıkarılmalıdır.

### 12.2 Renderer

```kotlin
@Composable
fun HomeSectionRenderer(
    sectionId: HomeSectionId,
    state: HomeScreenState,
    actions: HomeScreenActions,
    editMode: Boolean,
)
```

`HomeScreen` içindeki sabit sıralı çağrılar yerine:

```kotlin
layout.headerItems
    .filter { it.visible }
    .sortedBy { it.order }
    .forEach { item ->
        key(item.sectionId.name) {
            HomeSectionRenderer(...)
        }
    }
```

### 12.3 Favoriler bileşeninin ayrıştırılması

`HomeFavoritesSection` şu anda birden fazla satırı tek composable içinde birleştiriyor. Bölümlerin ayrı sıralanabilmesi için şu parçalara ayrılmalı:

- `HomeFavoriteAppsRow`
- `HomeSuggestedAppsRow`
- `HomeRecentNotificationAppsRow`
- `HomeRecentAppsRow`

Ortak ikon çizimi ve context menu callback'leri yardımcı composable'da paylaşılabilir.

---

## 13. Klasör Sıralamasının Edit Moda Alınması

### Yapılacaklar

- Mevcut `FolderPager` drag state'i korunur.
- Normal modda uzun basma klasör menüsünü açmaya devam eder.
- Edit modunda uzun basma/sürükleme klasörü taşır.
- Klasör context menu edit modunda açılmaz.
- Sayfalar arası taşıma ilk sürümde mevcut davranışla sınırlıysa açıkça gösterilir.
- Sayfalar arası drag edge-scroll güvenilir değilse MVP'de yalnız mevcut sayfa içinde reorder yapılır; “Önceki/Sonraki sayfaya taşı” aksiyonu context seçenek olarak korunur.

### Kabul kriteri

- Klasör sırası uygulama kapanıp açılınca korunur.
- Arama filtresi açıkken kalıcı sıralama değiştirilmez.
- Filtrelenmiş klasör listesi üzerinde drag ya kapatılır ya da doğru global indeks eşlemesi yapılır.

---

## 14. Widget Sıralamasının Edit Moda Entegrasyonu

### Mevcut durum

`WidgetArea` zaten kurulu Android widget'larını `AppWidgetHostView` ile gösterir ve kendi iç sırasını değiştirebilir.

### Yapılacaklar

- Widget alanı, ana bölüm olarak başka bölümlerin üstüne/altına taşınabilir.
- Widget alanının içindeki widget'lar ayrıca sıralanabilir.
- Normal modda widget içerikleri etkileşimli kalır.
- Edit modunda widget içerikleri `Box` overlay ile bloke edilir.
- Her widget üzerinde drag handle ve sil butonu görünür.
- Silme için kısa onay veya geri alma snackbar'ı eklenir.
- Widget alanı boşsa editörde “Widget ekle” placeholder'ı gösterilebilir.

### Kapsam dışı

- Serbest `spanX/spanY` grid resize.
- Widget'ı ekranın bağımsız koordinatına yerleştirme.
- Widget'ları klasör grid hücreleriyle karıştırma.

---

## 15. Dock Sıralaması

### Mevcut eksik

`DockEditSheet` ekleme/çıkarma yapıyor ancak mevcut öğelerin sırası değiştirilemiyor ve varsayılan kapasite hâlâ bazı yerlerde 4 olabilir.

### Yapılacaklar

- Mevcut dock öğeleri editörde yatay reorder edilebilir olacak.
- Uygulama ve klasör aynı stabil dock item ID modelini kullanacak.
- `onReorder: (List<String>) -> Unit` callback'i eklenecek.
- `LauncherViewModel.reorderDock(...)` veya eşdeğer fonksiyon oluşturulacak.
- `DockPrefs` tek atomik liste yazacak.
- Beş öğeli adaptif dock roadmap'iyle çakışan hardcoded `maxDock=4` kaldırılacak ve tek config kaynağı kullanılacak.

### Kabul kriteri

- Dock sırası kaydedilir.
- Bağlamsal dock önerileri kullanıcının sabitlediği slotların sırasını bozmaz.
- Klasör dock öğeleri reorder sırasında doğru kimlikle korunur.

---

## 16. Göster/Gizle Politikası

### Kurallar

- Kullanıcı gizlediği bölümün verisini silmez; yalnız ana ekranda gösterimini kapatır.
- Gizlenen bölümler editör altındaki ayrı listeden geri eklenir.
- `FOLDER_GRID` gizlenemez.
- `DOCK` ilk sürümde gizlenemez.
- Tüm taşınabilir üst bölümler gizlenirse klasör grid otomatik olarak üst alana genişler.
- Bir bölüm hem eski toggle ile kapalı hem layout config'te görünürse tek kaynak politikası uygulanır; çelişkili görünüm üretilmez.

### Minimum ana ekran koruması

Kullanıcı yanlışlıkla bütün işlevleri kapatsa bile şu iki alan kalır:

- Klasör gridi
- Dock

Ayrıca uzun basma ile editöre tekrar girilebilir.

---

## 17. Varsayılana Dön ve Geri Alma

### Varsayılana dön

İki aşamalı onay gerektirir:

1. `Önerilen ana ekran düzenine dönülsün mü?`
2. `Bölüm sırası ve görünürlük tercihlerin sıfırlanacak. Klasör, widget ve dock içerikleri silinmeyecek.`

Sıfırlanacaklar:

- Bölüm sırası
- Bölüm görünürlüğü
- Search section zone'u

Sıfırlanmayacaklar:

- Klasör kategorileri
- Klasörlerin içindeki uygulamalar
- Eklenmiş Android widget ID'leri
- Dock içeriği
- Duvar kâğıdı ve tema

### Geri alma

MVP için en az bir seviye undo:

- Drag sonrası snackbar: `Bölüm taşındı — Geri al`
- Gizleme sonrası snackbar: `Bölüm gizlendi — Geri al`

Editör kapanana kadar taslak geçmişinde son işlem saklanabilir.

---

## 18. Erişilebilirlik

Drag yalnız yöntem olmayacak.

Her bölüm için erişilebilir menü:

- Yukarı taşı
- Aşağı taşı
- En üste taşı
- Gizle / göster

TalkBack açıklamaları:

```text
Önerilen uygulamalar bölümü, 4. sırada.
Taşımak için çift dokunup basılı tutun veya işlemler menüsünü açın.
```

Ek kurallar:

- Minimum dokunma hedefi 48dp.
- Sadece renk ile drag state anlatılmaz.
- Reduce Motion açıksa scale/spring animasyonları sadeleştirilir.
- Editör traversal sırası görünür bölüm sırasıyla aynı olur.

---

## 19. Telefon, Tablet ve Küçük Ekran Kuralları

### Telefon

- Editör tam ekran.
- Bölümler tek sütun preview.
- Dock yatay reorder.

### Tablet

- Sol tarafta bölüm listesi, sağ tarafta canlı preview opsiyonel ikinci fazdır.
- MVP'de telefonla aynı tam ekran tek sütun editör yeterlidir.

### Küçük ekran

- Klasör gridinin minimum görünür yüksekliği korunur.
- Çok yüksek widget alanı header'ı tamamen işgal edemez; mevcut auto-resize ve yükseklik sınırlaması uygulanır.
- Ana ekran runtime'da gerekirse üst bölümler dikey kaydırılabilir olmamalıdır; bunun yerine kullanıcı editörde fazla bölümleri gizlemeye yönlendirilir.
- Kapasite aşımlarında açık uyarı verilir.

---

## 20. Backup ve Restore

Yeni yerleşim tercihleri backup kapsamına dahil edilmelidir:

```text
homeLayoutVersion
homeHeaderOrder
homeFooterOrder
homeHiddenSections
homeLayoutCustomized
```

Restore sırasında:

- Bilinmeyen bölüm ID'leri yok sayılır.
- Yeni sürümde eklenen bölüm ID'si varsayılan konumuna eklenir.
- Zorunlu bölümler eksikse otomatik geri eklenir.
- Eski backup'ta layout alanı yoksa mevcut ayarlardan migration yapılır.

---

## 21. Tanılama Raporu

`DiagnosticsReportManager` içine kişisel veri içermeyen özet eklenir:

```text
Home layout version: 1
Customized: true
Header order: CLOCK, MAIN_SEARCH, MISSIONS_AND_SCORE, ...
Hidden sections: GOOGLE_SEARCH, ASSISTANT_INSIGHTS
Search zone: HEADER
Widget count: 2
Dock item count: 5
```

Paket adları, widget sağlayıcı adları veya kullanıcı aramaları varsayılan rapora eklenmez.

---

## 22. Dosya Haritası

### Yeni dosyalar

```text
app/src/main/java/com/armutlu/apporganizer/domain/models/HomeSectionId.kt
app/src/main/java/com/armutlu/apporganizer/domain/models/HomeLayoutConfig.kt
app/src/main/java/com/armutlu/apporganizer/utils/HomeLayoutPrefs.kt
app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeLayoutEditorScreen.kt
app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeSectionRenderer.kt
app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/EditableHomeSection.kt
app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/ReorderState.kt
app/src/test/java/com/armutlu/apporganizer/utils/HomeLayoutPrefsTest.kt
```

### Değişecek dosyalar

```text
HomeScreen.kt
HomeScreenOverlays.kt
HomeLongPressSheet.kt
HomeScreenFavorites.kt
WidgetArea.kt
DockEditSheet.kt
LauncherViewModel.kt
DockPrefs.kt
AppPrefs.kt
BackupManager.kt
DiagnosticsReportManager.kt
AppNavigation.kt veya launcher overlay route yapısı
values/strings.xml veya ayrı home_layout_strings.xml
values-en/strings.xml veya ayrı home_layout_strings.xml
```

---

## 23. Sprint Planı

## 23.0 Cron Görev Panosu

Bu bölüm cron otomasyonu için tek tek işlenebilir atomik görev listesidir. Her görev tamamlandığında yalnız kendi `**Durum:** ⏳ Bekliyor` satırı güncellenir; tamamlanmayan veya dış doğrulama isteyen işler `⛔ Bloke` ya da `🟡 Kısmen tamamlandı` bırakılır.

### H1.1 — Home layout domain modeli

**Durum:** ✅ Tamamlandı

**Kapsam:** `HomeSectionId`, `HomeLayoutZone`, `HomeLayoutItem` ve `HomeLayoutConfig` modellerini ekle; zorunlu/gizlenebilir/taşınabilir bölüm kurallarını tipli hale getir.

**Kabul kriteri:** Varsayılan bölüm listesi tek kaynak olur, duplicate ID ve eksik zorunlu bölüm senaryoları unit test ile doğrulanır.

### H1.2 — HomeLayoutPrefs saklama ve sanitize

**Durum:** ✅ Tamamlandı

**Kapsam:** Ayrı `HomeLayoutPrefs` deposunu ekle; header/footer sırası, gizli bölümler, layout version ve customized flag alanlarını güvenli şekilde oku/yaz.

**Kabul kriteri:** Bozuk, eski, eksik ve bilinmeyen section ID içeren kayıtlar çökmeden sanitize edilir; unit testler eklenir.

### H1.3 — Eski ayar migration katmanı

**Durum:** 🟡 Kısmen tamamlandı

**Kapsam:** Search top/bottom tercihi ve mevcut görünürlük toggle'larını ilk layout config'e taşı; eski ayarlarla yeni config'in birbirini ezmesini engelle.

**Kabul kriteri:** `TOP`, `BOTTOM`, gizli/görünür toggle ve yeni section ekleme senaryoları test edilir; UI görünümü değişmeden kalır.

### H2.1 — HomeScreen section renderer iskeleti

**Durum:** ✅ Tamamlandı

**Kapsam:** Sabit bölüm çağrılarını kademeli taşıyacak `HomeSectionRenderer` ve action/state bağlantılarını ekle; mevcut davranışı koruyarak renderer üzerinden en az çekirdek bölümleri çalıştır.

**Kabul kriteri:** Ana ekran eski görünümünü korur, stabil key kullanılır, compile ve odak testleri geçer.

### H2.2 — Favoriler ve satır bileşenleri ayrıştırması

**Durum:** ✅ Tamamlandı

**Kapsam:** `HomeFavoritesSection` içindeki favoriler, öneriler, son bildirim alanlar ve son kullanılanlar satırlarını ayrı composable'lara ayır.

**Kabul kriteri:** Context menu, haptic feedback, app launch ve analytics davranışları gerilemez; renderer bağımsız satırları ayrı section olarak çağırabilir.

### H2.3 — Header/content/footer zone render

**Durum:** ✅ Tamamlandı

**Kapsam:** `HomeLayoutConfig` sırasını kullanarak header, content ve footer bölge render'ını bağla; `FOLDER_GRID` ve `DOCK` koruma kurallarını uygula.

**Kabul kriteri:** Search üst/alt migration'ı yeni layout ile çalışır, dock en altta kalır, klasör gridi gizlenmez.

### H3.1 — Editör giriş noktaları

**Durum:** ✅ Tamamlandı

**Kapsam:** `HomeLongPressSheet` içine `Ana Ekranı Düzenle` aksiyonu ekle; gerekiyorsa Settings > Ana Ekran kısa yolunu ekle.

**Kabul kriteri:** Giriş metinleri TR/EN resource'tan gelir, normal long-press aksiyonları bozulmaz.

### H3.2 — Tam ekran HomeLayoutEditorScreen

**Durum:** ✅ Tamamlandı

**Kapsam:** Tam ekran editör, taslak state, üst bar, `Bitti`, `İptal`, geri tuşu koruması ve kaydedilmemiş değişiklik uyarısını ekle.

**Kabul kriteri:** `İptal` kalıcı değişiklik yapmaz, `Bitti` atomik kaydeder, process death sonrası bozuk preference oluşmaz.

### H3.3 — Bölüm kartları, göster/gizle ve varsayılana dön

**Durum:** ✅ Tamamlandı

**Kapsam:** `EditableHomeSection`, drag handle görseli, göz/gizle butonu, kilitli bölüm davranışı, gizlenen bölümler listesi ve varsayılana dön akışını ekle.

**Kabul kriteri:** Zorunlu bölümler gizlenemez, varsayılana dön klasör/widget/dock içeriğini silmez, TalkBack açıklamaları eklenir.

### H3.4 — Bölüm reorder state ve animasyon

**Durum:** ⏳ Bekliyor

**Kapsam:** Ortak `ReorderState` veya eşdeğer yardımcı yapı ile bölüm sürükle-bırak sıralamasını uygula; haptic feedback ve placement animasyonlarını ekle.

**Kabul kriteri:** Stabil key korunur, edit modunda uygulama/klasör/widget tıklamaları çalışmaz, reorder kalıcı kayda yansır.

### H4.1 — Klasör reorder edit moda bağlama

**Durum:** ⏳ Bekliyor

**Kapsam:** Mevcut klasör uzun basma/sürükleme davranışını yalnız edit modda çalışacak şekilde bağla; normal mod context menu davranışını koru.

**Kabul kriteri:** Klasör sırası edit modda değişir, normal modda yanlışlıkla drag başlamaz.

### H4.2 — Widget reorder edit moda bağlama

**Durum:** ⏳ Bekliyor

**Kapsam:** `WidgetArea` iç reorder davranışını ortak edit moda bağla; edit modunda widget iç tıklamalarını bloke et.

**Kabul kriteri:** Widget sırası kaydedilir, widget yok/birden fazla widget senaryoları çökmez.

### H4.3 — Dock yatay reorder

**Durum:** ⏳ Bekliyor

**Kapsam:** Dock uygulama/klasör öğeleri için yatay reorder ekle; sabit kullanıcı slotları ve öneri/bağlamsal slotların karışmasını engelle.

**Kabul kriteri:** Dock sırası kalıcıdır, add/remove akışları bozulmaz, klasör ve uygulama karışık listede çalışır.

### H5.1 — Backup, restore ve diagnostics

**Durum:** ⏳ Bekliyor

**Kapsam:** Home layout alanlarını backup/restore kapsamına ve tanılama raporuna ekle; kişisel veri veya serbest metin sızıntısı olmadığını doğrula.

**Kabul kriteri:** Eski/bozuk backup sanitize edilir, diagnostics yalnız güvenli layout özetini gösterir.

### H5.2 — Erişilebilirlik, responsive smoke ve release kapısı

**Durum:** ⏳ Bekliyor

**Kapsam:** TalkBack taşıma aksiyonları, Reduce Motion, küçük telefon/tablet kuralları, TR/EN kaynaklar ve final kalite kapılarını tamamla.

**Kabul kriteri:** Unit/Compose/compile/assemble kapıları geçer; küçük telefon ve tablet smoke sonuçları HISTORY.md'ye yazılır.

---

## Sprint H1 — Model, varsayılanlar ve migration

**Zorluk:** 3/10

- `HomeSectionId`, `HomeLayoutZone`, `HomeLayoutItem`, `HomeLayoutConfig`.
- `HomeLayoutPrefs`.
- Varsayılan düzen.
- Eski search position ve görünürlük toggle migration'ı.
- Sanitize ve version upgrade fonksiyonları.
- Unit testler.

**Çıkış kriteri:** UI değişmeden, uygulama her kullanıcı için güvenli ve deterministik bir layout config üretebilir.

---

## Sprint H2 — Ana ekran renderer refactor

**Zorluk:** 6/10

- Sabit bölüm çağrılarını `HomeSectionRenderer` yapısına taşı.
- `HomeFavoritesSection` içindeki alt satırları bağımsız composable'lara ayır.
- Header/content/footer zone render'ı.
- Search top/bottom davranışını yeni config'e bağla.
- Mevcut IME ve focus mode davranışlarını koru.

**Çıkış kriteri:** Ana ekran eski görünümünü korur fakat bölüm sırasını config'ten okur.

---

## Sprint H3 — Tam ekran düzenleme modu

**Zorluk:** 6/10

- Long press menüsüne `Ana Ekranı Düzenle`.
- Tam ekran editör.
- Taslak state, Bitti, İptal, geri tuşu koruması.
- Bölüm drag handle'ları.
- Göster/gizle.
- Varsayılana dön.
- Haptic feedback ve reorder animasyonları.

**Çıkış kriteri:** Kullanıcı bölümleri güvenle taşır, gizler, kaydeder ve değişiklikler yeniden açılışta korunur.

---

## Sprint H4 — Klasör, widget ve dock entegrasyonu

**Zorluk:** 6/10

- Klasör drag davranışını edit moduna bağla.
- Widget iç reorder'ını edit moduna bağla ve içerik tıklamalarını bloke et.
- Dock yatay reorder.
- Context menu/drag gesture çakışmalarını çöz.
- Klasör filtresi ve çoklu sayfa edge-case'leri.

**Çıkış kriteri:** Bölüm, klasör, widget ve dock sıralaması tek düzenleme deneyiminde tutarlı çalışır.

---

## Sprint H5 — Backup, erişilebilirlik ve kalite kapısı

**Zorluk:** 4/10

- Backup/restore alanları.
- Diagnostics raporu.
- TalkBack aksiyonları.
- Reduce Motion.
- TR/EN kaynakları.
- Telefon/tablet smoke testleri.
- Process death ve orientation testi.

**Çıkış kriteri:** Özellik yayınlanabilir, erişilebilir ve restore edilebilir durumdadır.

---

## 24. Test Planı

### Unit test

- Varsayılan config doğru sırada oluşur.
- Eski `TOP` arama ayarı header'a migrate olur.
- Eski `BOTTOM` arama ayarı footer'a migrate olur.
- Gizlenemeyen `FOLDER_GRID` sanitize sırasında geri eklenir.
- `DOCK` footer'ın sonuna zorlanır.
- Bilinmeyen section ID uygulamayı çökertmez.
- Yeni section ID eski config'e varsayılan konumda eklenir.
- Duplicate ID'ler temizlenir.
- Varsayılana dön klasör/widget/dock içeriğini silmez.

### Compose UI test

- Long press → Ana Ekranı Düzenle açılır.
- Bölüm yukarı taşınır.
- Bölüm gizlenir ve gizlenenlerden geri eklenir.
- İptal değişiklikleri kaydetmez.
- Bitti değişiklikleri kaydeder.
- Geri tuşu kaydedilmemiş değişiklik uyarısı verir.
- Edit modunda uygulama/klasör/widget tıklaması çalışmaz.
- Dock öğesi reorder edilir.
- Zorunlu bölüm gizlenemez.

### Manuel smoke

- Küçük telefon.
- Standart telefon.
- Tablet veya `>=600dp` simülasyonu.
- Arama header'da.
- Arama footer'da.
- Klavye açıkken.
- Focus Mode açıkken.
- Widget yokken ve birden fazla widget varken.
- Birden fazla klasör sayfası varken.
- Dock uygulama + klasör karışıkken.

---

## 25. Kalite Kapısı

```powershell
.\gradlew.bat testDebugUnitTest -PskipGoogleServices --no-daemon
.\gradlew.bat compileDebugKotlin -PskipGoogleServices --no-daemon
.\gradlew.bat assembleDebug -PskipGoogleServices --no-daemon
```

Ek zorunluluklar:

- Kullanıcı metinleri hardcoded olmayacak; TR/EN resource kullanılacak.
- Her sprint bağımsız ve geri alınabilir commitlerden oluşacak.
- Editor state'i process death sonrası bozuk preference üretmeyecek.
- Ana ekran açılış performansı ölçülecek; layout parse her recomposition'da yapılmayacak.
- Bölüm listelerinde stabil key kullanılacak.
- Drag sırasında bütün HomeScreen'in gereksiz recomposition'ı engellenecek.
- Feature flag ile kademeli açılabilmesi değerlendirilecek.

---

## 26. Riskler ve Önlemler

| Risk | Seviye | Önlem |
|---|---:|---|
| HomeScreen refactor sırasında mevcut jestlerin bozulması | Yüksek | H2 öncesi mevcut gesture smoke senaryolarını sabitle |
| Search top/bottom eski ayarıyla yeni sıra çakışması | Yüksek | Tek yönlü migration ve tek kaynak politikası |
| Widget iç tıklamasının drag'i tüketmesi | Yüksek | Edit modunda interaction-blocking overlay |
| Klasör long press menüsü ile drag çakışması | Orta | Drag yalnız edit modunda, menü normal modda |
| Çok fazla header bölümü klasörleri ekran dışına iter | Yüksek | Zone yapısı, minimum content height, görünürlük uyarısı |
| Preference bozulması | Orta | Sanitize + version + default fallback |
| Dock sabit ve bağlamsal öğe sırasının karışması | Yüksek | Sabit kullanıcı slotları ile öneri slotlarını ayır |
| TalkBack ile drag kullanılamaması | Orta | Yukarı/aşağı taşı erişilebilir aksiyonları |
| Backup'tan eski ID gelmesi | Düşük | Bilinmeyen ID'yi yok say, zorunluları geri ekle |

---

## 27. Kapsam Dışı — Sonraki Sürüm Adayları

Bu MVP tamamlanmadan aşağıdakilere başlanmayacak:

- Tam serbest `x/y` hücre yerleşimi.
- Birden fazla bağımsız ana ekran sayfası.
- Uygulama ikonlarını ana ekran gridine tek tek bırakma.
- Widget serbest genişlik/yükseklik resize.
- Uygulamayı klasörün üstüne bırakarak klasör oluşturma.
- Tablet için çift panelli canlı layout editörü.
- Bulut üzerinden layout senkronizasyonu.
- Kullanıcının birden fazla layout profili kaydetmesi.

---

## 28. Nihai Kabul Kriterleri

Özellik ancak aşağıdakilerin tamamı sağlandığında tamamlandı sayılır:

1. Ana ekran uzun basma menüsünde `Ana Ekranı Düzenle` bulunur.
2. Kullanıcı taşınabilir bölümlerin sırasını değiştirebilir.
3. Kullanıcı isteğe bağlı bölümleri gizleyip geri ekleyebilir.
4. Klasör sırası edit modunda değiştirilebilir.
5. Android widget'larının bölümü ve kendi iç sırası değiştirilebilir.
6. Dock uygulama/klasör sırası değiştirilebilir.
7. Klasör gridi ve dock yanlışlıkla kaybolmaz.
8. İptal ve kaydet davranışı birbirinden nettir.
9. Varsayılana dön içerikleri silmez.
10. Eski arama üst/alt tercihi doğru migrate edilir.
11. Backup/restore yerleşimi korur.
12. TalkBack ile alternatif taşıma aksiyonları çalışır.
13. Küçük telefon ve tablette taşma oluşmaz.
14. Unit test, Compose test, compile ve assemble kapıları geçer.
15. Özellik HISTORY.md'ye değişen dosyalar ve kalan risklerle kaydedilir.

---

## 29. Net Sonuç

Bu roadmap ile AppOrganizer:

- Pixel Launcher'ın tam serbest grid karmaşıklığını kopyalamaz.
- Otomatik klasörleme kimliğini korur.
- Kullanıcıya gerçek kişiselleştirme kontrolü verir.
- Halihazırdaki klasör ve Android widget sıralama kodunu yeniden kullanır.
- Ana ekranı daha profesyonel, esnek ve yönetilebilir hale getirir.

**Öncelik önerisi:** P0 kritik akışlar tamamlandıktan sonra, ana ürün deneyimi içinde bağımsız bir sprint serisi olarak **H1 → H5** sırasıyla uygulanmalıdır.

---

## 30. Teknik Referanslar

- Android Developers — Jetpack Compose drag and drop: `dragAndDropSource` / `dragAndDropTarget` genel veri taşıma içindir; liste içi reorder ayrı bir “pick up and move” desenidir.
- Android Developers — Lazy list item'larında stabil ve benzersiz key kullanımı, yeniden sıralamada state'in doğru öğeyle hareket etmesini sağlar.
- Android Developers — `Modifier.animateItem()` liste öğelerinin eklenme, çıkarılma ve yer değiştirmelerini animasyonla göstermek için kullanılabilir.
