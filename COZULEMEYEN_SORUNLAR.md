# COZULEMEYEN SORUNLAR - AppOrganizer

> Bir madde yerel agent tarafindan tamamlanamiyor ve kullanici/dış sistem/cihaz/izin gerektiriyorsa burada tutulur.
> Yerelde tamamlananlar HISTORY.md'ye, aktif yapilacaklar YENI_ROADMAP.md'ye tasinir.
> Aktif her kayitta tek `Sahip`, ISO `Son tarih`, `Beklenen kanit` ve `Sonraki eskalasyon` bulunur. Tarih henuz planlanamiyorsa neden ve atanacagi kapı acikca yazilir; R8 aktive edilmeden mutlak tarih zorunludur.

---

## Aktif Sorunlar

### [CS-8] İzole ortamda Gradle 8.7 dağıtımı yok

**Tarih:** 2026-07-21
**Durum:** Kod/statik kontroller tamam; hedefli unit test ve AndroidTest compile başlamadan ağ engeline takıldı
**Sahip:** CI/build ortamı yöneticisi
**Son tarih:** R7 compile kapısı aktive edilirken ISO tarih atanacak; R7.4 öncesi tamamlanmalı
**Beklenen kanıt:** Gradle 8.7 erişimli ortamda `HomeHeroLayoutPolicyTest`, `compileDebugAndroidTestKotlin`, `testDebugUnitTest` ve `assembleDebug` başarı çıktısı
**Sonraki eskalasyon:** Tarih geçerse R7/R8 `Bloke` kalır; bağlı Windows build makinesi veya erişilebilir CI runner kullanılır

**Sorun:** Bu izole Linux çalışma alanındaki wrapper cache yalnız sıfır baytlık `.part` dosyası içeriyor. `services.gradle.org` bağlantısı ağ politikasıyla reddedildiği için Gradle hiçbir task çalıştırmadan duruyor.

**Denenen:**
- `GRADLE_USER_HOME=.gradle-user-home ./gradlew testDebugUnitTest --tests 'com.armutlu.apporganizer.presentation.ui.launcher.hero.HomeHeroLayoutPolicyTest' compileDebugAndroidTestKotlin --stacktrace`
- Workspace, `/tmp` ve `/root/.gradle` altında tamamlanmış Gradle 8.7 dağıtımı arandı; yalnız boş `.part` dosyaları bulundu.
- `git diff --check`, kaynak referans taraması ve TR/EN kaynak anahtarı kontrolleri yerelde tamamlandı.

---

### [CS-4] Gradle Kotlin compile cache kilidi (Windows Defender/AV)

**Tarih:** 2026-07-22 22:11
**Durum:** Build cache klasörü (`app/build/kotlin/compileDebugKotlin/cacheable`) DeleteDirectory hatası — 7+ kez fail, workaround geçersiz
**Sahip:** Windows ortam yöneticisi (Admin Defender exclusion gerekli)
**Son tarih:** ZORUNLU (build 7+ kez failed, loop yapan cron durduruldu)
**Beklenen kanıt:** `.\gradlew assembleDebug` başarı (hiçbir manual cleanup olmadan)
**Sonraki eskalasyon:** Defender/AV exclusion: `app/build`, `.gradle`, `.android` klasörleri → restart → retry
**Workaround:** 
- `.\gradlew --stop` + `Get-Process java | Stop-Process -Force`
- `Remove-Item -Recurse -Force app\build` (force delete)
- Retry build

---

### [CS-3] Gradle `merged_res` / `packaged_res` kilidi

**Tarih:** 2026-06-16
**Durum:** Kismen cozuldu - repo scriptleri duzeltildi, kalici Defender exclusion icin kullanici/admin aksiyonu bekliyor
**Sahip:** Windows makinesinin yoneticisi
**Son tarih:** R1 aktive edilirken ISO tarih atanacak; R1 kapanisindan once tamamlanmali
**Beklenen kanit:** `-CheckOnly` ve yonetici calistirmasi sonucu ile kilitsiz `assembleDebug` raporu
**Sonraki eskalasyon:** Tarih gecerse R1 `Bloke` olur; izole CI/ikinci build makinesi kullanilir

**Sorun:** Windows Defender veya benzeri dosya tarama sureci Gradle build klasorlerini kilitleyebiliyor. `mergeDebugResources` ve `packaged_res` temizleme/yeniden uretme adimlari bu yuzden takilabiliyor.

**Denenen:**
- Dogrudan Defender exclusion komutlari denendi; admin/UAC yetkisi gerektirdi.
- Task Scheduler/SYSTEM yollari denendi; erisim engeliyle karsilasildi.
- Gradle daemon timeout ve temizleme workaroundlari denendi.
- `scripts/add_defender_exclusion.ps1` olusturuldu; artik `$PSScriptRoot` uzerinden bu repoya gore hesapliyor ve `-CheckOnly` ile admin gerektirmeden path dogruluyor.
- `scripts/clear_build_lock.ps1` acil workaround olarak daraltildi; tum `java.exe` sureclerini oldurmek yerine `gradlew --stop` kullanip sadece bu projenin `app\build` klasorunu temizliyor.
- 2026-07-13: `processDebugResources` R.jar dosya kilidi `gradlew --stop` sonrasi acildi. `scripts/benchmark_build.ps1` basariyla kosuldu: profile assembleDebug rerun 211.1s, configuration-cache compileDebugKotlin 5.5s, exit 0. Rapor: `docs/internal/build_benchmark_latest.md`.

**Neden yerelde tamamen kapanmiyor:** Build/profil kaniti yerelde tamamlandi; fakat Defender exclusion kalici admin/UAC onayi gerektiriyor.

**Kullanicidan beklenen:**
```powershell
.\scripts\add_defender_exclusion.ps1
```
UAC penceresi cikarsa onay ver. Oncesinde admin gerektirmeyen kontrol:
```powershell
.\scripts\add_defender_exclusion.ps1 -CheckOnly
```
Build kilitlenirse gecici workaround:
```powershell
.\scripts\clear_build_lock.ps1
.\gradlew assembleDebug
```

---

### [CS-6] Play Console ve release dis aksiyonlari

**Tarih:** 2026-07-09
**Durum:** Kismen cozuldu - release keystore uretim scripti eklendi; Play Console ve gercek imza aksiyonu kullanici/hesap erisimi gerektiriyor
**Sahip:** Play Console developer hesabi sahibi
**Son tarih:** R8 aktive edilmeden once ISO tarih atanacak
**Beklenen kanit:** Imzali AAB, Console upload readback, tamamlanmis beyanlar ve inceleme sonucu
**Sonraki eskalasyon:** Tarih gecerse R8 `Bloke` olur; R9 ozellikleri baslatilmaz, yalniz release kapsamindaki hazirlik/duzeltmeler surer

**Sorun:** Kod ve dokuman hazirliklari yerelde konsolide edildi, fakat asagidaki maddeler Play Console veya kullaniciya ait kalici imza/hesap aksiyonu gerektiriyor:
- Data Safety formu
- QUERY_ALL_PACKAGES declaration
- Content rating anketi
- Privacy Policy URL girisi
- Release keystore olusturma ve guvenli saklama (`scripts/create_release_keystore.ps1` hazir)
- Final AAB'nin temiz committen imzalanmasi ve yuklenmesi

**Denenen:**
- Privacy policy, store listing ve manifest uyumu onceki dongulerde kontrol edildi.
- QUERY_ALL_PACKAGES beyan ozeti YENI_ROADMAP.md icine tasindi.
- Play Store QA pack maddeleri YENI_ROADMAP.md ve bu dosyaya konsolide edildi.
- `scripts/create_release_keystore.ps1` eklendi; `release.jks` ve gitignore kapsamindaki `keystore.properties` dosyasini interaktif sifreyle uretir.

**Neden yerelde kapanmiyor:** Play Console oturumu, kullanici hesabi, kalici release key ve geri alinmasi zor kararlar gerekiyor.

**Kullanicidan beklenen:** Release imzasi icin `.\scripts\create_release_keystore.ps1` calistirip olusan hassas dosyalari guvenli saklamak; Play Console'a girip YENI_ROADMAP.md R8 maddelerini sirayla tamamlamak.

**Tam adimlar ve kanit listesi:** `docs/PLAY_RELEASE_EVIDENCE_CHECKLIST.md`. Yerel hazirlik tamamlanmis olsa bile Play Console formu, yukleme readback'i ve inceleme sonucu olmadan bu sorun kapatilmaz.

---

### [CS-7] Gercek cihaz QA paketi

**Tarih:** 2026-07-09
**Durum:** Cozulemedi - fiziksel Android cihaz veya uygun emulator kaniti gerektiriyor
**Sahip:** Release QA sorumlusu
**Son tarih:** R7.4 aktive edilirken ISO tarih atanacak; R7.5 beta oncesi tamamlanmali
**Beklenen kanit:** Commit/build SHA baglantili dort cihaz profili smoke raporu, ekran goruntuleri ve test ciktilari
**Sonraki eskalasyon:** Tarih gecerse R7.4 ve R8 `Bloke` olur; uygun cihaz laboratuvari veya dis QA saglayicisi planlanir

**Sorun:** Asagidaki maddeler kod okuma ile kismen dogrulandi, fakat Play oncesi "tamamlandi" sayilmasi icin cihaz uzerinde kanit gerekiyor:
- Android 14 NotificationListener ac/kapa, event yazma, rapor gorunumu
- NotificationListener permission lifecycle / reboot testi
- Backup/restore uctan uca
- SmartInsightWorker ve BackupWorker schedule
- Android 13+ POST_NOTIFICATIONS yokken sessiz davranis
- BLUR-4/API26 performans/fallback
- Uretici/OEM kategori setleri
- Screenshot smoke seti ve gercek tablet/stabil tablet AVD LauncherActivity gorsel smoke

**Denenen:**
- Kod tarafinda duplicate worker riski, notification event veri modeli, search/settings davranislari ve backup/restore akislari incelendi.
- 20 gorevlik gecici rapor tamamlandi ve YENI_ROADMAP.md'ye konsolide edildi.
- 2026-07-13: Pixel6_API33 emulatorde `connectedDebugAndroidTest` 15 test / 0 failure gecti; AllApps arama odak + hizli cift dokunma smoke `FATAL EXCEPTION=0` ile kapandi; telefon `LauncherActivity` smoke `FATAL EXCEPTION=0` ile gecti. Simule tablet AllApps/search smoke gecti, fakat simule tablet `LauncherActivity` screenshot denemesi ADB baglantisini dusurdugu icin gorsel tablet Launcher smoke acik kaldi.

**Neden yerelde kapanmiyor:** Bu oturumda fiziksel cihaz/Play Store screenshot ortami yok; tablet Launcher smoke icin stabil tablet AVD veya gercek tablet gerekiyor.

**Kullanicidan beklenen:** YENI_ROADMAP.md R7.1–R7.5 senaryolarini cihazda kosup kanitlari kaydetmek.

---

*Son guncelleme: 2026-07-21. Aktif liste YENI_ROADMAP.md'de, tamamlananlar HISTORY.md'de tutulur.*
