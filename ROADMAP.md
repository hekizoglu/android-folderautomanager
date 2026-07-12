# ROADMAP.md - AppOrganizer Aktif Yol Haritasi

> Son guncelleme: 2026-07-09.
> Bu dosya aktif yapilacaklar icin tek kaynak olarak kullanilir.
> Tamamlanan isler HISTORY.md'ye, yerelde cozulmeyen/dis aksiyon gerektirenler COZULEMEYEN_SORUNLAR.md'ye tasinir.

---

## Hedef

Play Store yayini icin Production AAB v1.0.0 hazir.

Kalan ana kapilar:
- Play Console formlari ve beyanlari
- Release imza ve final AAB
- Magaza gorselleri
- Gercek cihaz QA
- Notification analytics ve build sureci icin kalan dar testler

---

## Kritik - Play Store ve Release Kapisi

| Gorev | Minimum cozum | Durum |
|---|---|---|
| QUERY_ALL_PACKAGES Play Store beyani | Launcher core functionality gerekcesiyle Play Console declaration doldurulacak. Metin ozeti: AppOrganizer tum yuklu uygulamalari organize etmek, app drawer/search gostermek ve uygulama baslatmak icin paket gorunurlugune ihtiyac duyar; paket/ad/kategori tercihleri cihazda kalir. | Bekliyor - dis aksiyon |
| Data Safety formu | Privacy policy, Firebase/Crashlytics/Analytics, optional contacts/files, NotificationListener, Accessibility Service, backup ve optional AI davranisi Play Console formuna kod gercegiyle uyumlu girilecek. | Bekliyor - dis aksiyon |
| Content rating | Play Console content rating anketi doldurulacak. | Bekliyor - dis aksiyon |
| Privacy Policy URL | GitHub Pages URL'i Play Console'a girilecek; policy dosyasi ve manifest URL'i ayni hikayeyi anlatmali. | Bekliyor - dis aksiyon |
| Accessibility Service declaration | Drag/drop icin tanimli servisin ne yaptigi/yapmadigi Play Console ve uygulama ici prominent disclosure ile uyumlu anlatilacak. | Bekliyor - dis aksiyon |
| Release keystore | `scripts/create_release_keystore.ps1` hazir. Kullanici scripti calistirip kalici release key'i guvenli saklayacak; final AAB temiz committen imzalanacak. | Bekliyor - kullanici aksiyonu |
| Screenshot seti | Light/dark phone screenshot seti alinacak: Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore. Kisisel veri gorunmeyecek. | Bekliyor - cihaz/emulator |

---

## Kritik - Gercek Cihaz QA

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Android 14 NotificationListener testi | Notification access ac/kapa, event yazma, rapor gorunumu ve reboot/permission lifecycle gercek cihazda kanitlanacak. | Bekliyor - gercek cihaz |
| Play oncesi gercek cihaz QA paketi | NotificationListener, Accessibility Service, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap, OEM kategori ve screenshot smoke tek pakette kosulacak. | Bekliyor - gercek cihaz |
| BLUR-4/API26 testi | Blur/fallback performansi API 26+ temsilci cihazlarda kontrol edilecek. | Bekliyor - gercek cihaz |
| Backup/restore kaniti | SAF export/import, Drive klasor secimi, missingPackages dialogu ve restore sonrasi ayar devamligi kanitlanacak. | Bekliyor - cihaz/hesap |
| AllApps double-tap testi | Emulator veya fiziksel cihazda cift tiklama/arama gesture cakismasi tekrarlanacak. | Bekliyor - cihaz |
| Uretici kategori testi | Samsung/Xiaomi/Google tarzinda farkli OEM app setlerinde kategori eslesmeleri kontrol edilecek. | Bekliyor - cihaz |

---

## Orta Oncelik - UX ve Urun

| Gorev | Minimum cozum | Durum |
|---|---|---|

---

## Orta Oncelik - Akilli Bildirim Analiz Sistemi

Mevcut temel:
- `AppNotificationListenerService` bildirim olaylarini yakaliyor.
- `notification_events` yalnizca `packageName` + `postedAt` tutuyor.
- `NotificationAnalyzer` cok konusan/rahatsiz eden/dikkat dagitan sinyalleri uretebiliyor.
- `SmartInsightWorker` unique periodic work olarak planlaniyor.

| Gorev | Minimum cozum | Durum |
|---|---|---|
| POST_NOTIFICATIONS yokken sessiz davranis | `SmartInsightWorker` `CoroutineWorker`, `work-testing` bagimliligi projede yok — unit testle kanitlanamadi. Kod incelemesi: `notify()` sadece null-check ile korunuyor, izin kontrolu yok; `SecurityException` disarida `runCatching`e dusup `Result.retry()` oluyor (cokme yok). Gercek cihazda dogrulanmali. | Bekliyor - gercek cihaz |
| 30 gun temizlik — uzun sureli persist kaniti | Tetikleme mantigi (`onListenerConnected()` -> `deleteOlderThan(now-30gun)`) unit testle kanitlandi (`AppNotificationListenerServiceTest.kt`, Dongu 221). Gercek 30+ gunluk veriyle uzun sureli persist/silme davranisi hala kanitlanmadi (Room instrumented/Robolectric gerekir, projede yok). | Bekliyor - gercek cihaz |

---

## Orta Oncelik - Build, Surec ve Token Maliyeti

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Configuration cache guard benchmark | Configuration cache sadece benchmark/CLI profilinde denenecek; kalici `gradle.properties` ayari icin uyumluluk kaniti istenecek. Bu ortamda `compileDebugKotlin` build kilidi nedeniyle olculemedi (bkz. CS-3); referans olarak 2026-07-01 tarihli profile verisi (rerun 97.8s, cache'li compileDebugKotlin 2.4s) kullanildi, karar korundu (KAPT+Hilt uyumsuzlugu nedeniyle acilmadi). | Bekliyor - build kilidi cozulunce tekrar denenecek |
| Build Analyzer / Gradle profile rutini | Bu oturumda build kilidi nedeniyle tam kosum yapilamadi; 2026-07-01 profile verisi referans alindi. | Bekliyor - build kilidi cozulunce tekrar denenecek |
| `cycle.ps1` uctan uca test | Kod incelemesiyle dogrulandi: encoding tarama -> duplicate kontrol -> ritimli build -> git add+commit+push -> Telegram bildirimi sirasiyla calisan orchestrator. Gercek uctan uca calistirilmadi. | Bekliyor - gercek tur denenecek |

---

## Yuksek Puanli - Wrapped Phase 2 (Fable analizi, Dongu 230-232)

| Puan | Gorev | Durum |
|---|---|---|
| 15p | UsageEvents oturum altyapisi gercek cihaz/OEM dogrulamasi - yerel agregator, AppOps izin kontrolu, Wrapped 7 gun entegrasyonu ve 11 unit test Dongu 232'de tamamlandi; API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylari fiziksel cihazda kanitlanacak | Bekliyor - dis dogrulama gerekli |

Wrapped MVP (skor, kisilik, rozetler, haftalik karsilastirma) Dongu 230'da; UsageEvents yerel oturum agregatoru Dongu 232'de tamamlandi. Diger Phase 2 adaylari (gizlilik analizi 14p, AI kocu 13p, hedef sistemi 13p, kilit sayaci 12p) FİKİRLER.md'de.

---

## Yuksek Puanli - Kullanici Geri Bildirimi (2026-07-12, kod bulgulari agent ile dogrulandi)

> Kullanici testi sonrasi 13 madde + 4 iyilestirme onerisi onaylandi. Her madde koda oturtuldu (dosya:satir).
> Onerilen calisma sirasi: A (temel veri) -> D (cokme) -> B/C -> E.

### Paket A - Kullanim metrigi (KOK SORUN)

| Puan | Gorev | Kok neden / dosya:satir | Durum |
|---|---|---|---|
| 17p | usageCount alanini ikiye ayir: launchCount (adet) + usageTimeMs (sure) | `AppInfo.usageCount` (AppInfo.kt:40) hem `+1` (LauncherViewModel.kt:363) hem `= totalTimeInForeground` ms (LauncherViewModel.kt:764) olarak yaziliyor; sync adet degerini ms ile eziyor -> "milyon adet" bug. Room migration + tum okuma noktalari (InsightEngine, WrappedEngine, WrappedReportScreen, NiagaraComponents, AppContextMenu) guncellenecek. | Bekliyor |
| 17p | S1 hotfix: Paket A bitene kadar raporlarda degeri sure olarak goster | Gecici kilif - `formatUsageTime(usageCount)` ile milyon gizlenir. Hizli, tek noktada. | Bekliyor |
| 15p | Raporda "saate gore / adete gore" toggle | Su an hicbir toggle yok; metrik secimi implicit ve tutarsiz. launchCount/usageTimeMs ayrimi sonrasi eklenecek. | Bekliyor |
| 15p | Oneri bolumu -> "Bu saatte en cok kullandiklarin" | Su an genel oneri (AppSuggestionsRow, HomeScreenComponents.kt:356). Saat dilimi bazli kullanim kovalari + baslik degisimi. UsageSessionAggregator (launchCount event bazli) beslenecek. Paket A verisine bagimli. | Bekliyor |

### Paket D - Performans ve cokme (KRITIK)

| Puan | Gorev | Kok neden / dosya:satir | Durum |
|---|---|---|---|
| 16p | Cold start: yuklu paket imzasi (adet+hash) cache'le, degismemisse PM taramasini atla | Kategorizasyon zaten cache'li (loadAppsIfEmpty delta, LauncherViewModel.kt:275-304); ama `getInstalledApps()` (:279) delta bos olsa bile her acilista TUM paketleri PM'den tariyor + init'te 4+ SharingStarted.Eagerly StateFlow (:155,168,602,747,756) ayni anda tetikleniyor -> cokme/yavaslik. | Bekliyor |

### Paket B - Oneri ve bilgilendirme UX

| Puan | Gorev | Kok neden / dosya:satir | Durum |
|---|---|---|---|
| 17p | Oneri ikon/etiket uyumsuzlugu fix (Instagram logo + Akbank yazi) | `AppSuggestionsRow` `Row.forEach` `key` YOK (HomeScreenComponents.kt:399); `produceState` key degisince eski ikonu tutuyor, etiket aninda guncelleniyor. `key` ekle + `AppIconView.kt:103` cache key'ine lastUpdatedTime ekle. | Bekliyor |
| 16p | Rapor/Dashboard satirlarini tiklanabilir yap | `TopAppRow`, `UsageRow`, `CategoryBar` onClick YOK (AppOrganizerDashboardScreen.kt, UsageReportScreen.kt). Satir -> uygulama detayi / ilgili rapor. | Bekliyor |
| 15p | Dijital yasam skorunu ana ekranda goster (+ S3 trend yonu) | `DigitalLifeScore`/`computeScore` var (WrappedEngine.kt:55,138) ama sadece Wrapped ekraninda. Ticker/oneride ara sira goster; anti-repeat zaten var (TickerComposer). S3: skorla birlikte haftalik trend (yukari/asagi). | Bekliyor |
| 15p | Ana ekran bilgilendirme deep-link denetimi | Ticker + AssistantInsightRow zaten onClick ile uygulamaya/klasore gidiyor (HomeScreen.kt:607-622, 660); packageName bos kalan kartlar dashboard'a dusuyor -> hedefi bos kartlari denetle/doldur. | Bekliyor |

### Paket C - Yeni uygulama ve bildirim netligi

| Puan | Gorev | Kok neden / dosya:satir | Durum |
|---|---|---|---|
| 17p | Yeni uygulama kategori bildirimi (+ S2 tek dokunusla duzelt) | onPackageAdded kategori atiyor ama bildirim YOK (PackageChangeReceiver.kt:42-58, LauncherViewModel.kt:470-483). "X -> Y kategorisine eklendi", tik -> klasor ac. S2: bildirimde yanlis kategoriyi tek dokunusla degistir (categoryReclassified ogrenme sinyali zaten var). | Bekliyor |
| 15p | Bildirim icerigi netlestir | Metin yoksa jenerik "1 yeni bildirim" (FolderTile.kt:288-294); ayrica en YENI degil en cok bildirimli uygulamayi gosteriyor (:280-284). En yeni bildirimi goster + fallback metni duzelt. | Bekliyor |

### Paket E - Izin ve arama bari

| Puan | Gorev | Kok neden / dosya:satir | Durum |
|---|---|---|---|
| 16p | "Tam Performans / Gerekli Izinler" ekrani (+ S4 somut ornekler) | SettingsPermissionsCard var (SettingsPermissionsSection.kt:75-170, eksik izinleri gosterir). Tam ekrana genislet: her izin + neden gerekli + S4 "kapaliyken ne olmaz" (orn. kullanim erisimi kapali -> raporlar bos). | Bekliyor |
| 15p | Fihrist (A-Z) titresim bug: cok fazla titriyor | `AllAppsDrawer.kt:737-762` sidebar scrubber onDrag her harf degisiminde `LongPress` (guclu) tetikliyor -> bir kaydirmada onlarca guclu buzz. Hafif tick (SegmentTick/TextHandleMove) + throttle. | Bekliyor |
| 15p | Arama bari focus gorseli | Focus'ta gorsel degisim YOK; alpha yalniz drag'e tepki (HomeScreenComponents.kt:863). isFocused -> renk/border degisimi. | Bekliyor |
| 14p | Arama barinda izin ipucu + tekrar iste | Eksik izinlerde arama alaninda ipucu goster; kullanici vermezse Paket E ekranina yonlendir. SettingsPermissionsCard ON_RESUME re-check zaten var (:85-96). | Bekliyor |

---

## Dusuk Oncelik ve Uzun Vade

| Gorev | Alan | Durum |
|---|---|---|
| Kendi sunucu API'si | `packageName -> category` endpoint; DeepSeek fallback alternatifi | Bekliyor |
| Wear OS companion app | Uzun vade companion deneyimi | Bekliyor |
| Widget ekran genisletme | Launcher disi hizli gorunum | Bekliyor |

---

## Dis Aksiyon Kayitlari

Detayli engel kaydi icin:
- COZULEMEYEN_SORUNLAR.md -> CS-3, CS-5, CS-6, CS-7

Tamamlanan raporlar ve kapanislar:
- HISTORY.md -> Dongu 220 ve onceki donguler.
