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

## Codex Is Listesi - Cozum Tarifli (D241, Fable hazirladi)

Asagidaki maddeler Codex 5.5 (baska bir AI) tarafindan uygulanacak sekilde hazirlandi. Her madde: sorun, dokunulacak dosyalar, adim adim cozum tarifi, kabul kriteri iceriyor. Kod degisikligi bu dokumanda yapilmadi. NOT: Gizlilik analizi (14p) LISTEDEN CIKARILDI - PrivacyReportScreen/PrivacyAnalyzer olarak D238'de zaten uygulandi ve baglandi; Codex yeniden yapmasin.

### Akilli Kategorileme K4 - Kullanim paternine gore baglamsal akilli klasor (13p)

**Sorun/istek:** Room `usageCount`/`lastUsedTimestamp` + `UsageStatsHelper` saat-dilimi verisi var ama "Sabah Rutini / Is Saatleri / Aksam" gibi dinamik/sanal klasor veya klasor ici otomatik yeniden siralama yok. 13p "kullanim sikligina gore dock siralama" fikriyle sinerjik, birlikte tek epik olarak ele alinabilir.

**Not:** CLAUDE.md kurali geregi bu madde zorluk 7/10 â€” uygulamadan once mimari karar gerekiyor. Codex once 2 kaynaktan (WebSearch + kod analizi) arastirma yapip 2+ secenek sunmali, sonra Huseyin onayi beklemeli. Direkt kod yazma.

**Cozum tarifi (arastirma + tasarim adimlari):**
1. `UsageStatsHelper.kt`'yi oku â€” saat-dilimi histogram cikarma yetenegi var mi dogrula (yoksa once bu eklenmeli).
2. `InsightEngine.kt` icindeki mevcut zaman-bazli insight uretme pattern'ini incele, ayni yaklasimla "su an hangi zaman dilimindeyiz" -> "bu dilimde en cok kullanilan N uygulama" hesaplamasi tasarla.
3. Secenek A: `HomeScreen.kt`'de mevcut klasorlerin ustune gecici bir "Simdi" seridi/karti ekle (kalici klasor degil, sadece siralama onerisi).
   Secenek B: `LauncherViewModel.kt`'deki dock/quick-access sec-4-uygulama mantigina (zaten `boosted` skor sistemi var, satir ~634-654) zaman-dilimi boost'u ekle.
4. Hangi secenek uygulanacaksa `AppPrefs.kt`'ye `KEY_CONTEXTUAL_FOLDER_ENABLED` + toggle ekle.
5. Karar ve arastirma sonucu once rapor edilmeli, kod degisikligi onay sonrasi yapilmali.

**Kabul kriteri:**
- Arastirma raporu + 2 secenek sunuldu, Huseyin onayi alindi (bu adim tamamlanmadan kod yazilmaz).
- Onay sonrasi: farkli saatlerde dock/oneri siralamasi degisiyor, tamamen cihaz ici, veri disari cikmiyor.

---

### Ayar aramasi (SETTING source) (13p)

**Sorun/istek:** Wi-Fi/Bluetooth/Bildirim gibi ~20 statik sistem ayari birlesik aramada bulunmuyor.

**Cozum tarifi:**
1. `SearchRepository.kt` icindeki mevcut `SearchDocument` modelini ve source_type alanini incele (ornek: APP/FOLDER/CONTACT/FILE gibi mevcut turler).
2. Yeni bir `SearchDocumentType.SETTING` (veya string sabiti) ekle.
3. Sabit bir liste olustur (yeni dosya: `utils/SystemSettingsCatalog.kt`) â€” her satir: baslik (Wi-Fi, Bluetooth, Bildirim Erisimi, Konum, vb.), eslesen `Settings.ACTION_*` intent sabiti (ornek: `Settings.ACTION_WIFI_SETTINGS`, `Settings.ACTION_BLUETOOTH_SETTINGS`, `Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
4. `SearchRepository.bootstrapIndex()` (AppListViewModel.kt satir 190 civarinda cagrilan yer) icine bu sabit listeyi FTS indeksine ekleyen bir adim koy â€” uygulama/klasor indexleme pattern'iyle ayni sekilde.
5. Arama sonuc tiklamasinda `SETTING` turu icin `Intent(action).let { context.startActivity(it) }` cagir (AppListViewModel.launchIntent zaten var, onu kullan).
6. `AppPrefs.kt`'ye `KEY_SEARCH_SETTINGS_SOURCE_ENABLED` + SettingsScreen toggle (Universal Search bolumune).

**Kabul kriteri:**
- Ana arama cubugunda "wifi" yazinca Wi-Fi ayarlari sonuc olarak cikiyor, tiklaninca `Settings.ACTION_WIFI_SETTINGS` aciliyor.
- Toggle kapatilinca SETTING sonuclari aramadan cikiyor.

---

### AI kocu haftalik yorumu (13p)

**Sorun/istek:** `WrappedReport` verisi var ama lokal AI/DeepSeek'e ozetletip 2 cumlelik kisisel yorum uretilmiyor.

**Cozum tarifi:**
1. Mevcut `CategoryLLMFallback.kt`'deki DeepSeek/lokal AI cagri pattern'ini (API key okuma, `.env`/AppPrefs'ten key alma, hata durumunda sessiz devam) referans al.
2. `WrappedReport` modelinin bulundugu dosyayi bul (Wrapped MVP Dongu 230 - `grep -r "WrappedReport" app/src`), agregat alanlarini (skor, en cok kullanilan kategori, trend) cikar.
3. Yeni fonksiyon: `WrappedAiCoach.summarize(report: WrappedReport, apiKey: String): String` â€” sadece agregat sayisal veri (paket adi/icerik YOK) prompt'a gider.
4. `AppPrefs.kt`'ye `KEY_WRAPPED_AI_COACH_ENABLED` (varsayilan KAPALI â€” opt-in, disaridan veri gittigi icin varsayilan acik olmamali) + SettingsScreen toggle, acik aciklama metniyle ("haftalik ozet skorun DeepSeek'e gonderilir, uygulama adi gitmez").
5. Wrapped rapor ekraninda (WrappedReportScreen veya benzeri) toggle aciksa bu yorumu bir kart olarak goster, yuklenirken skeleton/loading state.
6. Hata/API-key-yok durumunda kart hic gosterilmez (sessiz basarisizlik, CLAUDE.md "Asla Yapma" kuralina uygun degil ama Play Store guvenlik/gizlilik onceligi burada gecerli).

**Kabul kriteri:**
- Toggle kapaliyken (varsayilan) hicbir veri disari gitmiyor, kart gorunmuyor.
- Toggle acilinca Wrapped ekraninda 2 cumlelik yorum karti beliriyor, sadece agregat veri gonderildigi kod incelemesiyle dogrulanabiliyor.

---

### Hedef sistemi (13p)

**Sorun/istek:** Haftalik kategori kullanim hedefi + rozet odulu yok; oturum altyapisina (UsageEvents, Dongu 232) bagimli.

**Cozum tarifi:**
1. Dongu 232'de eklenen UsageEvents yerel oturum agregatorunu bul (`grep -r "UsageEvents" app/src` ile dosyayi dogrula) â€” haftalik kategori bazli kullanim suresi zaten hesaplaniyor olmali.
2. Room'a yeni tablo/entity: `WeeklyGoal` (categoryId, targetMinutes, weekStartEpochDay) â€” Room Migration sablonuna uy (CLAUDE.md Â§5 "Room Migration Sablonu": schemaLocation + Migration class + addMigrations, fallbackToDestructiveMigration YASAK).
3. `AppPrefs.kt`'ye `KEY_GOALS_ENABLED` + toggle; hedef belirleme UI'i (yeni ekran veya Dashboard'a ek bolum â€” `AppOrganizerDashboardScreen.kt`'deki `DashboardStats.compute()` pattern'ini kullan).
4. Haftalik kontrol: mevcut `SmartInsightWorker` veya `WeeklyDigestWorker` (hangisi haftalik tetikleniyor, dogrula) icine hedef karsilastirma + rozet hak edildiyse bildirim ekle.
5. Rozet gosterimi icin Wrapped/Dashboard ekranina basit bir liste/kart.

**Kabul kriteri:**
- Kullanici bir kategoriye haftalik dakika hedefi belirleyebiliyor, hafta sonunda hedefe ulasilip ulasilmadigi Dashboard'da goruluyor.
- Room migration test edilmis (schema dosyasi commit'li).

---

### Kilit acma sayaci (12p)

**Sorun/istek:** UsageEvents `KEYGUARD_HIDDEN` ile gunluk telefon acma sayisi + haftalik trend Wrapped'a eklenmemis.

**Cozum tarifi:**
1. Dongu 232'deki UsageEvents agregator dosyasini bul, `UsageEvents.Event.KEYGUARD_HIDDEN` (API 28+) event turunu okuyup gunluk sayaca ekleyen bir fonksiyon ekle (mevcut oturum sayma pattern'iyle ayni yapida).
2. API 28 altinda bu event yok â€” `Build.VERSION.SDK_INT >= 28` guard'i sart, altinda ozellik sessizce gizlenir (Wrapped karti gorunmez).
3. Gunluk sayim `WrappedSnapshotPrefs` (mevcut `updateDailyScore` pattern'i) benzeri bir yapida haftalik trend icin saklanabilir.
4. Wrapped rapor ekranina yeni bir kart: "Bu hafta telefonunu X kez actin, gecen haftaya gore %Y" .
5. `AppPrefs.kt`'ye ayri toggle gerekmez â€” mevcut `KEY_WRAPPED_ENABLED` semsiyesine dahil edilebilir (Wrapped zaten opt-in ise).

**Kabul kriteri:**
- API 28+ cihazda Wrapped raporunda kilit acma sayisi karti goruluyor, API 28 altinda kart hic render edilmiyor (crash yok).
- Sayim UsageEvents'ten geliyor, tahmini/uydurma deger yok (CLAUDE.md "sahte veri gosterilmez" kuralina uygun).

---

### B1 - "44 uygulamanin kategorisi belirsiz" sayi tutarsizligi

**Kok neden (kod kaniti):** `LauncherViewModel.kt` satir 767-768'de `tickerItems` icinde `lowConfidenceCount`, TUM klasorlerdeki TUM uygulamalarin `classifier.getConfidence(it, f.category.categoryId) < 60` kosuluyla sayiliyor:
```kotlin
val lowConfidenceCount = folderList.sumOf { f ->
    f.apps.count { classifier.getConfidence(it, f.category.categoryId) < 60 }
}
```
`AppClassifier.kt` satir 110-116'da `getConfidence` skor 50 donduren durum "hicbir exact/keyword/package-keyword eslesme yok" (else -> 50) â€” yani "belirsiz" aslinda cok gevsek bir tanim, cogu normal kategorize uygulamayi da kapsayabilir.

Bu sayi `TickerComposer.kt` satir 320-325'te "$lowConfidenceCount uygulamanin kategorisi belirsiz" mesajina donusuyor ve `routeKey = "APP_LIST"` ile Routes.APP_LIST'e yonlendiriyor (LauncherViewModel.kt satir 684: `"APP_LIST" -> Routes.APP_LIST`).

Ama `Routes.APP_LIST` parametresiz genel listeye gidiyor (`AppNavigation.kt` satir 88-89) ve `AppListScreen.kt`/`AppListViewModel.kt` hicbir "confidence" veya "belirsiz" filtresi bilmiyor â€” sadece `selectedCategory`/`searchQuery`/`showSystemApps`/`sortBy` filtreleri var (`AppListScreenState.kt` satir 59-87, `computeFilteredApps`). Sonuc: tiklaninca ayni "belirsiz" hesabini kullanan bir liste ACILMIYOR, genel "Tumu" listesi aciliyor â€” kullanici 44 sayisini goremiyor.

**Cozum tarifi:**
1. `presentation/navigation/AppNavigation.kt` â€” `Routes.APP_LIST` rotasina opsiyonel bir NavArgument ekle: `const val APP_LIST = "app_list"` yaninda `const val APP_LIST_FILTER_UNCERTAIN = "app_list?filter=uncertain"` gibi bir varyant VEYA `composable("app_list?filter={filter}", arguments = listOf(navArgument("filter") { defaultValue = ""; nullable = true }))` seklinde route'u parametrik yap.
2. `LauncherViewModel.kt` satir 681-689 `resolveTickerRoute` fonksiyonuna yeni bir routeKey ekle: `"APP_LIST_UNCERTAIN" -> "${Routes.APP_LIST}?filter=uncertain"`.
3. `TickerComposer.kt` satir 325 civarinda `routeKey = "APP_LIST"` yerine `routeKey = "APP_LIST_UNCERTAIN"` kullan.
4. `AppListViewModel.kt`'ye yeni bir `_confidenceFilter: MutableStateFlow<Boolean>` (veya nav arg'dan set edilen bir `setUncertainFilterEnabled(true)` fonksiyonu) ekle; `createScreenState`/`computeFilteredApps` (AppListScreenState.kt satir 59-87) icine `classifier.getConfidence(...)` kontrolu ekleyen bir ek filtre parametresi koy â€” AYNI esik (<60) ve AYNI classifier cagrisi kullanilmali (LauncherViewModel'deki ile birebir ayni fonksiyon/esik).
5. `AppListScreen.kt`'de nav'dan gelen filter parametresini oku (composable arguments), ViewModel'e ilet, ekranda "Belirsiz Kategoriler" basligi/filtre chip'i olarak goster.
6. Sayim ve liste ayni fonksiyondan beslenmeli: ideal olarak `AppClassifier.kt`'ye tek bir `fun isLowConfidence(app: AppInfo, categoryId: String): Boolean = getConfidence(app, categoryId) < 60` ekle, hem `LauncherViewModel.tickerItems` hem `AppListViewModel`/`computeFilteredApps` bu fonksiyonu cagirsin â€” esik degisirse tek yerden degisir.

**Kabul kriteri:**
- Ticker'da "N uygulamanin kategorisi belirsiz" yaziyorsa, tiklaninca acilan listede TAM OLARAK N uygulama gorunuyor.
- Esik degeri (`<60`) `AppClassifier.kt` icinde tek bir yerde tanimli, hem sayim hem liste ayni fonksiyonu cagiriyor.

---

### B3 - Ticker tiklamasi sonrasi ana ekrana hizli donuste bildirimler kilitleniyor

**Kok neden (kod kaniti):** `HomeScreen.kt` satir 660-672'de ticker `onItemClick` cagrildiginda `Intent(context, MainActivity::class.java)` ile MainActivity aciliyor (LauncherActivity arka plana geciyor, `onPause`/`onStop` tetiklenmiyor cunku activity yigin ustunde kaliyor â€” sadece durduruluyor).

Kullanici geri donunce `LauncherActivity.onResume()` (`LauncherActivity.kt` satir 221-239) senkron/yari-senkron sekilde su zinciri tetikliyor:
- `viewModel.refreshLastLaunched()` â€” tek paket icin `updateLastUsedTimestamp` (hafif).
- `AppPrefs.shouldReconcile(this)` true ise `viewModel.reconcileIfNeeded(this)` â€” TUM yuklu paketleri `packageManagerHelper.getInstalledApps(includeSystem = true, ...)` ile tarayip DB ile karsilastiriyor (`LauncherViewModel.kt` satir 257-282), potansiyel olarak `loadAppsIfEmpty()` (satir 285-337) tetikleyip her degisen app icin ayri ayri `repository.updateApp(...)` cagirabiliyor.
- `AppPrefs.shouldSyncUsageStats(this)` true ise `viewModel.syncUsageStats(this)` (satir 835+) â€” UsageStatsManager sorgusu, potansiyel agir IO.

Bunlarin YANI SIRA, HER ZAMAN calisan (onResume'a bagli olmayan, `init` bloguna bagli, activity/process suresince surekli aktif) `AppNotificationListenerService.badgeCounts.onEach` collector'i (`LauncherViewModel.kt` satir 196-212) her badge degisiminde:
```kotlin
counts.forEach { (pkg, count) -> repository.updateNotificationCount(pkg, count) }
```
seklinde HER PAKET ICIN AYRI bir `suspend fun updateNotificationCount` DAO cagrisi yapiyor (`AppDao.kt` satir 203, tek satirlik UPDATE, `@Transaction` YOK). `AppRepository.kt` satir 361-362'de de sarma katmani transaction eklemiyor. MainActivity'den donusce, arka planda birikmis bildirimler varsa bu forEach dongusu N ayri DB islemi calistiriyor â€” Room'un tek-yazici (single writer) kisitlamasi nedeniyle bu N ayri UPDATE, ayni anda `reconcileIfNeeded`/`syncUsageStats`'in okuma sorgulariyla siraya giriyor ve UI thread'e baglÄ± Flow collector'lari (badge/folder UI) bu kuyruk bosalana kadar guncellenmeden bekliyor â€” "birkac saniye donma" hissi budur.

**Muhtemel cozumler (onerilen sira):**
1. **(A - en yuksek etki, dusuk risk) Badge DB yazimini tek transaction'a topla:** `AppDao.kt`'ye yeni bir `@Transaction suspend fun updateNotificationCounts(counts: Map<String, Int>)` ekle (Room'da `@Transaction` + icinde bir `@Update`/dogrudan SQL `UPDATE apps SET notification_count = :count WHERE package_name = :pkg` dongusu, veya tek SQL ile `CASE WHEN package_name = ... THEN ... END` toplu update) â€” DAO metodunun govdesinde Kotlin `forEach` ile ayni ayri cagrilar olsa bile `@Transaction` sarmali TEK commit'e indirger, ara kilitlenmeyi onler. `LauncherViewModel.kt` satir 196-212'deki `counts.forEach { ... }` blogunu `repository.updateNotificationCounts(counts)` (tek cagri) ile degistir; `toReset` blogu icin de ayni pattern (`resetNotificationCounts(List<String>)`).
2. **(B) onResume zincirini debounce/gecikmeli calistir:** `LauncherActivity.kt` satir 221-239'daki `reconcileIfNeeded`/`syncUsageStats` cagrilarini `lifecycleScope.launch { delay(300) ... }` gibi kucuk bir gecikmeyle veya `Dispatchers.Default` uzerinde arka plana atarak ilk frame'in UI thread'i bloklamasini engelle (bu cagrilarin ic gÃ¶vdesi zaten `Dispatchers.IO` ama Flow collector tetiklemesi ve Room writer sirasi paylasimli).
3. **(C) dismissTickerItem sonrasi ticker recompute'unu hafiflet:** Bu senaryoda dogrudan ilgili degil (ticker zaten `WhileSubscribed(5_000L)`), oncelik A ve B'de.

Codex A'yi once uygulamali (en dogrudan kok nedene cozum), B'yi ikinci katman iyilestirme olarak ekleyebilir.

**Cozum tarifi:**
1. `AppDao.kt` satir ~203 civarina ekle:
   ```kotlin
   @Transaction
   suspend fun updateNotificationCounts(counts: Map<String, Int>) {
       counts.forEach { (pkg, count) -> updateNotificationCount(pkg, count) }
   }
   @Transaction
   suspend fun resetNotificationCounts(packageNames: List<String>) {
       packageNames.forEach { updateNotificationCount(it, 0) }
   }
   ```
2. `AppRepository.kt` satir 361-362 civarina karsilik gelen sarma fonksiyonlari ekle (`updateNotificationCounts`, `resetNotificationCounts`), try/catch + Timber.e pattern'i koru.
3. `LauncherViewModel.kt` satir 196-212'yi guncelle:
   ```kotlin
   counts.forEach { (pkg, count) -> repository.updateNotificationCount(pkg, count) } // ESKÄ°
   ```
   yerine
   ```kotlin
   repository.updateNotificationCounts(counts) // YENÄ° â€” tek transaction
   ```
   ve `toReset.forEach { repository.updateNotificationCount(it.packageName, 0) }` yerine `repository.resetNotificationCounts(toReset.map { it.packageName })`.
4. Ayni pattern `latestTexts.onEach` blogu (satir 214-230) icin de tekrarlanabilir (`updateNotificationTexts`/`resetNotificationTexts`) â€” ayni kok neden.
5. (Opsiyonel B) `LauncherActivity.kt` `onResume()` icindeki `reconcileIfNeeded`/`syncUsageStats` cagrilarini olcup, gercekten frame droplarina sebep oluyorsa `viewModelScope.launch { delay(250); ... }` ile hafif geciktir â€” ama once (1)'in etkisini olcmeden bu adimi atlama, cogu durumda (1) yeterli olabilir.

**Kabul kriteri:**
- Arka planda 10+ bildirim birikmisken ticker'a tiklayip MainActivity'ye gidip hemen geri donuldugunde, badge/folder UI'i 1 saniyeden kisa surede tepki veriyor (once birkac saniye donma varken).
- `AppDao`/`AppRepository` degisikligi sonrasi mevcut `AppNotificationListenerServiceTest.kt` (Dongu 221) testleri hala geciyor; yeni transaction fonksiyonu icin en az 1 unit test eklendi.

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
