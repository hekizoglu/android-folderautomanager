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

## Guncel Kalan Is Listesi ve Uygulama Plani (2026-07-13)

| Sira | Is | Plan | Durum |
|---|---|---|---|
| 1 | Yerel Play/gizlilik sertlestirme | Route whitelist, release log guard, notificationText backup dislama mevcut. 2026-07-13'te telefon/SMS intent URI encode edildi ve privacy policy'deki artik olmayan Accessibility izni kaldirildi. | Tamamlandi - yerel |
| 2 | Play Console beyanlari | QUERY_ALL_PACKAGES, Data Safety, Content Rating ve Privacy Policy URL alanlari kod gercegine gore doldurulacak. | Bekliyor - kullanici/Play Console |
| 3 | Release imza | `scripts/create_release_keystore.ps1` ile kalici key uretilecek, `keystore.properties` git disinda saklanacak, final AAB temiz committen alinacak. | Bekliyor - kullanici aksiyonu |
| 4 | Magaza screenshot seti | Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore ekranlari kisisel veri olmadan cekilecek. | Bekliyor - cihaz/emulator |
| 5 | Gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap ve OEM kategori smoke tek pakette kosulacak. | Bekliyor - gercek cihaz |
| 6 | Notification analytics runtime kaniti | POST_NOTIFICATIONS revoke, NotificationListener ac/kapa, reboot ve 30+ gun temizlik senaryolari cihazda kanitlanacak. | Bekliyor - gercek cihaz |
| 7 | Build sureci benchmark | CS-3 build kilidi cozulunce configuration cache guard ve Gradle profile rutini tekrar olculecek. | Bekliyor - build kilidi |
| 8 | `cycle.ps1` gercek tur | Temiz dal ve push hazirliginda bir kez uctan uca kosulup Telegram/commit/push kaniti alinacak. | Bekliyor - gercek tur |
| 9 | Wrapped Phase 2 dis dogrulama | UsageEvents oturum altyapisi API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylariyla cihazda kanitlanacak. | Bekliyor - dis dogrulama |
| 10 | Uzun vade backlog | Kendi sunucu API'si, Wear OS companion app ve widget ekran genisletme ayri sprintte ele alinacak. | Bekliyor |

---

## Kritik - Play Store ve Release Kapisi

| Gorev | Minimum cozum | Durum |
|---|---|---|
| QUERY_ALL_PACKAGES Play Store beyani | Launcher core functionality gerekcesiyle Play Console declaration doldurulacak. Metin ozeti: AppOrganizer tum yuklu uygulamalari organize etmek, app drawer/search gostermek ve uygulama baslatmak icin paket gorunurlugune ihtiyac duyar; paket/ad/kategori tercihleri cihazda kalir. | Bekliyor - dis aksiyon |
| Data Safety formu | Privacy policy, Firebase/Crashlytics/Analytics, optional contacts/files, NotificationListener, Accessibility Service, backup ve optional AI davranisi Play Console formuna kod gercegiyle uyumlu girilecek. | Bekliyor - dis aksiyon |
| Content rating | Play Console content rating anketi doldurulacak. | Bekliyor - dis aksiyon |
| Privacy Policy URL | GitHub Pages URL'i Play Console'a girilecek; policy dosyasi ve manifest URL'i ayni hikayeyi anlatmali. | Bekliyor - dis aksiyon |
| Accessibility Service declaration | 2026-07-13 kontrolunde manifestte Accessibility servisi bulunmuyor; privacy policy'deki eski izin metni kaldirildi. Servis geri eklenmedikce Play Console'da Accessibility beyani verilmemeli. | Tamamlandi - yerel uyum |
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
| Dock: gercek Android varsayilanlari + klasor destegi + ilk kurulumda Sosyal Medya (14p, arastirma tamamlandi Dongu 243) | Detay asagida. | Bekliyor - onay/uygulama |

### Dock — Gercek Varsayilanlar + Klasor Destegi — Arastirma (Dongu 243)

**Mevcut durum:** `DockPrefs.kt` dock'u `List<String>` (paket adi) olarak tutuyor, ilk kurulumda `DEFAULT_SLOTS` adinda 4 sabit aday listesinden (Dialer/Mesajlasma/Kamera/Tarayici) cihazda YUKLU OLANI seciyor — bu "Android'in gercek varsayilani" DEGIL, sadece bir tahmin listesi. Klasor dock'a hic eklenemiyor (sadece tek uygulama paketleri).

**"Android'de varsayilan ne geliyorsa o olsun" nasil yapilir:**
- Telefon/SMS icin: `RoleManager.getRoleHolders(RoleManager.ROLE_DIALER)` / `ROLE_SMS` (API 29+) — kullanicinin GERCEKTEN sectigi varsayilan uygulamayi dondurur. Proje zaten `LauncherSetupScreen.kt`'de RoleManager + ROLE_HOME pattern'ini kullaniyor, ayni pattern tekrarlanir.
- Tarayici icin: `PackageManager.resolveActivity(Intent(ACTION_VIEW, Uri.parse("http://")), MATCH_DEFAULT_ONLY)` — sistemin varsayilan tarayicisini dondurur (RoleManager'da browser rolu yok).
- Kamera icin: Android'de resmi bir "varsayilan kamera" rolu/API'si YOK — mevcut `DEFAULT_SLOTS` tahmin listesi (GoogleCamera/camera2/camera) korunmali, fallback olarak kalir.
- API 29 altinda (minSdk 26!) RoleManager rolleri yok — mevcut tahmin-listesi fallback zorunlu kaliyor, cihaz API seviyesine gore iki yollu (RoleManager varsa kullan, yoksa DEFAULT_SLOTS) mantik gerekir.

**Dock'a klasor eklenebilmesi icin:**
- `DockPrefs` veri modeli `List<String>` (sadece paket) yerine etiketli bir yapiya gecmeli, or: `"app:com.foo.bar"` / `"folder:social"` string prefix'i (basit, migration kolay) VEYA sealed class `DockItem` (App/Folder) + JSON serialization.
- Basit string-prefix yaklasimi mevcut virgullu format ile geriye donuk uyumlu kalir (eski kayitlarda prefix yoksa "app:" varsayilir) — `BackupManager.kt`'nin dock export/import'u da bu formata gore guncellenmeli.
- `DockEditSheet.kt`'ye "Klasor Ekle" sekmesi/secenegi eklenmeli (mevcut sadece uygulama listesi gosteriyor).
- Dock render tarafinda (`HomeScreenComponents.kt` icindeki `PixelDock`/`DockIcon`) klasor tipi icin `FolderTile`'daki gibi mini-ikon grid + tikla-ac (FolderScreen) davranisi eklenmeli.

**Ilk kurulumda Sosyal Medya klasorunu dock'a getirme:**
- Onboarding tamamlandiktan / ilk `classifyApps` taramasindan sonra (`LauncherViewModel` veya `OnboardingScreen.kt` bitis noktasi), `folders` icinde `categoryId == Category.CAT_SOCIAL` olan klasor bulunur; apps.isNotEmpty() ise ve dock'ta bos slot varsa `DockPrefs`'e `"folder:social"` olarak eklenir (yukaridaki veri modeli degisikligine bagli).
- Kullanici sonradan dock'tan cikarabilir (mevcut `removeFromDock` zaten calisir, sadece veri tipini anlamasi gerekir).

**Etkilenen dosyalar:** `DockPrefs.kt`, `DockEditSheet.kt`, `HomeScreenComponents.kt` (PixelDock/DockIcon), `BackupManager.kt` (dock export/import formati), `OnboardingScreen.kt` veya `LauncherViewModel.kt` (ilk kurulum hook'u), `LauncherSetupScreen.kt` (RoleManager pattern referansi).

**Puan:** KV:4 · U:3 · BR:3 · EA:4 = **14p** — veri modeli degisikligi mevcut kullanicilarin kayitli dock'unu bozmamali (migration + BackupManager guncellemesi sart), CLAUDE.md kurali geregi zorluk 6-7/10 sayilir, uygulamadan once kullanici onayi onerilir.

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
