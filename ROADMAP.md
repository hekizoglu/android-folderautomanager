# ROADMAP.md - AppOrganizer Aktif Yol Haritasi

> Son guncelleme: 2026-07-12.
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
| POST_NOTIFICATIONS yokken sessiz davranis | 2026-07-12: `SmartInsightWorker` bildirimler kapaliyken `NotificationManagerCompat.areNotificationsEnabled()` ile erken `Result.success()` donuyor; `notify()` de yeniden korunuyor. Unit tarafta scheduler helper testlendi, cihazda runtime izin/revoke akisi hala dogrulanmali. | Kismen tamam - gercek cihaz dogrulamasi bekliyor |
| 30 gun temizlik - uzun sureli persist kaniti | Tetikleme mantigi (`onListenerConnected()` -> `deleteOlderThan(now-30gun)`) unit testle kanitlandi (`AppNotificationListenerServiceTest.kt`, Dongu 221). Gercek 30+ gunluk veriyle uzun sureli persist/silme davranisi hala kanitlanmadi (Room instrumented/Robolectric gerekir, projede yok). | Bekliyor - gercek cihaz |

---

## Orta Oncelik - Build, Surec ve Token Maliyeti

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Configuration cache guard benchmark | Configuration cache sadece benchmark/CLI profilinde denenecek; kalici `gradle.properties` ayari icin uyumluluk kaniti istenecek. Bu ortamda `compileDebugKotlin` build kilidi nedeniyle olculemedi (bkz. CS-3); referans olarak 2026-07-01 tarihli profile verisi (rerun 97.8s, cache'li compileDebugKotlin 2.4s) kullanildi, karar korundu (KAPT+Hilt uyumsuzlugu nedeniyle acilmadi). | Bekliyor - build kilidi cozulunce tekrar denenecek |
| Build Analyzer / Gradle profile rutini | Bu oturumda build kilidi nedeniyle tam kosum yapilamadi; 2026-07-01 profile verisi referans alindi. | Bekliyor - build kilidi cozulunce tekrar denenecek |
| `cycle.ps1` uctan uca test | Kod incelemesiyle dogrulandi: encoding tarama -> duplicate kontrol -> ritimli build -> git add+commit+push -> Telegram bildirimi sirasiyla calisan orchestrator. Gercek uctan uca calistirilmadi. | Bekliyor - gercek tur denenecek |
| Build / surec / token maliyeti tek rehberi | 2026-07-12: komutlar, mutex davranisi, benchmark akisi ve harici fiyat referanslari `docs/internal/build_process_token_cost.md` altinda toplandi. | Tamamlandi |

---

## Yuksek Puanli - Wrapped Phase 2 (Fable analizi, Dongu 230-232)

| Puan | Gorev | Durum |
|---|---|---|
| 15p | UsageEvents oturum altyapisi gercek cihaz/OEM dogrulamasi - yerel agregator, AppOps izin kontrolu, Wrapped 7 gun entegrasyonu ve 11 unit test Dongu 232'de tamamlandi; API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylari fiziksel cihazda kanitlanacak | Bekliyor - dis dogrulama gerekli |

Wrapped MVP (skor, kisilik, rozetler, haftalik karsilastirma) Dongu 230'da; UsageEvents yerel oturum agregatoru Dongu 232'de tamamlandi. Diger Phase 2 adaylari (gizlilik analizi 14p, AI kocu 13p, hedef sistemi 13p, kilit sayaci 12p) asagida.

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
