# AppOrganizer — Akıllı Bildirim Gizlilik Denetimi

**Tarih:** 2026-07-26  
**Branch:** `agent/smart-notification-privacy-audit`  
**Kapsam:** Room, listener belleği, manuel yedek, Android Auto Backup / cihaz aktarımı, tanılama ve testler.

## Sonuç

Bildirim başlığı ve metni için hedef sözleşme:

- İçerik yalnız aktif process belleğinde tutulur.
- Room, JSON yedek, Android Auto Backup, cihaz aktarımı, Firebase, tanılama ve loglara yazılmaz.
- Kalıcı raporda yalnız paket adı, zaman, kategori, skor, bastırma ve sistem önceliği bulunabilir.

Denetimde bir **P0 ihlal** bulundu ve bu branch'te kapatıldı.

## P0 Bulgu — Room'a bildirim metni yazılıyordu

Eski akış:

```text
AppNotificationListenerService.latestTexts
    -> LauncherViewModel
    -> AppRepository.updateNotificationTexts
    -> AppDao.updateNotificationText
    -> apps.notificationText
```

Bu nedenle aktif bildirim özeti `apps` tablosunda kalıcılaşabiliyordu.

### Uygulanan düzeltme

`AppDao.updateNotificationText()` artık aldığı metni saklamaz; ilgili satırın `notificationText` değerini her zaman boş string yapar. Batch çağrı da aynı bariyerden geçer.

Listener bağlantısında `clearAllNotificationTexts()` çalıştırılarak eski sürümlerden kalmış içerikler temizlenir.

Canlı UI özeti değişmez: `AppNotificationListenerService.latestTexts` process içi `StateFlow` olarak çalışmaya devam eder; `FolderScreen` geçici `AppInfo.copy()` modeliyle görüntüler.

## Yedek denetimi

### Manuel JSON yedeği

`BackupManager.exportToJson()` uygulama metadata'sı ve tercihleri dışa aktarır; `notificationText` alanını JSON'a eklemez.

### Android Auto Backup

`backup_rules.xml` şu dosyaları hariç tutar:

```text
app_organizer_db
app_organizer_db-wal
app_organizer_db-shm
```

### Android 12+ cloud backup ve cihaz aktarımı

`data_extraction_rules.xml` aynı üç veritabanı dosyasını hem `cloud-backup` hem `device-transfer` kapsamından çıkarır.

## Tanılama denetimi

`DiagnosticsReportManager` bildirim tarafında yalnız şu değerleri kullanır:

- toplam olay sayısı
- son 7 gün / 24 saat olay sayısı
- son olay zamanı
- listener izin ve tazelik durumu

Başlık, gövde, gönderen, OTP, tutar veya hesap bilgisi rapora eklenmez.

## Test kanıtları

Eklenen testler:

- Dolu tekil bildirim metni DAO'ya verilse bile Room'dan boş okunur.
- Batch olarak OTP, bakiye veya doğrulama metni verilse bile tüm satırlar boş kalır.
- Eski kalıcı metinler `clearAllNotificationTexts()` ile silinir.
- Listener bağlanınca eski metin temizliği çağrılır.

## Kalan teknik borç

`AppInfo.notificationText` kolonu şimdilik şemada tutulmaktadır. Sebebi eski Room şemaları ve geniş uygulama modeliyle geriye uyumluluktur.

Bu alanın sözleşmesi artık:

```text
LEGACY FIELD — MUST REMAIN EMPTY
```

Tam kolon kaldırma ancak ayrı migration, schema fixture ve tüm eski sürüm yükseltme testleriyle yapılmalıdır. Mevcut güvenlik için kolonun kaldırılması zorunlu değildir; DAO bariyeri içerik yazımını engeller ve başlangıç temizliği eski içeriği siler.

## Bekleyen doğrulamalar

```powershell
.\gradlew.bat testDebugUnitTest --tests "*AppNotificationListenerServiceTest"
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest --tests "*NotificationTextPrivacyDaoTest"
```

Fiziksel cihaz kontrolü:

1. Bildirim metni gösterimini aç.
2. WhatsApp/banka/test bildirimi al.
3. Klasörde canlı özetin göründüğünü doğrula.
4. Uygulamayı yeniden başlat; eski içerik Room'dan geri gelmemeli.
5. Android bildirim panelindeki gerçek bildirimin silinmediğini doğrula.
