# ROADMAP.md - AppOrganizer Aktif Yol Haritasi

> Son guncelleme: 2026-07-14.
> Bu dosya aktif yapilacaklar icin tek kaynak olarak kullanilir.
> Tamamlanan isler HISTORY.md'ye, yerelde cozulmeyen/dis aksiyon gerektirenler COZULEMEYEN_SORUNLAR.md'ye tasinir.
> **Vizyon (2026-07-14, Hüseyin):** Pixel Launcher klonu değiliz — kendi kimliği olan harika bir uygulama. Karar ölçütü: kullanıcı için en iyisi.

---

## ⭐ Yüksek Puanlı - Pulse Clock / Dijital Nabız (D244 devamı)

Yerel Pulse/Rapor detayları HISTORY.md'dedir. Aktif kalan tek kapı cihaz/emülatör doğrulamasıdır:

| Puan | Görev | Durum |
|---|---|---|
| — | Emülatörde/tablette manuel doğrulama: Pulse Clock 3 stil, skor/içgörü toggle'ları, uzun basma → yönetim ekranı, kompakt ekranda grid kaybolmuyor, klasör fihristi alt konumda grid üstüne binmiyor. 2026-07-13 kanıt: `Pixel6_API33` emülatörde `connectedDebugAndroidTest` 15 test / 0 failure geçti; AllApps/search/double-tap ve telefon `LauncherActivity` smoke crashsiz geçti. Simüle tablet AllApps/search crashsiz geçti; simüle tablet `LauncherActivity` screenshot denemesi ADB bağlantısını düşürdüğü için gerçek tablet/stabil tablet AVD görsel smoke gerekir. | Kısmen tamam - tablet Launcher görsel smoke bekliyor |

---

## Hüseyin Geri Bildirim Listesi (2026-07-14, 20 madde)

> Kaynak: Hüseyin'in ham geri bildirim listesi. KOD YAZILMADI — sadece analiz + roadmap kaydı. 🐛 etiketli maddeler gerçek kırık davranış; diğerleri yeni özellik/iyileştirme istekleri. Puanlama: KV=Kullanıcı Değeri, U=Uygulanabilirlik, BR=Bağımlılık Riski, EA=Etki Alanı (her biri 1-5).

### ⭐ Yüksek Puanlı (15+)

### [1] 🐛 İzin iste butonu izin sonrası takılı kalıyor
**Sorun/İstek:** Kullanıcı sistem izin dialogunda "izin ver" dedikten sonra ekrandaki "İzin Ver" butonu loading/stuck state'te kalıyor, UI güncellenmiyor.
**Nasıl yapılmalı:** `ContextualPermissionDialog.kt` ve `SettingsPermissionsSection.kt` içindeki `ActivityResultContracts.RequestPermission()` launcher callback'i kontrol edilmeli — callback içinde izin durumunu tutan state (muhtemelen `remember { mutableStateOf(...) }`) `ContextCompat.checkSelfPermission` ile yeniden okunmuyor olabilir. `PermissionsGuideScreen.kt`'de de aynı pattern var mı taranmalı. Çözüm: launcher callback içinde state'i `checkSelfPermission` sonucuna göre zorla güncelle, ayrıca `ON_RESUME` lifecycle event'inde de yeniden kontrol et (kullanıcı ayarlardan izin verip geri dönebilir).
**Puan:** KV=5 U=4 BR=4 EA=3 → Toplam=16
**Durum:** Tamamlandı (Döngü 265)

### [2] 🐛 Silip tekrar kurunca onboarding başlamıyor
**Sorun/İstek:** APK silinip yeniden kurulduğunda onboarding akışı tekrar başlamıyor, sanki eski kurulumun devamıymış gibi davranıyor.
**Nasıl yapılmalı:** `AppPrefs.kt`'deki `KEY_ONBOARDING_DONE` okuması `LauncherActivity.kt`/`MainActivity.kt` içinde kontrol edilmeli. Olası kök neden: Android'in otomatik yedekleme (Auto Backup for Apps) SharedPreferences dosyasını (`AppPrefs`) uygulama silinse bile Google hesabına yedekleyip yeni kurulumda geri yüklemesi — `AndroidManifest.xml`'de `android:allowBackup` ve `android:fullBackupContent` ayarı kontrol edilmeli. Çözüm adayı: `AppPrefs` dosyasını backup kapsamı dışına al (`fullBackupContent` XML'inde `<exclude domain="sharedpref" path="..."/>`) ya da onboarding flag'i cihaz-bağımlı bir değerle (ilk kurulum zaman damgası) doğrula.
**Puan:** KV=5 U=3 BR=4 EA=3 → Toplam=15
**Durum:** Tamamlandı (Döngü 265)

### 🟡 Orta Puanlı (10-14) — özet FİKİRLER.md'de de kayıtlı

### [3] Ayarlar > Uygulamalar: güven skoruna göre otomatik kategorize toggle'ı
**Sorun/İstek:** Uzun-bas kategori değiştirme bilgisinin yanına, AppClassifier güven skoruna göre otomatik kategorize ayarı taşınsın; güven skoru düşükse kullanıcıya sorulsun.
**Nasıl yapılmalı:** `AppClassifier.kt` içindeki confidence/skor hesaplama mantığı (`classificationSource`, `isCategoryLocked` alanları D246'da zaten eklenmiş — `AppInfo.kt`) temel alınabilir. `SettingsAppsSection.kt`'e yeni toggle (`KEY_AUTO_CLASSIFY_LOW_CONFIDENCE`, AppPrefs.kt'ye yeni key), düşük güvenli sınıflandırmalar `ClassificationReviewScreen.kt` akışına (mevcut "Kontrol Bekleyenler" ekranı) yönlendirilebilir — madde 14 ile aynı ekranı paylaşabilir.
**Puan:** KV=4 U=4 BR=3 EA=3 → Toplam=14
**Durum:** ✅ Tamamlandı (Döngü 280) — Ayarlar > Uygulamalar > Uygulama Yönetimi altına "Düşük Güvenli Kararları Sor" toggle'ı eklendi. Varsayılan açık: düşük güvenli otomatik kararlar Kontrol Bekleyenler'e düşer. Kapalıyken otomatik sınıflandırma/LLM/reset-reclassify kararları review state'i `NOT_REQUIRED` yazar.

### [4] Arama çubuğu klavye ile hafif çakışıyor — ✅ Tamamlandı (D267)
Çözüm: `HomeScreen.kt` kök `Column`'a `Modifier.imePadding()` eklendi. Detay → HISTORY.md Döngü 267.

### [5] 🐛 Ticker açık item'a tekrar tıklayınca donuyor — ✅ Tamamlandı (D265)
Çözüm: `HomeTickerRow.kt` tıklamaya 700ms debounce (`lastClickAt`) eklendi. Detay → HISTORY.md Döngü 265.

### [6] 🐛 Ticker'da swipe (kaydırma) çalışmıyor — ✅ Tamamlandı (D265)
Kök neden: ayrı `pointerInput` blokları üst `HorizontalPager` ile nested-scroll çakışıyordu. Çözüm: tap+swipe tek `awaitEachGesture` döngüsünde birleştirilip `down.consume()` ile jest bu bileşene kilitlendi. Detay → HISTORY.md Döngü 265.

### [7] Pulse Clock altındaki insight metni kaldırılsın, saat küçültülsün
**Sorun/İstek:** Saatin altındaki bilgilendirme metni donuk/işlevsiz görünüyor; kaldırılıp saat biraz küçültülebilir.
**Nasıl yapılmalı:** Pulse Clock bileşeni ve insight text muhtemelen `HomeScreenComponents.kt` içinde; `KEY_TICKER_ENABLED`/skor-insight toggle'larıyla ilişkili görünürlük mantığı var (D244 Pulse Clock işi, ROADMAP başındaki "Pulse Clock / Dijital Nabız" bölümüyle çakışıyor — aynı bölüme not düşülmeli). Tasarım kararı: insight text kaldır ya da madde 10'daki "Dijital Yaşam Skoru" rozetiyle birleştir, saat boyutu (`fontSize`/`Modifier.size`) düşürülsün. UX kararı olduğu için Fable/kullanıcı onayı önerilir.
**Puan:** KV=3 U=4 BR=2 EA=3 → Toplam=12
**Durum:** ✅ Tamamlandı (Döngü 277) — Pulse Clock insight metni yeni/varsayılan kurulumda kapalı hale getirildi; mevcut ayar korunuyor, kullanıcı Ayarlar > Ana Ekran bölümünden tekrar açabilir. Pulse kart yüksekliği ve saat fontu küçültülerek ana ekranda klasör/grid alanı açıldı.

### [8] Onboarding'de güçlü özellikler öne çıkarılsın
**Sorun/İstek:** Onboarding ekranları güzel ama AppClassifier 3702 paket, gizlilik-öncelikli bildirim analizi gibi güçlü yanlar yeterince öne çıkmıyor.
**Nasıl yapılmalı:** `OnboardingScreen.kt`'deki WELCOME/QUICK_SETTINGS adımlarına (onboarding sırası CLAUDE.md kuralı gereği bozulmayacak) somut sayı ve gizlilik vurgusu içeren metin/kart eklenmeli: "3700+ uygulama otomatik tanınır", "Bildirim içerikleri asla okunmaz, sadece sayılır" gibi. Metin İngilizce/Türkçe locale ile tutarlı olmalı (FİKİRLER.md'deki EN string sorunu ile çakışabilir, birlikte ele alınabilir).
**Puan:** KV=3 U=4 BR=2 EA=2 → Toplam=11
**Durum:** ✅ Tamamlandı (Döngü 275) — Welcome ekranındaki güçlü yanlar kartı resource tabanlı TR/EN metinlere taşındı; 3700+ uygulama tanıma, tek arama kutusu, Dijital Nabız raporları ve gizlilik vaadi somut anlatıldı. Hızlı ayarlardaki ana ekran araması açıklaması gerçek kapsamla uyumlu hale getirildi.

### [9] "En Çok Kullandıklarım" alanı küçültülüp yanına teknik bilgi eklensin
**Sorun/İstek:** Bu alan büyük yer kaplıyor; küçültülüp yanına ilginç/teknik bilgi eklenebilir.
**Nasıl yapılmalı:** `HomeScreenComponents.kt` içindeki ilgili composable boyutu (kart yüksekliği/ikon sayısı) küçültülmeli; yanına `LauncherViewModel.kt`'de zaten hesaplanan istatistiklerden biri (örn. günlük ortalama ekran açma sayısı, en yoğun saat aralığı) eklenebilir — `DashboardStats.compute()` (AppOrganizer Dashboard) ile aynı veri kaynağı kullanılabilir, CLAUDE.md §3 "AppOrganizer Dashboard Kuralı" ile uyumlu.
**Puan:** KV=3 U=3 BR=2 EA=2 → Toplam=10
**Durum:** ✅ Tamamlandı (Döngü 278) — Öneri satırı 4 yerine 3 uygulama gösterecek şekilde kompaktlaştırıldı; satırın yanına öneri sayısı ve sinyal kaynağı ("Son 28 gün + bu saat") teknik bilgi pili eklendi. TR/EN metinler resource'a taşındı.

### [10] "Dijital Yaşam Skoru" renk kodlu rozet ticker'a eklensin
**Sorun/İstek:** Ticker'a "Dijital Yaşam Skoru: 70" gibi bir skor eklensin, kötüyse kırmızı iyiyse yeşil.
**Nasıl yapılmalı:** `LauncherViewModel.kt`'deki `tickerItems` üretim mantığına yeni bir ticker item tipi eklenmeli; skor hesaplama muhtemelen mevcut `InsightEngine`/`UsageStatsHelper` verilerinden (ekran açma sıklığı, bildirim yoğunluğu, uzun kullanım süresi) türetilebilir. Renk: `Color.Red`↔`Color.Green` arası `lerp()` ile skor değerine göre interpolasyon (madde 7'deki insight text kaldırma kararıyla birlikte tasarlanmalı — belki onun yerini alır).
**Puan:** KV=4 U=3 BR=3 EA=3 → Toplam=13
**Durum:** ✅ Tamamlandı (Döngü 276) — Skor üretimi zaten `TickerComposer.computeDigitalLifeScore()` ile gerçek sinyallerden geliyordu; `HomeTickerRow` artık dijital/skor/denge ticker metnindeki `NN/100` değerini algılayıp "Skor NN" rozetini renk kodlu gösteriyor (80+ koyu yeşil, 60+ yeşil, 40+ sarı, altı kırmızı).

### [13] Ana ekranda "Görevler" (gamification) giriş noktası
**Sorun/İstek:** Saatin altına Görevler'e giden bir giriş noktası konsun; mevcut görev sistemi varsa profesyonelleştirilsin.
**Nasıl yapılmalı:** Önce mevcut bir "Görevler"/task sistemi olup olmadığı doğrulanmalı (bu taramada bulunamadı — muhtemelen yok, sıfırdan tasarım gerekir). Yeni bir `TasksScreen.kt` + `Routes.TASKS` + `HomeScreenComponents.kt`'ye saat altına küçük bir "Görevler (N)" chip/buton eklenmeli. Madde 15'teki puanlama motoruyla birlikte tasarlanmalı (aynı özelliğin iki yüzü) — mimari karar gerektirir, zorluk 7-8 sayılmalı (CLAUDE.md Görev Zorluk Puanı kuralı gereği önce 2+ kaynak araştırma).
**Puan:** KV=4 U=2 BR=2 EA=3 → Toplam=11
**Durum:** ✅ Tamamlandı (Döngü 274) — Mevcut `MissionsScreen`/`Routes.MISSIONS` altyapısı kullanıldı; ana ekranda saat kartının altına Görevler chip'i eklendi. Görev sistemi ayardan kapalıysa chip gizleniyor.

### [14] "Direkt Onayla" butonuna açıklama eklensin
**Sorun/İstek:** Kontrol Bekleyenler bölümünde "Direkt Onayla" butonu ne yaptığını açıklamıyor.
**Nasıl yapılmalı:** `ClassificationReviewScreen.kt:93` civarındaki "Onayla" butonunun yanına küçük bir açıklama metni veya `IconButton` + tooltip/`Text` eklenmeli: "Bu uygulama önerilen kategoriye taşınır, istersen sonra değiştirebilirsin." CLAUDE.md §6 "Ayarlar Metin ve Kod İnceleme Kuralı" — bilgi satırı ayar gibi davranmamalı, sade ve anlaşılır olmalı.
**Puan:** KV=3 U=5 BR=1 EA=2 → Toplam=11
**Durum:** ✅ Tamamlandı (Döngü 268)

### [15] Görev puanlama motoru — durum bazlı artan/azalan puan sistemi
**Sorun/İstek:** Klasör önerileri/birleştirme önerileri/kontrol bekleyenler işlem gördüğünde Görevler puanı artmalı; puan sistemi durum bazlı artıp azalabilmeli.
**Nasıl yapılmalı:** Yeni bir `GamificationEngine`/`TaskScoreManager` gerekir — Room'a yeni tablo (`task_events` veya `user_score`) veya `AppPrefs`'e basit sayaç ile başlanabilir. Puan artışı tetikleyicileri: `ClassificationReviewScreen.kt` onay aksiyonu, klasör birleştirme önerisi kabul (`AppClassifier.findSimilarUnclassifiedApps()` / `SimilarAppsSuggestionDialog.kt` — FİKİRLER.md K2 maddesiyle aynı altyapı). Madde 13 ile birlikte tasarlanmalı; mimari karar gerektirir (Görev Zorluk Puanı 7-8).
**Puan:** KV=3 U=2 BR=2 EA=3 → Toplam=10
**Durum:** ✅ Tamamlandı (Döngü 272) — `TaskScoreManager` SharedPreferences tabanlı ilk faz olarak eklendi. Sınıflandırma onayı/düzeltmesi, sınıflandırma erteleme, klasör önerisi kabul/ertele/gizle ve benzer uygulama önerisi kabul aksiyonları durum bazlı puan deltası yazıyor; Görevler ekranı toplam görev puanı ve son işlem deltasını gösteriyor. Build kullanıcı isteğiyle çalıştırılmadı.

### [17] 🐛 Birleşik arama kapsamı eksik — kategori/klasör/dosya adı aranmıyor
**Durum:** ✅ Tamamlandı (D265, doğrulama) — İnceleme sonucu kök neden analizi GÜNCEL DEĞİLMİŞ: `SearchDocument.kt` zaten `sourceType`/`SourceType` enum'una sahip (APP/CATEGORY/SETTING/CONTACT/FILE), `SearchIndexer.kt` kategori+app'i FTS'e indexliyor (D192 Room FTS5 iskeleti). `HomeAppSearchBar` (`HomeScreenComponents.kt:742-`) ayrıca klasör adını (özel ad dahil, `folderCustomNames`) yerel `folders` listesi üzerinden filtreleyip "Klasörler" grubunda gösteriyor (satır 850-858, 1011-1052) — kategori adı = klasör adı olduğundan kategori araması pratikte klasör grubunda karşılanıyor. Dosya adı arama zaten `FilesIndexer.kt`/`FilesIndexWorker` ile SAF üzerinden mevcut (kapsam dışı bırakılması istenen kısım zaten yapılmıştı). Kod değişikliği gerekmedi, sadece doğrulandı. Dosya adı arama zaten SAF kullanıyor (Android 16 kısıtına uygun).

### [18] AllAppsDrawer'da uygulama altına bildirim özeti
**Sorun/İstek:** Tüm Uygulamalar listesinde her uygulamanın altına, o uygulamadan bildirim geldiyse bunu yazalım.
**Nasıl yapılmalı:** `AllAppsDrawer.kt` (grep'te doğrudan bulunamadı, `AppIconView.kt`/`FolderTile.kt` bildirim badge mantığını zaten kullanıyor — CLAUDE.md §8 madde 10) her satıra `notification_events` tablosundan (paket bazlı) son N saatteki bildirim sayısı/özeti çekip küçük bir alt metin ("3 bildirim") eklenmeli. `AppNotificationListenerService.kt` + `NotificationAnalyzer` mevcut altyapı kullanılabilir, yeni sorgu (`AppDao`/`NotificationEventDao` paket bazlı count) gerekir.
**Puan:** KV=3 U=3 BR=2 EA=3 → Toplam=11
**Durum:** ✅ Tamamlandı (Döngü 279) — `notification_events` için reaktif paket bazlı son 24 saat sayımı eklendi. All Apps satırları bildirim metni kapalıyken ve veri varsa uygulama altında "Son 24 saatte N bildirim" gösteriyor; bildirim içeriği okunmuyor/gösterilmiyor.

### [21] Son bildirim gelen uygulamalar — "Günlük Öneriler" tarzı isteğe bağlı bölüm + Favoriler gibi çekmece/ana ekran eklenebilirliği
**Sorun/İstek:** Ana ekranda mevcut "Günlük Öneriler" (AppSuggestionsRow) bölümüne benzer, ama son bildirim alan uygulamaları gösteren, kullanıcının isteğe bağlı açıp kapatabildiği ayrı bir bölüm istendi. Bu bölüm — favoriler mekanizmasına benzer şekilde — hem AllAppsDrawer (çekmece) içinde hem de ana ekranda gösterilebilmeli.
**Nasıl yapılmalı:** Veri kaynağı madde 18 ile aynı (`notification_events` tablosu, `NotificationEventDao`/`AppDao` paket bazlı son-bildirim sorgusu) — bu iki madde ORTAK ALTYAPI paylaşır, aynı döngüde ele alınmaları maliyeti düşürür. Yeni bir `RecentNotificationAppsRow` composable'ı `HomeScreenComponents.kt`'deki mevcut `AppSuggestionsRow`/`SuggestionAppItem` pattern'i taklit ederek yazılabilir (aynı `iconSizeDp`, `GlassCard` görsel dili). Görünürlük: `AppPrefs.kt`'ye `KEY_RECENT_NOTIFICATIONS_ROW_ENABLED` (varsayılan kapalı — CLAUDE.md "Yeni Özellik = Ayarlar Kuralı" gereği) + `SettingsHomeScreenSection.kt`'e toggle. Favoriler mevcut altyapısı (`AppPrefs`/Room favori flag'i, favori chip/dock ekleme akışı — `DockEditSheet.kt` veya benzeri) incelenip aynı "çekmeceye ekle / ana ekrana ekle" UX pattern'i (muhtemelen uzun-bas context menüsünde "Ana ekrana ekle" seçeneği) bu yeni bölüm için de uygulanmalı — kod tekrarından kaçınmak için favori-ekleme mantığı ortak bir fonksiyona çıkarılabilir. Gizlilik notu: bildirim içeriği hiçbir zaman gösterilmez (CLAUDE.md/Privacy Policy ile tutarlı), sadece "son bildirim alan uygulama" paket+zaman bilgisi kullanılır.
**Puan:** KV=4 U=3 BR=3 EA=3 → Toplam=13
**Durum:** ✅ Tamamlandı (Döngü 279) — Ayarlar > Ana Ekran > Öneriler ve bildirimler altına varsayılan kapalı "Son Bildirim Alanlar" toggle'ı eklendi. Açıkken ana ekranda son 24 saatte bildirim alan uygulamalar satırı, All Apps çekmecesinde de aynı uygulamalar bölümü görünür. Yalnız sayı/paket zamanı kullanılır; içerik gösterilmez.

### [19] Genel arama sonuçlarına tür etiketi (uygulama/kişi/dosya/klasör)
**Durum:** ✅ Tamamlandı (D265, doğrulama) — `HomeAppSearchBar` sonuç listesi zaten türe göre gruplanmış ayrı bölümler halinde: "Uygulamalar" (Search ikon), "Klasörler" (Folder ikon), "Ayarlar" (Search ikon), "Kişiler" (Person ikon), "Dosyalar" (Description ikon) — her grup `HomeSearchGroupHeader(label, icon)` ile başlık+ikon alıyor (satır 969, 1019, 1062, 1102, 1259), çoklu grup olduğunda gösteriliyor. Satır bazlı ikon değil grup başlığı bazlı etiketleme — kullanıcı değerini karşılıyor, ek kod değişikliği gerekmedi.

### [20] Klasörler arası geçiş animasyonu iyileştirilsin (iPhone tarzı)
**Sorun/İstek:** Mevcut page-turn efekti yetersiz, iPhone'daki gibi akıcı bir geçiş isteniyor.
**Nasıl yapılmalı:** `HomeScreenFolderPager.kt` + `HomeScreenPageIndicator.kt` içindeki `HorizontalPager` transform mantığı incelenmeli — muhtemelen `graphicsLayer` ile basit alpha/scale efekti var. iOS-tarzı akıcı geçiş için `pagerSnapDistance`, `flingBehavior` ve `graphicsLayer { translationX, scaleX/Y, cameraDistance }` kombinasyonu (Compose "carousel" pattern'i) araştırılmalı — CLAUDE.md Araştırma Önceliği kuralı gereği yeni animasyon pattern'i için WebSearch zorunlu (daha önce yapılmamış işlem). UX/görsel karar olduğundan Fable model ile tasarım onayı önerilir.
**Puan:** KV=3 U=3 BR=3 EA=3 → Toplam=12
**Durum:** ✅ Tamamlandı (Döngü 281) — `HorizontalPager` tek sayfalık snap/fling davranışıyla sınırlandı; sayfa offset'ine bağlı alpha/scale/rotationY `graphicsLayer` efekti eklendi. Sayfa göstergesi noktaları da animasyonlu boyuta geçirildi. Görsel tablet smoke hâlâ release QA kapısında ayrıca doğrulanmalı.

---

## Hedef

Play Store yayini icin Production AAB v1.0.0 hazir.

Kalan ana kapilar:
- Play Console formlari ve beyanlari
- Release imza ve final AAB
- Magaza gorselleri
- Gercek cihaz QA

## Guncel Kalan Is Listesi ve Uygulama Plani (2026-07-13)

| Sira | Is | Plan | Durum |
|---|---|---|---|
| 1 | Play Console beyanlari | QUERY_ALL_PACKAGES, Data Safety, Content Rating ve Privacy Policy URL alanlari kod gercegine gore doldurulacak. | Bekliyor - kullanici/Play Console |
| 2 | Release imza | `scripts/create_release_keystore.ps1` ile kalici key uretilecek, `keystore.properties` git disinda saklanacak, final AAB temiz committen alinacak. | Bekliyor - kullanici aksiyonu |
| 3 | Magaza screenshot seti | Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore ekranlari kisisel veri olmadan cekilecek. D260: 5/9 cekildi (`docs/store_screenshots/` - home, arama, settings, bildirim raporu, onboarding); kalan: klasor detay, arama ayarlari, izinler, dashboard, ozellestirme, yedekleme, gorevler + light/dark varyantlar. | Kismen tamam - 5/9 |
| 4 | Gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26 ve OEM kategori smoke tek pakette kosulacak. AllApps double-tap Pixel6_API33 emülatörde 2026-07-13 crashsiz geçti; fiziksel cihazda genel QA parçası olarak tekrar edilebilir. | Bekliyor - gercek cihaz |
| 5 | `cycle.ps1` gercek tur | Temiz dal ve push hazirliginda bir kez uctan uca kosulup Telegram/commit/push kaniti alinacak. | Bekliyor - gercek tur |
| 6 | Wrapped Phase 2 dis dogrulama | UsageEvents oturum altyapisi API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylariyla cihazda kanitlanacak. | Bekliyor - dis dogrulama |
| 7 | Uzun vade backlog | Kendi sunucu API'si, Wear OS companion app ve widget ekran genisletme ayri sprintte ele alinacak. | Bekliyor |

---

## Kritik - Play Store ve Release Kapisi

| Gorev | Minimum cozum | Durum |
|---|---|---|
| QUERY_ALL_PACKAGES Play Store beyani | Launcher core functionality gerekcesiyle Play Console declaration doldurulacak. Metin ozeti: AppOrganizer tum yuklu uygulamalari organize etmek, app drawer/search gostermek ve uygulama baslatmak icin paket gorunurlugune ihtiyac duyar; paket/ad/kategori tercihleri cihazda kalir. | Bekliyor - dis aksiyon |
| Data Safety formu | Privacy policy, Firebase/Crashlytics/Analytics, optional contacts/files, NotificationListener, backup ve optional AI davranisi Play Console formuna kod gercegiyle uyumlu girilecek. Manifestte Accessibility servisi yok; servis geri eklenmedikce beyan edilmeyecek. | Bekliyor - dis aksiyon |
| Content rating | Play Console content rating anketi doldurulacak. | Bekliyor - dis aksiyon |
| Privacy Policy URL | GitHub Pages URL'i Play Console'a girilecek; policy dosyasi ve manifest URL'i ayni hikayeyi anlatmali. | Bekliyor - dis aksiyon |
| Release keystore | `scripts/create_release_keystore.ps1` hazir. Kullanici scripti calistirip kalici release key'i guvenli saklayacak; final AAB temiz committen imzalanacak. | Bekliyor - kullanici aksiyonu |
| Screenshot seti | Light/dark phone screenshot seti alinacak: Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore. Kisisel veri gorunmeyecek. | Bekliyor - cihaz/emulator |

---

## Kritik - Gercek Cihaz QA

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Android 14 NotificationListener testi | Notification access ac/kapa, event yazma, rapor gorunumu ve reboot/permission lifecycle gercek cihazda kanitlanacak. | Bekliyor - gercek cihaz |
| Play oncesi gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap, OEM kategori ve screenshot smoke tek pakette kosulacak. Accessibility servisi manifestte olmadigi icin bu paketten cikarildi. | Bekliyor - gercek cihaz |
| BLUR-4/API26 testi | Blur/fallback performansi API 26+ temsilci cihazlarda kontrol edilecek. | Bekliyor - gercek cihaz |
| Backup/restore kaniti | SAF export/import, Drive klasor secimi, missingPackages dialogu ve restore sonrasi ayar devamligi kanitlanacak. | Bekliyor - cihaz/hesap |
| AllApps double-tap testi | Pixel6_API33 emülatörde arama alanı odak + `app` sorgusu + hızlı çift dokunma koşturuldu; app focus korundu, temiz logcat `FATAL EXCEPTION=0`. | Tamamlandi - emulator |
| Uretici kategori testi | Samsung/Xiaomi/Google tarzinda farkli OEM app setlerinde kategori eslesmeleri kontrol edilecek. | Bekliyor - cihaz |

## Orta Oncelik - Build, Surec ve Token Maliyeti

| Gorev | Minimum cozum | Durum |
|---|---|---|
| `cycle.ps1` uctan uca test | Kod incelemesiyle dogrulandi: encoding tarama -> duplicate kontrol -> ritimli build -> git add+commit+push -> Telegram bildirimi sirasiyla calisan orchestrator. Gercek uctan uca calistirilmadi. | Bekliyor - gercek tur denenecek |

---

## Yuksek Puanli - Wrapped Phase 2 (Fable analizi, Dongu 230-232)

| Puan | Gorev | Durum |
|---|---|---|
| 15p | UsageEvents oturum altyapisi gercek cihaz/OEM dogrulamasi - API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylari fiziksel cihazda kanitlanacak | Bekliyor - dis dogrulama gerekli |

Diger Phase 2 adaylari (gizlilik analizi 14p, AI kocu 13p, hedef sistemi 13p, kilit sayaci 12p) uzun vadeli backlog olarak izlenecek.

---

## Dusuk Oncelik ve Uzun Vade

| Gorev | Alan | Durum |
|---|---|---|
| Kendi sunucu API'si | `packageName -> category` endpoint; DeepSeek fallback alternatifi | Bekliyor |
| Wear OS companion app | Uzun vade companion deneyimi | Bekliyor |
| Widget ekran genisletme | Launcher disi hizli gorunum | Bekliyor |
| Coklu cihaz senkronizasyonu (Firebase Auth+Firestore+E2EE, 9 faz) | v1.0 Play Store yayini SONRASI ele alinacak — 11p fizibilite puani, 3-6 ay efor, Blaze plan + Play beyan hikayesi yeniden yazimi gerektiriyor. Detay: asagidaki "Analiz — Coklu Cihaz Senkronizasyonu" bolumu. Kullanici karariyla (2026-07-13) uzun vadeye tasindi; ara adim olarak "Yedekle ve Yedekten Don" (Drive/SAF) guclendirilmesi tercih edildi. | Bekliyor - uzun vade |

---

## Analiz — Çoklu Cihaz Senkronizasyonu (Fable fizibilite analizi, Dongu 247)

> Hüseyin'in 9 fazlı "AppOrganizer Sync Cloud" önerisi (Firebase Auth + Firestore + Cloud Functions + E2EE + QR eşleştirme) gerçek kod tabanına karşı doğrulandı. KOD YAZILMADI — sadece analiz.

### Kısa Özet

Öneri mimari olarak sağlam (yıldız topolojisi, outbox pattern, tombstone, HMAC belge ID, field-level merge — hepsi endüstri standardı doğru kararlar). Kod tabanı varsayımlarının çoğu DOĞRU çıktı. Ancak: (a) toplam efor tek geliştirici için 3-6 ay, (b) Cloud Functions için Firebase Blaze (ücretli) plana geçiş ZORUNLU, (c) minSdk 26 ile Keystore ECDH imkânsız (API 31+ gerekir — Android 8-11'de yazılımsal anahtar fallback'i şart, "private key Keystore'dan hiç çıkmaz" garantisi kısmen bozulur), (d) mevcut Play Store konumlandırması ("paket/ad/kategori tercihleri cihazda kalır" — bu metin QUERY_ALL_PACKAGES beyan taslağında bile var) kökten değişir: Data Safety formu, privacy policy ve beyan hikayesi yeniden yazılmalı. E2EE olsa bile Play politikasına göre cihazdan çıkan veri "collected" olarak beyan edilir.

### Doğrulanan / Yanlışlanan Varsayımlar

| Varsayım | Sonuç |
|---|---|
| `AppInfo.packageName` primary key | ✅ Doğru (`AppInfo.kt:21-22`) |
| Kategoriler sabit string ID (`CAT_SOCIAL="social"` vb.) | ✅ Doğru — 32 kategori + 9 üretici kategorisi, sync için stabil anahtar (`Category.kt`) |
| Backup kullanım/bildirim verisi de taşıyor | ✅ Doğru — BackupManager v4 `usageCount`(ms)/`launchCount`/`lastUsedTimestamp`/`notificationCount` export ediyor; önerinin DEVICE-kapsamı ihlali tespiti isabetli (`BackupManager.kt:41-44`) |
| Firebase zaten entegre | ✅ Kısmen — `google-services.json` mevcut, BOM 33.7.0, Analytics+Crashlytics+FCM aktif ve null-guard'lı. Ama Auth/Firestore/Functions bağımlılığı YOK; eklemek kolay, işletmek değil |
| Dock/klasör özelleştirme PROFILE kapsamıyla örtüşür | ✅ Örtüşür AMA kritik uyumsuzluk: bu veriler Room'da değil SharedPreferences'ta (`DockPrefs`, `AppPrefs` map'leri). "Her aksiyon Room transaction içinde outbox'a yazılır" varsayımı bu veriler için ÇALIŞMAZ — ya prefs→Room taşıma ya da atomiklikten feragat gerekir |
| Room migration kolay | ✅ Şu an v16 (CLAUDE.md'deki v12 bayat), `schemaLocation` + şablon mevcut; 7 yeni sync tablosu additive v16→v17 ile eklenebilir |
| Kullanım/bildirim verisi zaten bulutta değil | ✅ Doğru — `WrappedSnapshotPrefs` sadece cihaz-yerel agregat; hiçbir kullanım verisi cihaz dışına çıkmıyor (JSON backup hariç) |
| Keystore ECDH P-256 | ❌ EKSİK — `PURPOSE_AGREE_KEY` API 31+; minSdk 26'da Android 8-11 için yazılımsal ECDH + Keystore AES-wrap fallback tasarlanmalı |

### Faz Puanlama (KV: Kullanıcı Değeri · U: Uygulanabilirlik · BR: Bağımlılık Riski, 5=düşük risk · EA: Etki Alanı)

| Faz | İçerik | KV | U | BR | EA | Toplam | Durum |
|---|---|---|---|---|---|---|---|
| F1 | Veri sözleşmesi + sync entity modelleri (Room v17: SyncOutbox/Conflict/Device/Profile/Tombstone/State/EntityMeta) | 1 | 4 | 4 | 2 | **11** | Beklet |
| F2 | Firebase Auth + Credential Manager + cihaz UUID kaydı (Blaze plan kapısı burada açılır) | 2 | 3 | 2 | 2 | **9** | Beklet |
| F3 | E2EE (workspace key + key envelope) + QR eşleştirme (Keystore ECDH API 31+ sorunu burada) | 2 | 2 | 2 | 3 | **9** | Beklet |
| F4 | Tek yönlü ilk sync (upload + read-only preview) | 3 | 3 | 2 | 3 | **11** | Beklet |
| F5 | Çift yönlü sync (outbox + SyncWorker + mutation + listener) — asıl kullanıcı değeri burada | 4 | 2 | 2 | 4 | **12** | Beklet |
| F6 | Conflict engine (öncelik zinciri + fieldVersions merge + kullanıcıya sorma UI) | 3 | 2 | 2 | 3 | **10** | Beklet |
| F7 | Profil sistemi (Ortak/Telefon/Tablet) | 3 | 3 | 3 | 3 | **12** | Beklet |
| F8 | Güvenlik sertleştirme (App Check, Play Integrity, Security Rules, rate limit) | 2 | 2 | 1 | 2 | **7** | Beklet |
| F9 | Üretim QA (fiziksel çoklu cihaz + OEM matrisi) — tek geliştiricide cihaz parkı yok | 3 | 1 | 1 | 2 | **7** | Beklet |
| **Genel** | **Tüm proje** | **4** | **2** | **1** | **4** | **11** | **ERTELE — v1.0 sonrası** |

### Fizibilite Notları

- **Süre:** 9 faz, tek geliştirici + Claude döngüleriyle gerçekçi tahmin 3-6 takvim ayı (Cloud Functions TS toolchain'i, Rules testleri ve çoklu cihaz QA dahil). Play Store v1.0 kapısındaki işlerin (beyanlar, keystore, QA) tamamı bundan kısa.
- **Maliyet:** Cloud Functions deploy = Blaze plan ZORUNLU (Spark'ta Functions deploy edilemez). Firestore free kotası (50k okuma/20k yazma/gün) erken dönem için yeter; entity-başına-belge + realtime listener modeli kullanıcı başına ilk sync ~500-700 okuma üretir, 5-10k aktif kullanıcıya kadar maliyet önemsiz, sonrası lineer büyür. Asıl maliyet para değil bakım karmaşıklığı.
- **Kurtarma kodu UX riski:** XXXX-XXXX-XXXX-XXXX kodunu kullanıcıların büyük kısmı kaybeder; "tüm cihazlar kaybedilirse veri gider" destek yükü yaratır. Sync edilen verinin çoğu (kategori kararları, özelleştirme) yeniden üretilebilir olduğundan tam E2EE yerine "Google hesabına bağlı, sunucu tarafı şifreli, kurtarma kodu opsiyonel" modeli düşünülmeli — ya da E2EE kalacaksa veri kaybı kabul edilebilir çünkü felaket senaryosunda kullanıcı sadece özelleştirmelerini kaybeder.
- **Konumlandırma gerilimi:** YÜKSEK. Mevcut Play beyan taslağı ve privacy policy "cihazda kalır" hikayesi anlatıyor; sync bunu "opsiyonel bulut, E2EE" hikayesine çevirir. Bu değişiklik Play Store İLK yayından önce yapılırsa beyan süreci sıfırdan karmaşıklaşır — yayın SONRASI ayrı release olarak eklenmesi süreç açısından da doğru.

### Tavsiye

**ERTELE + KÜÇÜLT.** v1.0 Play Store yayını tamamlanana kadar başlanmasın. Ara adım olarak çok daha küçük MVP: mevcut BackupManager v4 JSON'unu kullanıcının seçtiği Drive/SAF klasörüne otomatik yükleyen + ikinci cihazda "yedekten kur" akışı sunan "fakir adamın sync'i" (~1-2 döngü, sıfır sunucu, sıfır plan değişikliği, değerin ~%60-70'i). Bu MVP yapılmadan önce backup'taki DEVICE-kapsam sızıntısı (usage/notification alanları) opsiyonel hale getirilmeli — FİKİRLER.md'de zaten 11p kayıtlı ("Yedekte usage verisini dahil etme secenegi"). Tam sync'e ilerideki bir sprintte dönülürse fazlar F1→F4→F5 çekirdeği önce, F3 E2EE en sona alınmalı (ilk sürüm sunucu tarafı şifreleme ile çıkabilir).

### Uyarlanmış Faz Planı (ileride başlanırsa — gerçek dosya haritası)

1. **F0 (ÖN KOŞUL — öneride yok):** SharedPreferences→Room taşıma kararı: `DockPrefs`, `AppPrefs` klasör özelleştirme/gesture/tema map'leri için ya `sync_prefs_mirror` tablosu ya da prefs-listener tabanlı outbox köprüsü. Bu çözülmeden F5 outbox atomikliği kurulamaz.
2. **F1:** `domain/models/sync/` altına SyncOutboxEntity, SyncTombstoneEntity, SyncEntityMeta, SyncStateEntity, SyncDeviceEntity, SyncProfileEntity, SyncConflictEntity; `AppDatabase` v16→v17 (şablon: MIGRATION_15_16); DAO'lar + `schemas/17.json`.
3. **F2:** `firebase-auth` + `firebase-firestore` bağımlılıkları (BOM zaten var), Credential Manager sign-in, `SyncIdentityManager` (cihaz UUID, `AppPrefs` değil ayrı `sync_identity_prefs`); SettingsScreen'e "Cihazlar Arası Eşitleme (Deneysel)" toggle (CLAUDE.md Yeni Özellik=Ayarlar kuralı).
4. **F3:** `SyncCryptoManager` — API 31+ Keystore ECDH, API 26-30 yazılımsal ECDH + Keystore AES-GCM wrap fallback; QR üretim/okuma (`zxing` veya ML Kit — yeni bağımlılık, WebSearch şart); pairing UI.
5. **F4:** `SyncUploader` — Room→Firestore ilk yükleme (HMAC belge ID), ikinci cihazda read-only önizleme ekranı.
6. **F5:** `SyncWorker` (WorkManager, mevcut `BackupWorker` pattern'i), outbox draining, Firestore listener→Room applier; Cloud Functions repo'su (`functions/` TS): applySyncMutations, finalizePairing vb.
7. **F6:** `ConflictResolver` (öncelik zinciri; `classificationSource`/`isCategoryLocked` alanları D246'da eklendi — manuel karar tespiti için hazır altyapı MEVCUT, önerinin lehine) + çakışma soru UI'ı.
8. **F7:** Profil sistemi — `SyncProfileEntity` + Settings profil seçici; PROFILE kapsam alanları BackupManager v4 settings bloğuyla birebir örtüşüyor (hazır envanter).
9. **F8-F9:** App Check + Play Integrity + Security Rules emulator testleri; fiziksel cihaz matrisi (dış aksiyon — COZULEMEYEN_SORUNLAR adayı).

---

## Dis Aksiyon Kayitlari

Detayli engel kaydi icin:
- COZULEMEYEN_SORUNLAR.md -> CS-3, CS-5, CS-6, CS-7

Tamamlanan raporlar ve kapanislar:
- HISTORY.md -> Dongu 220 ve onceki donguler.
