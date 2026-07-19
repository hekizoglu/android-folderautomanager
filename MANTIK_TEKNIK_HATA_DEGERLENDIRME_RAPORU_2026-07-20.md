# AppOrganizer Mantik ve Teknik Hata Degerlendirme Raporu

Tarih: 2026-07-20

Kapsam: Saglik raporu mantigi, WorkManager dosya indeksleme, one-shot worker yorumu, Dijital Yasam saglik modeli, uygulama arama/siniflandirma sinyalleri, uygulama baslatma davranisi ve yayin oncesi riskler.

## Cozum Durumu

- [x] One-shot worker icin `work=yok` durumunun tek basina hata sayilmamasi kod/test tarafinda mevcut: `NORMAL`, `NORMAL_TAMAMLANDI`, `NORMAL_CALISIYOR`, `WARN_GECIKMIS` ayrimi var.
- [x] Dosya indeksleme health raporuna `eligible` ayrimi eklendi; izin yokken `enabled=evet, work=yok` yanlis periodic missing bulgusu uretmeyecek.
- [x] Backup/restore sonrasi `FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled(context)` cagrisi garanti altina alindi.
- [x] `appFileName` backfill akisi eklendi; reconcile mevcut kayitlarda dosya adini metadata olarak guncelliyor.
- [x] Room 18 -> 19 migration testi eklendi; `appFileName` sutunu ve index dogrulaniyor.
- [ ] Health snapshot testleri periodic permission, one-shot completed ve digital life stale/low confidence senaryolariyla genisletilecek.

## Arastirma Ozeti

- Android WorkManager resmi dokumani, tekrar eden islerin `enqueueUniquePeriodicWork()` ile benzersiz isim uzerinden yonetilmesini onerir. Bu, ayni isin birden fazla kez planlanmasini engeller ve scheduler/health reporter tarafinda tek isim kullanmayi gerekli kilar.
- WorkManager one-time ve periodic isleri ayridir. One-shot is basariyla tamamlandiktan sonra aktif is listesinde gorunmemesi tek basina hata sayilmamalidir; yorum son talep, son basari ve aktif bekleyen is durumuyla birlikte yapilmalidir.
- Room resmi dokumani, sema degisikliginde migration yazilmasini ve `exportSchema=true` iken JSON semalarin versiyon kontrolunde tutulmasini onerir.
- Android background activity launch kisitlari, uygulamanin arka plandan baska uygulama acmasini UX ve guvenlik riski olarak sinirlar. AppOrganizer icin uygulama baslatma yalniz kullanici etkilesimiyle kalmalidir.

## Genel Sonuc

Bu surum yayin oncesi "calisir durumda, ancak saglik modeli ve release kapilari izlenmeli" seviyesindedir.

Mevcut kodda onceki ana sikayetlerin bir kismi giderilmis gorunuyor:

- Dosya indeksleme periodic work adi tek sabite alinmis: `FILES_INDEX_PERIODIC_WORK_NAME = "files_index_periodic_v1"`.
- Uygulama baslangicinda `FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled()` cagrisi var.
- Uygulama guncellemesi sonrasi `AppUpdateReceiver` ayni coordinator'i tetikliyor.
- Dijital Yasam health modeli `STALE` ve `LOW` confidence durumlarini uyarilara ceviriyor.
- Arama ve siniflandirma artik `appFileName` sinyalini kullanabiliyor.

Yine de yayin oncesi asagidaki maddeler netlestirilmeden "tam saglikli" denmemeli.

## 1. Dosya Indeksleme Scheduler Durumu

Durum: Kismen duzeltilmis.

Kod bulgusu:

- `FilesIndexWorker.FILES_INDEX_PERIODIC_WORK_NAME = "files_index_periodic_v1"`.
- `FilesIndexWorker.schedule()` `enqueueUniquePeriodicWork(... ExistingPeriodicWorkPolicy.UPDATE ...)` kullaniyor.
- `FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled()` unique work adiyla mevcut isleri okuyor.
- `ENQUEUED`, `RUNNING`, `BLOCKED` varsa dokunmuyor.
- `CANCELLED` veya `FAILED` gorurse telemetry failure sebebi yazip yeniden planliyor.
- Kaynak kapaliysa veya MediaStore izni yoksa worker iptal ediliyor.

Mantik degerlendirmesi:

Bu tasarim dogru yonde. Onceki "enabled=evet, work=yok" bulgusu icin beklenen self-healing davranis artik kodda var. Ancak iki risk kaliyor:

- Health reporter `enabled=evet` kontrolunu yalniz preference uzerinden yapiyor; coordinator ise `filesIndexEnabled && FilesIndexer.hasMediaStoreReadAccess()` kosuluyla karar veriyor. Izin yokken health "enabled=evet, work=yok" diyebilir ve bu teknik olarak dogru ama kullanici icin eksik aciklanmis olur.
- Restore sonrasi backup akisinda coordinator cagrisi her yerde garanti degil. App startup bunu toparlar, ama restore tamamlandigi anda rapor uretilirse kisa sureli yalanci uyari cikabilir.

Olmasi gereken:

- Health satiri iki alanli olmali: `preferenceEnabled` ve `schedulerEligible`.
- Ornek: `Files index periodic: pref=evet, eligible=hayir(permission), work=yok, durum=BLOCKED_PERMISSION`.
- Restore tamamlandiktan sonra `FilesIndexWorkCoordinator.ensurePeriodicWorkScheduled(context)` dogrudan cagrilmali.
- Telemetry sebep kodlari rapora ham durum olarak yansimali: `REPLAN_CANCELLED_WORK`, `REPLAN_FAILED_WORK`, `BLOCKED_PERMISSION`.

Oncelik: Yuksek, ama mevcut kod release blocker degilse de health dogrulugu icin kapatilmali.

## 2. One-Shot Worker Saglik Mantigi

Durum: Mantik riski devam ediyor.

Kod bulgusu:

- Diagnostics worker listesinde `Files index one-shot`, `enabled = AppPrefs.isSearchSourceFilesEnabled(context)` olarak tanimli.
- One-shot work unique adi: `files_index_once`.
- One-shot basariyla bittiginde aktif WorkManager listesinde gorunmemesi normaldir.

Mantik hatasi:

One-shot is icin kalici `enabled=true` kavrami yanlis yorum uretebilir. One-shot bir ozellik degil, bir taleptir. Talep yoksa work yok olmasi normaldir.

Olmasi gereken durum matrisi:

| Durum | Saglik sonucu |
|---|---|
| Talep yok + work yok | NORMAL_IDLE |
| Son talep var + work ENQUEUED/RUNNING | NORMAL_RUNNING |
| Son talep var + work yok + son basari yeni | NORMAL_COMPLETED |
| Son talep var + uzun suredir ENQUEUED | WARN_DELAYED |
| FAILED | ERROR |
| CANCELLED + talep hala gerekli | ERROR |

Gereken veri modeli:

- `requested`
- `pending`
- `lastRequestedAt`
- `lastStartedAt`
- `lastCompletedAt`
- `lastResult`
- `lastFailureCode`

Oncelik: Yuksek. Bu, saglik raporunun guvenilirligini dogrudan etkiler.

## 3. Dijital Yasam Saglik Modeli

Durum: Buyuk olcude duzeltilmis.

Kod bulgusu:

- `HomeIntelligenceHealthReport.buildDigitalLifeSection()` kaynak durumunu `READY`, `STALE`, `MISSING`, `FAILED` olarak ayiriyor.
- `freshness == STALE || UNAVAILABLE` ise `DIGITAL_LIFE_DATA_STALE` ekliyor.
- `confidence == LOW` ise `DIGITAL_LIFE_LOW_CONFIDENCE` ekliyor.
- Tum uyarilar non-fatal ise overall status `DEGRADED_WARN`.

Degerlendirme:

Onceki "Skor 66, Confidence LOW, Veri tazeligi STALE, ama uyari yok" problemi kod seviyesinde giderilmis gorunuyor. Beklenen sonuc artik:

- Dijital Yasam: `DEGRADED`
- Warning codes: `DIGITAL_LIFE_DATA_STALE`, `DIGITAL_LIFE_LOW_CONFIDENCE`
- Overall: `DEGRADED_WARN`

Kalan risk:

- Rapor metninde "Kaynak durumu: READY" ile "Veri tazeligi: STALE" ayni anda gorunebilir. Teknik olarak kaynak erisilebilir ama veri bayat anlamina gelir; kullanici bunu "her sey hazir" gibi okuyabilir.

Olmasi gereken:

- Rapor satiri ayrilmali:
  - `Kaynak erisimi: READY`
  - `Veri tazeligi: STALE`
  - `Saglik durumu: DEGRADED`
- Ana ekranda skor kesin deger gibi sunulmamali:
  - Iyi: `Dijital Yasam: 66 - veri eski, dusuk guven`
  - Riskli: `Dijital yasam skoru: 66`

Oncelik: Orta-yuksek.

## 4. Uygulama Arama Kalitesi

Durum: Duzeltilmis, regresyon testleri var.

Kod bulgusu:

- `AppInfo` modeline `appFileName` eklendi.
- `PackageManagerHelper` `ApplicationInfo.publicSourceDir/sourceDir` uzerinden APK dosya adini aliyor.
- `SearchCache` arama metnini `appName + packageName + appFileName` olarak kuruyor.
- Compact arama metni sayesinde `remote desktop` sorgusu `chromeremotedesktop` / `remote-desktop` gibi varyasyonlari yakalayabiliyor.
- `AppDao` sorgulari `appName`, `packageName`, `appFileName` alanlarini ariyor.
- `SearchIndexer` app dokumanlarina kategori, paket ve dosya adi alias'larini ekliyor.
- Test: `SearchCacheTest` remote desktop senaryosunu kapsiyor.

Degerlendirme:

Kullanici ornegi olan "Uzak Masaustu" gorunen adina karsilik "remote desktop" teknik adiyla arama hedefi artik karsilaniyor.

Kalan risk:

- `appFileName` mevcut cihazlarda migration sonrasi bos gelebilir; ancak `packageName` sinyali yine aramaya giriyor.
- Tam backfill icin uygulama reconcile akisi mevcut uygulamalarin `appFileName` alanini guncellemelidir.

Olmasi gereken:

- Uygulama listesi reconcile edilirken mevcut kayitlarda `appFileName` bos ise PackageManager'dan doldurulmali.
- Search ranking'de tam dosya adi/compact paket eslesmesi, fuzzy appName eslesmesinden daha yuksek puan almali.
- Kullaniciya ham paket/dosya adi gosterilmemeli; yalniz arama sinyali olarak kullanilmali.

Oncelik: Orta.

## 5. Siniflandirma Kalitesi

Durum: Duzeltilmis, ancak backfill ve conflict izleme gerekir.

Kod bulgusu:

- `AppClassifier` artik `appFileName` keyword kararini da kullaniyor.
- Paket/dosya adi sinyali `PACKAGE_NAME_KEYWORD` confidence ile degerlendiriliyor.
- Keyword motoru ilk kategoriye takilmak yerine daha uzun/anlamli eslesmeyi seciyor.
- Test: `remote-desktop` dosya adi utilities kategorisine dusuyor.

Degerlendirme:

Bu, yerellestirilmis uygulama adlarinda onemli bir kalite artisidir. Ancak keyword veritabani genis oldugu icin kategori cakismalari hala olabilir.

Olmasi gereken:

- Conflict telemetry: `appNameCategory`, `packageCategory`, `fileNameCategory`, `chosenCategory`.
- Low-confidence inceleme listesinde "dosya adindan siniflandirildi" nedeni ayrica gorunmeli.
- Kategori duzeltmesi yapildiginda benzer paket/dosya adi sinyallerine sahip uygulamalar icin oneriler uretilmeli.

Oncelik: Orta.

## 6. Uygulama Otomatik Baslatma

Durum: Arka plandan otomatik baslatma gerekli degil; eklenmemeli.

Kod bulgusu:

- `launchApp` cagrilari UI etkilesimlerinden geliyor: ana ekran, klasor, arama, bildirim tap/swipe gibi kullanici aksiyonlari.
- Background receiver/service icinden genel amacli otomatik app launch gereksinimi gorulmedi.

Arastirma degerlendirmesi:

Android background activity launch kisitlari nedeniyle kullanici etkilesimi olmadan app acmak hem guvenlik hem Play Store/UX riski tasir.

Olmasi gereken:

- AppOrganizer yalniz kullanici aksiyonuyla uygulama baslatmali.
- Otomatik baslatma gerekiyorsa dogrudan activity acmak yerine bildirim/CTA kullanilmali.
- Health raporunda "auto-launch kapali" bir hata degil, beklenen privacy-safe davranis sayilmali.

Oncelik: Dusuk. Ek is yapilmamali.

## 7. Room Migration ve Veri Tutarliligi

Durum: Teknik olarak dogru, backfill eksigi var.

Kod bulgusu:

- DB version `19`.
- `MIGRATION_18_19` `apps.appFileName` sutununu ekliyor.
- `index_apps_appFileName` index'i var.
- `app/schemas/.../19.json` uretilmis.

Degerlendirme:

Room migration yaklasimi dogru. Ancak migration yalniz semayi degistirir; mevcut satirlarin dosya adini kendiliginden dolduramaz.

Olmasi gereken:

- Ilk app reconcile sonrasi `appFileName == ""` olan mevcut app kayitlari backfill edilmeli.
- Migration testine 18 -> 19 dogrulamasi eklenmeli:
  - sutun var
  - index var
  - eski veri kaybolmuyor
  - default bos string

Oncelik: Orta.

## 8. Raporlama Dili ve Severity Modeli

Durum: Daha iyi, ama semantik netlik artirilmali.

Mevcut sorun tipi:

- "enabled=evet, work=yok" gibi satirlar teknik dogru olsa bile kullanici icin "hata" gibi okunabiliyor.
- "Kaynak durumu READY" ile "veri STALE" birlikte gelince rapor iki farkli katmani karistiriyor.

Olmasi gereken severity modeli:

| Severity | Anlam |
|---|---|
| OK | Sistem beklenen durumda |
| NORMAL_IDLE | Talep yok, is yok |
| NORMAL_COMPLETED | One-shot basariyla bitmis |
| WARN | Calisir ama izlenmeli |
| DEGRADED_WARN | Kullanici akisi calisir, veri/guven/scheduler eksigi var |
| ERROR | Kullanici vaadi bozuluyor |
| BLOCKED_PERMISSION | Izin nedeniyle beklenen is yapilamiyor |

Oncelik: Yuksek, cunku release oncesi raporun yanlis alarm uretmesi kararlari bulandirir.

## Yayin Oncesi Kontrol Listesi

- Dosya indeksleme acikken temiz kurulumda periodic work var mi?
- Uygulama update senaryosunda `files_index_periodic_v1` yeniden garanti ediliyor mu?
- Izin kapaliyken rapor `BLOCKED_PERMISSION` diyor mu?
- One-shot basarili tamamlaninca `work=yok` hata sayilmiyor mu?
- Dijital Yasam `LOW + STALE` durumunda `DEGRADED_WARN` ve iki warning code uretiyor mu?
- `remote desktop` aramasi "Uzak Masaustu" / Chrome Remote Desktop gibi uygulamalari buluyor mu?
- Dosya adindan siniflandirma localize isimlerde dogru kategoriye katki yapiyor mu?
- Mevcut kullanici DB'sinde `appFileName` backfill calisiyor mu?
- Arka plandan otomatik uygulama baslatma yok mu?
- Room 18 -> 19 migration testi geciyor mu?

## Onerilen Is Sirasi

1. [x] One-shot worker health modelini `requested/pending/completed` durumlarina ayir.
2. [x] Health raporunda `preferenceEnabled`, `schedulerEligible`, `permissionState` alanlarini ayir.
3. [x] Restore tamamlandiktan sonra files index coordinator'i tetikle.
4. [x] `appFileName` backfill icin reconcile/update akisini ekle.
5. [x] Room migration 18 -> 19 testi ekle.
6. Health raporu snapshot testlerine su senaryolari ekle:
   - periodic enabled + permission yok
   - one-shot completed + work yok
   - digital life stale + low confidence
7. Release smoke'ta bu maddeleri gercek cihaz raporuyla kanitla.

## Kisa Karar

Yayin icin en kritik risk artik crash/kararlilik degil, saglik raporunun yanlis yorum uretmesi ve mevcut DB'lerde yeni arama sinyalinin tam backfill edilmemesi. Uygulama baslatma tarafinda ek otomatik launch yapilmamali; mevcut user-initiated launch modeli dogru.
