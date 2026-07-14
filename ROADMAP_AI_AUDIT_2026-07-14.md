# AppOrganizer — AI Kod Denetimi Sonrası Geliştirme Roadmap'i

**Tarih:** 2026-07-14  
**Depo:** `hekizoglu/android-folderautomanager`  
**Ürün:** AppOrganizer / Android Auto Folder  
**Kaynak:** 2026-07-14 depo incelemesi ve nokta atışı geliştirme planı  
**Durum:** Hüseyin tarafından roadmap'e alınması onaylandı.

> Bu dosya, ana `ROADMAP.md` içindeki Play Store/release kapılarını değiştirmez. Buradaki ürün ve teknik geliştirmeler, release güvenliğini bozmadan P0 → P1 → P2 sırasıyla uygulanır.

---

## 1. Kapsam ve Tamamlanan Temel Düzeltmeler

Aşağıdaki iki iş PR #5 ile `main` dalına taşındı ve bu backlog'a tekrar eklenmedi:

- Widget seçme/yapılandırma sırasında `AppWidgetId` yaşam döngüsünün düzeltilmesi.
- Kontrol Bekleyenler ekranında kategori seçmeden sınıflandırma onayının kaldırılması.

**Not:** Otomatik CI workflow'u çalışmadığı için her yeni geliştirme döngüsünde Gradle derleme/test kapısı ayrıca uygulanmalıdır.

---

# P0 — Doğrudan Bozuk veya Yanlış Çalışan Akışlar

## P0.1 — Klasör içinden kategori değiştirme akışını düzelt

**Durum:** Bekliyor — gerçek hata  
**Kök neden:** `FolderScreen.kt` içinde `categoryPickerApp` state'i `contextMenuApp?.let` bloğunda tanımlı. Menü kapatılınca state'i render eden blok da kayboluyor.

**Yapılacaklar:**

- `categoryPickerApp` state'ini koşullu context-menu bloğunun dışına taşı.
- `CategoryPickerSheet` render'ını context menüden bağımsız yap.
- Kategori güncellemesi sonrası açık klasör, klasör listesi ve sayaçların Room Flow üzerinden anında yenilendiğini doğrula.
- Aynı davranışı Home, All Apps ve Folder ekranlarında ortaklaştır.

**Ana dosyalar:**

- `presentation/ui/launcher/FolderScreen.kt`
- `presentation/ui/launcher/AppContextMenu.kt`
- `presentation/ui/launcher/LauncherViewModel.kt`

**Kabul kriteri:** Uzun bas → Kategori Değiştir her giriş noktasında sheet açar; seçim sonrası uygulama doğru klasöre taşınır ve eski klasörde kalmaz.

---

## P0.2 — Sınıflandırılmamış / Diğer / Kontrol Bekleyenler sayaçlarını tek politikada birleştir

**Durum:** Bekliyor  
**Sorun:** `uncategorized`, `other`, düşük güven, eksik kategori ve review-pending durumları farklı filtrelerle hesaplanabildiği için sayaç dolu iken liste boş kalabilir.

**Yapılacaklar:**

- Tek bir `ClassificationAttentionPolicy` veya eşdeğer domain katmanı oluştur.
- Her uygulama için açıklanabilir neden üret:
  - `UNCATEGORIZED`
  - `OTHER_WITHOUT_CONFIDENCE`
  - `LOW_CONFIDENCE`
  - `REVIEW_PENDING`
  - `MISSING_CATEGORY`
  - `CLASSIFIER_CONFLICT`
- Sayaç, liste ve dashboard aynı sorgu/politikadan beslensin.
- “Neden burada?” metni kullanıcıya gösterilsin.

**Kabul kriteri:** Ekrandaki sayı ile açılan listedeki öğe sayısı aynıdır; hiçbir uygulama açıklamasız şekilde “Diğer” veya “Kontrol Bekleyenler” altında görünmez.

---

## P0.3 — Dosya aramasına açık izin ve indeks durumu ekle

**Durum:** Bekliyor  
**Sorun:** İzin yokken indeksleme sessizce atlanabiliyor ve kullanıcı “sonuç yok” ile “izin yok” durumunu ayırt edemiyor.

**Yeni durum modeli:**

- `Disabled`
- `PermissionRequired`
- `Indexing(progress)`
- `Ready(itemCount, lastIndexedAt)`
- `Failed(reason)`

**Yapılacaklar:**

- Android sürümüne göre izin/SAF akışını açıkça göster.
- Arama ve Ayarlar ekranında aynı indeks state'i kullan.
- Yeniden indeksle, izin ver ve klasör seç aksiyonlarını doğru duruma bağla.
- Geçersiz URI izinlerini temizle.

**Kabul kriteri:** Kullanıcı dosya sonucunun neden görünmediğini her durumda anlayabilir; indeks hazır değilken “0 sonuç” yanlış mesajı gösterilmez.

---

## P0.4 — İstatistik sıfırlama sihirbazını tamamla

**Durum:** Bekliyor — mevcut iki aşamalı genel onay yeterli değil  
**Yapılacaklar:**

- Kullanıcının hangi veriyi sileceğini seçmesini sağla:
  - kullanım sayaçları,
  - son kullanım zamanları,
  - bildirim geçmişi,
  - Wrapped/haftalık karşılaştırmalar,
  - görev puanı ve görev geçmişi,
  - tamamı.
- İlk ekranda kapsam seçimi, ikinci ekranda geri alınamaz onay göster.
- Silme işlemini transaction/atomik servis üzerinden çalıştır.
- Başarılı ve başarısız sonuçları açıkça raporla.

**Kabul kriteri:** Kullanıcı sadece seçtiği veri grubunu siler; diğer raporlar ve tercihler korunur.

---

## P0.5 — Bildirimlerde gerçek “okunmamış” yerel modelini kur

**Durum:** Bekliyor  
**Sorun:** Aktif Android bildirimi, son 24 saat bildirimi ve uygulama içinde okunmuş durum aynı kavram gibi ele alınıyor.

**Yapılacaklar:**

- Paket bazlı `lastOpenedAt` veya `lastReadAt` sakla.
- “Okunmamış” filtresini `notificationTimestamp > lastReadAt` olarak hesapla.
- Launcher üzerinden uygulama açıldığında yalnız yerel okunma state'ini güncelle.
- Sistem bildirimini ancak kullanıcı açıkça dismiss ettiğinde veya güvenli `autoCancel` davranışında iptal et.
- Okunmamış sayısı, son bildirim sayısı ve aktif badge'i ayrı göster.

**Kabul kriteri:** Uygulamayı launcher'dan açınca yerel okunmamış etiketi temizlenir; kullanıcının sistem bildirimleri keyfi olarak silinmez.

---

## P0.6 — Sınıflandırma modlarını tek seçime indir

**Durum:** Bekliyor  
**Sorun:** Üretici sınıflandırması, yerel kural ve LLM seçenekleri paralel toggle'lar olarak birbirini ezebilir.

**Önerilen enum:**

- `LOCAL_ONLY`
- `LOCAL_WITH_MANUFACTURER`
- `LOCAL_WITH_LLM_FALLBACK`
- `MANUAL_REVIEW_ONLY`

**Yapılacaklar:**

- Ayarlardaki paralel toggle'ları tek sınıflandırma modu seçicisine dönüştür.
- Seçilmeyen motorun hiçbir arka plan akışında çalışmadığını garanti et.
- Sonuçlara `source`, `confidence`, `reason` ekle.
- Kullanıcı manuel kategori seçtiğinde otomatik motorun kararı ezmesini engelle.

**Kabul kriteri:** Ayarlarda seçilen mod ile gerçek çalışan classifier birebir aynıdır.

---

## P0.7 — “Diğer/Bilinmeyen” uygulamaları öneri akışına dahil et

**Durum:** Bekliyor  
**Yapılacaklar:**

- `categoryId=other` olup güveni 0/düşük olan uygulamaları “inceleme gerekiyor” listesine dahil et.
- Üretici/keyword/benzer uygulama sinyali varsa önerilen kategoriyi göster.
- Sinyal yoksa “yeterli sinyal yok” nedenini göster; sahte öneri üretme.

**Kabul kriteri:** Diğer/Bilinmeyen sayısı ile öneri ekranı arasında açıklanamayan fark kalmaz.

---

# P1 — Ana Ürün Deneyimi

## P1.1 — Tam ekran “Her Şeyi Ara” deneyimi

**Durum:** Bekliyor  
**Yapılacaklar:**

- Arama alanına dokununca tam ekran overlay veya ayrı route aç.
- Küçük `320dp` sonuç kartı sınırını kaldır; `LazyColumn` kullan.
- Uygulama, klasör, kişi, dosya ve ayar sonuçlarını tek akışta grupla.
- Klavye, sistem barları, geri tuşu ve TalkBack sırasını düzelt.
- Arama kapanınca sorguyu temizle; geçmiş tercihi ayrı ayar olabilir.

**Kabul kriteri:** Küçük telefon, büyük telefon ve tablette sonuçlar taşmadan tam ekran görünür.

---

## P1.2 — Arama sıfır durumunu bağlamsal hale getir

**Durum:** Bekliyor  
**Varsayılan içerik:**

1. Bu saatlerde kullanılan 5 uygulama
2. Bu saatlerde iletişim kurulan en fazla 3 kişi
3. Son 3 arama veya son açılan sonuç

**Kurallar:**

- Dock arama ekranında tekrar gösterilmez.
- Sorgu yazılınca sıfır durum tamamen kaybolur.
- Arama geçmişi cihazda tutulur, sınırlıdır ve temizlenebilir.
- Aynı sorgu tekrar tekrar çoğaltılmaz.

**Kabul kriteri:** Boş sorgu ekranı ana ekranın kopyası değil, gerçek bağlamsal başlangıç noktasıdır.

---

## P1.3 — Saat bazlı kişi önerileri

**Durum:** Bekliyor  
**Gizlilik kararı:** İlk sürümde `READ_CALL_LOG` isteme.

**Yapılacaklar:**

- Launcher arama sonucundaki Ara/SMS/WhatsApp aksiyonlarını yerel olay olarak kaydet.
- Telefon numarası yerine `contactId` veya geri döndürülemez kimlik kullan.
- Saat dilimi, haftanın günü, yakınlık ve son kullanım üzerinden puanla.
- Yeterli veri yoksa bölüm gösterme.
- Geçmişi kapatma ve temizleme ayarı ekle.

**Kabul kriteri:** Öneriler yalnız launcher içinden gerçekten başlatılan kişi aksiyonlarından öğrenir.

---

## P1.4 — Görev Sistemi V2: kalıcı, ölçülebilir ve yaşam kalitesi odaklı

**Durum:** Bekliyor — mevcut görev sistemi temel seviyede  
**Yapılacaklar:**

- SharedPreferences tabanlı toplam puanı Room tabanlı görev/olay geçmişine taşı.
- Günlük/haftalık görev örnekleri:
  - gece belirli saatten sonra ekran açmama,
  - belirli uygulama süresini azaltma,
  - bildirim yoğunluğunu düşürme,
  - kontrol bekleyen uygulamaları kategorize etme,
  - gerçek klasör birleştirme önerisini değerlendirme.
- Sistem tarafından doğrulanamayan görevlerde “yaptım” butonu kullanma.
- Görevler gerçek sinyal ile otomatik başarılı/başarısız olsun.
- Görev tekrarını, cooldown'u ve kullanıcı uygunluğunu dikkate al.

**Kabul kriteri:** Uygulama yeniden başlatıldığında görev durumu ve puan geçmişi kaybolmaz; hiçbir görev sahte/onur sistemiyle tamamlanmaz.

---

## P1.5 — Görev puanını Dijital Yaşam Skoru ile kontrollü ilişkilendir

**Durum:** Bekliyor  
**Yapılacaklar:**

- Dijital Yaşam Skoru temel davranış sinyallerinden hesaplanmaya devam etsin.
- Görev başarısı yalnız sınırlı bir katkı/ceza katsayısı olsun; skoru tek başına belirlemesin.
- Önerilen sınır: görev etkisi toplam skorun en fazla ±10 puanı.
- Kullanıcı skor detayında hangi sinyalin kaç puan etkilediğini görebilsin.

**Kabul kriteri:** Görev puanı spam'lenerek Dijital Yaşam Skoru yapay biçimde yükseltilemez.

---

## P1.6 — Saat, Görevler ve Dijital Yaşam Skoru kartlarını tam ayrıştır

**Durum:** Kısmen mevcut  
**Yapılacaklar:**

- `PulseClockWidget` içindeki skor ringini kaldır veya sadece saat stiline indir.
- Görevler ve skor kartlarını bağımsız tıklama alanları yap.
- Kartların göster/gizle ayarlarını birbirinden ayır.
- Küçük ekranda tek satır/iki sütun adaptasyonunu test et.

**Kabul kriteri:** Saat, görev ve skor aynı alanda üst üste binen üç farklı işlev gibi görünmez.

---

## P1.7 — Gerçek hava durumu ve saatlik sıcaklık şeridi

**Durum:** Bekliyor  
**Yapılacaklar:**

- `WeatherRepository` ve provider abstraction oluştur.
- Yaklaşık konum veya manuel şehir seçimi sun.
- Güncel durum + saatlik sıcaklık + günlük min/max göster.
- 30–60 dakika cache uygula; ağ yoksa son başarılı veriyi ve zamanını göster.
- Hava kartı kapatılabilir olmalı.

**Kabul kriteri:** Saat kartına dokununca Google araması açmak yerine uygulama gerçek, zaman damgalı hava verisi gösterir.

---

## P1.8 — Uygulama kataloğu cache ve olay bazlı yenileme

**Durum:** Kısmen mevcut  
**Yapılacaklar:**

- Room'daki uygulama kataloğunu launcher açılışında doğrudan göster.
- `ACTION_PACKAGE_ADDED/REMOVED/REPLACED` ile yalnız değişen paketi güncelle.
- Tam taramayı yalnız şu durumlarda çalıştır:
  - DB boş,
  - uygulama sürümü/schema değişmiş,
  - boot sonrası güvenlik uzlaştırması,
  - 6–12 saatlik düşük frekanslı fallback.
- All Apps açılışında tam `PackageManager` taraması yapma.
- Mevcut 5 dakikalık reconcile aralığını kaldır veya yükselt.

**Kabul kriteri:** Yüzlerce uygulamalı cihazda ana ekran ve All Apps cache'den anında açılır; yeni kurulan uygulama broadcast sonrası görünür.

---

## P1.9 — Genel tanılama ve TXT sağlık raporu

**Durum:** Bekliyor  
**Raporda bulunacaklar:**

- uygulama sürümü, cihaz/API,
- izin durumları,
- uygulama kataloğu sayısı ve son reconcile,
- sınıflandırma modları ve sayaçları,
- arama indeks kaynakları ve son indeks zamanı,
- bildirim listener/olay sayısı,
- görev engine/son olay,
- widget ID ve provider durumları,
- worker schedule özeti,
- son kritik hata özetleri.

**Gizlilik:** Paket listesi, bildirim metni, kişi adı/numarası ve arama sorgusu varsayılan rapora eklenmez.

**Kabul kriteri:** Kullanıcı tek aksiyonla paylaşılabilir, kişisel veri içermeyen `.txt` teşhis raporu üretir.

---

## P1.10 — Bildirim önizlemesini son 1–2 olayla profesyonelleştir

**Durum:** Bekliyor  
**Yapılacaklar:**

- `package -> latestText` tek değer modelini, paket başına en fazla 2 aktif önizlemeye dönüştür.
- Bildirim key, timestamp, uygulama paketi ve güvenli kısa önizleme tut.
- Hassas uygulamalar ve içerik gösterimi için kullanıcı ayarı/engelleme listesi sun.
- İçerik gösterimi kapalıysa yalnız “N bildirim” göster.
- Önizlemeleri NotificationListener aktif verisiyle uzlaştır.

**Kabul kriteri:** All Apps satırında en fazla iki kısa, güncel bildirim önizlemesi görünür; eski/silinmiş metin kalmaz.

---

# P2 — Görsel Kalite ve Sadeleştirme

## P2.1 — Yeni kurulumda varsayılan ikon ölçeği %130

**Durum:** Bekliyor  
**Kurallar:**

- Yeni kurulum ve “varsayılana dön” değeri `1.3f` olsun.
- Mevcut kullanıcının kaydedilmiş ölçeğini ezme.
- Dock, klasör ve All Apps taşma testlerini yap.

---

## P2.2 — Klasör blur özelliğini tamamen kaldır

**Durum:** Bekliyor  
**Yapılacaklar:**

- Ayarlar toggle'ını, pref key kullanımını ve render kodunu kaldır.
- Eski preference değerini güvenli şekilde yok say veya migration ile temizle.
- Düz, performanslı surface/tonal arka plan kullan.

**Kabul kriteri:** Kod tabanında klasör blur özelliğine ait aktif UI veya render yolu kalmaz.

---

## P2.3 — Varsayılan klasör şeklini yumuşak köşeli yap

**Durum:** Bekliyor  
**Kurallar:**

- Yeni varsayılan `rounded` olsun.
- Mevcut kullanıcı tercihi korunur.
- Onboarding ve preview ekranındaki varsayılan aynı değeri göstermeli.

---

## P2.4 — Adaptif 5 öğeli dock

**Durum:** Bekliyor  
**Yapılacaklar:**

- `DOCK_MAX_SIZE` ve tüm hardcoded `.take(4)` kullanımlarını tek config kaynağına bağla.
- Varsayılan kapasite 5 olsun.
- Uygulama ve klasör aynı dock slot modelini kullansın.
- Küçük ekranda ikon/spacing adaptif küçülsün; yatay taşma olmasın.
- Akıllı öneri doldurması sabit kullanıcı öğelerini yerinden oynatmasın.

**Kabul kriteri:** Beş öğe küçük telefonda da dengeli görünür; sabit ve bağlamsal slotlar kararlı kalır.

---

## P2.5 — Raporlar merkezini tek düz listeye indir

**Durum:** Bekliyor  
**Sorun:** Ana rapor grupları ile “Hızlı Erişim” aynı route'ları tekrar ediyor.

**Yapılacaklar:**

- Tek `LazyColumn` içinde raporları anlamlı sırayla göster.
- Duplicate hızlı erişim bloklarını kaldır.
- Her satırda kısa açıklama, veri dönemi ve son güncelleme bilgisi ver.
- Boş raporları gizlemek yerine neden boş olduğunu açıkla.

**Kabul kriteri:** Aynı rapora giden iki farklı tekrar satır kalmaz.

---

## P2.6 — Tek öneri kanalı politikası

**Durum:** Bekliyor  
**Yapılacaklar:**

- `SuggestionCoordinator` veya eşdeğer tek koordinatör oluştur.
- Aynı önerinin ticker, görev kartı ve sistem bildiriminde aynı anda görünmesini engelle.
- Kanal önceliği:
  1. uygulama içi görev/kart,
  2. ticker,
  3. yalnız yüksek değerli ve zaman duyarlıysa sistem bildirimi.
- Dedupe key, cooldown ve kullanıcı reddi geçmişi uygula.

**Kabul kriteri:** Kullanıcı aynı öneriyi kısa sürede birden fazla kanalda görmez.

---

## P2.7 — Klasör sınıflandırma önerilerini yeni kurulumda varsayılan açık yap

**Durum:** Bekliyor  
**Kurallar:**

- Yeni kurulumda öneriler açık olsun.
- Mevcut kullanıcı tercihi korunur.
- İlk öneride özelliğin ne yaptığı kısa ve kapatılabilir biçimde anlatılır.
- Düşük güvenli öneriler otomatik uygulanmaz; review akışına gider.

---

## P2.8 — Klasör geçiş animasyonlarını yeniden yaz

**Durum:** Bekliyor — mevcut D281 iyileştirmesi tam çözüm değil  
**Yeni iki mod:**

1. `ANDROID_SMOOTH` — varsayılan; parmakla birebir takip, velocity/threshold settle, 220–280 ms.
2. `IOS_ZOOM_FADE` — opsiyonel; kontrollü scale + fade.

**Teknik kurallar:**

- Klasör ID'sini threshold anında değil, animasyon settle sonunda değiştir.
- Tek `transitionProgress` kaynağı kullan.
- Reduce Motion açıkken animasyonu sadeleştir.
- Page-turn/3D karmaşasını varsayılan olmaktan çıkar.

**Kabul kriteri:** Hızlı ardışık swipe, ters yöne dönme ve yarım bırakma durumlarında klasör zıplamaz veya yanlış klasöre geçmez.

---

## P2.9 — Ana ekran yoğunluğunu azalt ve ürün hiyerarşisini netleştir

**Durum:** Bekliyor  
**Varsayılan hiyerarşi:**

1. Saat + tarih + isteğe bağlı hava
2. Arama
3. Görevler ve Dijital Yaşam Skoru kompakt kartları
4. Otomatik klasörler
5. Tek bağlamsal öneri satırı
6. Dock

**Kurallar:**

- Aynı bilgiyi ticker, kart ve satırda tekrar etme.
- “En Çok Kullandıklarım”, önerilenler ve dock aynı uygulamaları sürekli tekrarlamasın.
- Ana ekran ilk bakışta üç şeyi anlatmalı: otomatik düzen, hızlı arama, bağlamsal erişim.
- Küçük ekran için alan bütçesi ve minimum klasör görünürlüğü belirle.

**Kabul kriteri:** Ana ekran görsel olarak dolu değil; kullanıcının ilk dokunuş hedefi açık ve kararlıdır.

---

# Uygulama Sırası

| Sprint | Kapsam | Çıkış kapısı |
|---|---|---|
| Sprint 1 | P0.1–P0.3 | Kategori değişimi, sayaç politikası ve dosya indeks state'i testli |
| Sprint 2 | P0.4–P0.7 | Reset, okunmamış modeli ve sınıflandırma politikası testli |
| Sprint 3 | P1.1–P1.3 | Tam ekran arama ve bağlamsal sıfır durum |
| Sprint 4 | P1.4–P1.6 | Görev V2 ve skor entegrasyonu |
| Sprint 5 | P1.7–P1.10 | Hava, cache, tanılama, bildirim önizleme |
| Sprint 6 | P2.1–P2.5 | Varsayılanlar, dock ve rapor sadeleştirme |
| Sprint 7 | P2.6–P2.9 | Öneri politikası, animasyon ve ana ekran final düzeni |

---

# Her Sprint İçin Zorunlu Kalite Kapısı

```powershell
.\gradlew.bat testDebugUnitTest -PskipGoogleServices --no-daemon
.\gradlew.bat compileDebugKotlin -PskipGoogleServices --no-daemon
.\gradlew.bat assembleDebug -PskipGoogleServices --no-daemon
```

Ek zorunluluklar:

- Yeni kullanıcı metinleri `values/` ve `values-en/` resource dosyalarında olmalı.
- İlgili unit/UI testleri aynı commit serisinde eklenmeli.
- Her iş bağımsız, geri alınabilir commit olmalı.
- Her sprint sonunda fiziksel cihaz veya emülatör smoke senaryosu yazılmalı.
- Build çalıştırılamadıysa görev “tamamlandı” işaretlenmemeli.
- Privacy/Data Safety hikayesini değiştiren özellikler Play Store belgeleriyle birlikte güncellenmeli.

---

# Tamamlanmış Sayılma Standardı

Bir roadmap maddesi ancak aşağıdakilerin tümü sağlandığında tamamlanır:

1. Kod değişikliği ana akışta çalışıyor.
2. Unit veya uygun UI testi eklendi.
3. `compileDebugKotlin` ve `assembleDebug` geçti.
4. Manuel smoke adımı raporlandı.
5. TR/EN metinleri tamamlandı.
6. Ayar, privacy, backup ve migration etkisi gözden geçirildi.
7. Değişen dosyalar ve kalan risk HISTORY.md'ye yazıldı.
