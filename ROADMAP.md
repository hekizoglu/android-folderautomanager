# AppOrganizer — Birleşik Teknik Roadmap

> **Tek aktif yol haritası**  
> **Birleştirme tarihi:** 2026-07-21  
> **Kaynak önceliği:** Daha yeni commit/dosya kararı, eski kararı geçersiz kılar. `YENI_HERO_DASHBOARD_BIREBIR_UYGULAMA_ROADMAP.md` geçmişte silinmiş olsa da daha sonra geri getirilmiştir; 21 Temmuz 2026 tarihli kesin ürün kararı ve onu izleyen kod değişikliği geçerlidir.
> **Arşiv kuralı:** Tamamlanan işler `HISTORY.md`, dış sistem/cihaz gerektiren engeller `COZULEMEYEN_SORUNLAR.md`, kalıcı ürün kararları `DECISIONS.md` içine taşınır. Bu dosyada yalnız aktif veya kısmen tamamlanmış iş kalır.

> **Hero tasarım şartnamesi:** `YENI_HERO_DASHBOARD_BIREBIR_UYGULAMA_ROADMAP.md` silinmeyecek ve bu birleşik roadmap’in H1 fazı için bağlayıcı teknik/görsel referans olarak kullanılacaktır. Dosya ayrı bir rakip backlog değil; kesin ürün kararının ayrıntılı uygulama sözleşmesidir.

## 1. Sabit ürün kararları

- AppOrganizer bir Pixel Launcher kopyası değildir; kendi kimliği olan, gizlilik öncelikli akıllı düzenleyicidir.
- Ana ekran mimarisi: Sayfa 0 yalnız Hero Dashboard, klasörler sayfa 1..N, tek yatay pager.
- Hero Dashboard sırası: büyük saat/tarih, Dijital Yaşam, Her Şeyi Ara, Akıllı Erişim sekmeleri, sayfa göstergesi, sabit uygulama dock’u.
- Sayfa 0’da klasör, widget, ticker, AssistantInsight, FolderStats, ayrı favoriler/öneriler/son kullanılanlar/bildirim satırları bulunmaz.
- Sabit Hero dock yalnız uygulama içerir; klasör ve dinamik öneri içermez.
- Eski dashboard, feature flag, safe-mode görünümü ve kullanıcıya geri dönüş seçeneği tutulmaz; tek ürün yolu Hero Dashboard’dur.
- `Her Şeyi Ara` ve dock tüm sayfalarda sabittir; yukarı kaydırma uygulama çekmecesini açar.
- Home tek basış başlangıç sayfasına, çift basış uygulama çekmecesine gider.
- Sayfa geri yükleme ham indeksle değil semantic anchor ile yapılır.
- Kontrol Bekleyenler ekranında A tasarımı uygulanır: aynı anda tek aktif uygulama, kategori seçimi bottom sheet, Türkçe alfabetik sıralama.
- Klasör birleştirmede A tasarımı uygulanır: kullanıcı kaynak/hedefi ve taşınacak uygulamaları görmeden işlem uygulanmaz.
- Birleştirme atomik Room transaction olmalı ve uygulama yeniden başladıktan sonra geri alınabilmelidir.
- Otomatik/sessiz klasör birleştirme yapılmaz. Split ve cleanup akışları merge refactor’ından etkilenmez.
- Telemetri yalnız açık rıza ile çalışır; sorgu, kişi, dosya, klasör adı, uygulama adı veya paket adı gönderilmez.
- Çoklu cihaz senkronizasyonu ilk production yayın sonrasıdır. Önce SAF/Drive yedekle–geri yükle akışı güçlendirilir.
- İlk production kapsamı Türkçe ve İngilizcedir. Desteklenmeyen locale İngilizce kaynaklara ve deterministik kök sıralamaya düşer; eksik/boş `values-*` klasörü oluşturulmaz.
- Paging3 ve `beyondViewportPageCount` artırımı mevcut ölçekte kapsam dışıdır.

## 2. Çalışma protokolü — token ve döngü bütçesi

Her döngü tek bir teslimat sınırına sahip olmalıdır. Bir döngüde en fazla bir domain değişikliği, bir UI dilimi veya bir doğrulama paketi yapılır.

### Planlama varsayımları

- Eforlar tek geliştiricinin aktif çalışma süresidir; cihaz, mağaza incelemesi, beta gözlemi ve hesap erişimi bekleme süresine dahil değildir.
- `1 puan ≈ 0,5 geliştirici günü` yalnız kaba kapasite planlaması içindir. Faz ilk kez `Devam ediyor` durumuna alınırken kalan iş yeniden tahmin edilir.
- Hedef bitiş tarihi, faz sahibi ve bağımlılıklar hazır olduğunda ISO `YYYY-MM-DD` biçiminde atanır. Sahibi veya başlangıç tarihi belli olmayan faza sahte kesin tarih yazılmaz.
- R0–R8 için ilk aktif efor bütçesi toplam `27,5–44,5 geliştirici günü`dür; bu takvim taahhüdü değildir ve paralel çalışma/dış bekleme içermez.

### Döngü başlangıcı

1. Yalnız bu dosyadaki aktif döngüyü ve doğrudan etkilenen kodu oku.
2. `HISTORY.md` dosyasını baştan sona okuma; yalnız görev kimliği/dosya adıyla hedefli `rg` kullan.
3. Aynı konu daha yeni committe tamamlanmışsa kod yazma; doğrula ve roadmap’i güncelle.
4. Çalışma ağacını ve son ilgili commitleri kontrol et; kullanıcı değişikliklerini koru.
5. Değişecek dosyaları ve kabul testini döngü başlamadan sabitle.

### Döngü uygulaması

1. Önce saf model/policy/use-case, sonra state/ViewModel, sonra UI, en son persistence/telemetry sırasını kullan.
2. Büyük dosyanın tamamını tekrar tekrar okutma; sembol ve satır aralığıyla çalış.
3. Yeni paralel altyapı kurma; mevcut repository, policy, mapper ve UI bileşenlerini genişlet.
4. Bir başarısız testte aynı komutu körlemesine en fazla bir kez tekrarla; sonra kök nedeni incele.
5. Her döngüde yalnız hedefli testleri çalıştır; tam test/lint/build paketi faz kapısında çalışır.

### Döngü kapanışı

1. Kod + hedefli test + kabul kriteri birlikte tamamlanmadan işaretleme yapma.
2. Tamamlanan döngüyü özet olarak `HISTORY.md` içine taşı; ayrıntılı planı burada bırakma.
3. Bu dosyada yalnız durum, kanıt commit’i ve kalan bağımlılık tutulur.
4. Dış cihaz/hesap gerektiren işi `COZULEMEYEN_SORUNLAR.md` içine taşı ve geliştirme döngüsünü bloke etme.
5. Bir faz kapısında `testDebugUnitTest`, `lintDebug`, `detekt`, `assembleDebug`; cihaz varsa `connectedDebugAndroidTest` çalıştır.

## 3. Bağımlılık zinciri

```text
R0 Kaynak birleştirme
 └─ H1 Yarım kalan Hero Dashboard'u tamamla
     ├─ R1 Mevcut ana ekran/performance güvenlik kapısı
     ├─ R2 Kontrol Bekleyenler A tasarımı (kod H1 sonrası; cihaz kapısı R1 sonrası)
     │   └─ R3 Klasör birleştirme domain + inceleme UI
     │       └─ R4 Atomik merge + kalıcı undo
     ├─ R5 Hero Dashboard cihaz/telemetri doğrulaması
     ├─ R6A Güvenli legacy/dead-code temizliği
     └─ R6B Doğrulama sonrası kalıcı legacy kaldırma

R2–R6
 └─ R7 Birleşik cihaz/erişilebilirlik/telemetri QA
     └─ R7.5 Kapalı beta kapısı
         └─ R8 İlk production release
             └─ R9 Release sonrası ürün geliştirmeleri
```

R1, R2 kod çalışması, R5 ve R6A; H1’in temel composition kapısı geçtikten sonra paralel ilerleyebilir. R2’nin domain/state/unit-test işleri R1 cihaz ölçümünü beklemez; R2 faz kapanışı ve cihaz smoke kanıtı R1 baseline sonrasında yapılır. R4, R3 bitmeden; R6B adayı Hero doğrulaması bitmeden; R7.5, R7.1–R7.4 bitmeden; R8, R7.5 bitmeden başlatılamaz.

## 4. Faz R0 — Baseline ve belge konsolidasyonu

**Amaç:** Tek gerçek kaynak oluşturmak ve eski kararların yeniden uygulanmasını engellemek.
**Tahmini kalan efor:** 0,5 gün (1 puan). **Hedef:** Faz aktive edildiğinde atanır.
**Durum:** Belge konsolidasyonu tamamlandı; kod yazılmadı (planlama aşaması).

- [ ] Eski roadmap’lerdeki açık/kısmi işleri birleştir.
- [ ] Daha yeni kararları üstün tut; Hero Dashboard’un yerel roadmap silme commit’ini karar iptali olarak yorumlama.
- [ ] Tamamlanmış maddeleri yeniden backlog’a alma.
- [ ] `ROADMAP.md`, `ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md`, `ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md`, `KATEGORI_ROADMAP.md` ve `KLASOR_BIRLESTIRME_ROADMAP.md` dosyalarını bu dosya yayınlandıktan sonra kaldır.
- [ ] Ana dalda hedefli doğrulama: temiz checkout, `git status`, roadmap bağlantısı araması.

**Çıkış:** Depoda yalnız `YENI_ROADMAP.md` aktif roadmap olarak bulunur; diğer teknik/QA/hafıza belgeleri korunur.
**CRON-48 notu:** R0 planlama aşamasında; kod yazılmamış. Belge konsol.tasyonu yapılmadığında [x] işareti yanlış.

## 5. Faz H1 — Acil: Hero Dashboard dönüşümünü tamamla

**Neden ilk:** `main` üzerindeki son dönüşüm eski dashboard state/bölümlerinin önemli kısmını kaldırdı; `SmartDashboardPage` ise yeni `HeroDashboardPage` bağlanmadan geçici Pulse Clock + Today/HomeIntelligence içeriğinde kaldı. Bu ara durum yeni özelliklerden önce kapatılmalıdır.
**Tahmini kalan efor:** 1–2 gün (2–4 puan; dış cihaz doğrulaması hariç). **Hedef:** Faz aktive edildiğinde atanır.

### H1.0 Baseline ve kırık HEAD kontrolü

- [x] Hero kaynak/state/test sözleşmesini temiz çalışma ağacında statik olarak doğrula; gerçek compile/build/smoke kanıtını R5/R7 ortak doğrulama paketinde tamamla.
- [x] `SmartDashboardPage` ve `DashboardUiState` Hero sözleşmesine indirildi; artık bulunmayan assembler/section yardımcılarını kullanan kırık testler temizlendi.
- [x] Kategori roadmap durumunu commit mesajından değil kod/test kanıtından belirle; Hero commitlerini kategori ilerlemesi sayma.

### H1.1 Hero tasarım altyapısı — kod tamam; dış doğrulama R5/R7’de

- [x] `hero/` altında tek kaynak `HomeHeroTokens` ve adaptif `HomeHeroProfile`/`HomeHeroLayoutPolicy` oluştur.
- [x] Hero’ya özel, API 26 uyumlu `PremiumGlassSurface` oluştur.
- [x] Referans düzeni 360×640dp esas al: saat, Dijital Yaşam, Her Şeyi Ara ve Akıllı Erişim aynı adaptif 304dp genişliği paylaşır; yükseklikler sırasıyla 114/96/74/162dp, dock 340×64dp olur.
- [x] Ham px kullanma; 320×568–412×915, tablet, landscape ve fontScale 1.5 kararlarını policy ile yönet.
- [x] Mevcut global `GlassCard` bileşenini değiştirmeden bırak.
- [x] Policy için 320×568, 360×640, 412×915, tablet, landscape, fontScale 1.5 ve geçersiz ölçü unit testlerini yaz.
- [x] Hero kartlarını gerçek parent constraint’inden hesaplanan tek içerik genişliğine bağla; küçük pencere/telefonlarda yatay padding sonrası küçült, tablet/landscape’de profil tavanını aşma.

### H1.2 Hero kartları

- [x] Mevcut Pulse Clock davranışını `HeroClockCard` içine bağla; saat/tarih sunumunu değiştir, motoru yeniden yazma.
- [x] `DigitalPulseEngine`/`HomePulseSummary` verisini `HeroDigitalLifeCard` içine tek skor kaynağı olarak bağla.
- [x] Search overlay’i yeniden yazmadan ayrı `HeroSearchCard` launch surface oluştur ve `FullScreenSearchOverlayV2` akışını aç.

### H1.3 Akıllı Erişim

- [x] `SmartAccessCoordinator` ile Şimdi ağırlıklarını tek yerde topla; modeller, deterministik ranker ve dedupe policy hazır.
- [x] `Şimdi`, `Son Açılanlar`, `Bildirimler` sekmelerini aynı beş slotlu UI’a bağla.
- [x] Şimdi: mevcut zaman dilimi + kullanım sinyali; Son Açılanlar: gerçek timestamp; Bildirimler: son bildirim zamanı kullanır.
- [x] Kaldırılmış/gizli/geçersiz uygulamaları dışla ve sonuçları tekilleştir.
- [x] Usage/Notification izni yoksa açıklama ve doğru ayar yönlendirmesi göster.
- [x] Yeni Hero akışında uygulama/paket/kişi/dosya adını telemetriye gönderme.

### H1.4 Sabit uygulama dock’u ve klasör migration’ı

- [x] Hero dock’u beş sabit uygulama slotu olarak uygula; klasör ve dinamik öneri kabul etme.
- [x] Mevcut dock’u bir kez yedekleyerek uygulamaları migrate et; klasör öğelerini Hero görünümünden çıkar.
- [x] Klasörleri Sayfa 0’dan çıkar; Sayfa 1+ pager ve semantic anchor düzenini koru.

### H1.5 Composition ve ilk kapı

- [x] `SmartDashboardPage` içeriğini `HeroDashboardPage` composition’ına dönüştür.
- [x] Saat → Dijital Yaşam → Arama → Akıllı Erişim → gösterge → dock sırasını uygula.
- [x] 320×568, 360×640, 412×915, tablet ve landscape layout policy testlerini yaz.
- [x] Ranker, coordinator, dedupe, dock migration ve Hero Compose interaction testlerini ekle.
- [x] CI workflow’una compile, unit test, lint, debug build ve instrumentation-test compilation kapılarını ekle; gerçek çalıştırma ve cihaz smoke kanıtını R5/R7’ye bırak.

**Bu fazda yapılmayacak:** Eski ve yeni dashboard’u feature flag ile paralel tutmak, veri motorlarını yeniden yazmak, global cam temasını değiştirmek, legacy temizliğini doğrulama tamamlanmadan körlemesine bitirmek.

**Çıkış:** Sayfa 0 gerçek Hero Dashboard’dur; temel kartlar ve üç Akıllı Erişim sekmesi gerçek veriye bağlıdır; klasörler ve kullanıcı tercihleri kaybolmaz.

## 6. Faz R1 — Ölçüm ve mevcut sistem güvenlik kapısı

**Tahmini efor:** 2–3 gün (4–6 puan; cihaz erişimi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R1.1 Performans ölçümü

- [ ] Baseline Profile sonucunu aynı cihaz, build türü ve senaryoda en az 5 ısınma + 10 ölçümle karşılaştır; cold start medyanı ve P95 değerlerini kanıta yaz.
- [ ] Samsung SM-X210 üzerinde kaydedilen `%14,11` janky frame oranını aynı senaryoda `%7` altına indir. Eşik geçilmezse veya cold start medyanı `%5`ten fazla kötüleşirse R1 kapanmaz; ölçüm ve kök neden blokaj kaydına eklenir.
- [ ] Donanım/ölçüm varyansı nedeniyle eşik değişecekse yeni tekrarlanabilir baseline, gerekçe ve ürün sahibi onayını `DECISIONS.md` içine yaz; eşiği sessizce gevşetme.
- [ ] Macrobenchmark çıktısını kanıt dosyasına yaz; ölçüm olmadan yeni performans refactor’ı yapma.
- [ ] Periyodik worker’larda pil kısıtlarının gerçek schedule davranışını doğrula.

### R1.2 Serbest yerleşim doğrulaması

- [ ] `Klasörde Serbest Yerleşim` ve `Widget Alanında Serbest Yerleşim` toggle’larını yoğun veriyle test et.
- [ ] Frame drop, process death, rotation, TalkBack ve drag davranışını doğrula.
- [ ] `LauncherAccessibilityService` için gerçek ihtiyaç yoksa stub’ı büyütme; gerekiyorsa ayrı karar kaydı oluştur.
- [ ] Ekranlar arası gerçek item taşımasını bu faza ekleme; kullanım kanıtından sonra ayrı değerlendirme yap.

**Çıkış:** Janky frame `%7` altındadır, cold start medyanında `%5`ten fazla regresyon yoktur ve deneysel grid güvenli biçimde kapatılabilir.

## 7. Faz R2 — Kontrol Bekleyenler A tasarımı

**Bağımlılık:** Domain/state/UI kodu için H1 temel kapı; faz kapanışı ve cihaz smoke için R1 baseline.
**Ana dosyalar:** `ClassificationReviewScreen.kt`, `AppListViewModel.kt`, classification review state/bileşen/test dosyaları.
**Tahmini efor:** 4–6 gün (8–12 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R2.1 Saf kategori altyapısı

- [x] `TurkishCategorySorter` oluştur; kullanıcıya gösterilen ad üzerinden Türkçe locale sıralaması yap.
- [x] TR için Türkçe `Collator`, EN için İngilizce `Collator`, desteklenmeyen locale için deterministik kök/İngilizce fallback uygula; locale ve aksan testlerini ekle.
- [x] `Kategorisiz` seçeneğini dışla; önerilen kategori tekrar etmesin.
- [x] Uygulama kategorileri ve marka klasörlerini ayrı section’lara ayır.
- [x] Sıralama ve grouping unit testlerini tamamla.

### R2.2 State ve ViewModel

- [x] Tek aktif package, package→seçim map’i, sheet state, arama sorgusu ve processing package içeren immutable UI state oluştur.
- [x] Pending liste değişince aktif uygulamayı güvenli uzlaştır.
- [x] Onay, düzeltme ve erteleme işlemlerini çift tıklamaya/idempotency sorununa karşı koru.
- [x] Repository hatasında processing state’i temizle ve kullanıcıya hata göster.
- [x] ViewModel testlerini tamamla.

### R2.3 Bottom sheet ve ekran refactor’ı

- [x] Arama, section başlıkları, seçili işareti ve boş durum içeren kategori picker sheet oluştur.
- [x] Tek aktif uygulama kartında ikon, güven seviyesi, neden, sistem önerisi, seçilen kategori ve eylemleri göster.
- [x] Eski yatay kategori `LazyRow`, sabit `560.dp`, nested liste ve UI-local seçim state’ini kaldır.
- [x] Özet kartı ve sıradaki uygulamalar kuyruğunu bağla.
- [x] Tıklama alanlarını en az 48dp yap; TalkBack, font scale 1.5, açık/koyu ve Pixel görünümünü doğrula.

### R2.4 Telemetri ve faz kapısı

- [x] Yalnız privacy-safe event’leri ekle; paket adı gönderme.
- [x] TR/EN string kaynaklarını tamamla; sabit UI metni bırakma.
- [x] Kaynak anahtarı eşitliğini test et; TR/EN dışındaki diller R9 kararı verilene kadar ürün kapsamına alınmaz.
- [x] Unit, ViewModel ve Compose UI testlerini; lint, detekt ve debug build’i çalıştır.
- [x] Küçük telefon, standart telefon ve tablette smoke test yap.

**Çıkış:** Tek kart + bottom sheet akışı güvenli çalışır; onay/düzeltme/ertele persistence ve sıra ilerlemesi kanıtlanır.

## 8. Faz R3 — Klasör birleştirme motoru ve inceleme UI

**Bağımlılık:** R2 ile manuel kategori/override kurallarının sabitlenmesi.
**Tahmini efor:** 5–7 gün (10–14 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R3.1 Domain ve öneri motoru

- [ ] `FolderSuggestion` modelini kaynak, önerilen hedef, neden, güven ve sayılarla genişlet.
- [ ] `FolderSuggestionReason`, `FolderMergePlan` ve `FolderMergeCandidateScorer` oluştur.
- [ ] Mevcut sabit eşleştirmeleri scorer içine taşı; bilinmeyen hedef üretme.
- [ ] Manuel kilitli uygulamaları varsayılan seçim dışında bırak.
- [ ] Güven ve minimum uygulama eşiklerini uygula; deterministik sıralama yap.
- [ ] Engine unit testlerini tamamla.

### R3.2 UI state ve ViewModel

- [ ] `FolderMergeUiState` oluştur.
- [ ] Öneriyi açma, hedef değiştirme, uygulama seçme ve review state işlemlerini ekle.
- [ ] Eski `acceptFolderSuggestion()` davranışını silme; merge türünü review ekranına route et.
- [ ] `SPLIT_LARGE_FOLDER` ve `CLEAN_UNUSED_APPS` yollarını değiştirme.
- [ ] ViewModel testlerini tamamla.

### R3.3 A tasarımı inceleme ekranı

- [ ] Öne çıkan öneri kartı ve `FolderMergeReviewScreen` oluştur.
- [ ] Kaynak/hedef önizleme, uygulama grid’i, hedef picker, uygulama picker ve önce/sonra sayı kartını ekle.
- [ ] Kilitli uygulamaları açıkça göster; hedef 20+ uygulama olacaksa uyar.
- [ ] Seçili taşınabilir uygulama yoksa onayı kapat.
- [ ] Loading/error/empty state, dark mode, büyük font ve TalkBack desteğini tamamla.
- [ ] Compose UI testlerini tamamla.

**Çıkış:** Kullanıcı kalıcı işlem yapılmadan önce eksiksiz merge planını görür ve düzenler.

## 9. Faz R4 — Atomik merge, işlem geçmişi ve gerçek undo

**Bağımlılık:** R3 review planı ve UI state kararlı olmalı.
**Tahmini efor:** 5–8 gün (10–16 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R4.1 Persistence

- [ ] Operation ve operation-item entity/DAO modellerini ekle.
- [ ] Tek bir Room migration yaz; schema JSON’u commit et.
- [ ] `mergeFolders()` işlemini transaction içinde uygula.
- [ ] Kısmi başarıya izin verme; hata halinde tamamını rollback et.
- [ ] Eski kategori/manuel override verisini geri alma için operation item içinde sakla.

### R4.2 Undo ve yan sistem tutarlılığı

- [ ] `undoFolderMerge()` işlemini transaction içinde ve idempotent uygula.
- [ ] Aynı operation’ın ikinci kez geri alınmasını reddet.
- [ ] Merge ve undo sonrasında Room, manuel override, launcher klasörleri ve search index’i aynı sonucu göstermeli.
- [ ] Başarılı öneriyi yeniden gösterme; erteleneni 7 gün gizle.
- [ ] Boş sistem klasörünü görünür listeden düşür fakat veritabanından silme.
- [ ] TaskScore bağlantısını yalnız başarıdan sonra tetikle.

### R4.3 Faz kapısı

- [ ] Migration, rollback, process death ve restart sonrası undo testlerini tamamla.
- [ ] Unit/repository/ViewModel/Compose testleri, lint, detekt ve debug build’i çalıştır.
- [ ] Telefon/tablet; 20+ hedef; tüm uygulamaları kilitli kaynak; rotation/background senaryolarını test et.

**Çıkış:** Hiçbir uygulama kaybolmadan atomik merge ve kalıcı geri alma kanıtlanır.

## 10. Faz R5 — Hero Dashboard adaptif düzen ve telemetri doğrulaması

**Tahmini efor:** 2–4 gün (4–8 puan; cihaz/Firebase erişimi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R5.1 Dört cihaz matrisi

- [ ] Mevcut iki kanıtı koru: Samsung SM-X210 ve Pixel6 API 33 emülatör.
- [ ] Temiz kurulum telefonu ve izinleri kapatılmış ayrı cihaz/konfigürasyon ile matrisi 4/4 tamamla.
- [ ] Ortak R7 senaryosunu kullanarak portrait/landscape, rotasyon+swipe, arama, dock, Dashboard, klasör grid ve All Apps’i doğrula; aynı matrisi ikinci kez üretme.
- [ ] Küçük/standart/büyük telefon ile 7–8 ve 10+ inç tablet kırılımlarında taşma olmadığını tek evidence paketine yaz.

### R5.2 Tek ürün yolu ve privacy-safe telemetry

- [ ] Üretim pager planlayıcısını boolean/feature-flag kabul etmeyen `buildHeroPages()` girişine bağla; Sayfa 0’ı kod seviyesinde yalnız Hero yap.
- [ ] Gerçek cihazda her açılış/restore senaryosunda Sayfa 0’ın Hero olduğunu smoke ile doğrula.
- [ ] Rıza kapalıyken hiçbir home telemetry event’i gönderilmediğini Firebase tarafında doğrula.
- [ ] Rıza açıkken yalnız izinli enum/bucket parametrelerinin gittiğini DebugView ile doğrula.
- [ ] Klasör adı, kategori, app/package, arama sorgusu, kişi ve dosya verisinin gönderilmediğini kanıtla.

**Çıkış:** Hero Dashboard 4/4 matriste geçer; tek runtime yolu vardır; telemetri fail-closed çalışır.

## 11. Faz R6 — Legacy Hero dashboard temizliği

**Tahmini efor:** 2–4 gün (4–8 puan; beta gözlem süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R6A — Güvenli dead-code temizliği

**Bağımlılık:** H1 temel composition kapısı. Görünür davranış, migration veya restore sözleşmesi değiştirilemez.

- [ ] Eski `DashboardContentGroup`, `dashboardGroupOrder`, `countVisibleSections` referanslarını repo genelinde doğrula; runtime referansı kalmadığını kanıtla. Kullanıcı tercihlerini etkileyen kalıcı layout ayarlarını R6B migration kararına bırak.
- [ ] Dashboard widget/ticker/FolderStats/favorites/suggestions/recent-install dallarını yalnız başka ekran tüketmiyorsa kaldır.
  - [ ] Üretim tüketicisi olmayan eski `HomeFavoritesSection` contextual row seçici/composable zincirini ve yalnız ona ait testleri kaldır; All Apps veri akışlarını koru.
- [ ] Kullanılmayan Dashboard state alanlarını ve kırık eski testleri sil; geçerli testleri Hero/layout testlerine dönüştür.
- [ ] Pager’ın üstünde tüm sayfalara sızan eski `FolderStatsRow`/`StatChip` bandını ve yalnız onu besleyen HomeScreen state aboneliklerini kaldır.
- [ ] Eski folder-only pager çağrılarını repo genelinde ara; runtime’da yalnız `HomePagerHost` + tek-sayfa folder grid renderer kaldığını doğrula.
- [ ] Kullanılmayan eski pager branch’lerini, duplicate search lambda’larını ve eski test fixture’larını kaldır.
- [ ] `HomeScreen.kt` dosyasını orchestration seviyesine indir; davranış değişikliği yapma.

### R6B — Doğrulama sonrası kalıcı kaldırma

**Bağımlılık:** R5 cihaz/telemetri doğrulaması tamamlanmalı.

- [ ] `last_home_page` eski anahtarını yalnız migration/restore uyumluluğu için tut.
- [ ] Eski dashboard/feature-flag/safe-mode ayarlarını ve restore alanlarını geriye uyumluluk kararıyla kalıcı kaldır veya açıkça deprecated migration alanı olarak sınırla.
- [ ] Hâlâ kullanılan `FOLDER_GRID`, yeni indicator ve tek sayfa grid renderer’ını yanlışlıkla silme.
- [ ] Her davranış/migration kaldırmasını ayrı, küçük ve `git revert` ile geri alınabilir committe yap; önce referans taraması + hedefli test, sonra internal/beta build kanıtı al.
- [ ] Persisted state, restore veya migration davranışını etkileyen kaldırmayı R7.5 beta adayında en az 7 takvim günü gözlemle; kritik regresyon varsa commit’i geri al. Sıfır referanslı salt dead-code için bekleme gerekmez.
- [ ] Eski runtime feature flag veya ikinci dashboard yolunu güvenlik amacıyla yeniden ekleme; rollback commit/sürüm/backup üzerinden yapılır.
- [ ] Regression testleri ve dört cihaz kısa smoke testini tekrar çalıştır.

**Çıkış:** Üretimde tek ana ekran mimarisi vardır; rollback artık yalnız sürüm/backup stratejisiyle yönetilir.

## 12. Faz R7 — Birleşik yayın öncesi QA

Bu faz, önceki fazlarda tarif edilen cihaz matrislerini tek kanonik senaryo ve evidence paketinde toplar. Alt fazlar aynı test matrisini yeniden yazmaz; yalnız kendi sonuç bağlantısını buraya ekler.

**Tahmini aktif efor:** 4–6 gün (8–12 puan; beta bekleme süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R7.1 Veri, izin ve arka plan işleri

**Bağımlılık:** R2–R6A kod kapıları. R7.2 ve R7.3 ile paralel yürüyebilir.

- [ ] Android 14 NotificationListener izin aç/kapa, reboot ve event/rapor testi.
- [ ] SAF export/import, Drive klasör seçimi, missing packages ve restore sonrası ayar sürekliliği.
- [ ] SmartInsightWorker, BackupWorker ve diğer periyodik worker schedule/pil testleri.
- [ ] Android 13+ POST_NOTIFICATIONS reddinde sessiz/güvenli davranış.
- [ ] Rıza kapalı/açık Firebase davranışını ve hassas veri gönderilmediğini doğrula.
- [ ] Widget sağlayıcı seçimini ROM `Settings.ActivityPicker` bağımlılığından çıkar; uygulama içi liste + `bindAppWidgetIdIfAllowed` + `ACTION_APPWIDGET_BIND` izin fallback’i kullan.
- [ ] Widget seçme, bind izni red/onay, yapılandırmalı/yapılandırmasız sağlayıcı ve iptal durumlarını gerçek cihazda doğrula; ayrılan widget ID’nin hata/iptalde silindiğini kanıtla.

### R7.2 UI ve erişilebilirlik

**Bağımlılık:** R2–R6A UI kapıları. R7.1 ve R7.3 ile paralel yürüyebilir.

- [ ] Pulse Clock 3 stil, görevler, Dijital Yaşam skoru ve Akıllı Nabız ticker görsel matrisi.
- [ ] Serbest grid, Kontrol Bekleyenler, merge/undo, Dashboard pager ve global arama regresyonu.
- [ ] TalkBack, animasyonlar kapalı, font `%200`, açık/koyu tema, farklı duvar kâğıtları ve rotasyon.
- [ ] API 26 blur fallback ve Samsung/Xiaomi/Google OEM kategori davranışı.
- [ ] Akıllı Erişim Bildirimler sekmesinde tek otoritatif sayaç çiz; Son Açılanlar/paket değişiminde eski ikon state'ini temizle ve kararlı package key kullan.

### R7.3 Süreç dayanıklılığı

**Bağımlılık:** R4 transaction/undo ve ilgili worker akışları tamamlanmalı. R7.1/R7.2’den bağımsız hata ayıklanır.

- [ ] Günlük/haftalık görev settlement, duplicate ödül ve timezone sınırlarını doğrula.
- [ ] Process death, reboot, rotasyon+swipe ve background/foreground geçişlerini doğrula.
- [ ] Merge/undo, restore, semantic page anchor ve görev settlement durumlarının yeniden başlatma sonrası tutarlı kaldığını kanıtla.

### R7.4 Uçtan uca smoke — dört cihaz profili

**Bağımlılık:** R7.1–R7.3 kritik bulgusuz tamamlanmalı.

- [ ] Dört profil kullan: küçük telefon, standart/büyük telefon, 7–8 inç tablet ve 10+ inç tablet. API/OEM çeşitliliğini bu dört profil arasında dağıt.
- [ ] Her profilde temiz kurulum, yükseltme/restore, izinler kapalı ve temel Hero → arama → klasör → Kontrol Bekleyenler → merge/undo akışını çalıştır.
- [ ] `testDebugUnitTest`, `lintDebug`, `detekt`, `assembleDebug` ve uygun cihazda `connectedDebugAndroidTest` çalıştır.
- [ ] Sonuçları cihaz/build/commit SHA ile tek release evidence paketinde birleştir.

### R7.5 Kapalı beta kapısı

**Bağımlılık:** R7.1–R7.4 ve R6B release-candidate değişiklikleri tamamlanmalı.

- [ ] Hero Dashboard, Kontrol Bekleyenler ve merge/undo akışlarını içeren imzalı adayı kapalı/dahili test kanalına dağıt.
- [ ] Hedef 50 gerçek kullanıcıdır. Bu erişilemiyorsa en az 10 farklı testçi ve temsilî dört cihaz profiliyle gerekçeli iç test yapılır; daha küçük örnek yalnız ürün sahibinin `DECISIONS.md` istisnasıyla kabul edilir.
- [ ] En az 7 takvim günü geri bildirim, Crash/ANR, veri kaybı, restore ve privacy sinyallerini izle. Bilinen kritik hata, veri kaybı veya güvenlik/gizlilik ihlali varken R8’e geçme.
- [ ] Yeterli telemetry örneği varsa crash-free session oranı en az `%99,5` olmalı; örnek yetersizse bunu başarı gibi yorumlama ve manuel smoke kanıtını kaydet.
- [ ] Release bloklayan bulguları ilgili faza geri aç; iyileştirme önerilerini kanıt bağlantısıyla R9’a ekle.

**Çıkış:** R7.1–R7.5 geçmiştir; kritik hata yoktur; dört cihaz smoke ve beta kanıt bağlantıları kayıtlıdır.

## 13. Faz R8 — İlk production yayın kapısı

**Bağımlılık:** R7.1–R7.5 tamamlanmalı. Hesap/cihaz gerektiren maddeler `COZULEMEYEN_SORUNLAR.md` ile birlikte yürütülür.
**Tahmini aktif efor:** 2–4 gün (4–8 puan; mağaza inceleme süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

**Dış bağımlılık yönetimi:** R8 aktive edilmeden önce her Play Console/hesap/cihaz engeline `COZULEMEYEN_SORUNLAR.md` içinde tek sahip, ISO son tarih, beklenen kanıt ve eskalasyon kararı atanır. Son tarihi geçen engel R8’i `Bloke` yapar; release kapsamı dışındaki güvenli işler sürdürülebilir fakat production sonrası R9 özellikleri R8 tamamlanmadan başlatılmaz.

- [ ] QUERY_ALL_PACKAGES beyanını launcher temel işlevi gerekçesiyle doldur.
- [ ] Data Safety formunu gerçek Firebase, opsiyonel kişi/dosya, NotificationListener, backup ve AI davranışıyla eşleştir.
- [ ] Content rating anketi ve Privacy Policy URL alanını tamamla.
- [ ] Kalıcı release keystore oluştur ve güvenli/yedekli sakla; hassas dosyaları git’e ekleme.
- [ ] Temiz committen imzalı production AAB üret.
- [ ] Kişisel veri içermeyen light/dark mağaza görsellerini tamamla: Home, All Apps, klasör, arama, izinler, dashboard/rapor, özelleştirme, backup/restore, görevler.
- [ ] `cycle.ps1` uçtan uca gerçek turunu temiz dalda çalıştır; commit/push/bildirim kanıtını kaydet.
- [ ] Play Console yükleme readback ve inceleme sonucunu kanıt dosyasına işle.

**Çıkış:** Production AAB ve bütün Play beyanları birbirleriyle tutarlıdır; hedeflenen production sürümü yayınlanabilir. Sürüm numarası `app/build.gradle.kts` ile aynı olmalıdır.

## 14. Faz R9 — Production yayın sonrası backlog

**Tahmini efor:** Backlog maddesi sprint’e alınırken ayrı tahmin edilir. **Hedef:** R8 sonrası stabilizasyon kapısında atanır.

**Başlangıç koşulu:** R8 tamamlandıktan sonra 2 haftalık ilk stabilizasyon sprintinde yalnız production izleme ve kritik düzeltmeler yapılır. R9 özellik geliştirmesi bu sprintin sonunda, açık kritik hata/Crash/ANR/veri kaybı yoksa başlar; kritik hata varsa bütün R9 maddeleri en az bir sprint ertelenir.

Bu sıra release’den önce değiştirilmez:

1. [ ] Wrapped Phase 2 UsageEvents oturum altyapısını API 28/29+, split-screen, kilit/aç, reboot ve izin grant/revoke ile OEM cihazlarda doğrula.
2. [ ] SAF/Drive “yedekle ve ikinci cihazda kur” akışını sadeleştir; usage/notification verisini yedeğe dahil etmeyi açık seçim yap.
3. [ ] Kullanım verisine göre ekranlar arası gerçek serbest item taşımasını değerlendir.
4. [ ] Çoklu cihaz senkronizasyonu için önce SharedPreferences→Room/outbox köprüsü kararını ver; Firebase Auth/Firestore/E2EE’ye daha sonra geç.
5. [ ] Kendi kategori sunucu API’si.
6. [ ] Wear OS companion.
7. [ ] Launcher dışı widget ekran genişletmesi.
8. [ ] TR/EN dışındaki diller için locale bazlı sıralama, çoğul kuralları, çeviri QA ve fallback politikasını tasarla; dil eklenmeden test matrisi ve kaynak anahtarı eşitliği kapısını tanımla.

## 15. Durum tablosu

| Faz | Durum | Başlama kapısı | Tamamlanma kanıtı |
|---|---|---|---|
| R0 Konsolidasyon | Bekliyor | — | Tek aktif roadmap + bağlayıcı Hero şartnamesi |
| H1 Hero Dashboard | ✅ Tamamlandı | R0 | 11 Hero*.kt dosyası + compile/test kanıtı |
| R1 Baseline/performance | Bekliyor | H1 temel kapı | Ölçüm ve deneysel grid cihaz kanıtı |
| R2.1–R2.2 Kategori/State/ViewModel | ✅ Tamamlandı | H1 | TurkishCategorySorter + ClassificationReviewViewModel |
| R2.3–R2.4 UI Screen + Test | Bekliyor | H1 | ClassificationReviewScreen + test kanıtı |
| R3 Merge motoru/UI | Bekliyor | R2 | Engine/ViewModel/UI testleri (planlama aşaması) |
| R4 Transaction/undo | Bekliyor | R3 | Migration/rollback/restart kanıtı (planlama aşaması) |
| R5 Hero doğrulama | Bekliyor | H1 | 4/4 cihaz + Firebase doğrulaması |
| R6A Güvenli legacy temizlik | Bekliyor | H1 | Davranışsız dead-code/test temizliği |
| R6B Kalıcı legacy kaldırma | Bloke | R5 | Migration kararı + regresyon paketi |
| R7 Birleşik QA + beta | Bekliyor | R2–R6A; beta için R6B adayı | R7.1–R7.5 evidence paketi |
| R8 İlk production yayın | Bloke | R7.5 | İmzalı AAB + Console readback |
| R9 Production sonrası | Ertelendi | R8 | Ayrı ürün kararı |

**Tablo bakım kuralı:** Aktif geliştirmede her çalışma döngüsü/stand-up sonunda durum, kalan efor, sahip ve kanıt bağlantısı güncellenir. Bir faz `Devam ediyor` durumunda 3 iş günü boyunca yeni kanıt veya durum değişimi üretmezse kök neden incelenir; gerçek dış bağımlılık varsa `COZULEMEYEN_SORUNLAR.md` kaydına sahip ve son tarihle taşınır, normal planlı çalışma otomatik olarak blokaj sayılmaz.

## 15.5. Paralel Faz — UI Redesign (R-HOME-LAYOUT, R-HOME-NAV, R-HOME-TICKER, R-FOLDER-SUMMARY, R-ALLAPPS-MODERN, R-SETTINGS-AUDIT)

**Bağımlılık:** H1 temel kapı (UI bileşenleri hazır olmalı)  
**Başlama:** 2026-07-22  
**Tahmini efor:** 12–16 gün (24–32 puan; cihaz testi hariç)

### Görevler (Uygulama Sırası)

#### R-HOME-LAYOUT: HomeScreen Layout Standardizasyon
**Durum:** Yapılacak  
**Tahmini:** 3 gün (6 puan)

- [ ] `StandardLayoutContainer` composable oluştur (responsive padding: telefon 16dp/tablet 24-32dp)
- [ ] HomeScreen, AllAppsDrawer, FolderScreen, SettingsScreen'e uygula
- [ ] Responsive grid: 4 sütun (<600dp), 5 sütun (600-800dp), 6 sütun (800+dp)
- [ ] Telefon/tablet testleri, taşma kontrol

**Dosyalar:** HomeScreen.kt, AllAppsDrawer.kt, FolderScreen.kt, SettingsScreen.kt

#### R-HOME-NAV: Navigation Dots Senkronizasyonu
**Durum:** Yapılacak  
**Tahmini:** 2 gün (4 puan)

- [ ] HorizontalPager state'i HomeScreenPageIndicator'e direkt bağla
- [ ] Gesture debounce: sayfa geçişi sırasında tıklama reddet
- [ ] Sağ-sol kaydırma + dot tıklama testleri

**Dosyalar:** HomePagerHost.kt, HomeScreenPageIndicator.kt

#### R-HOME-TICKER: Görevler/Haber Şeridi Etkinleştir
**Durum:** Yapılacak  
**Tahmini:** 1,5 gün (3 puan)

- [ ] AppPrefs.KEY_TICKER_ENABLED kontrol et (varsayılan: açık)
- [ ] HomeTickerRow HomeScreen'e entegre et
- [ ] SettingsScreen > Görünüm > "Haber Şeridi" toggle ekle

**Dosyalar:** HomeScreen.kt, SettingsScreen.kt, AppPrefs.kt

#### R-FOLDER-SUMMARY: Klasör Visual Summary (Sayı Yerine)
**Durum:** Yapılacak  
**Tahmini:** 3 gün (6 puan)

- [ ] FolderTile: 3-4 uygulama ikonunu grid içinde preview göster
- [ ] HomeIntelligenceCardsRow: "Yönetim" kartı ekle (klasör/app istatistikleri)
- [ ] Dashboard açılış navigasyonu

**Dosyalar:** FolderTile.kt, HomeIntelligenceCardsRow.kt, SmartDashboardCard.kt

#### R-ALLAPPS-MODERN: AllAppsDrawer Modernizasyon
**Durum:** Yapılacak  
**Tahmini:** 4 gün (8 puan)

- [ ] Modern search bar + filter UI (dropdown kategoriler)
- [ ] LazyVerticalGrid layout (responsive sütun)
- [ ] Section headers (kategori başlıkları)
- [ ] Performance test (scroll, filter)

**Dosyalar:** AllAppsDrawer.kt, AllAppsDrawerUtils.kt

#### R-SETTINGS-AUDIT: Ayarlar Tam Gözden Geçirme
**Durum:** Yapılacak  
**Tahmini:** 5 gün (10 puan)

- [ ] 6 kategoriyi gözden geçir (Görünüm, Launcher, Bildirimler, Arama, Uygulamalar, İstatistikler, Güvenlik, Hakkında)
- [ ] Gereksiz toggle'ları sil (Quick Wheel, duplicate Recent/Favorites rows)
- [ ] Search toggle logic: 3 toggle'dan sadece biri aktif olmalı
- [ ] Widget logic: area + free-grid uyumlu çalışması
- [ ] Her toggle bağımsız test

**Dosyalar:** SettingsScreen.kt, tüm SettingsXxxScreen.kt'ler, AppPrefs.kt

### Test Protokolü (Her görev sonunda)
- ✅ Telefon (4.5-6.5")
- ✅ Tablet (7-10")
- ✅ Responsive: 80%, 100%, 120% font scale
- ✅ Portrait + Landscape
- ✅ Edge-to-Edge padding
- ✅ Taşma/clipping kontrol

**Çıkış:** Tüm ekranlar tutarlı layout, navigation dots çalışıyor, haber şeridi etkin, klasörlerde visual preview, modern AllApps, temiz ayarlar.

---

## 16. Roadmap bakım kuralları

- Yeni iş doğrudan bu dosyaya eklenir; yeni `*_ROADMAP.md` oluşturulmaz. Tek istisna mevcut bağlayıcı Hero tasarım şartnamesidir.
- Çelişki varsa commit tarihi daha yeni karar kazanır; aynı committe açık ürün kararı önceliklidir.
- Tamamlanan ayrıntılar burada büyütülmez, `HISTORY.md` içine kısa kanıtla taşınır.
- Durum yalnız `Bekliyor`, `Devam ediyor`, `Kısmen tamamlandı`, `Bloke`, `Tamamlandı`, `Ertelendi` olabilir.
- Her aktif iş tek faz ve tek döngü kimliği taşır; aynı iş iki yerde izlenmez.
- Dış aksiyon backlog değildir; `COZULEMEYEN_SORUNLAR.md` içinde sahip ve beklenen kanıtla tutulur.
- Aktif dış engel kaydı `Sahip`, `Son tarih (YYYY-MM-DD)`, `Beklenen kanıt` ve `Sonraki eskalasyon` alanları olmadan R8 planına alınmaz.
- Yeni özellik release kapısını riske atıyorsa R9’a taşınır.
