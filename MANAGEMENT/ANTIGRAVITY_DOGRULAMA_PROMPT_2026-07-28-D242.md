# Antigravity — Bağımsız Doğrulama Görevi (D242 serisi, 2026-07-28)

Bu, aynı gün içinde art arda yapılan 10 işlemin (D241 sonrası → D242c ve devamı) bağımsız
doğrulama görevidir. Claude'un (orkestra şefi) raporlarına GÜVENME — her maddeyi dosyayı açıp
KENDİN oku, gerekirse emülatörde dene. Sonucu
`MANAGEMENT/ANTIGRAVITY_DOGRULAMA_RAPORU_2026-07-28-D242.md` dosyasına yaz.

İlgili commit'ler: `60a8efe5`, `e0fc1cec`, `febab000`, `9200ee93` (+ henüz commit edilmemiş,
çalışma ağacında duran bir bildirim-kaynağı-birleştirme değişikliği — madde 10'a bak).

## 1) UsageStats senkron throttle (commit `60a8efe5`)

**İddia:** `AppPrefs.kt` içinde `USAGE_SYNC_INTERVAL_MS` 30 dakikadan 2 saate çıkarıldı.

**Doğrula:** `app/src/main/java/com/armutlu/apporganizer/utils/AppPrefs.kt` içinde
`USAGE_SYNC_INTERVAL_MS` sabitinin gerçekten `2L * 60 * 60 * 1000` olduğunu, `shouldSyncUsageStats`/
`markUsageStatsSynced` mekanizmasının bozulmadığını kontrol et.

## 2) Akıllı Erişim Favoriler sekmesi (commit `e0fc1cec`)

**İddia:** Akıllı Erişim kartına 4. sekme olarak Favoriler eklendi, uygulama açılınca varsayılan
seçili sekme bu oldu.

**Doğrula:**
- `domain/home/smartaccess/SmartAccessModels.kt`: `SmartAccessTab` enum'unda `FAVORITES` var mı,
  `SmartAccessUiState.favoriteApps: List<AppInfo>` alanı var mı.
- `presentation/ui/launcher/LauncherViewModel.kt`: `smartAccessState` içinde `favoriteApps = favorites`
  gerçekten dolduruluyor mu (mevcut `_favoritePkgs`'tan türetilen `favorites` değişkeni).
- `presentation/ui/launcher/hero/SmartAccessCard.kt`: `FAVORITES` sekmesi için `apps` hesaplaması,
  `labelRes()`, `emptyMessageRes()` fonksiyonlarında doğru dallar var mı.
- `presentation/ui/launcher/hero/HeroDashboardPage.kt`: `selectedTab` başlangıç değeri
  `SmartAccessTab.FAVORITES` mi.
- `strings.xml` (TR+EN): `hero_smart_access_favorites`, `hero_smart_access_favorites_empty` var mı,
  ikisi de anlamlı çeviri mi.

## 3) Dock çelişkisi fix'i (commit `e0fc1cec`)

**İddia:** `DockPrefs.migrateToHeroDock()` migration bir kez yapıldıktan sonra bile her
`onResume`'da (`loadDockPackages`) mevcut dock listesini fallback (en çok kullanılan uygulamalar)
ile birleştirip dock'u yeniden dolduruyordu — kullanıcı Ayarlar'dan dock'u sıfırlasa/uygulama
kaldırsa bile Home'a dönüşte dock otomatik geri doluyordu.

**Doğrula:** `app/src/main/java/com/armutlu/apporganizer/utils/DockPrefs.kt` içinde
`migrateToHeroDock()` fonksiyonunu oku. `KEY_HERO_DOCK_MIGRATED` bayrağı `true` ise artık
`buildHeroDockItems`/fallback birleştirmesi ATLANIP sadece `sanitizeHeroDockItems(current)`
döndürüldüğünü doğrula. `DockPrefsTest.kt`/`HeroDockMigrationPolicyTest.kt` testlerinin
`buildHeroDockItems`/`sanitizeHeroDockItems`'ı doğrudan test ettiğini (yani `migrateToHeroDock`'un
kendisini DEĞİL) ve bu değişiklikle kırılmadığını teyit et — `./gradlew testDebugUnitTest
--tests "*DockPrefsTest*" --tests "*HeroDockMigrationPolicyTest*"` çalıştırıp sonucu raporla.

**Kritik soru:** Bu fix mantıklı mı, yoksa bir regresyon riski taşıyor mu? Örneğin: kullanıcı
dock'taki TÜM uygulamaları elle kaldırırsa (dock tamamen boş kalırsa), bu artık kalıcı mı oluyor
(yani sistem bir daha hiç otomatik doldurmuyor mu)? Bunun istenen davranış olup olmadığını
kod bağlamından değerlendir, şüpheli bulursan raporda belirt.

## 4) TodayCard / Görevler kartı çakışması (commit `febab000`)

**İddia:** `TodayCardSelector`'daki `DAILY_MISSIONS` önceliği (5. sıra) kaldırıldı çünkü
`HomeMissionCard`'ın zaten gösterdiği aynı `HomeMissionSummary` verisini (tamamlanan/toplam görev)
tekrar gösteriyordu.

**Doğrula:** `domain/home/TodayCardSelector.kt` içinde `select()` fonksiyonunun artık
CRITICAL_PERMISSION → RISKY_MISSION → FOLDER_REVIEW → REPORT_READY → BALANCE_SUMMARY → ADVICE
sırasını takip ettiğini, `DAILY_MISSIONS` dalının select() içinden kaldırıldığını (enum'da hâlâ
durabilir, bu sorun değil — sadece `select()` bunu artık DÖNMEMELİ) doğrula.
`TodayCardSelectorTest.kt`'de ilgili 5 testin güncellendiğini ve mantıklı olduğunu kontrol et
(`./gradlew testDebugUnitTest --tests "*TodayCardSelectorTest*"` çalıştır).

## 5) Klasör birleştirme kartı görünmezliği (commit `febab000`)

**İddia:** `LauncherViewModel.editingCenterState`'te `folderMergeCandidates` alanı hep `0` sabitti
(motor bağlanmamıştı) — bu yüzden Düzenleme Merkezi kartındaki klasör birleştirme önerisi HİÇ
görünmüyordu (emülatör testiyle doğrulanmıştı: `hasAnyAlert=false` olduğu için kart render
edilmiyordu).

**Doğrula:** `LauncherViewModel.kt`'de `editingCenterState`'in artık `allAppsSource`'u da combine
ettiğini, `FolderMergeCandidateScorer.score(apps).size` ile gerçek aday sayısını hesapladığını
doğrula. Bu, `FolderMergeViewModel.kt`'nin kullandığı AYNI saf fonksiyon mu (import yolu ve
parametre imzası eşleşiyor mu) kontrol et. **Emülatörde test et:** Ana ekranda Düzenleme Merkezi
kartının artık göründüğünü (varsa küçük kategorilerin birleştirme adayı olarak tespit edildiğini)
doğrula — küçük/az uygulamalı 2 kategori varsa kart görünmeli.

## 6) Bildirim raporu linki metni (commit `febab000`)

**İddia:** `HomeScreenFolderPager.kt`'de "Yeni bildirim ve öneriler burada görünür" metni
"Bildirim raporunu gör" olarak değiştirildi (kullanıcı "gönderi" bekliyordu ama gerçekte
`NotificationReportScreen` istatistik ekranı açılıyordu — metin artık ne olduğunu açıkça söylüyor).

**Doğrula:** Metnin gerçekten değiştiğini, `pageNotifications > 0` durumundaki metnin de
("$pageNotifications okunmamış bildirim — rapora dokun") tutarlı olduğunu kontrol et.

## 7) Bildirim Geçmişi ekranı (commit `9200ee93`) — EN BÜYÜK MADDE

**İddia:** Yeni bir Room tablosu (`notification_history`, migration 25→26) eklendi. Bu tablo
SADECE kullanıcı Ayarlar > Bildirimler > "Bildirim metnini göster" ayarını AÇTIYSA
(`AppPrefs.isNotificationTextEnabled`, varsayılan KAPALI) doldurulur. Retention: 7 gün veya 500
kayıt. `NotificationReportScreen`'e "Geçmiş" adında yeni bir sekme eklendi.

**Doğrula (bu maddeyi en dikkatli incele, gizlilik riski taşıyor):**
- `domain/models/NotificationHistoryEntity.kt`: `title`/`text` alanları var mı, entity'nin
  dokümantasyonu gizlilik davranışını doğru anlatıyor mu.
- `data/local/NotificationHistoryDao.kt`: `insert`, `observeRecent`, `deleteOlderThan`,
  `trimToLatest` fonksiyonları doğru mu. `trimToLatest`'in SQL sorgusu (`LIMIT -1 OFFSET :keepLatest`)
  gerçekten "en yeni N kaydı tut, gerisini sil" mantığını doğru uyguluyor mu — SQLite'ta bu
  sözdiziminin geçerli olduğunu doğrula (bazı SQL lehçelerinde `LIMIT -1 OFFSET n` desteklenmez).
- `data/local/AppDatabase.kt`: `MIGRATION_25_26`'nın `notification_history` tablosunu doğru
  şema ile oluşturduğunu, `version = 26`'ya çıkarıldığını, migration listesine eklendiğini,
  `app/schemas/com.armutlu.apporganizer.data.local.AppDatabase/26.json` dosyasının commit'e
  girdiğini doğrula.
- `service/AppNotificationListenerService.kt`: `onNotificationPosted` içinde
  `AppPrefs.isNotificationTextEnabled(this)` KAPALIYKEN kesinlikle hiçbir `notificationHistoryDao.insert`
  çağrısının YAPILMADIĞINI kod okuyarak kanıtla (bu en kritik gizlilik garantisi — açıkken de
  kapalıyken de test et, mümkünse emülatörde ayarı kapalı bırakıp bildirim gönderip
  `notification_history` tablosunun boş kaldığını `adb shell` ile doğrula).
- `presentation/viewmodel/NotificationReportViewModel.kt`: `history` StateFlow'unun
  `notificationHistoryDao.observeRecent()`'e bağlı olduğunu, `historyEnabled` getter'ının
  `AppPrefs.isNotificationTextEnabled` okuduğunu doğrula.
- `presentation/ui/screens/NotificationReportScreen.kt`: TabRow ile "Rapor"/"Geçmiş" sekmeleri
  arasında geçiş çalışıyor mu (emülatörde tıkla, ikisi de açılıyor mu). Ayar kapalıyken "Geçmiş"
  sekmesinin `notif_history_disabled_title`/`desc` mesajını gösterdiğini doğrula.
- `di/AppModule.kt`: `provideNotificationHistoryDao` Hilt provider'ının eklendiğini doğrula.

## 8) versionCode/versionName ilerlemesi

**İddia:** Bu turlarda versionCode sırasıyla 161→169 (kullanıcı isteğiyle atlama)→170 ilerledi,
versionName buna paralel 1.4.37→1.4.45→1.4.46.

**Doğrula:** `app/build.gradle.kts`'te güncel `versionCode`/`versionName` değerini oku, HISTORY.md'deki
kayıtlarla tutarlı olduğunu kontrol et.

## 9) HISTORY.md kayıtları

**Doğrula:** `MANAGEMENT/HISTORY.md`'nin en üstünde D242, D242b, D242c döngü kayıtlarının
gerçekten var olduğunu, her birinin commit hash referanslarının doğru olduğunu kontrol et.

## 10) Bildirim kaynağı birleştirme (HENÜZ COMMIT EDİLMEMİŞ — çalışma ağacında)

**İddia:** Ana ekrandaki Akıllı Erişim > Bildirimler sekmesi ile AllAppsDrawer (çekmece)
rozetinin farklı sonuç gösterdiği bulundu — kök neden: ana ekran Room'daki tarihsel
`notification_events` tablosundan (24 saatlik pencere) besleniyordu, çekmece ise
`AppNotificationListenerService`'in anlık aktif bildirim snapshot'ından besleniyordu. Bu iki farklı
metrik olduğu için kullanıcı bildirim panelini temizleyince çekmece sıfırlanıyor ama Hero kartı
24 saat boyunca göstermeye devam ediyordu. Fix: ana ekran artık da aynı anlık snapshot kaynağını
(`AppNotificationListenerService.smartNotifications`) kullanıyor.

**Doğrula:** `git status` ile bu değişikliğin henüz commit edilip edilmediğini kontrol et (commit
edilmişse en son commit'e bak). `LauncherViewModel.kt`'de `latestNotificationSummaries`'in artık
`notificationEventDao.observeLatestSummaries()` yerine `AppNotificationListenerService.smartNotifications`
StateFlow'unu `map` ile `PackageNotificationSummary` listesine çevirdiğini doğrula
(`shouldSuppress` olanların filtrelendiğini, `groupBy(packageName)` ile `count`/`lastPostedAt`
hesaplandığını). **Bu değişikliğin build+test'ten geçip geçmediğini kontrol et** — henüz
doğrulanmamış olabilir, `./gradlew assembleDebug testDebugUnitTest -PskipGoogleServices` çalıştırıp
sonucu raporla.

## Rapor formatı

`MANAGEMENT/ANTIGRAVITY_DOGRULAMA_RAPORU_2026-07-28-D242.md` içine, her madde için:
- Doğrulandı / kısmen doğrulandı / sorun bulundu
- Sorun varsa: dosya + satır + ne yanlış + önerilen düzeltme
- Madde 3 ve 7 için ayrıca "risk değerlendirmesi" (regresyon/gizlilik açısından) bir cümle
- Genel sonuç: kritik sorun var mı yok mu, hangi maddeler ek dikkat gerektiriyor
