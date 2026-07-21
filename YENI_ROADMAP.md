# AppOrganizer — Birleşik Teknik Roadmap

> **Tek aktif yol haritası**  
> **Birleştirme tarihi:** 2026-07-21  
> **Kaynak önceliği:** Daha yeni commit/dosya kararı, eski kararı geçersiz kılar. `YENI_HERO_DASHBOARD_BIREBIR_UYGULAMA_ROADMAP.md` yerel kopyası kaldırılmış olsa da 21 Temmuz 2026 tarihli kesin ürün kararı ve onu izleyen kod değişikliği geçerlidir.  
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
- Çoklu cihaz senkronizasyonu v1.0 sonrasıdır. Önce SAF/Drive yedekle–geri yükle akışı güçlendirilir.
- Paging3 ve `beyondViewportPageCount` artırımı mevcut ölçekte kapsam dışıdır.

## 2. Çalışma protokolü — token ve döngü bütçesi

Her döngü tek bir teslimat sınırına sahip olmalıdır. Bir döngüde en fazla bir domain değişikliği, bir UI dilimi veya bir doğrulama paketi yapılır.

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
     │   └─ R2 Kontrol Bekleyenler A tasarımı
     │       └─ R3 Klasör birleştirme domain + inceleme UI
     │           └─ R4 Atomik merge + kalıcı undo
     ├─ R5 Hero Dashboard cihaz/telemetri doğrulaması
     │   └─ R6 Legacy dashboard temizliği
     └─ R7 Birleşik cihaz/erişilebilirlik/telemetri QA
     └─ R8 Play Store release
         └─ R9 V1 sonrası ürün geliştirmeleri
```

R1 ve R5, H1’in temel composition kapısı geçtikten sonra paralel ilerleyebilir. R2, R1 baseline olmadan; R4, R3 bitmeden; R6, Hero doğrulaması bitmeden; R8, R7 bitmeden başlatılamaz.

## 4. Faz R0 — Baseline ve belge konsolidasyonu

**Amaç:** Tek gerçek kaynak oluşturmak ve eski kararların yeniden uygulanmasını engellemek.

- [x] Eski roadmap’lerdeki açık/kısmi işleri birleştir.
- [x] Daha yeni kararları üstün tut; Hero Dashboard’un yerel roadmap silme commit’ini karar iptali olarak yorumlama.
- [x] Tamamlanmış maddeleri yeniden backlog’a alma.
- [x] `ROADMAP.md`, `ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md`, `ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md`, `KATEGORI_ROADMAP.md` ve `KLASOR_BIRLESTIRME_ROADMAP.md` dosyalarını bu dosya yayınlandıktan sonra kaldır.
- [ ] Ana dalda hedefli doğrulama: temiz checkout, `git status`, roadmap bağlantısı araması.

**Çıkış:** Depoda yalnız `YENI_ROADMAP.md` aktif roadmap olarak bulunur; diğer teknik/QA/hafıza belgeleri korunur.

## 5. Faz H1 — Acil: Hero Dashboard dönüşümünü tamamla

**Neden ilk:** `main` üzerindeki son dönüşüm eski dashboard state/bölümlerinin önemli kısmını kaldırdı; `SmartDashboardPage` ise yeni `HeroDashboardPage` bağlanmadan geçici Pulse Clock + Today/HomeIntelligence içeriğinde kaldı. Bu ara durum yeni özelliklerden önce kapatılmalıdır.

### H1.0 Baseline ve kırık HEAD kontrolü

- [ ] Temiz checkout’ta compile, hedefli unit test ve debug build al; geçici işlev kaybını ekran görüntüsü/smoke ile kaydet.
- [ ] `SmartDashboardPage`, `DashboardUiState`, `DashboardStateAssembler` ve ilgili testlerde son commit etkisini doğrula.
- [ ] Commit mesajına dayanarak kategori roadmap’ini başlamış/tamamlanmış sayma; son commit kategori kodunu değiştirmedi.

### H1.1 Hero tasarım altyapısı

- [ ] `hero/` altında tek kaynak `HomeHeroTokens`, adaptif `HomeHeroProfile`/`HomeHeroLayoutPolicy` ve `PremiumGlassSurface` oluştur.
- [ ] Referans düzeni 360×640dp esas al: saat 252×114, ana kartlar 304dp genişlik, Dijital Yaşam 96dp, arama 74dp, Akıllı Erişim 162dp, dock 340×64dp.
- [ ] Ham px kullanma; sistem bar/cutout ve 320×568–412×915 aralığını policy ile yönet.
- [ ] Mevcut global `GlassCard` bileşenini Hero uğruna değiştirme.

### H1.2 Hero kartları

- [ ] Mevcut Pulse Clock verisini `HeroClockCard` içine bağla; saat/tarih sunumunu değiştir, motoru yeniden yazma.
- [ ] `DigitalPulseEngine`/`HomePulseSummary` verisini `HeroDigitalLifeCard` içine tek skor kaynağı olarak bağla.
- [ ] Search overlay’i yeniden yazmadan ayrı `HeroSearchCard` launch surface oluştur ve `FullScreenSearchOverlayV2` akışını aç.

### H1.3 Akıllı Erişim

- [ ] `SmartAccessModels`, `SmartAccessCoordinator`, deterministik `SmartAccessRanker` ve dedupe policy oluştur.
- [ ] `Şimdi`, `Son Açılanlar`, `Bildirimler` sekmelerini aynı beş slotlu UI’a bağla.
- [ ] Şimdi: zaman bağlamı + kullanım sinyali; Son Açılanlar: gerçek timestamp; Bildirimler: son bildirim zamanı kullanmalı.
- [ ] Kaldırılmış/gizli/kilitli-geçersiz uygulamaları dışla; sekmeler arasında gereksiz tekrar üretme.
- [ ] Usage/Notification izni yoksa yanıltıcı boş veri yerine açıklama ve doğru ayar yönlendirmesi göster.
- [ ] Uygulama/paket/kişi/dosya adını telemetriye gönderme.

### H1.4 Sabit uygulama dock’u ve klasör migration’ı

- [ ] Hero dock’u beş sabit uygulama slotu olarak uygula; klasör ve dinamik öneri kabul etme.
- [ ] Mevcut kullanıcı dock uygulamalarını veri kaybetmeden migrate et; klasör dock öğelerini silme, yalnız Hero dock’ta gösterme.
- [ ] Klasörleri Sayfa 0’dan çıkar; Sayfa 1+ pager ve semantic anchor düzenini koru.

### H1.5 Composition ve ilk kapı

- [ ] `SmartDashboardPage` içeriğini `HeroDashboardPage` composition’ına dönüştür.
- [ ] Saat → Dijital Yaşam → Arama → Akıllı Erişim → gösterge → dock sırasını uygula.
- [ ] 320×568, 360×640, 412×915, tablet ve landscape layout policy testlerini yaz.
- [ ] Ranker/dedupe unit testleri ile bounds/interaction testlerini ekle.
- [ ] Compile, unit test, lint ve debug build kapısını geçir; en az bir telefon/tablet smoke yap.

**Bu fazda yapılmayacak:** Eski ve yeni dashboard’u feature flag ile paralel tutmak, veri motorlarını yeniden yazmak, global cam temasını değiştirmek, legacy temizliğini doğrulama tamamlanmadan körlemesine bitirmek.

**Çıkış:** Sayfa 0 gerçek Hero Dashboard’dur; temel kartlar ve üç Akıllı Erişim sekmesi gerçek veriye bağlıdır; klasörler ve kullanıcı tercihleri kaybolmaz.

## 6. Faz R1 — Ölçüm ve mevcut sistem güvenlik kapısı

### R1.1 Performans ölçümü

- [ ] Baseline Profile sonucunu gerçek telefon/tablette ölç; cold start ve ana ekran geçişini karşılaştır.
- [ ] Samsung SM-X210 üzerinde kaydedilen `%14,11` janky frame değerini aynı senaryoyla yeniden ölç.
- [ ] Macrobenchmark çıktısını kanıt dosyasına yaz; ölçüm olmadan yeni performans refactor’ı yapma.
- [ ] Periyodik worker’larda pil kısıtlarının gerçek schedule davranışını doğrula.

### R1.2 Serbest yerleşim doğrulaması

- [ ] `Klasörde Serbest Yerleşim` ve `Widget Alanında Serbest Yerleşim` toggle’larını yoğun veriyle test et.
- [ ] Frame drop, process death, rotation, TalkBack ve drag davranışını doğrula.
- [ ] `LauncherAccessibilityService` için gerçek ihtiyaç yoksa stub’ı büyütme; gerekiyorsa ayrı karar kaydı oluştur.
- [ ] Ekranlar arası gerçek item taşımasını bu faza ekleme; kullanım kanıtından sonra ayrı değerlendirme yap.

**Çıkış:** Performans regresyonu yoktur veya ölçülmüş bir eşik/iyileştirme hedefi vardır; deneysel grid güvenli biçimde kapatılabilir.

## 7. Faz R2 — Kontrol Bekleyenler A tasarımı

**Bağımlılık:** R1 baseline.  
**Ana dosyalar:** `ClassificationReviewScreen.kt`, `AppListViewModel.kt`, classification review state/bileşen/test dosyaları.

### R2.1 Saf kategori altyapısı

- [ ] `TurkishCategorySorter` oluştur; kullanıcıya gösterilen ad üzerinden Türkçe locale sıralaması yap.
- [ ] `Kategorisiz` seçeneğini dışla; önerilen kategori tekrar etmesin.
- [ ] Uygulama kategorileri ve marka klasörlerini ayrı section’lara ayır.
- [ ] Sıralama ve grouping unit testlerini tamamla.

### R2.2 State ve ViewModel

- [ ] Tek aktif package, package→seçim map’i, sheet state, arama sorgusu ve processing package içeren immutable UI state oluştur.
- [ ] Pending liste değişince aktif uygulamayı güvenli uzlaştır.
- [ ] Onay, düzeltme ve erteleme işlemlerini çift tıklamaya/idempotency sorununa karşı koru.
- [ ] Repository hatasında processing state’i temizle ve kullanıcıya hata göster.
- [ ] ViewModel testlerini tamamla.

### R2.3 Bottom sheet ve ekran refactor’ı

- [ ] Arama, section başlıkları, seçili işareti ve boş durum içeren kategori picker sheet oluştur.
- [ ] Tek aktif uygulama kartında ikon, güven seviyesi, neden, sistem önerisi, seçilen kategori ve eylemleri göster.
- [ ] Eski yatay kategori `LazyRow`, sabit `560.dp`, nested liste ve UI-local seçim state’ini kaldır.
- [ ] Özet kartı ve sıradaki uygulamalar kuyruğunu bağla.
- [ ] Tıklama alanlarını en az 48dp yap; TalkBack, font scale 1.5, açık/koyu ve Pixel görünümünü doğrula.

### R2.4 Telemetri ve faz kapısı

- [ ] Yalnız privacy-safe event’leri ekle; paket adı gönderme.
- [ ] TR/EN string kaynaklarını tamamla; sabit UI metni bırakma.
- [ ] Unit, ViewModel ve Compose UI testlerini; lint, detekt ve debug build’i çalıştır.
- [ ] Küçük telefon, standart telefon ve tablette smoke test yap.

**Çıkış:** Tek kart + bottom sheet akışı güvenli çalışır; onay/düzeltme/ertele persistence ve sıra ilerlemesi kanıtlanır.

## 8. Faz R3 — Klasör birleştirme motoru ve inceleme UI

**Bağımlılık:** R2 ile manuel kategori/override kurallarının sabitlenmesi.

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

### R5.1 Dört cihaz matrisi

- [ ] Mevcut iki kanıtı koru: Samsung SM-X210 ve Pixel6 API 33 emülatör.
- [ ] Temiz kurulum telefonu ve izinleri kapatılmış ayrı cihaz/konfigürasyon ile matrisi 4/4 tamamla.
- [ ] Portrait/landscape, rotasyon+swipe, arama, dock, Dashboard, klasör grid ve All Apps’i doğrula.
- [ ] Küçük/standart/büyük telefon ile 7–8 ve 10+ inç tablet kırılımlarında taşma olmadığını doğrula.

### R5.2 Tek ürün yolu ve privacy-safe telemetry

- [ ] Sayfa 0’ın her açılışta yalnız Hero Dashboard olduğunu; eski dashboard/feature flag/safe-mode yolunun kalmadığını doğrula.
- [ ] Rıza kapalıyken hiçbir home telemetry event’i gönderilmediğini Firebase tarafında doğrula.
- [ ] Rıza açıkken yalnız izinli enum/bucket parametrelerinin gittiğini DebugView ile doğrula.
- [ ] Klasör adı, kategori, app/package, arama sorgusu, kişi ve dosya verisinin gönderilmediğini kanıtla.

**Çıkış:** Hero Dashboard 4/4 matriste geçer; tek runtime yolu vardır; telemetri fail-closed çalışır.

## 11. Faz R6 — Legacy Hero dashboard temizliği

**Bağımlılık:** R5 tamamlanmadan başlamaz.

- [ ] Eski `DashboardContentGroup`, `dashboardGroupOrder`, `countVisibleSections` ve section-order ayarlarını repo genelinde ara ve kaldır.
- [ ] Dashboard widget/ticker/FolderStats/favorites/suggestions/recent-install dallarını yalnız başka ekran tüketmiyorsa kaldır.
- [ ] Kullanılmayan Dashboard state alanlarını, string’leri ve testleri sil veya Hero testlerine dönüştür.
- [ ] Eski folder-only pager çağrılarını repo genelinde ara.
- [ ] `last_home_page` eski anahtarını yalnız migration/restore uyumluluğu için tut.
- [ ] Kullanılmayan eski pager branch’lerini, duplicate search lambda’larını ve eski test fixture’larını kaldır.
- [ ] Hâlâ kullanılan `FOLDER_GRID`, yeni indicator ve tek sayfa grid renderer’ını yanlışlıkla silme.
- [ ] `HomeScreen.kt` dosyasını orchestration seviyesine indir; davranış değişikliği yapma.
- [ ] Regression testleri ve dört cihaz kısa smoke testini tekrar çalıştır.

**Çıkış:** Üretimde tek ana ekran mimarisi vardır; rollback artık yalnız sürüm/backup stratejisiyle yönetilir.

## 12. Faz R7 — Birleşik yayın öncesi QA

Bu faz, önceki roadmap’lerde tekrar eden cihaz matrislerini tek pakette toplar.

- [ ] Pulse Clock 3 stil, görevler, Dijital Yaşam skoru ve Akıllı Nabız ticker test matrisi.
- [ ] Günlük/haftalık görev settlement, duplicate ödül, timezone ve process-death testleri.
- [ ] Android 14 NotificationListener izin aç/kapa, reboot ve event/rapor testi.
- [ ] SAF export/import, Drive klasör seçimi, missing packages ve restore sonrası ayar sürekliliği.
- [ ] SmartInsightWorker, BackupWorker ve diğer periyodik worker schedule/pil testleri.
- [ ] Android 13+ POST_NOTIFICATIONS reddinde sessiz/güvenli davranış.
- [ ] API 26 blur fallback; Samsung/Xiaomi/Google OEM kategori smoke.
- [ ] Serbest grid, Kontrol Bekleyenler, merge/undo, Dashboard pager ve global arama regresyonu.
- [ ] TalkBack, animasyonlar kapalı, font %200, açık/koyu tema ve farklı duvar kâğıtları.
- [ ] `testDebugUnitTest`, `lintDebug`, `detekt`, `assembleDebug`, uygun cihazda `connectedDebugAndroidTest`.
- [ ] Sağlık raporu ve Firebase kanıtlarını tek release evidence paketinde topla.

**Çıkış:** Kritik hata yoktur; telefon ve tablet smoke geçmiştir; kanıt bağlantıları kayıtlıdır.

## 13. Faz R8 — Play Store v1.0 yayın kapısı

**Bağımlılık:** R7 tamamlanmalı. Hesap/cihaz gerektiren maddeler `COZULEMEYEN_SORUNLAR.md` ile birlikte yürütülür.

- [ ] QUERY_ALL_PACKAGES beyanını launcher temel işlevi gerekçesiyle doldur.
- [ ] Data Safety formunu gerçek Firebase, opsiyonel kişi/dosya, NotificationListener, backup ve AI davranışıyla eşleştir.
- [ ] Content rating anketi ve Privacy Policy URL alanını tamamla.
- [ ] Kalıcı release keystore oluştur ve güvenli/yedekli sakla; hassas dosyaları git’e ekleme.
- [ ] Temiz committen imzalı production AAB üret.
- [ ] Kişisel veri içermeyen light/dark mağaza görsellerini tamamla: Home, All Apps, klasör, arama, izinler, dashboard/rapor, özelleştirme, backup/restore, görevler.
- [ ] `cycle.ps1` uçtan uca gerçek turunu temiz dalda çalıştır; commit/push/bildirim kanıtını kaydet.
- [ ] Play Console yükleme readback ve inceleme sonucunu kanıt dosyasına işle.

**Çıkış:** Production AAB ve bütün Play beyanları birbirleriyle tutarlıdır; v1.0 yayınlanabilir.

## 14. Faz R9 — v1.0 sonrası backlog

Bu sıra release’den önce değiştirilmez:

1. [ ] Wrapped Phase 2 UsageEvents oturum altyapısını API 28/29+, split-screen, kilit/aç, reboot ve izin grant/revoke ile OEM cihazlarda doğrula.
2. [ ] SAF/Drive “yedekle ve ikinci cihazda kur” akışını sadeleştir; usage/notification verisini yedeğe dahil etmeyi açık seçim yap.
3. [ ] Kullanım verisine göre ekranlar arası gerçek serbest item taşımasını değerlendir.
4. [ ] Çoklu cihaz senkronizasyonu için önce SharedPreferences→Room/outbox köprüsü kararını ver; Firebase Auth/Firestore/E2EE’ye daha sonra geç.
5. [ ] Kendi kategori sunucu API’si.
6. [ ] Wear OS companion.
7. [ ] Launcher dışı widget ekran genişletmesi.

## 15. Durum tablosu

| Faz | Durum | Başlama kapısı | Tamamlanma kanıtı |
|---|---|---|---|
| R0 Konsolidasyon | Devam ediyor | — | Tek roadmap + eski roadmap silme commit’i |
| H1 Hero Dashboard | Acil/devam ediyor | R0 | Gerçek Hero composition + test/build/smoke |
| R1 Baseline/performance | Bekliyor | H1 temel kapı | Ölçüm ve deneysel grid cihaz kanıtı |
| R2 Kontrol Bekleyenler | Bekliyor | R1 | Testler + telefon/tablet smoke |
| R3 Merge motoru/UI | Bekliyor | R2 | Engine/ViewModel/UI testleri |
| R4 Transaction/undo | Bekliyor | R3 | Migration/rollback/restart kanıtı |
| R5 Hero doğrulama | Bekliyor | H1 | 4/4 cihaz + Firebase doğrulaması |
| R6 Legacy temizlik | Bloke | R5 | Tek runtime yol + regresyon paketi |
| R7 Birleşik QA | Kısmen tamamlandı | R2–R5 | Release evidence paketi |
| R8 Play Store | Dış aksiyon bekliyor | R7 | İmzalı AAB + Console readback |
| R9 V1 sonrası | Ertelendi | R8 | Ayrı ürün kararı |

## 16. Roadmap bakım kuralları

- Yeni iş doğrudan bu dosyaya eklenir; yeni `*_ROADMAP.md` oluşturulmaz. Tek istisna mevcut bağlayıcı Hero tasarım şartnamesidir.
- Çelişki varsa commit tarihi daha yeni karar kazanır; aynı committe açık ürün kararı önceliklidir.
- Tamamlanan ayrıntılar burada büyütülmez, `HISTORY.md` içine kısa kanıtla taşınır.
- Durum yalnız `Bekliyor`, `Devam ediyor`, `Kısmen tamamlandı`, `Bloke`, `Tamamlandı`, `Ertelendi` olabilir.
- Her aktif iş tek faz ve tek döngü kimliği taşır; aynı iş iki yerde izlenmez.
- Dış aksiyon backlog değildir; `COZULEMEYEN_SORUNLAR.md` içinde sahip ve beklenen kanıtla tutulur.
- Yeni özellik release kapısını riske atıyorsa R9’a taşınır.
