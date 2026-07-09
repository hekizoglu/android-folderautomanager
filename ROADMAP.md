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
| U10: Acik kaynak referans launcher ile Home revizyonu | `docs/internal/home_revizyon_karar_listesi.md` ile Lawnchair/Kvaesitso referanslari, kalacak/gidecek/yeniden gruplancak karar listesi ve Home revizyonu uygulama parcalari cikarildi. | Tamamlandi - kod okuma/dokuman |
| Setup friction azaltma | Onboarding zaten launcher secimi ve "Simdi Degil" sundugu icin `MainActivity.kt` icindeki tekrar otomatik launcher picker tetiklemesi kaldirildi; kullanici Settings > Launcher uzerinden manuel devam eder. | Tamamlandi - statik dogrulandi |
| Search-first Home modu | Mevcut `KEY_FOCUS_MODE` search-first davranisa genisletildi: Home arama cubugu korunuyor, klasor pager gizleniyor, dock/favoriler/oneriler/son kullanilanlar one cikiyor; kucuk ekranda ikincil satirlar bu modda saklanmiyor. | Tamamlandi - statik dogrulandi |
| Home onerileri tekrar azaltma | Home ust satirlarinda oncelik sirasi Dock > Favoriler > Oneriler > Son Kullanilanlar olacak sekilde `HomeFavoritesSection.kt` filtrelendi; `HomeScreen.kt` aktif contextual dock paketlerini section'a geciyor. | Tamamlandi - statik dogrulandi |
| Settings Home bilgi mimarisi | `SettingsHomeScreenSection.kt` icindeki Home ayarlari Arama / Oneriler ve bildirimler / Temel davranislar / Gorsel alt basliklarina ayrildi; davranis degismeden taranabilirlik artirildi. | Tamamlandi - statik dogrulandi |
| Settings hiyerarsi smoke | Settings alt route'lari emulator/cihazda gezilip navigation kopuklugu olmadigi kanitlanacak. | Bekliyor |
| Search/launcher regression smoke | Search bar TOP/BOTTOM, kaynak toggle'lari, AllApps gruplama, contacts/files izin red fallback akislari tek smoke senaryoda kosulacak. | Bekliyor |

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

**Tamamlanan (Dongu 221, unit test ile kanitlandi):**
- Analiz kapaliyken event yazmama — `AppNotificationListenerServiceTest.kt`.
- NotificationAnalyzer cok konusan/gece+burst rahatsiz eden/dikkat dagitan/trend senaryolari — `NotificationAnalyzerTest.kt` (12 test).

**Tamamlanan (Dongu 224 — UX iyilestirmesi):**
- NotificationReport UI state ayrimi COZULDU: `NotificationReportUiState` sealed interface eklendi (Loading / PermissionMissing / AnalyticsDisabled / CollectingData / Ready). "Analiz kapali", "izin yok" ve "veri henuz yok" artik ayri tam-ekran durumlar; her biri aciklama + eylem butonu iceriyor ("Analizi Ac" tek dokunusla toggle'i acar, ayara gitmeye gerek yok). Veri varken sorunlar banner olarak gosterilir. Ekran ON_RESUME'da yenilenir (izin verip donunce takili kalmaz). "Bildirim Analizi" toggle'i Ana Ekran Ayarlari'ndan Ayarlar > Bildirimler'e tasindi ve yanina "Bildirim Raporu" kisayolu eklendi. Rapor ekrani metinleri strings.xml'e tasindi (TR+EN). State eslemesi `NotificationReportUiStateTest.kt` (9 test) ile kanitlandi.

---

## Orta Oncelik - Build, Surec ve Token Maliyeti

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Configuration cache guard benchmark | Configuration cache sadece benchmark/CLI profilinde denenecek; kalici `gradle.properties` ayari icin uyumluluk kaniti istenecek. Bu ortamda `compileDebugKotlin` build kilidi nedeniyle olculemedi (bkz. CS-3); referans olarak 2026-07-01 tarihli profile verisi (rerun 97.8s, cache'li compileDebugKotlin 2.4s) kullanildi, karar korundu (KAPT+Hilt uyumsuzlugu nedeniyle acilmadi). | Bekliyor - build kilidi cozulunce tekrar denenecek |
| `--no-watch-fs` A/B benchmark | `org.gradle.vfs.watch=false` zaten `gradle.properties`'te kalici acik (no-watch-fs esdegeri). | Tamamlandi |
| Build Analyzer / Gradle profile rutini | Bu oturumda build kilidi nedeniyle tam kosum yapilamadi; 2026-07-01 profile verisi referans alindi. | Bekliyor - build kilidi cozulunce tekrar denenecek |
| Kotlin build reports | `kotlin.build.report.output=file` zaten `gradle.properties`'te kalici acik. | Tamamlandi |
| `cycle.ps1` uctan uca test | Kod incelemesiyle dogrulandi: encoding tarama -> duplicate kontrol -> ritimli build -> git add+commit+push -> Telegram bildirimi sirasiyla calisan orchestrator. Gercek uctan uca calistirilmadi. | Bekliyor - gercek tur denenecek |

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
