# Sistem Denetim 30 Dongu - 2026-07-14

Kapsam: `C:\Users\huseyinekizoglu\android-folderautomanager`

Durum: Bu dosya P0/P1 gizlilik + backup restore fix commit'i (`3557345`) sonrasinda guncellendi. 2026-07-14 ikinci turda F04, F08, F09, F10, F11, F12, F13, F14, F15, F16, F17 ve F18 kod/dokuman seviyesinde kapatildi. F20 icin tablet emulator smoke yapildi; tam klasor swipe gorsel QA kismi acik risk olarak birakildi.

## 0. 2026-07-14 Cozum Kapanis Ozeti

Toplam aktif kod/dokuman bulgusu: 0

Kismi runtime risk: 1 (`F20` tablet klasor swipe gorsel QA)

Kapatilanlar:
- F04: About surumu artik `BuildConfig.VERSION_NAME` ile runtime build degerinden geliyor.
- F08: Dock ayarlari `folder:` item'larini klasor adi/emoji ve folder ikonu ile gosteriyor.
- F09/F15: Arama ve ayarlar ekranlari icin ortak SharedPreferences listener helper'i eklendi; restore/dis kaynak degisimleri ayni oturumda UI'a yansiyor.
- F10: LIKE fallback pattern `%`, `_` ve `\` karakterlerini escape ediyor; SQL `ESCAPE '\'` kullaniyor.
- F11: Eski unbounded `searchAppsByName` deprecated edildi; repository yolu limitli sorguya tasindi.
- F12: Android 13+ medya izinleri manifest ve runtime permission akisina eklendi; indexer izinsiz MediaStore taramasini erken kesiyor.
- F13: FilesIndexWorker periodic is politikasinda `KEEP` yerine `UPDATE` kullaniliyor.
- F14: Home permission hint count/dismiss state'i setter sonrasi lokal state'i guncelliyor.
- F16: `LauncherViewModel` tek shared `allAppsSource` uzerinden turetilmis state'ler uretiyor.
- F17: Release task'lari keystore yokken fail ediyor; lokal test icin `-PallowDebugReleaseSigning=true` acik flag'i gerekiyor.
- F18: Eski security audit dokumanina stale/cozuldu notu eklendi.

Calistirilan dogrulamalar:
- `.\gradlew.bat testDebugUnitTest --tests "com.armutlu.apporganizer.TurkishSearchTest" -PskipGoogleServices --no-daemon`
- `.\gradlew.bat testDebugUnitTest --tests "com.armutlu.apporganizer.data.repository.AppRepositoryTest" -PskipGoogleServices --no-daemon`
- `.\gradlew.bat compileDebugKotlin -PskipGoogleServices --no-daemon`
- `.\gradlew.bat :app:validateSigningRelease -PskipGoogleServices --no-daemon` beklenen sekilde keystore guard hatasi verdi.
- `.\gradlew.bat assembleDebug -PskipGoogleServices --no-daemon`
- API 33 emulator tablet override: `wm size 1280x800`, `wm density 160`, app process calisiyor, `AndroidRuntime` fatal log yok.
- Medya izinleri package dump'ta gorundu: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`.

KanÄ±t dosyasi:
- `artifacts/emulator-smoke/f20_tablet_after_fixes.png`

Acik risk:
- F20 icin build + tablet launch smoke + screenshot var; ancak cok uygulamali klasorde elle sayfa cevirme/swipe ve top/middle/bottom fihrist varyantlari bu turda tam gorsel QA olarak yapilmadi.

## 1. Yonetici Ozeti

Toplam gercek acik bulgu: 0

P0: 0

P1: 0

P2: 0

P3: 0

Kismi runtime risk: 1 (`F20`)

Kapanmis onceki kritikler:
- F01/F03 gizlilik metni tutarsizligi kapandi: `SettingsBackupAboutSection.kt:94-97`.
- F02 gizlilik sifirlama eksigi kapandi: `AppListViewModel.kt:834-835`, `NotificationEventDao.kt:36-37`.
- F05 backup kapsam eksigi kapandi: `BackupManager.kt:134-139`, `BackupManager.kt:378-383`.
- F06/F07 restore sonrasi search source lifecycle kismen kapandi: `BackupManager.kt:400`, `BackupManager.kt:444-459`, `AppListViewModel.kt:870`.

Ilk cozulmesi gereken 5 is:
1. Tamamlandi: F12 Android 13+ dosya arama izin/model uyumsuzlugu netlestirildi ve kod akisi duzeltildi.
2. Tamamlandi: F10 LIKE fallback icin `ESCAPE '\'` ve escape helper testi eklendi.
3. Tamamlandi: F08 dock ayarlarinda `folder:` item renderi duzeltildi.
4. Tamamlandi: F04 About ekraninda hardcoded surum yerine runtime version gosteriliyor.
5. Kismi kanitlandi: F20 tablet launch smoke tamamlandi; detayli klasor swipe gorsel QA acik risk.

## 2. Kullanilan Kaynaklar

- Google Play Data Safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Android WorkManager existing work update/cancel: https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/update-work
- Android Adaptive App Quality: https://developer.android.com/docs/quality-guidelines/adaptive-app-quality
- Android 13 media permissions: https://developer.android.com/about/versions/13/behavior-changes-13
- OWASP MASVS: https://mas.owasp.org/MASVS/

## 3. Calistirilan Komutlar

```powershell
memory-palace list
git status --short --branch
rg -n "Firebase ve|Bildirim metni varsayÄ±lan|notification_events|clearAll\(|missionsEnabled|searchShineEnabled|autoFolderColorEnabled|biometricSettingsLockEnabled|quickWheelEnabled|focusModeEnabled|syncSearchSourcesAfterRestore|enableContactsSource|enableFilesSource|v1.0 beta|LIKE \?|READ_EXTERNAL_STORAGE|READ_MEDIA|ExistingPeriodicWorkPolicy\.KEEP|remember \{ AppPrefs.isSearchShineEnabled" app/src/main/java app/src/test/java app/build.gradle.kts app/src/main/AndroidManifest.xml -S
rg -n "fun resetAllPrivacyData|clearAllNotificationTexts|clearAllNotificationEvents|importBackup\(|BACKUP_VERSION|importFromJson\(" app/src/main/java app/src/test/java -S
```

Not: Bu rapor guncellemesi dokuman odakli oldugu icin kod degisikligi yapilmadi. Son basarili genel build onceki fix turunde `assembleDebug -PskipGoogleServices --no-daemon` ile alindi ve `HISTORY.md:9` altinda kayitli.

## 4. 30 Dongu Durumu Tablosu

| Dongu | Alan | Sonuc | En yuksek oncelik |
|---:|---|---|---:|
| 01 | Git/build/proje yapisi | Bulgu var | 68 |
| 02 | Room/DAO/migration | Kritik bulgu yok | 0 |
| 03 | Repository veri tutarliligi | Kritik bulgu yok | 0 |
| 04 | Backup/restore | Fix sonrasi test riski var | 60 |
| 05 | SharedPreferences/local state | Bulgu var | 48 |
| 06 | WorkManager | Bulgu var | 48 |
| 07 | Bildirim sistemi/izinler | Kritikler kapandi | 0 |
| 08 | LauncherActivity lifecycle | Bu turda yeni bulgu yok | 0 |
| 09 | LauncherViewModel | Performans riski var | 58 |
| 10 | Ana ekran UI | Bulgu var | 48 |
| 11 | Klasor ekrani/gecis | Runtime smoke eksik | 62 |
| 12 | Dock/contextual dock | Bulgu var | 70 |
| 13 | All Apps | Bulgu var | 45 |
| 14 | Arama sistemi | Bulgu var | 78 |
| 15 | Kategori siniflandirma | Yeni dogrulanmis bulgu yok | 0 |
| 16 | FolderSuggestionEngine | Yeni dogrulanmis bulgu yok | 0 |
| 17 | Kullanim istatistikleri | Yeni dogrulanmis bulgu yok | 0 |
| 18 | Wrapped/haftalik rapor | Yeni dogrulanmis bulgu yok | 0 |
| 19 | Pulse Clock | Onceki listener riski kapali | 0 |
| 20 | Dashboard/hedef sistemi | Test boslugu var | 55 |
| 21 | Gizlilik/guvenlik | P0 kapandi, Play form kontrolu kaldi | 60 |
| 22 | Ayarlar mimarisi | Bulgu var | 48 |
| 23 | Onboarding/izin rehberi | Yeni dogrulanmis bulgu yok | 0 |
| 24 | Navigation/deep link | Yeni dogrulanmis bulgu yok | 0 |
| 25 | Widget sistemi | F21 kapandi (backup exclude + validation eklendi); F22 acik (dusuk oncelik, belirsizlik yuksek) | 38 |
| 26 | Package receiver | Bu turda yeni bulgu yok | 0 |
| 27 | Compose reactivity | Bulgu var | 48 |
| 28 | Tablet/adaptive | Bulgu var | 62 |
| 29 | Test altyapisi | Bulgu var | 60 |
| 30 | Dokumantasyon/yayin | Bulgu var | 60 |

## 5. Bulgu Listesi

### F04

Dongu: 30

Baslik: About ekraninda hardcoded versiyon var

Kategori: Urun dogrulugu / yayin hazirligi

Oncelik puani: 60

Siddet: P2

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt:53` `v1.0 beta` gosteriyor.
- `app/build.gradle.kts:41-42` `versionCode=40`, `versionName="1.3.17"`.

Etkilenen dosya/satir:
- `SettingsBackupAboutSection.kt:53`
- `app/build.gradle.kts:41-42`

Neden gercek sorun:
- QA, Play kaniti ve kullanici ekrani farkli surum bilgisini gorebilir.

Kullaniciya etkisi:
- Hata bildirimi, destek ve release kanitlari yanlis surumle karisir.

Onerilen cozum:
- `BuildConfig.VERSION_NAME` veya PackageManager `versionName` kullan.

Nereye uygulanacak:
- `SettingsBackupAboutSection.kt`

Test plani:
- `compileDebugKotlin`
- Emulator Settings > About ekraninda versionName dogrulama.

Risk:
- Dusuk; sadece UI metni.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F08

Dongu: 12

Baslik: Dock ayarlari `folder:` item'i paket adi gibi render ediyor

Kategori: UX / veri modeli uyumsuzlugu

Oncelik puani: 70

Siddet: P2

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/utils/DockPrefs.kt:14` `folder:` prefix var.
- `app/src/main/java/com/armutlu/apporganizer/utils/DockPrefs.kt:57-63` folder item helper'lari var.
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsLauncherScreen.kt:135-146` her dock item icin `PackageManager.getApplicationInfo(pkg, 0)` deniyor.

Etkilenen dosya/satir:
- `DockPrefs.kt:14`, `DockPrefs.kt:57-63`
- `SettingsLauncherScreen.kt:135-146`

Neden gercek sorun:
- Dock'ta klasor item varsa ayarlarda ham `folder:social` gibi gorunebilir.

Kullaniciya etkisi:
- Dock yonetimi guvensiz/bozuk algilanir.

Onerilen cozum:
- `DockPrefs.isFolderItem(pkg)` branch'i ekle, kategori adi/emoji/icon goster.

Nereye uygulanacak:
- `SettingsLauncherScreen.kt`

Test plani:
- Dock'a sosyal klasor ekle, Ayarlar > Launcher dock listesini ac.

Risk:
- Orta; UI modeli ve kaldirma aksiyonu ayni item string'iyle korunmali.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F09

Dongu: 13

Baslik: All Apps arama parlamasi ayar degisince reaktif degil

Kategori: Compose state reactivity

Oncelik puani: 45

Siddet: P3

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt:134` `remember { AppPrefs.isSearchShineEnabled(context) }`.
- Ana ekran aramasinda listener var: `HomeScreenComponents.kt:768-783`.

Etkilenen dosya/satir:
- `AllAppsDrawer.kt:134`
- `HomeScreenComponents.kt:768-783`

Neden gercek sorun:
- Ayardan kapat/ac sonrasi All Apps drawer ayni oturumda eski degeri kullanabilir.

Kullaniciya etkisi:
- Ayar calismiyor gibi gorunur.

Onerilen cozum:
- SharedPreferences listener veya ortak preference-state helper kullan.

Nereye uygulanacak:
- `AllAppsDrawer.kt`

Test plani:
- Ayari degistir, All Apps arama barinda shine davranisini ayni oturumda dogrula.

Risk:
- Dusuk.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F10

Dongu: 14

Baslik: LIKE fallback `%` ve `_` kacirma mantigi SQL `ESCAPE` kullanmiyor

Kategori: Arama mantigi

Oncelik puani: 72

Siddet: P2

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/data/repository/SearchRepository.kt:125` pattern backslash ile kaciriliyor.
- `app/src/main/java/com/armutlu/apporganizer/data/repository/SearchRepository.kt:134` `LIKE ?` var ama `ESCAPE '\'` yok.

Etkilenen dosya/satir:
- `SearchRepository.kt:125`
- `SearchRepository.kt:134`

Neden gercek sorun:
- SQLite `LIKE` sorgusunda backslash escape karakteri olarak belirtilmezse `%` ve `_` literal arama dogru calismayabilir.

Kullaniciya etkisi:
- Ozel karakterli uygulama/kisi/dosya aramalarinda yanlis sonuc.

Onerilen cozum:
- `title LIKE ? ESCAPE '\'` ve `subtitle LIKE ? ESCAPE '\'` kullan.

Nereye uygulanacak:
- `SearchRepository.buildLikeQuery`

Test plani:
- SQLite tabanli unit/instrumentation test: `%`, `_`, `\` query.

Risk:
- Orta; FTS fallback davranisi etkilenir.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F11

Dongu: 03

Baslik: Eski `searchAppsByName` akisi limitsiz ve wildcard kacirmasiz

Kategori: Repository/DAO teknik borc

Oncelik puani: 42

Siddet: P3

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/data/local/AppDao.kt:186-187` limitsiz `LIKE`.
- `app/src/main/java/com/armutlu/apporganizer/data/repository/AppRepository.kt:157` metodu expose ediyor.

Etkilenen dosya/satir:
- `AppDao.kt:186-187`
- `AppRepository.kt:157`

Neden gercek sorun:
- Yeni UI bu yolu kullanirsa buyuk app listesinde performans ve wildcard tutarliligi bozulabilir.

Kullaniciya etkisi:
- Arama gecikmesi veya yanlis sonuc.

Onerilen cozum:
- Deprecated yap veya limitli/escape'li versiyona yonlendir.

Nereye uygulanacak:
- `AppDao.kt`, `AppRepository.kt`

Test plani:
- `rg "searchAppsByName("` ile consumer kontrolu; repository test.

Risk:
- Dusuk/orta.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F12

Dongu: 14

Baslik: Android 13+ dosya arama izin modeli belirsiz

Kategori: Izin / Play policy / dosya arama

Oncelik puani: 78

Siddet: P1

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/data/local/FilesIndexer.kt:16-20` "Ek runtime izni gerektirmez" diyor.
- `app/src/main/java/com/armutlu/apporganizer/data/local/FilesIndexer.kt:55-58` Images/Video/Audio/Downloads MediaStore query yapiyor.
- `app/src/main/AndroidManifest.xml:17` sadece `READ_EXTERNAL_STORAGE` var.
- `PrivacyAnalyzer.kt:104-107` READ_MEDIA izinlerini analiz ediyor ama manifestte granular izinler yok.

Etkilenen dosya/satir:
- `FilesIndexer.kt:16-20`, `FilesIndexer.kt:55-58`
- `AndroidManifest.xml:17`
- `PrivacyAnalyzer.kt:104-107`

Neden gercek sorun:
- Android 13+ cihazlarda dosya arama kaynagi acik gorunup bos/eksik sonuc verebilir veya izin metni gercegi yansitmayabilir.

Kullaniciya etkisi:
- "Dosya arama" acik ama dosya bulunmuyor algisi.

Onerilen cozum:
- Android 13+ davranisini emulatorle kanitla.
- Gerekirse granular media permission akisi veya daha dar "izin verilen MediaStore kapsami" metni ekle.

Nereye uygulanacak:
- `AndroidManifest.xml`, `SearchSettingsScreen.kt`, `FilesIndexer.kt`, Play evidence docs.

Test plani:
- API 33/34 emulator: dosya kaynagi ac/kapat, image/audio/video/downloads sonuc kontrolu.

Risk:
- Yayin ve kullanici beklentisi riski yuksek.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F13

Dongu: 06

Baslik: FilesIndexWorker `KEEP` politikasi schedule degisikliklerini yutabilir

Kategori: WorkManager

Oncelik puani: 48

Siddet: P3

Kanit:
- `app/src/main/java/com/armutlu/apporganizer/data/local/FilesIndexWorker.kt:78` `ExistingPeriodicWorkPolicy.KEEP`.

Etkilenen dosya/satir:
- `FilesIndexWorker.kt:78`

Neden gercek sorun:
- Periyodik is constraint/frequency degisirse eski is korunur, yeni politika uygulanmayabilir.

Kullaniciya etkisi:
- Dosya indeksi beklenen siklikta yenilenmeyebilir.

Onerilen cozum:
- `UPDATE` veya explicit cancel+enqueue politikasi kullan; surum migration gerekiyorsa ekle.

Nereye uygulanacak:
- `FilesIndexWorker.kt`

Test plani:
- WorkManager test helper veya emulatorda unique work state kontrolu.

Risk:
- Dusuk/orta.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F14

Dongu: 10

Baslik: Home permission hint count/dismiss state stale kalabilir

Kategori: Compose state

Oncelik puani: 45

Siddet: P3

Kanit:
- `HomeScreenComponents.kt:912` count `remember` ile tek sefer okunuyor.
- `HomeScreenComponents.kt:915` dismissed `remember` ile tek sefer okunuyor.
- `HomeScreenComponents.kt:919-920` count artiyor.
- `HomeScreenComponents.kt:1472-1475` dismissed/count pref degisiyor.

Etkilenen dosya/satir:
- `HomeScreenComponents.kt:912`, `915`, `919-920`, `1472-1475`

Neden gercek sorun:
- Pref degisimi ayni compose oturumunda UI'a yansimayabilir.

Kullaniciya etkisi:
- Izin ipucu fazla gorunebilir veya kapanmasi gecikebilir.

Onerilen cozum:
- Local mutable state'i setter sonrasi guncelle veya pref listener kullan.

Nereye uygulanacak:
- `HomeScreenComponents.kt`

Test plani:
- Izin hint X/pasif mod davranisi emulator smoke.

Risk:
- Dusuk.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F15

Dongu: 22 / 27

Baslik: Settings ekranlarinda restore/dis kaynak degisimine reaktif olmayan pref state'leri var

Kategori: Compose reactivity / ayarlar mimarisi

Oncelik puani: 48

Siddet: P3

Kanit:
- `SettingsStatsScreen.kt:165-174` missions state lokal.
- `SettingsSecurityScreen.kt:42-66` biometric lock state lokal.
- `SettingsLauncherScreen.kt:258-299` folder transition state lokal.
- `SettingsLauncherScreen.kt:309-329` quick wheel/focus state lokal.
- `SearchSettingsScreen.kt:74-89` search source state lokal.

Etkilenen dosya/satir:
- Yukaridaki dosya/satirlar.

Neden gercek sorun:
- Restore sonrasi ayni ekranda eski state gorunebilir.

Kullaniciya etkisi:
- Ayarlar kaydedilmedi veya restore bozuk sanilabilir.

Onerilen cozum:
- Ortak `rememberPreferenceState(key)` veya restore-complete refresh event'i.

Nereye uygulanacak:
- Ayarlar ekranlari.

Test plani:
- Backup restore sonrasi ayni ekran acikken toggle degerleri.

Risk:
- Orta.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F16

Dongu: 09 / 13

Baslik: `getAllAppsFlow` birden cok StateFlow icin tam tablo emit ediyor

Kategori: Performans

Oncelik puani: 58

Siddet: P2

Kanit:
- `AppDao.kt:115-116` tum app listesini flow donduruyor.
- `LauncherViewModel.kt:146`, `167`, `174`, `180`, `590`, `640`, `885`, `946` ayni tam tablo flow'u farkli derive state'lerde kullaniliyor.

Etkilenen dosya/satir:
- `AppDao.kt:115-116`
- `LauncherViewModel.kt` ilgili stateFlow satirlari.

Neden gercek sorun:
- Buyuk app listesinde tek DB degisikligi coklu tam liste transformu tetikler.

Kullaniciya etkisi:
- Dusuk/orta seviye jank, ozellikle tablet/cok uygulamali cihazda.

Onerilen cozum:
- ViewModel icinde tek shared `allAppsFlow` derive et; recent/favorite/suggested icin DAO limitli sorgular kullan.

Nereye uygulanacak:
- `LauncherViewModel.kt`, `AppDao.kt`

Test plani:
- Unit test + profiler/emulator buyuk app dataseti.

Risk:
- Orta; refactor dikkatli yapilmali.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F17

Dongu: 01

Baslik: Release build keystore yoksa debug imzaya dusuyor

Kategori: Release guard

Oncelik puani: 68

Siddet: P2

Kanit:
- `app/build.gradle.kts:67-73` keystore yoksa release build debug signing ile aliniyor.

Etkilenen dosya/satir:
- `app/build.gradle.kts:67-73`

Neden gercek sorun:
- Play'e yuklenecek artifact yanlis imzali uretilirse release sureci bozulur.

Kullaniciya etkisi:
- Yayin gecikmesi.

Onerilen cozum:
- Play/CI release gorevlerinde keystore zorunlu; local test icin ayri flag.

Nereye uygulanacak:
- `app/build.gradle.kts`, release scripts.

Test plani:
- Keystore yokken `assembleRelease` beklenen sekilde fail veya local-only mode verir.

Risk:
- Orta.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F18

Dongu: 30

Baslik: Eski security audit dokumani current code ile drift olmus

Kategori: Dokumantasyon

Oncelik puani: 35

Siddet: P3

Kanit:
- `docs/internal/security_audit_2026-07-13.md:155` Timber DebugTree'nin kosulsuz oldugunu soyluyor.
- `AppOrganizerApp.kt:27-30` current code DEBUG guard kullaniyor.

Etkilenen dosya/satir:
- `docs/internal/security_audit_2026-07-13.md:155`
- `AppOrganizerApp.kt:27-30`

Neden gercek sorun:
- Ajanlar ve insan denetciler eski bulguyu aktif sanabilir.

Kullaniciya etkisi:
- Yanlis is planlama.

Onerilen cozum:
- Eski audit dokumanina "stale/cozuldu" etiketi ekle veya HISTORY'ye tasinmis kapanis bolumu olustur.

Nereye uygulanacak:
- `docs/internal/security_audit_2026-07-13.md`, `HISTORY.md`

Test plani:
- Dokuman grep kontrolu.

Risk:
- Dusuk.

Durum: Kapandi (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F20

Dongu: 11 / 28 / 29

Baslik: Tablet ve klasor gecisleri icin runtime smoke eksik

Kategori: Adaptive layout / test boslugu

Oncelik puani: 62

Siddet: P2

Kanit:
- `HISTORY.md:95` daha once tablet benzeri smoke yapildigini gosteriyor, ancak son klasor gecis/restore lifecycle degisiklikleri icin yeni cihaz kaniti yok.
- `FolderScreen.kt:143-155` transition offset animasyonu var.
- `FolderScreen.kt:616` nav alpha offset'e bagli.

Etkilenen dosya/satir:
- `FolderScreen.kt:143-155`, `FolderScreen.kt:616`
- `HISTORY.md:95`

Neden gercek sorun:
- Build basarisi tablet/landscape overlap veya animasyon bindirme hatasini kanitlamaz.

Kullaniciya etkisi:
- Tablet kullanicilarinda klasor gecisi veya arama paneli ust uste binebilir.

Onerilen cozum:
- 10 inch landscape emulator smoke: klasor swipes, bottom/middle/top fihrist, All Apps search focus.

Nereye uygulanacak:
- Test/artifact akisi; kod degisikligi smoke sonucuna gore.

Test plani:
- `adb shell wm size 1280x800`, `wm density 160`, screenshot kanitlari.

Risk:
- Orta.

Durum: Kismi runtime risk (bkz. 0. 2026-07-14 Cozum Kapanis Ozeti)

### F21

Dongu: 25

Baslik: widget_prefs.xml cloud backup/device-transfer disinda tutulmuyor -> restore sonrasi gecersiz widget ID hayalet slot birakiyor

Kategori: Widget sistemi / Backup-Restore

Oncelik puani: 58

Siddet: P2

Kanit:
- `app/src/main/res/xml/data_extraction_rules.xml` — `<include domain="sharedpref" path="." />` tum sharedpref dosyalarini (widget_prefs dahil) hem `cloud-backup` hem `device-transfer` bloklarina dahil ediyor; sadece `deepseek_prefs.xml` ve Room DB dosyalari `exclude` edilmis, `widget_prefs.xml` icin exclude yok.
- `app/src/main/res/xml/backup_rules.xml` (eski `fullBackupContent`) ayni sekilde `sharedpref path="."` dahil, widget_prefs icin exclude yok.
- `WidgetPrefs.kt:5-19` widget ID listesini `"widget_prefs"` adli SharedPreferences'e duz CSV string olarak yaziyor (`KEY_WIDGET_IDS`) — bu dosya Android Auto Backup/Cihazlar Arasi Aktarim ile tasiniyor.
- `LauncherViewModel.kt:569-571` `loadWidgetIds()` prefs'ten okunan ID'leri dogrudan `_widgetIds` state'ine yaziyor; `AppWidgetManager.getAppWidgetInfo(id)` ile gecerlilik kontrolu YOK.
- `WidgetArea.kt:133-145` (`WidgetCard`) `WidgetHostManager.createView` null donerse (`WidgetHostManager.kt:39-43`, info null oldugunda) `hostView` null kalir ve `hostView?.let { ... }` (satir 147) hicbir sey render etmez — kaldirma butonu da bu blogun icinde oldugu icin gosterilmez.

Neden gercek sorun:
- Widget ID'leri host+cihaza ozeldir; sistem tarafindan atanir ve baska bir cihaza (ya da ayni cihazda `AppWidgetHost` yeniden olusturuldugunda, ornegin veri temizleme/yeniden kurulum sonrasi) tasindiginda geçersizdir — bu proje genelinde zaten biliniyor (Room DB gizlilik nedeniyle backup'tan haric tutulmus), ama widget_prefs.xml icin ayni ihtiyat uygulanmamis.
- Restore sonrasi `WidgetPrefs` gecersiz ID'lerle dolu gelir, `loadWidgetIds()` bunlari dogrulamadan state'e yukler, `WidgetArea` bu ID'ler icin bos/gorunmez bir kart uretir (crash yok ama kalici "hayalet" bosluk).
- Kullanici bu hayalet slotu KUI'dan silemez cunku kaldirma butonu sadece `hostView` non-null oldugunda gorunur (satir 183-215) — kullanici manuel olarak uygulama verisini temizlemek zorunda kalir.

Kullaniciya etkisi:
- Telefon degistirme / yedekten geri yukleme / uygulama verisini temizleyip yeniden kurma sonrasi widget alaninda bos, tiklanamaz, silinemez bosluklar kalir; kullanici bunu bug sanip destek talebi acabilir veya App Organizer'i "bozuk" olarak degerlendirebilir.

Onerilen cozum:
- `data_extraction_rules.xml` ve `backup_rules.xml`'e `<exclude domain="sharedpref" path="widget_prefs.xml" />` ekle (Room DB icin uygulanan gizlilik/tasima haric tutma paterni ile ayni).
- `LauncherViewModel.loadWidgetIds()` icine gecerlilik filtresi ekle: `AppWidgetManager.getAppWidgetInfo(id) == null` olan ID'leri otomatik `WidgetPrefs.removeWidgetId` ile temizle (defensive cleanup, restore haric tutma eklenmemis eski kurulumlar icin de fayda saglar).

Nereye uygulanacak:
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/LauncherViewModel.kt:569-571`

Test plani:
- `adb shell bmgr backupnow com.armutlu.apporganizer` + veri temizleme + restore, widget alaninin bos/hayalet slot icermedigini dogrula.
- Birim test: sahte gecersiz ID icin `loadWidgetIds()` sonrasi `WidgetPrefs.getWidgetIds()` bos donmeli.

Risk:
- Dusuk (sadece manifest/xml + defensive filtre; davranissal regresyon riski yok).

Durum: Kapandi (2026-07-14) — `data_extraction_rules.xml` ve `backup_rules.xml`'e `widget_prefs.xml` exclude eklendi; `LauncherViewModel.loadWidgetIds()` artik `AppWidgetManager.getAppWidgetInfo()` ile gecerlilik kontrolu yapip gecersiz ID'leri `WidgetPrefs`'ten otomatik temizliyor. `compileDebugKotlin`, `testDebugUnitTest` ve `assembleDebug -PskipGoogleServices` basarili. versionCode 40->41, versionName 1.3.17->1.3.18.

### F22

Dongu: 25

Baslik: widgetConfigureLauncher sonucunda EXTRA_APPWIDGET_ID eksikse widget ne eklenir ne de host ID serbest birakilir

Kategori: Widget sistemi / Kaynak sizintisi

Oncelik puani: 38

Siddet: P3

Kanit:
- `LauncherActivity.kt:79-90` (`widgetConfigureLauncher` callback) `widgetId`'yi sadece `result.data?.getIntExtra(EXTRA_APPWIDGET_ID, INVALID)` ile okuyor. `result.resultCode == RESULT_OK && widgetId != INVALID` dogruysa `addWidgetId`, `widgetId != INVALID` (RESULT_CANCELED durumunda) dogruysa `deleteId` cagriliyor.
- Configure aktivitesi `RESULT_OK` ile donup sonuc Intent'ine `EXTRA_APPWIDGET_ID` eklemezse (bu extra'yi geri kopyalamak widget yazarinin sorumlulugu, zorunlu degil), `widgetId` `INVALID_APPWIDGET_ID` kalir; hem `if` hem `else if` kosulu false olur — ne `addWidgetId` ne `deleteId` cagrilir.

Neden gercek sorun:
- Bu durumda: widget sistemde bind edilmis (`AppWidgetManager`de gecerli) ama `WidgetPrefs`e hic eklenmemis olur — kullaniciya gorunmez, host ID de asla `deleteAppWidgetId` ile serbest birakilmaz (sessiz kaynak sizintisi). Kullanicinin "widget ekledim ama gorunmuyor" sikayeti aciklanamaz kalir.

Kullaniciya etkisi:
- Nadir (bazi 3. parti widget'larin configure ekrani standart disi davranirsa) ama olustugunda kullanici widget'i tekrar tekrar eklemeyi dener, her denemede bir host ID daha sizar; kullaniciya gorunen tek belirti "widget eklenmiyor" hissi.

Onerilen cozum:
- `pendingWidgetId`'yi `widgetConfigureLauncher` cagrilmadan once class-level bir degiskene de ata (ör. `pendingConfigureWidgetId`), callback icinde `result.data` extra'sini bulamazsa bu fallback ID'yi kullan.

Nereye uygulanacak:
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/LauncherActivity.kt:78-90`

Test plani:
- Sahte configure Activity ile RESULT_OK donup extra eklemeyen senaryoyu instrumentation testinde simule et, widget ID'nin ya eklendigini ya da serbest birakildigini dogrula.

Risk:
- Dusuk (kucuk callback degisikligi, mevcut mutlu yolu etkilemiyor).

Durum: Acik (belirsizlik yuksek — cogu widget configure aktivitesi bu extra'yi standart olarak geri dondurur, bu yuzden P3)

## 6. P0/P1 Acil Is Listesi

P0: Yok.

P1: Yok.

Kapanan P0/P1:
- Gizlilik metni tutarsizligi: kapandi.
- Gizlilik resetinin bildirim gecmisini temizlememesi: kapandi.
- Backup pref kapsam eksigi: kapandi.
- Restore sonrasi search source lifecycle eksigi: kod seviyesinde kapandi, runtime smoke bekliyor.
- F12 Android 13+ dosya arama izin/model uyumsuzlugu: kapandi.

## 7. P2/P3 Planli Is Listesi

P2: F21 — Kapandi (2026-07-14).

P3: F22 widgetConfigureLauncher sonucunda EXTRA_APPWIDGET_ID eksikse host ID sizintisi. Acik, dusuk oncelik.

Kismi runtime risk:
- F20 tablet/adaptive smoke: launch + fatal-log + screenshot kanitlandi; detayli klasor swipe gorsel QA acik risk.

## 8. Yanlis Pozitif / Dogrulanamayanlar

| Iddia | Guncel karar |
|---|---|
| Gizlilik merkezi internete veri yok diyor | Artik kapandi; metin Firebase/DeepSeek/online DB beyanina dondu. |
| Reset notification_events temizlemiyor | Artik kapandi; `NotificationEventDao.clearAll()` ve ViewModel cagrisi var. |
| Backup missions/searchShine/autoFolderColor/biometric/quickWheel/focus kapsamiyor | Artik kapandi; BackupManager v5 export/import ekledi. |
| Restore search source worker/observer hic senkronlamiyor | Kod seviyesi kapandi; runtime smoke eksik. |
| Timber release'te kosulsuz DebugTree | Yanlis/stale; current code DEBUG guard kullaniyor. |

## 9. Eksik Test Matrisi

| Test | Hedef | Durum |
|---|---|---|
| Android 13+ file search smoke | F12 | Kod + manifest + package permission dump tamamlandi; real file result smoke acik risk degil, ek QA adayi |
| SQLite LIKE fallback `%/_/\` | F10 | Tamamlandi: TurkishSearchTest |
| Folder dock item settings render | F08 | Kod/compile tamamlandi; manuel UI smoke ek QA adayi |
| Backup restore source lifecycle instrumentation | Restore worker/observer | Eksik |
| Tablet landscape folder transition smoke | F20 | Kismi: tablet launch + screenshot + fatal log temiz; detayli swipe gorsel QA eksik |
| VersionName About UI smoke | F04 | Compile tamamlandi; manuel UI smoke ek QA adayi |
| Preference restore stale state smoke | F15 | Kod/compile tamamlandi; restore instrumentation ek QA adayi |
| Widget restore sonrasi hayalet slot smoke | F21 | Eksik — `bmgr backupnow` + veri temizleme + restore senaryosu manuel dogrulanmali |
| Widget configure sonucu eksik extra simulasyonu | F22 | Eksik — sahte configure Activity instrumentation testi yok |

## 10. Onerilen Duzeltme Sirasi

1. Tamamlandi: F12 dosya arama izin modeli.
2. Tamamlandi: F10 LIKE ESCAPE.
3. Tamamlandi: F08 folder dock item render.
4. Tamamlandi: F04 versionName UI.
5. Kismi tamamlandi: F20 tablet smoke.
6. Tamamlandi: F15 ortak preference state helper.

## 11. Sonraki Ajan Icin Devam Promptu

```text
C:\Users\huseyinekizoglu\android-folderautomanager reposunda docs/internal/sistem_denetim_30_dongu_2026-07-14.md raporundaki guncel acik bulgulari sirayla ele al.

Kod duzeltme sirasinda:
1. Once F12 icin Android 13+ emulator davranisini kanitla; dogrulamadan manifest izni ekleme.
2. Sonra F10 LIKE ESCAPE fixini yap ve SQLite/targeted test ekle.
3. F08 folder dock item render fixini yap.
4. F04 About versionName fixini yap.
5. Her fix sonrasi en kucuk testi calistir.
6. En sonda compileDebugKotlin ve gerekiyorsa assembleDebug al.
7. Test edilemeyen runtime maddeleri HISTORY ve raporda acik risk olarak birak.
```
