# Sistem Denetim 30 Dongu - 2026-07-14

Kapsam: `C:\Users\huseyinekizoglu\android-folderautomanager` repo statik inceleme.

Kural: Bu fazda kod duzeltmesi yapilmadi. Sadece gercek kanitli mantik/kod/urun riskleri listelendi.

Online arastirma ozeti:
- Android Core App Quality: gizlilik, guvenilirlik, performans ve izin davranisi Play kalitesi icin temel kabul edilir. Kaynak: https://developer.android.com/docs/quality-guidelines/core-app-quality
- Android Adaptive App Quality: tablet/buyuk ekran davranisi, resize ve state devamlıligi ayrica kontrol edilmelidir. Kaynak: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- Android 13+ medya izinleri: `READ_EXTERNAL_STORAGE` yerine granular medya izinleri veya sistem seciciler gerekir. Kaynak: https://developer.android.com/about/versions/13/behavior-changes-13
- OWASP MASVS: mobil uygulamada veri saklama, telemetri ve hassas veri temizleme davranisi acik ve test edilebilir olmalidir. Kaynak: https://mas.owasp.org/MASVS/

MemPalace / hafiza notu:
- `memory-palace list` calisti; bu repo icin ayrintili yeni kayit donmedi.
- Yerel hafizada ROADMAP/HISTORY/audit dokumanlari ve markdown encoding hassasiyeti bulundu; rapor yeni dosya olarak ASCII agirlikli yazildi.

## Oncelik Skalasi

| Skor | Anlam |
|---:|---|
| 10 | Play/gizlilik/veri kaybi veya release engeli |
| 8-9 | Kullanici verisi, restore, temel akis veya ciddi UX dogruluk hatasi |
| 6-7 | Ozellik bozulmasi, stale state, tablet/perf riski |
| 4-5 | Teknik borc, yanlis dokuman, lokal UX pürüzü |
| 1-3 | Izleme/iyilestirme adayi |

## 30 Dongu Ozeti

| Dongu | Alan | Sonuc | En Yuksek Skor |
|---:|---|---|---:|
| 01 | Build/release metadata | Bulgu var | 6 |
| 02 | Gizlilik metinleri | Bulgu var | 10 |
| 03 | Gizlilik sifirlama | Bulgu var | 10 |
| 04 | Bildirim analizi DB | Bulgu var | 9 |
| 05 | Yedek export/import | Bulgu var | 8 |
| 06 | Restore sonrasi worker/index | Bulgu var | 8 |
| 07 | Dock + klasor item | Bulgu var | 7 |
| 08 | Arama bar glow state | Bulgu var | 5 |
| 09 | Arama SQL fallback | Bulgu var | 7 |
| 10 | Dosya arama izinleri | Bulgu var | 8 |
| 11 | Rehber arama observer | Bulgu var | 7 |
| 12 | WorkManager politikasi | Bulgu var | 5 |
| 13 | App listesi performans | Bulgu var | 6 |
| 14 | Settings stale state | Bulgu var | 5 |
| 15 | Home permission hint | Bulgu var | 5 |
| 16 | Firebase/DeepSeek aktarim beyanlari | Bulgu var | 9 |
| 17 | Notification text persistence | Bulgu var | 9 |
| 18 | Remote category DB beyanlari | Bulgu var | 7 |
| 19 | Tablet/buyuk ekran | Risk var | 6 |
| 20 | Compose state listener tutarliligi | Bulgu var | 5 |
| 21 | Backup kapsam regresyonu | Bulgu var | 8 |
| 22 | Search source lifecycle | Bulgu var | 8 |
| 23 | Release signing guard | Bulgu var | 6 |
| 24 | Eski audit drift | Bulgu var | 4 |
| 25 | LLM network path | Izleme | 4 |
| 26 | Crash/log davranisi | Kritik bulgu yok | 0 |
| 27 | Package sync/index | Risk var | 5 |
| 28 | Play policy evidence | Bulgu var | 7 |
| 29 | Test kapsami | Eksik test var | 6 |
| 30 | Sonraki fix plani | Hazir | 10 |

## Kanitli Bulgular

### F01 - Gizlilik merkezindeki "internete veri gonderilmez" iddiasi gercek davranisla celisiyor

Skor: 10

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt:94` "Internete veri gonderilmez" diyor.
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt:97` "Reklamcilik veya izleme yok" diyor.
- `app/src/main/java/com/armutlu/apporganizer/AppOrganizerApp.kt:38` Crashlytics release'te aciliyor.
- `app/src/main/java/com/armutlu/apporganizer/AppOrganizerApp.kt:52` `AppAnalytics.appStarted` calisiyor.
- `app/src/main/java/com/armutlu/apporganizer/AppOrganizerApp.kt:91` FCM token aliniyor.
- `app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/CategoryLLMFallback.kt:109` DeepSeek API endpoint'i var.
- `app/src/main/java/com/armutlu/apporganizer/domain/usecase/wrapped/WrappedAiCoach.kt:43` DeepSeek API endpoint'i var.
- `app/src/main/java/com/armutlu/apporganizer/data/remote/AppDatabaseService.kt:12` GitHub raw DB endpoint'i var.

Etki:
- Play Data Safety ve kullanici guveni acisindan en riskli tutarsizlik.
- Gizlilik politikasi daha dogruyken, ayarlar ekrani fazla kesin ve yanlis iddia veriyor.

Yapilacak:
- Gizlilik karti metinlerini "varsayilan/istege bagli" ayrimina gore yeniden yaz.
- Firebase/Crashlytics/FCM/DeepSeek/remote DB icin net opt-in veya net beyan ekle.
- Play Data Safety formuyla birebir kontrol et.

### F02 - "Tum kullanim verisini sifirla" bildirim gecmisini ve bildirim metnini temizlemiyor

Skor: 10

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt:74` dialog "bildirim gecmisi ... silinir" diyor.
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt:122` satiri ayni vaadi tekrar ediyor.
- `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt:823` reset fonksiyonu basliyor.
- `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt:827-834` usage, launch, lastUsed, notificationCount ve notlari sifirliyor.
- `app/src/main/java/com/armutlu/apporganizer/data/local/AppDao.kt:337` `clearAllNotificationTexts()` var ama reset fonksiyonundan cagrilmiyor.
- `app/src/main/java/com/armutlu/apporganizer/data/local/NotificationEventDao.kt:33` sadece eski olaylari silen `deleteOlderThan` var; tum tablo temizleme yok.

Etki:
- Kullanici "tum veriyi sildim" sanarken `notification_events` ve muhtemel `apps.notificationText` kalabilir.
- Gizlilik/guvenlik acisindan yuksek risk.

Yapilacak:
- `NotificationEventDao.clearAll()` ekle.
- `AppRepository.clearAllNotificationTexts()` ve notification event temizligini `resetAllPrivacyData` icine al.
- Reset sonrasi rapor/pulse/wrapped ekranlarinda bos veri dogrula.

### F03 - Bildirim icerigi okunmaz metni, opsiyonel notification text kaliciligini aciklamiyor

Skor: 9

Kanit:
- `SettingsBackupAboutSection.kt:96` "Bildirim icerigi okunmaz (sadece sayi)" diyor.
- `AppNotificationListenerService.kt:41-50` title/text okuyup `combined` uretiyor.
- `AppNotificationListenerService.kt:52-54` ayar aciksa metni yayinliyor.
- `LauncherViewModel.kt:217` metinleri repository uzerinden DB'ye yaziyor.
- `AppDao.kt:326-338` `notificationText` kalici sutunu guncelleniyor/temizlenebiliyor.

Etki:
- Ayar kapaliyken varsayilan dogru olabilir; ancak metin "hic okunmaz" dedigi icin opsiyonel acik durumda yanlis.

Yapilacak:
- Metni "varsayilan kapali; acilirsa son bildirim metni cihazda saklanir" seklinde degistir.
- Notification text ayari ile gizlilik merkezini ayni ekranda bagla.

### F04 - About ekraninda versiyon hardcoded ve build versiyonuyla uyumsuz

Skor: 6

Kanit:
- `SettingsBackupAboutSection.kt:53` "v1.0 beta" gosteriyor.
- `app/build.gradle.kts:41-42` `versionCode=40`, `versionName="1.3.17"`.

Etki:
- Test/Play release kanitlarinda kullaniciya ve QA'ya yanlis surum gosterilir.

Yapilacak:
- `BuildConfig.VERSION_NAME` veya PackageManager versionName kullan.
- Release checklist'e UI versiyon smoke kontrolu ekle.

### F05 - Backup yeni ayarlarin bir kismini kapsamiyor

Skor: 8

Kanit:
- `AppPrefs.kt:325-327` `missions_enabled` var.
- `AppPrefs.kt:336-337` `search_shine_enabled` var.
- `AppPrefs.kt:569-571` `auto_folder_color_enabled` var.
- `AppPrefs.kt:600-602` `biometric_settings_lock` var.
- `SettingsLauncherScreen.kt:309-329` quick wheel ve focus mode ayarlari var.
- `BackupManager.kt:86-145` settings export blogunda bu anahtarlar yok.
- `BackupManager.kt:325-386` import blogunda bu anahtarlar yok.

Etki:
- Yedekten donen cihazda Pulse/Missions, search shine, otomatik klasor rengi, guvenlik kilidi, quick wheel/focus mode kullanici bekledigi gibi gelmeyebilir.

Yapilacak:
- Backup schema version artir.
- Bu prefleri export/import listesine ekle.
- Backward compatibility testine "yeni ayarlar round-trip" testi ekle.

### F06 - Restore dosya arama kaynagini prefs olarak aciyor ama worker/index lifecycle'i senkronlamiyor

Skor: 8

Kanit:
- `BackupManager.kt:139` `searchSourceFilesEnabled` export ediliyor.
- `BackupManager.kt:376` pref restore ediliyor.
- `BackupManager.kt:384-385` sadece WeeklyDigest/SmartInsight schedule ediliyor; `FilesIndexWorker.schedule/enqueueNow/cancel` yok.
- `SearchRepository.kt:198-205` dogru acma/kapama akisinda worker enqueue/schedule/cancel yapiliyor.

Etki:
- Restore sonrasi ayar acik gorunebilir ama dosya indeksi/periodic worker baslamayabilir.
- Tersi durumda ayar kapali restore edilse eski worker/indeks kalabilir.

Yapilacak:
- Backup import sonunda `searchSourceFilesEnabled` degerine gore `SearchRepository.enableFilesSource()` veya `disableFilesSource()` esdegeri calistir.
- Bu is icin repository dependency zor olacaksa restore-sonrasi lifecycle task queue ekle.

### F07 - Restore rehber arama kaynagini prefs olarak aciyor ama observer/register lifecycle'i eksik

Skor: 7

Kanit:
- `BackupManager.kt:138` `searchSourceContactsEnabled` export ediliyor.
- `BackupManager.kt:375` pref restore ediliyor.
- `SearchRepository.kt:186-193` dogru akis index + observer register/unregister yapiyor.
- `ContactsIndexer.kt:65-81` observer sadece explicit register ile devreye giriyor.
- `AppOrganizerApp.kt:65-72` startup yalnizca pref default set ediyor; observer register etmiyor.

Etki:
- Restore veya izinli ilk calistirma sonrasi kisi arama calissa bile rehber degisiklikleri izlenmeyebilir.

Yapilacak:
- Restore sonrasi contacts source icin `enableContactsSource/disableContactsSource` esdegeri calistir.
- App startup veya search bootstrap sonunda contacts observer register durumunu garanti et.

### F08 - Dock ayarlarinda klasor dock item'i paket adi gibi render ediliyor

Skor: 7

Kanit:
- `DockPrefs.kt:14` folder prefix `folder:`.
- `DockPrefs.kt:57-63` folder item helper'lari var.
- `HomeScreen.kt:1031` dock item'i folder olarak ayirt ediyor.
- `DockEditSheet.kt:120` folder item'i dogru cozuyor.
- `SettingsLauncherScreen.kt:135-146` her dock item icin `pm.getApplicationInfo(pkg, 0)` yapiyor ve `Icons.Default.Apps` basiyor.

Etki:
- Dock'ta sosyal klasor varsa ayarlarda "folder:social" gibi ham deger gorunebilir.
- Kaldirma calisir ama kullanici deneyimi ve tema tutarliligi bozulur.

Yapilacak:
- `DockPrefs.isFolderItem(pkg)` branch'i ekle.
- Folder category ad/emoji/icon goster.
- Remove butonu ayni item string'i ile calismaya devam etsin.

### F09 - All Apps arama parlamasi ayar degisince reaktif degil

Skor: 5

Kanit:
- `AllAppsDrawer.kt:134` `shineEnabled = remember { AppPrefs.isSearchShineEnabled(context) }`.
- `AllAppsDrawer.kt:159` `.diamondShine(shineEnabled, ...)` kullaniliyor.
- `HomeScreenComponents.kt:768-783` ana ekran aramasinda ayni tercih icin listener var.

Etki:
- Ayarlardan search shine kapat/ac sonrasi All Apps drawer ayni oturumda eski degeri kullanabilir.

Yapilacak:
- AllAppsDrawer icin de `OnSharedPreferenceChangeListener` ekle.
- Ana ekran ile ayni preference-state helper'ina tasimak daha temiz.

### F10 - Arama LIKE fallback `%` ve `_` kacirma mantigi SQL ESCAPE kullanmiyor

Skor: 7

Kanit:
- `SearchRepository.kt:125` pattern `%` ve `_` karakterlerini backslash ile kacar.
- `SearchRepository.kt:134` `title LIKE ? OR subtitle LIKE ?` kullaniyor; `ESCAPE '\'` yok.

Etki:
- Kullanici `%` veya `_` iceren arama yaparsa literal arama dogru calismayabilir.
- Testler helper seviyesinde olabilir ama gercek SQLite sorgusu ESCAPE olmadan farkli davranabilir.

Yapilacak:
- SQL'e `LIKE ? ESCAPE '\'` ekle.
- Instrumented veya Robolectric SQLite testi ile `%`, `_`, `\` query'lerini dogrula.

### F11 - AppDao eski searchAppsByName akisi limitsiz ve wildcard kacirmasiz

Skor: 5

Kanit:
- `AppDao.kt:186-187` `appName LIKE '%' || :query || '%'` limitsiz flow.
- `AppDao.kt:192-193` limitli alternatif var.
- `AppRepository.kt:157` limitsiz metodu expose ediyor.

Etki:
- Bu yol UI'da tekrar kullanilirsa buyuk cihazlarda performans ve wildcard arama tutarliligi bozulur.

Yapilacak:
- Eski metodu `@Deprecated` yap veya repository'de limitli/escape'li metoda yonlendir.
- Mevcut kullanicilarini `rg` ile temiz tutan test ekle.

### F12 - Dosya arama "runtime izin gerektirmez" varsayimi Android 13+ icin riskli

Skor: 8

Kanit:
- `FilesIndexer.kt:16-20` MediaStore dosya arama icin "Ek runtime izni gerektirmez" diyor.
- `FilesIndexer.kt:55-58` Images/Video/Audio/Downloads MediaStore query yapiyor.
- `AndroidManifest.xml:17-20` sadece `READ_EXTERNAL_STORAGE` var ve `maxSdkVersion=32`.
- Manifest'te Android 13+ `READ_MEDIA_IMAGES/VIDEO/AUDIO` yok.

Etki:
- Android 13+ cihazlarda dosya arama kaynagi acik gorunup bos/eksik sonuc verebilir.
- Play izin beyanlari ve kullanici beklentisi acisindan risk.

Yapilacak:
- Android 13+ davranisini emulator/tablet smoke ile dogrula.
- Ya granular izin akisi ekle ya da "sadece izin verilen MediaStore/Downloads kapsami" diye UX metnini daralt.

### F13 - FilesIndexWorker periyodik is `KEEP` ile schedule degisikliklerini yutabilir

Skor: 5

Kanit:
- `FilesIndexWorker.kt:67-80` 24 saatlik periodic request olusturuyor.
- `FilesIndexWorker.kt:76-80` `ExistingPeriodicWorkPolicy.KEEP` kullaniyor.

Etki:
- Gelecek surumde constraint/frequency degisirse eski cihazlarda yeni plan uygulanmayabilir.

Yapilacak:
- Ayar veya surum degisince `UPDATE`/cancel+enqueue kullan.
- Work policy secimini release notuna bagla.

### F14 - Home permission hint sayac/dismiss state'i kalici pref degisiminden sonra stale kalabilir

Skor: 5

Kanit:
- `HomeScreenComponents.kt:912` `permHintCount` sadece `remember` ile okunuyor.
- `HomeScreenComponents.kt:915` `permHintPermDismissed` sadece `remember` ile okunuyor.
- `HomeScreenComponents.kt:919-920` sayac artiriliyor ama local state yenilenmiyor.
- `HomeScreenComponents.kt:1472-1475` kalici dismiss/count degisiyor ama state listener yok.

Etki:
- Hint pasif moda gecme veya kalici kapanma ayni oturumda tutarsiz gorunebilir.

Yapilacak:
- Count/dismissed icin `mutableStateOf` + setter sonrasi update veya SharedPreferences listener kullan.

### F15 - Settings ekranlarinda bircok pref restore/dis kaynak degisimine reaktif degil

Skor: 5

Kanit:
- `SettingsStatsScreen.kt:133-142` notif analytics state lokal.
- `SettingsStatsScreen.kt:165-174` missions state lokal.
- `SettingsSecurityScreen.kt:42-66` biometric lock state lokal.
- `SettingsLauncherScreen.kt:258-299` folder transition state lokal.
- `SettingsLauncherScreen.kt:309-329` quick wheel/focus state lokal.
- `SearchSettingsScreen.kt:74-89` search source ve UI prefleri lokal.

Etki:
- Backup restore, baska ekrandan ayar degisikligi veya prefs migration sonrasi ayar ekrani eski deger gosterebilir.

Yapilacak:
- Ortak `rememberPreferenceState(key)` helper'i olustur.
- Restore tamamlandiktan sonra settings screen state refresh event'i yayinla.

### F16 - getAllAppsFlow birden cok StateFlow tarafindan tam tablo olarak dinleniyor

Skor: 6

Kanit:
- `AppDao.kt:115-116` tum app listesini flow olarak donduruyor.
- `LauncherViewModel.kt:146`, `167`, `174`, `180`, `590`, `640`, `885`, `946` ayni tam tablo flow'unu farkli turetilmis state'ler icin kullaniyor.
- `AppDao.kt:108-109` paging icin `getAppsPage` zaten var.

Etki:
- App sayisi yuksek cihazlarda tek DB degisikligi birden fazla tam liste transformunu tetikler.
- Tablet/buyuk ekran ve cok uygulamali kullanicida frame drop riski.

Yapilacak:
- LauncherViewModel icinde tek shared `allAppsFlow` uzerinden derive et.
- Ozet/favorite/recent gibi listeleri DAO seviyesinde limitli sorgulara ayir.

### F17 - Release build keystore yoksa debug imzaya dusuyor

Skor: 6

Kanit:
- `app/build.gradle.kts:67-73` keystore yoksa release build debug signing ile aliniyor.

Etki:
- Yerel dogrulama icin yararli ama Play release artifact'i yanlis imzali uretilebilir.

Yapilacak:
- `assembleRelease` icin CI/Play gorevlerinde keystore zorunlu olsun.
- Lokal smoke icin ayri `assembleReleaseLocal` veya property flag kullan.

### F18 - Eski security audit dokumani current code ile drift olmus

Skor: 4

Kanit:
- `docs/internal/security_audit_2026-07-13.md:155` Timber DebugTree'nin kosulsuz oldugunu soyluyor.
- `AppOrganizerApp.kt:27-30` current code `if (BuildConfig.DEBUG)` ile sarmis.

Etki:
- Denetim dokumanlari kullanici/ajan icin yanlis restart noktasi verebilir.

Yapilacak:
- Eski audit bulgulari HISTORY'ye tasinirken "cozuldu/stale" etiketi ekle.
- Aktif sorun listesi tek kaynak: `COZULEMEYEN_SORUNLAR.md` veya yeni audit raporu olsun.

### F19 - Remote category DB beyanlari ve gercek endpoint tutarliligi test edilmeli

Skor: 7

Kanit:
- `SettingsBackupAboutSection.kt:95` "Online kategori DB varsayilan kapali" diyor.
- `AppDatabaseService.kt:12` remote DB URL GitHub raw.
- `AndroidManifest.xml:13-14` INTERNET/ACCESS_NETWORK_STATE var.

Etki:
- Varsayilan kapali dogruysa sorun yok; ancak Play Data Safety ve gizlilik metni "internete veri yok" ile beraber okundugunda celiski buyuyor.

Yapilacak:
- Online DB ayarinin default degerini ve acilis akisinda otomatik indirme olup olmadigini test et.
- Gizlilik merkezine "online DB acilirsa GitHub'dan kategori listesi indirir" aciklamasi ekle.

### F20 - Tablet/buyuk ekran icin arama sonuc yukseklikleri ve folder transition ic ice binme regresyonu test kapsamina girmeli

Skor: 6

Kanit:
- Kullanici once klasor gecislerinde ve cok uygulamali sayfada ust uste binme bildirdi.
- `FolderScreen.kt:143-155` folder transition offset animasyonu var.
- `FolderScreen.kt:616` nav alpha offset'e bagli.
- `HomeScreenComponents.kt:941-949` arama sonuc paneli coklu kaynaklara gore genisliyor.

Etki:
- Telefon smoke gecse bile tablet landscape ve cok uygulamali klasorde layout overlap tekrar edebilir.

Yapilacak:
- Tablet emulator smoke: 10 inch, landscape, cok uygulamali klasor, all apps search, folder carousel bottom/top/middle.
- Screenshot kaniti `artifacts/emulator-smoke/tablet-*` altina kaydedilsin.

## False Positive / Kapanmis Eski Bulgular

| Eski iddia | Current durum |
|---|---|
| Timber release'te kosulsuz DebugTree | Kapanmis: `AppOrganizerApp.kt:27-30` DEBUG guard var. |
| Backup folder transition/home usage chart/search source fields eksik | Kismen kapanmis: `BackupManager.kt:129`, `132`, `138-139`, `365-376` var. Ancak F05'te yeni eksikler kaldi. |
| Pulse Clock pref listener yok | Kapanmis: `PulseClockWidget.kt:95-108` listener var. |
| FolderScreen carousel/search pref listener yok | Kapanmis: `FolderScreen.kt:67-89` listener var. |

## Oncelikli Fix Plani

1. P0/P1 gizlilik duzeltmesi:
   - F01, F02, F03 birlikte cozulmeli.
   - Once metinleri dogrula, sonra reset fonksiyonunu gercekten tum yerel bildirim/veri izlerini temizleyecek hale getir.

2. P1 backup/restore lifecycle:
   - F05, F06, F07 birlikte cozulmeli.
   - Backup schema version artir, yeni prefleri round-trip test et, restore sonrasi worker/index senkronu yap.

3. P2 search/dock UX:
   - F08, F09, F10, F11, F12 sirayla cozulmeli.
   - Ozellikle Android 13+ dosya arama davranisi emulator ile kanitlanmadan Play metni kesin yazilmasin.

4. P3 performans/tablet:
   - F13-F20 icin smoke/test matrisi kur.
   - Buyuk ekran ve cok uygulamali cihaz profili olmadan "tamam" denmesin.

## Test Matrisi

| Test | Komut / Yontem | Beklenen |
|---|---|---|
| Kotlin compile | `.\gradlew.bat compileDebugKotlin -PskipGoogleServices --no-daemon` | Basarili |
| Unit narrow | `.\gradlew.bat testDebugUnitTest --tests "*LauncherViewModelLogicTest*" -PskipGoogleServices --no-daemon` | Basarili |
| Backup round-trip | Yeni test | missions/searchShine/autoFolderColor/biometric/quickWheel/focus korunur |
| Privacy reset | Yeni DAO/ViewModel test | notification_events, notificationText, counts, usage, notes sifir |
| Search LIKE | Yeni SQLite test | `%`, `_`, `\` literal aramalari dogru |
| Restore lifecycle | Yeni integration test | file worker schedule/cancel, contact observer/index dogru |
| Tablet smoke | Emulator 10 inch landscape | overlap yok, folder transition temiz |
| Android 13+ file search | API 33/34 emulator | izin/sonuc davranisi net |

## Sonraki Uygulama Promptu

```text
C:\Users\huseyinekizoglu\android-folderautomanager reposunda docs/internal/sistem_denetim_30_dongu_2026-07-14.md raporundaki bulgulari oncelik sirasiyla coz.

Kurallar:
1. Once F01-F03 gizlilik ve reset bulgularini coz.
2. Sonra F05-F07 backup/restore lifecycle bulgularini coz.
3. Sonra F08-F12 search/dock/dosya arama bulgularini coz.
4. Her fix icin ilgili test veya en azindan targeted compile calistir.
5. ROADMAP/HISTORY gerekiyorsa yalnizca gercek tamamlananlari guncelle.
6. Build/push/Telegram gonderimi icin ayrica onay bekle.
```
