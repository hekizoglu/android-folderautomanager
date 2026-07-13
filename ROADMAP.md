# ROADMAP.md - AppOrganizer Aktif Yol Haritasi

> Son guncelleme: 2026-07-13.
> Bu dosya aktif yapilacaklar icin tek kaynak olarak kullanilir.
> Tamamlanan isler HISTORY.md'ye, yerelde cozulmeyen/dis aksiyon gerektirenler COZULEMEYEN_SORUNLAR.md'ye tasinir.

---

## ⭐ Yüksek Puanlı - Pulse Clock / Dijital Nabız (D244 devamı)

Döngü 244'te previousScore kritik bug'ı, tek skor motoru (DigitalPulseEngine V2, sosyal/oyun
otomatik cezası kaldırıldı) ve Pulse Clock widget temel görünümü (Minimal/Pulse/Glass) teslim
edildi. Aşağıdakiler kapsam nedeniyle BEKLİYOR — sessizce atlanmadı, HISTORY.md Döngü 244'te de not düşüldü:

| Puan | Görev | Durum |
|---|---|---|
| 16 | `ReportsCenterScreen` üst özet kartı: toplam skor + confidence + en güçlü/zayıf alt skor + tek öneri, altında 5 alt skor progress + detay giriş kartları | Bekliyor |
| 14 | `WrappedReportScreen` madde sırası tam speke göre revize (skor→alt skor→en önemli içgörü→profil→istatistik→bildirim özeti→rozet→değişim→detay) | Bekliyor |
| 12 | Rozet kriterlerini "anlamlı hale getirme" — Bildirim Terbiyecisi, Sessiz Gece, Hedef Takipçisi gibi yeni/iyileştirilmiş rozetler; izin yoksa "erişimle ölçülür" açıklaması | Bekliyor |
| 10 | Canvas grafikleri: haftalık kullanım trendi (7 bar), bildirim trendi (7 sparkline, gece işaretli), kategori dağılımı (ilk 5 yatay bar) — harici kütüphane yok | Bekliyor |
| 8 | Glass saat stili görsel ayrımı (Pulse'tan daha belirgin cam/gradient) güçlendirme | Bekliyor |
| — | Emülatörde manuel doğrulama: Pulse Clock 3 stil, skor/içgörü toggle'ları, uzun basma → yönetim ekranı, kompakt ekranda grid kaybolmuyor | Bekliyor - cihaz/emülatör |
| 9 | Skor halkasının altına (mini açıklama "Denge" etiketinin yanına/altına) ekran kullanım süresi de yazılsın — örn. "Denge · 3sa 12dk" gibi günlük/haftalık toplam ekran süresi. `UsageStatsHelper` zaten süre verisini üretiyor, `PulseClockViewModel.PulseClockUiState`'e alan eklenip `PulseScoreRing`'in altına küçük bir satır olarak basılabilir. Kullanıcı talebi — kod yazılmadı, sadece backlog. | Bekliyor |

---

## Yüksek Puanlı - Klasör Zekâsı ve Akıllı Düzenleme

Amaç: AppOrganizer'ın ana vaadini güçlendirmek; uygulamaları açıklanabilir ve güven puanlı kararlarla doğru klasörlere yerleştirmek, emin olunmayanları kullanıcıya kontrol ettirmek ve kullanıcı düzeltmelerini kalıcı öğrenmek.

| Alt iş | Durum |
|---|---|
| Classification Decision V2 | Tamamlandı - yerel çekirdek: `ClassificationDecision`, source/confidence/reason/review metadata, kullanıcı kararı önceliği, remote catalog bağlantısı, legacy string API uyumu |
| Kalıcı Kullanıcı Kararları | Tamamlandı - Room `apps` metadata alanları ve v15->v16 migration eklendi; manuel repository güncellemeleri `USER_CORRECTED` + lock yazıyor; eski `KEY_MANUAL_CAT_OVERRIDES` ilk açılışta Room metadata'ya taşınıyor |
| Kontrol Bekleyenler | Tamamlandı - `classification_review` route'u, ViewModel queue, confirm/correct/7 gün skip akışı ve Ayarlar > Uygulamalar girişi eklendi |
| Onboarding Düzen Önizlemesi | Tamamlandı - kurulumda uygulama/klasör/kategorili/kontrol bekleyen sayaçları gösteriliyor |
| Klasör İçi Akıllı Sıralama | Tamamlandı - `SMART` sıralama modu `KEY_FOLDER_SORT_MODE` ile folder render'a bağlandı; kullanım süresi, launch count ve son kullanım sinyali birlikte kullanılıyor |
| Bölme/Birleştirme/Temizlik Önerileri | Tamamlandı - `FolderSuggestionEngine`, öneri ekranı, accept/dismiss/7 gün snooze persistence ve v4 backup/restore desteği eklendi |

Doğrulama: `compileDebugKotlin` geçti; `AppClassifierTest` ve `AppRepositoryTest` geçti. Tam debug build bu turun sonunda çalıştırılacak.

---

## Hedef

Play Store yayini icin Production AAB v1.0.0 hazir.

Kalan ana kapilar:
- Play Console formlari ve beyanlari
- Release imza ve final AAB
- Magaza gorselleri
- Gercek cihaz QA
- Notification analytics icin kalan dar cihaz testleri

## Guncel Kalan Is Listesi ve Uygulama Plani (2026-07-13)

| Sira | Is | Plan | Durum |
|---|---|---|---|
| 1 | Yerel Play/gizlilik sertlestirme | Route whitelist, release log guard, notificationText backup dislama mevcut. 2026-07-13'te telefon/SMS intent URI encode edildi ve privacy policy'deki artik olmayan Accessibility izni kaldirildi. | Tamamlandi - yerel |
| 2 | Play Console beyanlari | QUERY_ALL_PACKAGES, Data Safety, Content Rating ve Privacy Policy URL alanlari kod gercegine gore doldurulacak. Yerel store listing taslagi 2026-07-13'te `docs/store_listing.md` icinde temizlendi. | Bekliyor - kullanici/Play Console |
| 3 | Release imza | `scripts/create_release_keystore.ps1` ile kalici key uretilecek, `keystore.properties` git disinda saklanacak, final AAB temiz committen alinacak. | Bekliyor - kullanici aksiyonu |
| 4 | Magaza screenshot seti | Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore ekranlari kisisel veri olmadan cekilecek. | Bekliyor - cihaz/emulator |
| 5 | Gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap ve OEM kategori smoke tek pakette kosulacak. | Bekliyor - gercek cihaz |
| 6 | Notification analytics runtime kaniti | POST_NOTIFICATIONS revoke, NotificationListener ac/kapa, reboot ve 30+ gun temizlik senaryolari cihazda kanitlanacak. | Bekliyor - gercek cihaz |
| 7 | Build sureci benchmark | 2026-07-13 benchmark kosuldu: profile assembleDebug rerun 211.1s, configuration-cache compileDebugKotlin 5.5s, ikisi de exit 0. Kalici configuration cache acilmadi. | Tamamlandi - yerel |
| 8 | `cycle.ps1` gercek tur | Temiz dal ve push hazirliginda bir kez uctan uca kosulup Telegram/commit/push kaniti alinacak. | Bekliyor - gercek tur |
| 9 | Wrapped Phase 2 dis dogrulama | UsageEvents oturum altyapisi API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylariyla cihazda kanitlanacak. | Bekliyor - dis dogrulama |
| 10 | Dock UX tamamlandi | Gercek handler tabanli ilk dock, klasor dock item'i ve Sosyal Medya klasoru icin ilk uygun acilista otomatik ekleme uygulandi. | Tamamlandi - yerel; cihaz smoke bekliyor |
| 11 | Uzun vade backlog | Kendi sunucu API'si, Wear OS companion app ve widget ekran genisletme ayri sprintte ele alinacak. | Bekliyor |

---

## Kritik - Play Store ve Release Kapisi

| Gorev | Minimum cozum | Durum |
|---|---|---|
| QUERY_ALL_PACKAGES Play Store beyani | Launcher core functionality gerekcesiyle Play Console declaration doldurulacak. Metin ozeti: AppOrganizer tum yuklu uygulamalari organize etmek, app drawer/search gostermek ve uygulama baslatmak icin paket gorunurlugune ihtiyac duyar; paket/ad/kategori tercihleri cihazda kalir. | Bekliyor - dis aksiyon |
| Data Safety formu | Privacy policy, Firebase/Crashlytics/Analytics, optional contacts/files, NotificationListener, backup ve optional AI davranisi Play Console formuna kod gercegiyle uyumlu girilecek. Manifestte Accessibility servisi yok; servis geri eklenmedikce beyan edilmeyecek. | Bekliyor - dis aksiyon |
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
| Play oncesi gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap, OEM kategori ve screenshot smoke tek pakette kosulacak. Accessibility servisi manifestte olmadigi icin bu paketten cikarildi. | Bekliyor - gercek cihaz |
| BLUR-4/API26 testi | Blur/fallback performansi API 26+ temsilci cihazlarda kontrol edilecek. | Bekliyor - gercek cihaz |
| Backup/restore kaniti | SAF export/import, Drive klasor secimi, missingPackages dialogu ve restore sonrasi ayar devamligi kanitlanacak. | Bekliyor - cihaz/hesap |
| AllApps double-tap testi | Emulator veya fiziksel cihazda cift tiklama/arama gesture cakismasi tekrarlanacak. | Bekliyor - cihaz |
| Uretici kategori testi | Samsung/Xiaomi/Google tarzinda farkli OEM app setlerinde kategori eslesmeleri kontrol edilecek. | Bekliyor - cihaz |

---

## Orta Oncelik - UX ve Urun

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Dock: gercek Android varsayilanlari + klasor destegi + ilk kurulumda Sosyal Medya (14p, arastirma tamamlandi Dongu 243) | Telefon/SMS/tarayici icin varsayilan handler intent cozumleme, kamera fallback aday listesi, `folder:<categoryId>` dock item'i, DockEditSheet klasor sekmesi ve Sosyal Medya klasoru otomatik ekleme uygulandi. `compileDebugKotlin` ve `LauncherViewModelLogicTest` gecti. | Tamamlandi - yerel; cihaz smoke bekliyor |
| Klasor alti "X gundur acilmadi" bilgisi cok kucuk/anlasilmiyordu | `FolderTile.kt`: duz metin yerine mini cerceve (chip) — saat ikonu + hafif kontrast arka plan (RoundedCornerShape 8dp, siyah alpha 0.22) + daha yuksek metin alfa (0.55->0.80) + FontWeight.Medium. Tek dosya, davranis degismedi (hala `unusedInfoEnabled` toggle'ina bagli, varsayilan kapali). Build henuz dogrulanmadi (kullanici build'i iptal etti) — bir sonraki build turunda derleme kontrolu yapilmali. | Kod yazildi - build dogrulamasi bekliyor |

### Dock — Gercek Varsayilanlar + Klasor Destegi — Arastirma (Dongu 243)

**2026-07-13 uygulama sonucu:** `DockPrefs.kt` geriye uyumlu `List<String>` formatini koruyor. Eski kayitlar paket adi olarak calismaya devam ediyor; klasorler `folder:<categoryId>` prefix'i ile temsil ediliyor. Telefon/SMS/tarayici ilk dock secimi Android'in varsayilan handler cozumlemesinden geliyor; kamera icin resmi varsayilan rol olmadigindan aday listesi fallback olarak korundu. `LauncherViewModel` Sosyal Medya klasoru doluysa ve dock'ta bos slot varsa ilk uygun yuklemede `folder:social` ekliyor.

**"Android'de varsayilan ne geliyorsa o olsun" nasil yapilir:**
- Telefon/SMS icin uygulanan cozum: `PackageManager.resolveActivity()` ile `ACTION_DIAL tel:` ve `ACTION_SENDTO smsto:` intent'lerinin `MATCH_DEFAULT_ONLY` handler'i okunur. Bu compile SDK uyumlu ve API 26+ icin tek kod yoludur.
- Tarayici icin: `PackageManager.resolveActivity(Intent(ACTION_VIEW, Uri.parse("http://")), MATCH_DEFAULT_ONLY)` — sistemin varsayilan tarayicisini dondurur.
- Kamera icin: Android'de resmi bir "varsayilan kamera" rolu/API'si YOK — mevcut `DEFAULT_SLOTS` tahmin listesi (GoogleCamera/camera2/camera) korunmali, fallback olarak kalir.
- RoleManager alternatifi derleme uyumlulugu nedeniyle uygulanmadi; intent resolver yontemi minSdk 26 ile daha az riskli.

**Dock'a klasor eklenebilmesi icin:**
- `DockPrefs` veri modeli `List<String>` olarak kaldi; paket adlari uygulama, `folder:<categoryId>` ise klasor kabul ediliyor. Bu sayede eski dock kayitlari ve `BackupManager` string listesi bozulmadi.
- `DockEditSheet.kt`'ye Uygulamalar/Klasorler secimi eklendi.
- Dock render tarafinda (`HomeScreenComponents.kt` icindeki `PixelDock`) klasor tipi icin `DockFolderIcon`, tikla-ac ve uzun bas klasor menusu davranisi eklendi.

**Ilk kurulumda Sosyal Medya klasorunu dock'a getirme:**
- `LauncherViewModel.loadDockPackages()` sonrasi `folders` icinde `categoryId == Category.CAT_SOCIAL` ve `apps.isNotEmpty()` kontrol ediliyor; dock'ta bos slot varsa `DockPrefs`'e `folder:social` ekleniyor.
- Kullanici sonradan dock'tan cikarabilir; `removeFromDock` ayni string item'i kaldirir.

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
| Configuration cache guard benchmark | `scripts/benchmark_build.ps1` kosuldu. Sonuc: profile assembleDebug rerun 211.1s, configuration-cache compileDebugKotlin 5.5s, exit 0. Rapor: `docs/internal/build_benchmark_latest.md`. Configuration cache sadece CLI benchmark icin kullanildi; `gradle.properties` konservatif kaldi. | Tamamlandi - yerel |
| Build Analyzer / Gradle profile rutini | Gradle `--profile` raporu uretildi: `build/reports/profile/profile-2026-07-13-10-14-57.html`. Android Studio Build Analyzer ayri IDE aksiyonu olarak kalabilir, fakat CLI profil kapisi kapandi. | Tamamlandi - CLI profil |
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
