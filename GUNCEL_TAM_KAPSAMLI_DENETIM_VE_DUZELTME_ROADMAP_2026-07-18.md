# AppOrganizer — Güncel Tam Kapsamlı Sistem Denetimi ve Düzeltme Roadmap'i

> **Revizyon:** 2  
> **Son denetim tarihi:** 2026-07-18  
> **Denetlenen dal:** `main`  
> **Denetlenen HEAD:** `be08f7b667bfbbb6be14f2c500b6d72f87e98d5c`  
> **Önceki denetim commit'i:** `7e8f49436e4cd4845108d3373e3416830b4c03c7`  
> **Denetlenen sürüm:** `1.3.89 (112)`  
> **Kapsam:** veri bütünlüğü, paket olayları, Room, repository sözleşmeleri, arama, izin yaşam döngüsü, dosya/kişi indeksleri, bildirimler, Dashboard rollout, ana ekran yerleşimi, görev/skor sistemi, zaman sınırları, performans, erişilebilirlik, güvenlik, yedekleme, telemetri, CI ve yayın süreci.  
> **Ana karar:** Yeni özellik geliştirme ikinci plandadır. Öncelik, mevcut sistemin veri kaybetmeden, kullanıcının seçtiği ayarı gerçekten uygulayarak ve gizlilik sözünü teknik olarak doğrulayarak çalışmasıdır.

---

# 0. Revizyon 2 — Neden yeniden yazıldı?

İlk roadmap `7e8f494...` kodunu denetliyordu. Denetim sürerken `main` dalına `be08f7b...` merge commit'i geldi ve sürüm `1.3.89 (112)` oldu. Bu commit;

- P20 adaptif layout altyapısı,
- P21 ana ekran telemetri şeması,
- P22 diagnostics özeti,
- P23 bazı recomposition iyileştirmeleri,
- P24 rollout preference/policy altyapısı,
- P25 bazı legacy temizlikleri

eklediğini beyan ediyor.

İkinci kod geçişinde şu gerçek durum doğrulandı:

1. `HomeAdaptiveLayoutPolicy` ve testleri var; fakat `HomeScreen` runtime'da hâlâ kendi `600/840dp` eşiklerini, sabit `380.dp` panel genişliğini ve drag sırasında sabit `colCount = 4` değerini kullanıyor.
2. `HomePagerRolloutPolicy` ve preference anahtarları var; fakat policy'nin üretim çağrısı yok. `HomeScreen` hâlâ `val dashboardEnabledForPager = false` kullanıyor.
3. Home page telemetry event sınıfları, validator ve `AppAnalytics` wrapper'ları var; fakat `homePageViewed`, `homePageSwiped` ve `homeSearchOpened` için üretim çağrı noktaları yok.
4. Diagnostics altyapısı genişletilmiş; ancak gerçek cihaz raporu, dört cihaz karşılaştırması ve privacy-safe çıktı kanıtı henüz yok.
5. P23 kapsamında yalnız iki mikro optimizasyon var; trace, baseline profile, macrobenchmark ve jank kanıtı yok.
6. P25'te runtime ham page-index yazımı kaldırılmış; diğer legacy yollar gerçek cihaz rollout kanıtını bekliyor.
7. Merge commit'i için GitHub combined status/check sonucu bulunmuyor. Roadmap içindeki “test yeşil” notu tek başına bağımsız CI kanıtı değildir.

**Sonuç:** P20, P21 ve P24 için “runtime entegrasyonu tamamlandı” kabul edilmeyecektir. Bunların durumu `🚧 Altyapı var, üretim bağlantısı eksik` olarak değerlendirilir.

---

# 1. Yönetici özeti

Repo mimari olarak ciddi ilerleme kaydetmiştir. Tek pager, semantic page anchor, global shell, Dashboard component, HomeLayout v2, Focus Mode politikası, telemetri doğrulayıcıları ve diagnostics altyapısı doğru yöndedir.

Ancak uygulamanın güvenilir bir launcher ve Play Store ürünü sayılması için aşağıdaki ana riskler kapanmalıdır:

1. Paket güncellemesi gerçek uninstall gibi işlenebilir.
2. Aynı package event iki receiver tarafından işlenebilir.
3. `PACKAGE_CHANGED` metadata güncellemesi `INSERT IGNORE` nedeniyle kaybolabilir.
4. Package event yolu launchable/visible katalog uygunluğunu kontrol etmiyor.
5. Repository mutasyonları hataları yutup üst katmana başarı izlenimi veriyor.
6. Kategori silme ve paket referans temizliği atomik değil.
7. Exported package receiver dışarıdan explicit intent ile tetiklenmeye açık.
8. DeepSeek anahtarı gerçekte `app_organizer_prefs` içindeyken backup kuralı kullanılmayan `deepseek_prefs.xml` dosyasını hariç tutuyor.
9. Auto Backup geniş `include path="."` denylist yaklaşımıyla kişi aksiyon geçmişi, hava konum cache'i, crash logları, FCM token ve cihaz-yerel state'i yedekleyebilir.
10. Dashboard preference, rollout preference ve rollout policy kullanıcıya/diagnostics'e sunulsa da runtime page planını değiştirmiyor.
11. Adaptive layout policy oluşturulmuş olsa da üretim hâlâ dağınık sabit eşikler kullanıyor.
12. Layout editöründeki görünürlük ve sıra kararları renderer ile birebir değil.
13. Arama çubuğu konumu iki ayrı preference kaynağında tutuluyor.
14. Arama kaynakları tek global limitte app-first sıralamayla kişi/dosya/ayar sonuçlarını aç bırakıyor.
15. Arama ekranındaki ranking, fuzzy, phonetic, instant, usage-sort ve max-result ayarlarının önemli bölümü runtime tarafından tüketilmiyor.
16. Rehber veya medya izni sistemden geri alındığında eski indeks kayıtları aramada kalabilir.
17. Contact observer process restart sonrasında yeniden kurulmayabilir ve sıradan `Job` hatası gelecekteki güncellemeleri durdurabilir.
18. Files index sabit Images→Video→Audio→Downloads sırasıyla tek 1000 sınırı kullanıyor; fotoğraf çoksa diğer kaynaklar tamamen aç kalabilir.
19. Arama indeksleri delete-then-insert ile transaction dışında yenileniyor; doğal unique key yok.
20. Bildirim reconnect, emisyon sırası ve hareketli 24 saat sınırı hatalı sonuç üretebilir.
21. `initialLoadDone` gerçek ilk Room emisyonunu ayırmıyor; HomePage legacy migration boş klasör listesinde Dashboard'a kalıcı yanlış migration yapabilir.
22. Remote katalog mutable `main` dalından timeout/hash/schema/category allowlist olmadan indiriliyor.
23. Remote katalog birleştirme kararı kayıt sayısına dayanıyor; silinen veya düşürülen veriler doğru yönetilemiyor.
24. FCM token yerel preference'a yazılıyor fakat topic subscription veya authenticated server registration görünmüyor; `db_update` dağıtım kanalı işlevsel olmayabilir.
25. Haftalık ve günlük dönem anahtarları ViewModel/process ömründe sabitlenebilir; pazartesi veya timezone değişiminde eski dönemde kalabilir.
26. CI'da ktlint görevi çağrılıyor fakat ktlint Gradle plugin/task tanımı yok.
27. Ana CI lint/detekt/format kontrolünü zorunlu bloklamıyor; lint `abortOnError=false`.
28. Launcher zorla dark theme açıyor, semantic renk yerine yoğun `Color.White` kullanıyor.
29. Uygulama boyutu yalnız base APK `sourceDir.length()` ile ölçülüyor.
30. Manuel backup import bazı alanları tam doğrulamadan önce mevcut dock/customization/override verisini temizleyebilir.
31. Notification permission metni “içerik okunmaz” diyor; uygulamada isteğe bağlı preview text çıkarma desteği bulunuyor.
32. Telemetri şeması var fakat ana sayfa event call-site'ları yok; “event var” ile “ölçüm çalışıyor” karıştırılıyor.

## 1.1 Yayın kararı

Aşağıdaki şartlar tamamlanmadan:

- Dashboard üretimde varsayılan açılmayacak.
- `KEY_HOME_PAGER_V2_ENABLED` geniş kullanıcı grubuna açılmayacak.
- Play Store üretim sürümü hazır kabul edilmeyecek.
- Yeni büyük özellik eklenmeyecek.
- “Tamamlandı” etiketi yalnız roadmap notuna bakılarak verilmeyecek.

## 1.2 Güncel yayın engelleyici sıra

```text
R00 → R01 → R02 → R03 → R04 → R05 → R06 → R07 → R08
→ R09 → R10 → R11 → R12 → R33 → R34 → R35
→ R13 → R14 → R15 → R37 → R30 → R32
```

`R20`, `R21`, `R27` ve `R28` rollout öncesi kalite kapısında paralel ilerleyebilir; ancak R30 kapanmadan tamamlanmış sayılmaz.

---

# 2. Yapay zekâ çalışma protokolü

Her döngü tek başına uygulanacaktır. Bir görev tamamlanmadan bağımlı göreve geçilmeyecektir.

## 2.1 Zorunlu çalışma sırası

1. Görevde belirtilen dosyaları güncel `main` üzerinden yeniden oku.
2. Roadmap ile kod farklıysa güncel kodu esas al ve farkı görev raporuna yaz.
3. Önce kök nedeni doğrulayan test ekle; yalnız sonra üretim kodunu değiştir.
4. Yalnız döngü kapsamındaki dosyaları değiştir.
5. Room değişikliği varsa migration/schema testi ekle.
6. Permission veya backup değişikliği varsa hem legacy hem modern Android kuralını test et.
7. Kullanıcı metni varsa `values`, `values-en` ve pseudolocale doğrulamasını güncelle.
8. Visible setting ekleniyorsa runtime consumer'ı aynı committe bulunmalıdır.
9. Policy/helper dosyası eklendiyse en az bir production call-site aynı committe olmalıdır.
10. Event schema ekleniyorsa event'in gerçek call-site ve consent-off testi aynı committe olmalıdır.
11. Kod değişikliğinden sonra ilgili testleri ve en az `compileDebugKotlin` çalıştır.
12. Döngü sonunda tek, anlamlı commit oluştur.
13. Test geçmeden `✅ Tamamlandı` yazma.
14. Gerçek cihaz kriteri varsa cihaz kanıtı olmadan `✅ Tamamlandı` yazma.
15. GitHub Actions sonucu yoksa “CI yeşil” yazma; yalnız yerel test çalıştırıldı de.
16. Hata yakalayıp sessizce yutma; typed result veya exception ile üst katmana taşı.
17. `delay(...)` ile Room/UI veri tutarlılığı çözme.
18. Aynı veri için ikinci preference/state kaynağı oluşturma.
19. İlgisiz formatlama ve geniş refactor yapma.
20. Roadmap durumunu değiştirmeden önce production usage search yap.

## 2.2 Her görev sonunda yazılacak rapor

```text
Döngü:
Commit:
Denetlenen önceki HEAD:
Değişen dosyalar:
Kök neden:
Uygulanan çözüm:
Production call-site:
Eklenen testler:
Çalıştırılan komutlar:
GitHub Actions sonucu:
Sonuç:
Cihaz kanıtı:
Kalan risk:
Sonraki bağımlı döngü:
```

## 2.3 Durum değerleri

- `⏳ Bekliyor`: Kodlama başlamadı.
- `🚧 Altyapı var, runtime eksik`: Model/policy/test var; production call-site yok.
- `🚧 Devam ediyor`: Runtime kodu var; test veya doğrulama eksik.
- `🟡 Kısmen tamamlandı`: Otomatik test kanıtı var; gerçek cihaz/yayın kanıtı eksik.
- `✅ Tamamlandı`: Runtime bağlantısı, otomatik test, CI ve gerekli cihaz kanıtı tamamlandı.
- `⛔ Bloke`: Harici bağımlılık, cihaz, mağaza veya izin bekleniyor.

## 2.4 Temel doğrulama komutları

```bash
./gradlew :app:compileDebugKotlin -PskipGoogleServices=true --console=plain
./gradlew :app:assembleDebug -PskipGoogleServices=true --console=plain
./gradlew :app:testDebugUnitTest -PskipGoogleServices=true --console=plain
./gradlew :app:lintDebug -PskipGoogleServices=true --console=plain
./gradlew :app:detekt -PskipGoogleServices=true --console=plain
./gradlew qualityGate -PskipGoogleServices=true --console=plain
```

> `ktlintCheck` ancak ktlint Gradle plugin'i ve görevi gerçekten eklendikten sonra komut listesine alınacaktır. Şu an workflow'da adı geçmesi, görevin mevcut olduğu anlamına gelmez.

Room değişikliği olan döngüler:

```bash
./gradlew :app:connectedDebugAndroidTest -PskipGoogleServices=true --console=plain
```

Release/R8 doğrulaması:

```bash
./gradlew :app:assembleRelease -PskipGoogleServices=true -PallowDebugReleaseSigning=true --console=plain
```

---

# 3. Doğrulanmış hata envanteri

| ID | Seviye | Sorun | Kök dosyalar | Hedef |
|---|---|---|---|---|
| DATA-01 | P0 | `PACKAGE_REMOVED + EXTRA_REPLACING=true` gerçek uninstall gibi silinebilir | `PackageChangeReceiver.kt` | R01 |
| DATA-02 | P0 | Statik receiver ve Activity receiver aynı package event'i işliyor | receiver, `LauncherActivity.kt` | R02 |
| DATA-03 | P0 | `PACKAGE_CHANGED` metadata güncellemesi `INSERT IGNORE` ile kayboluyor | receiver, `AppDao.kt` | R03 |
| DATA-04 | P0 | Repository mutasyon exception'larını yutuyor | repository'ler | R04 |
| DATA-05 | P0 | Kategori silme/taşıma transaction değil | ViewModel, DAO | R05 |
| DATA-06 | P0 | Uninstall referans temizliği dağınık ve eksik | receiver, prefs, dock, search | R06 |
| DATA-07 | P0 | Event path launchable/hidden katalog uygunluğunu doğrulamıyor | `PackageManagerHelper.kt` | R01-R03 |
| DATA-08 | P1 | PM yarışında retry biterse uygulama 12 saatlik reconcile'a kadar kaybolabilir | receiver, reconcile | R01 |
| SEC-01 | P0 | DeepSeek anahtarı `app_organizer_prefs` içinde; backup kuralı yanlış `deepseek_prefs.xml` dosyasını hariç tutuyor | `AppPrefs.kt`, backup XML | R07 |
| SEC-02 | P0 | Geniş backup include, kişi aksiyonu/konum cache/crash log/FCM/device state'i taşıyabilir | backup XML, prefs | R07 |
| SEC-03 | P0 | Package receiver `exported=true` ve yıkıcı mutasyon yapıyor | manifest, receiver | R02/R29 |
| CI-01 | P0 | Ana CI kalite kontrollerini zorunlu bloklamıyor | workflows | R08 |
| CI-02 | P0 | Workflow `ktlintCheck` çağırıyor fakat plugin/task tanımı yok | root/app Gradle, QA workflow | R08 |
| CI-03 | P1 | `be08f7b...` merge için combined status/check kanıtı yok | GitHub Actions | R08/R32 |
| HOME-01 | P0 | Dashboard preference görünür; runtime `dashboardEnabledForPager=false` | `HomeScreen.kt` | R09 |
| HOME-02 | P0 | Rollout prefs/policy var; production consumer yok | `AppPrefs`, rollout policy, HomeScreen | R09/R30 |
| HOME-03 | P0 | Layout visibility gerçek Dashboard state'ine tam uygulanmıyor | editor, Dashboard state | R10-R11 |
| HOME-04 | P1 | Search zone `AppPrefs` ve `HomeLayoutPrefs` ile çift kaynak | settings, layout prefs | R10 |
| HOME-05 | P1 | Editor tekil section, renderer üç grup sıralıyor | editor, SmartDashboard | R11 |
| HOME-06 | P1 | Legacy page migration boş folder listesinde kalıcı Dashboard anchor yazabilir | `HomePagePrefs.kt`, load state | R14 |
| ADAPT-01 | P1 | Adaptive policy var; HomeScreen hâlâ inline 600/840 eşikleri kullanıyor | policy, HomeScreen | R20 |
| ADAPT-02 | P1 | Tablet All Apps paneli yorumda adaptive, runtime sabit `380.dp` | HomeScreen | R20 |
| ADAPT-03 | P1 | Folder drag hesabı tablette de `colCount=4` | HomeScreen | R20 |
| SEARCH-01 | P1 | Tek global limit/app-first diğer kaynakları aç bırakıyor | SearchRepository | R12 |
| SEARCH-02 | P1 | Index rebuild transaction dışı delete-then-insert | SearchRepository, indexer'lar | R12 |
| SEARCH-03 | P1 | Search document için doğal unique `(sourceType, sourceId)` index yok | entity, Room migration | R12 |
| SEARCH-04 | P1 | Global search ve All Apps aynı query state'i kullanıyor | ViewModel/UI | R18 |
| SEARCH-05 | P0 | Ranking/fuzzy/phonetic/instant/usage/max-result ayarlarının runtime consumer'ı yok | Search settings, repository | R33 |
| SEARCH-06 | P0 | Contact/file izni geri alınsa eski docs aramada kalabilir | source eligibility/indexer | R34 |
| SEARCH-07 | P1 | Contact observer restart'ta kurulmayabilir; normal Job tek hatada ölebilir | `ContactsIndexer.kt` | R34 |
| SEARCH-08 | P1 | Unknown `SourceType` güvenli fallback yerine crash üretebilir | `SearchDocument.kt` | R12 |
| FILE-01 | P1 | Tek 1000 sınırı images-first nedeniyle video/audio/downloads aç bırakabilir | FilesIndexer | R35 |
| FILE-02 | P1 | “Any permission” tüm kaynaklar hazır gibi yorumlanabilir | permission dialog/index state | R35 |
| FILE-03 | P1 | Dokümantasyon “izin gerekmez” derken runtime medya izni istiyor | FilesIndexer/UI | R35 |
| CONTACT-01 | P1 | Contact yükleme 500 kişiye kadar N+1 telefon sorgusu yapıyor | ContactsIndexer | R34/R28 |
| NOTIF-01 | P1 | Listener reconnect post-time/importance state'ini tam kurmuyor | listener | R13 |
| NOTIF-02 | P1 | Nested coroutine DB emisyon sırasını bozabilir | LauncherViewModel | R13 |
| NOTIF-03 | P1 | 24 saat cutoff ViewModel ömründe sabit | LauncherViewModel | R13/R36 |
| NOTIF-04 | P1 | İzin açıklaması “içerik okunmaz” diyor; optional preview extraction var | permission dialog, listener | R22/R29 |
| START-01 | P1 | `initialLoadDone` initial empty StateFlow'u gerçek Room emisyonu sanabilir | LauncherViewModel | R14 |
| START-02 | P1 | Reconcile başka coroutine başlatıp gerçek bitişi beklemiyor | LauncherViewModel | R14 |
| START-03 | P1 | `delay(50)` ile Room/UI refresh bekleniyor | LauncherViewModel | R14 |
| REMOTE-01 | P1 | Mutable main URL, timeout/hash/imza/schema yok | AppDatabaseService | R15 |
| REMOTE-02 | P1 | Category ID/package/version doğrulaması ve downgrade koruması yok | service, classifier | R15 |
| REMOTE-03 | P1 | FCM token saklanıyor; topic/backend registration görünmüyor | FCM service, AppPrefs | R37 |
| BACKUP-01 | P1 | Import validation tamamlanmadan mevcut bazı alanları temizleyebilir | BackupManager | R26 |
| BACKUP-02 | P1 | SAF URI/biometric/device state cihazlar arası restore edilebilir | BackupManager/AppPrefs | R26 |
| PRIV-01 | P0 | `crash_logs`, contact actions ve location cache Auto Backup'a girebilir | CrashReporter, prefs, XML | R07 |
| TIME-01 | P1 | Singleton ZoneId process ömründe timezone değişimini kaçırabilir | AppModule, PeriodBoundaryResolver | R36 |
| TIME-02 | P1 | Weekly goals anahtarı ViewModel creation'da sabitleniyor | AppListViewModel | R36 |
| TEL-01 | P1 | Home telemetry schema/wrapper var; production page call-site yok | AppAnalytics, HomeScreen | R27 |
| TEL-02 | P1 | Roadmap event entegrasyonu iddiası usage search ile doğrulanmıyor | P21 notu | R27/R32 |
| PERF-01 | P1 | Macrobenchmark/baseline/jank kanıtı yok | benchmark altyapısı | R28 |
| UI-01 | P1 | Launcher `darkTheme=true` zorlaması | LauncherActivity | R21 |
| UI-02 | P1 | Semantic token yerine yoğun `Color.White` | launcher UI | R21 |
| UI-03 | P1 | Kullanıcı metinleri Kotlin içinde hardcode | Settings/UI | R22 |
| A11Y-01 | P1 | Pager accessibility action navigation lock'u bypass edebilir | HomePagerHost | R22 |
| SIZE-01 | P2 | `sourceDir.length()` gerçek app storage değil | PackageManagerHelper | R24 |
| MISSION-01 | P2 | XP, yıldız, digital-life score kavramları karışık | görev/skor | R25 |
| GOV-01 | P1 | Roadmap durumu production call-site ve CI kanıtı olmadan yükseltiliyor | roadmap süreci | R00/R32 |

---

# 4. Faz A — Yayın engelleyici veri bütünlüğü ve güvenlik

## R00 — Regresyon bazını ve kanıt sözleşmesini kilitle

**Amaç:** Düzeltme yaparken mevcut çalışan davranışların bozulmasını ve yalnız yorumla “tamamlandı” ilan edilmesini engellemek.

**Değişecek/yeni dosyalar:**

- `docs/internal/current_system_contract_1_3_89.md`
- `app/src/test/.../PackageEventContractTest.kt`
- `app/src/test/.../SettingsRuntimeContractTest.kt`
- `scripts/audit_runtime_consumers.*`
- CI workflow artifact ayarları.

**Yapılacaklar:**

1. `be08f7b...` HEAD için davranış envanteri çıkar.
2. Her görünür preference kontrolü için en az bir production read/consumer doğrula.
3. Her policy/helper için test dışında production call-site doğrula.
4. Her telemetry event için gerçek call-site, validator ve consent-off testi doğrula.
5. Test raporları, lint, detekt ve runtime consumer raporu artifact olarak saklansın.
6. Roadmap durum parser'ı “Tamamlandı” satırında commit/test/device kanıtı arayabilsin.

**Kabul:** P20/P21/P24 benzeri “dosya var ama runtime yok” durumu otomatik raporlanır.

**Durum:** ⏳ Bekliyor

---

## R01 — Replacing paket olayını ve katalog uygunluğunu düzelt

**Değişecek dosyalar:**

- `PackageChangeReceiver.kt`
- `PackageManagerHelper.kt`
- yeni `PackageCatalogEligibility.kt`
- package event testleri.

**Kesin çözüm:**

```kotlin
sealed interface PackageCatalogDecision {
    data class AddOrUpdate(val app: AppInfo) : PackageCatalogDecision
    data object RemoveFromCatalog : PackageCatalogDecision
    data object Ignore : PackageCatalogDecision
    data class Retry(val reason: String) : PackageCatalogDecision
}
```

1. `ACTION_PACKAGE_REMOVED` + `EXTRA_REPLACING=true` hiçbir kullanıcı verisini silmemeli.
2. `getAppInfo` yerine `resolveCatalogEligibility(packageName)` kullanılmalı.
3. Paket launchable değilse veya `shouldHide` eşleşiyorsa katalogdan çıkarılmalı/ignore edilmeli.
4. Launcher component disable/enable geçişi test edilmeli.
5. PM üç retry sonunda hazır değilse WorkManager one-time reconcile planlanmalı; sessiz skip yapılmamalı.
6. `goAsync` işi timeout ile sınırlandırılmalı.

**Testler:** update, uninstall, non-launchable install, hidden system package, component disabled/enabled, PM delayed visibility.

**Durum:** ⏳ Bekliyor

---

## R02 — Tek receiver ve güvenli PackageEventCoordinator

**Değişecek dosyalar:** receiver, `LauncherActivity.kt`, manifest, yeni coordinator.

1. Activity dynamic receiver kaldırılmalı.
2. Manifest receiver yalnız event adapter olmalı.
3. `android:exported="false"` kullanılmalı ve gerçek cihazda sistem package broadcast alımı doğrulanmalı.
4. Explicit spoof intent instrumentation testi eklenmeli.
5. Tek paket için aynı anda gelen event'ler `Mutex`/actor ile seri işlenmeli.
6. Tek coordinator DB, search, dock/favorite cleanup ve notification kararını yönetmeli.

**Durum:** ⏳ Bekliyor

---

## R03 — Insert-only ve metadata update yollarını ayır

1. `insertApps` yalnız yeni kayıt sınıflandırması için kullanılmalı.
2. `updateInstalledMetadata` kullanıcı category/hidden/usage/classification alanlarını korumalı.
3. DAO update row count döndürmeli; `0` ise typed not-found sonucu üretmeli.
4. `INSERT IGNORE` update amacıyla kullanılmamalı.
5. Package changed, renamed app, version update ve icon change testleri eklenmeli.

**Durum:** ⏳ Bekliyor

---

## R04 — Repository mutasyon sonuç sözleşmesi

```kotlin
sealed interface MutationResult<out T> {
    data class Success<T>(val value: T) : MutationResult<T>
    data class NotFound(val key: String) : MutationResult<Nothing>
    data class Failure(val operation: String, val cause: Throwable) : MutationResult<Nothing>
}
```

- Loglayıp normal dönme kaldırılmalı.
- ViewModel yalnız gerçek success sonrasında toast, XP, migration flag ve telemetry üretmeli.
- Search/bootstrap/import partial failure ayrıntılı rapor vermeli.

**Durum:** ⏳ Bekliyor

---

## R05 — Atomik kategori mutasyonları

- `db.withTransaction` altında app taşıma + category silme + search index update planı.
- Default/locked category silme engeli domain policy'ye alınmalı.
- Mid-transaction failure rollback testi.
- User override ve folder customizations kategoriyle birlikte kontrollü temizlenmeli.

**Durum:** ⏳ Bekliyor

---

## R06 — Paket referans cleanup use-case

Tek `RemovePackageReferencesUseCase` şu kaynakları temizlemeli:

- apps row,
- search document,
- dock app item,
- favorites,
- manual category override,
- notification read/preview state,
- suggestion/rejection cache,
- icon cache,
- app-specific note,
- package bazlı pending work.

Update/replacing akışında bu use-case çalışmamalı.

**Durum:** ⏳ Bekliyor

---

## R07 — Secret store ve backup privacy allowlist

**Bu görev P0'dır. Önceki roadmap bulgusu düzeltilmiştir.**

DeepSeek key `deepseek_prefs.xml` içinde değildir; `AppPrefs.PREFS_NAME = app_organizer_prefs` içinde saklanır. Bu nedenle mevcut XML exclusion anahtarı korumaz.

**Yeni mimari:**

```text
backup_safe_prefs.xml       → tema, görünüm, kullanıcı tercihleri
runtime_device_prefs.xml    → FCM token, SAF URI, install/session marker, rate limits
private_history_prefs.xml   → contact actions, search/usage/read histories, weather target/cache
secret_prefs.xml            → DeepSeek/API secrets; Keystore-backed
```

**Yapılacaklar:**

1. Secret key Keystore-backed store'a taşınmalı.
2. Legacy `app_organizer_prefs/deepseek_api_key` bir kez migrate edilmeli; başarılı yazımdan sonra eski key silinmeli.
3. `backup_rules.xml` ve `data_extraction_rules.xml` denylist yerine mümkünse allowlist kullanmalı.
4. `secret_prefs`, runtime/device prefs, private history prefs, `crash_logs`, database WAL/SHM ve widget IDs kesin hariç tutulmalı.
5. `contact_action_prefs`, weather cache, crash reporter prefs/logları, FCM token, SAF URI, telemetry rate limit ve notification read state sınıflandırılmalı.
6. “Veri cihazda kalır” metni Auto Backup gerçeğiyle uyumlu hale getirilmeli.
7. Legacy Android ve Android 12+ XML parity testi yazılmalı.
8. ADB backup/device-transfer testinde secret marker ve private history bulunmadığı kanıtlanmalı.

**Kabul:** DeepSeek key ve private local histories hiçbir cloud backup/device transfer çıktısında yoktur.

**Durum:** ⏳ Bekliyor

---

## R08 — Gerçek bloklayıcı CI kalite kapısı

**Düzeltilecekler:**

1. JDK tüm workflow'larda 17 olmalı.
2. Root Gradle'a ktlint plugin/task gerçekten eklenmeli veya workflow'daki sahte `ktlintCheck` kaldırılıp detekt-formatting tek formatter olarak belirlenmeli.
3. `continue-on-error` kalite adımlarından kaldırılmalı.
4. lint `abortOnError=true` olmalı.
5. Ana CI: compile, unit, lint, detekt, format, duplicate category, runtime-consumer audit.
6. Release workflow: R8, migration, backup rules, screenshot verify, benchmark smoke.
7. Merge commit status görünmüyorsa release evidence eksik sayılmalı.

**Kabul:** Bilerek oluşturulan lint/format/no-op-setting hatası CI'ı kırar.

**Durum:** ⏳ Bekliyor

---

# 5. Faz B — Dashboard, layout ve rollout doğruluğu

## R09 — Dashboard no-op kontrolünü kaldır

1. `dashboardEnabledForPager=false` kaldırılmadan önce R10/R11 ve rollout fallback hazır olmalı.
2. Gerçek karar:

```kotlin
val dashboardEnabled = HomePagerRolloutPolicy.dashboardEnabled(
    flagEnabled = rolloutEnabled,
    safeMode = rolloutSafeMode,
    dashboardPreferenceEnabled = smartDashboardPrefEnabled,
)
```

3. Rollout ve safe-mode prefs reaktif okunmalı.
4. Toggle görünür olacaksa anında page planını değiştirmeli; aksi halde UI'dan gizlenmeli.
5. Legacy page content Dashboard açıkken ikinci kez render edilmemeli.
6. Empty folder + Dashboard off güvenli fallback sayfası üretmeli.

**Durum:** 🚧 Altyapı var, runtime eksik — policy/prefs mevcut; HomeScreen hâlâ `false`.

---

## R10 — Tek HomeLayout preference kaynağı

- `HomeLayoutRepository` typed `StateFlow<HomeLayoutConfig>` sunmalı.
- Search position yalnız HomeLayout içindeki MAIN_SEARCH zone'dan gelmeli.
- SearchSettingsScreen'deki ayrı AppPrefs kontrolü kaldırılmalı veya repository'ye delege edilmeli.
- HomeScreen `remember(context)` snapshot yerine reaktif collect kullanmalı.
- Visibility/order/zone tek atomik write ile saklanmalı.

**Durum:** ⏳ Bekliyor

---

## R11 — Editor ve renderer birebir sözleşmesi

1. Editörde görünen her `HomeSectionId` renderer'da tekil karşılığa sahip olmalı.
2. Üç temsilci grup yaklaşımı kaldırılmalı veya editör yalnız gerçek üç grubu göstermeli.
3. Section visibility DashboardUiState'e uygulanmalı.
4. `hasAnyContent` gerçek görünür layout item'larından türetilmeli.
5. Contract test her editable section için hide/show/move sonucunu doğrulamalı.

**Durum:** ⏳ Bekliyor

---

## R20 — Telefon/tablet adaptif runtime entegrasyonu

**Mevcut:** Policy ve JVM testleri var.

**Eksik runtime değişiklikleri:**

1. HomeScreen inline `isTablet` ve `screenColumns` hesaplarını policy'ye bağla.
2. All Apps panel sabit `380.dp` yerine policy değeri kullansın.
3. Search/dock gerçek `widthIn(max=...)` ile ortalansın; yalnız yorum eklemek yetmez.
4. Folder drag `colCount=4` yerine güncel `screenColumns` kullansın.
5. Screen width/orientation değişiminde state yeniden hesaplansın.
6. 600/840dp, portrait/landscape ve foldable config change testleri.
7. Gerçek 7–8 ve 10+ inç tablet screenshot kanıtı.

**Durum:** 🚧 Altyapı var, runtime eksik

---

## R21 — Tema ve semantic renk sistemi

- `AppOrganizerTheme(darkTheme=true)` kaldırılmalı veya ürün kararı açık enum olmalı.
- Wallpaper content contrast modeli oluşturulmalı.
- `Color.White.copy` yerine semantic tokens.
- Light/dark/dynamic + wallpaper contrast snapshot'ları.

**Durum:** ⏳ Bekliyor

---

## R22 — Lokalizasyon, erişilebilirlik ve doğru izin açıklaması

1. Hardcode TR metinleri resource'a taşı.
2. Notification listener açıklaması optional preview text'i dürüstçe açıklamalı.
3. Pager custom actions yalnız navigation/pager lock açıkken sunulmalı.
4. Mission/Digital Life kartları tek semantic description.
5. 48dp target, font 2.0, RTL, pseudolocale.
6. Reduce motion runtime değişimine reaktif olmalı.

**Durum:** ⏳ Bekliyor

---

## R23 — Screenshot/golden regresyon altyapısı

- Tek araç: Paparazzi, Roborazzi veya Compose Preview Screenshot Testing.
- Matris: 360/411/600/840dp, portrait/landscape, light/dark, font 1.0/1.3/2.0, TR/EN/pseudo.
- Dashboard off/on/empty/dense, 0/1/8/20/42 folder, All Apps search/empty/long list.
- CI yalnız verify; golden update manuel.

**Durum:** ⏳ Bekliyor

---

## R30 — Kontrollü Dashboard rollout

**Mevcut:** preference anahtarları, saf policy, debug toggle ve diagnostics alanları eklenmiş.

**Eksikler:**

1. Policy production route'a bağlanmalı.
2. Safe mode gerçek eski render path'e dönmeli.
3. Toggle değişimi process restart gerektirmeden uygulanmalı.
4. Rollback category/order/anchor/preferences kaybetmemeli.
5. Consent/telemetry kapalıyken rollout çalışmalı.
6. Dört cihaz flag on/off/safe-mode kanıtı.
7. Crash/ANR threshold'a bağlı otomatik rollback kararı tanımlanmalı.

**Durum:** 🚧 Altyapı var, runtime eksik

---

## R31 — Legacy render ve preference temizliği

**Mevcut ilerleme:** semantic anchor yazılırken eski ham index tekrar yazılmıyor.

**Kalan:**

- Eski page APIs yalnız migration reader olarak sınırlandırılmalı.
- Duplicate legacy Dashboard content branch rollout sonrası silinmeli.
- Kullanılmayan comments/TODO/P24 notları güncellenmeli.
- Old tests yeni typed modellerle taşınmalı.

**Durum:** 🟡 Kısmen tamamlandı — rollout kanıtı eksik

---

# 6. Faz C — Arama, kişi ve dosya kaynakları

## R12 — Search fairness, doğal unique key ve atomik indeks

1. `(sourceType, sourceId)` unique index + Room migration.
2. Upsert doğal key ile yapılmalı.
3. Rebuild staging/transaction ile atomik olmalı.
4. Kaynak başına minimum kota + global rerank.
5. Unknown SourceType güvenli `UNKNOWN`/skip davranışı.
6. Contacts/Files indexer delete-then-insert transaction'a alınmalı.
7. App/category rename/remove indeks tutarlılık testi.

**Durum:** ⏳ Bekliyor

---

## R18 — Global Search ve All Apps query ayrımı

- `globalSearchQuery`, `allAppsQuery`, `folderFilterQuery` ayrı state.
- Drawer kapanması global query'yi temizlememeli.
- Search source enable/permission state global query'den bağımsız.

**Durum:** ⏳ Bekliyor

---

## R19 — Türkçe normalizasyon ve gerçek ranking

- Tek `SearchNormalizer`: Locale.ROOT storage + Türkçe aliases/transliteration.
- `ş/s, ı/i, ğ/g, ç/c, ö/o, ü/u` toleransı ayara bağlı.
- FTS ve LIKE aynı normalize modelini kullanmalı.
- Ranking profile gerçekten query order/quotas/rerank'i değiştirmeli.

**Durum:** ⏳ Bekliyor

---

## R33 — Görünür arama ayarlarının runtime sözleşmesi

**P0 mantık hatası:** SearchSettingsScreen'de bulunan bazı ayarlar yalnız preference'a yazılıyor.

**Yeni model:**

```kotlin
data class SearchPreferencesSnapshot(
    val rankingProfile: SearchRankingProfile,
    val fuzzyEnabled: Boolean,
    val phoneticEnabled: Boolean,
    val instantEnabled: Boolean,
    val sortByUsage: Boolean,
    val maxResults: Int,
    val showIcons: Boolean,
    val showContactAvatar: Boolean,
)
```

**Yapılacaklar:**

1. Typed repository/flow ile reaktif snapshot üret.
2. Ranking profile source order/quotas'ı değiştirsin.
3. Usage sort yalnız app grubunda stabil tie-breaker olsun.
4. Max result gerçek repository/UI limitini belirlesin; hardcoded 24 kaldırılmalı.
5. Instant kapalıysa IME submit olmadan search çalışmamalı.
6. Fuzzy/phonetic gerçek normalizer/ranker'a bağlanmalı.
7. Uygulanmayacak ayar UI'dan kaldırılmalı; no-op bırakılmamalı.
8. `SettingsRuntimeContractTest` production consumer bulunmayan görünür setting'i fail etmeli.

**Durum:** ⏳ Bekliyor

---

## R34 — Permission-aware source lifecycle ve Contact observer

1. Source eligibility = preference açık **ve** runtime permission valid **ve** index state güvenli.
2. Permission revoke edildiğinde stale contact/file docs anında silinmeli.
3. Process start'ta preference/permission/index reconcile yapılmalı.
4. Contacts observer app scope `SupervisorJob` veya WorkManager actor ile yönetilmeli.
5. Bir index hatası gelecekteki observer event'lerini öldürmemeli.
6. Contact loading N+1 phone sorgusu yerine batch query.
7. Grant→index→revoke→search testi: sonuç sıfır.
8. Process restart'ta observer yeniden kayıt testi.

**Durum:** ⏳ Bekliyor

---

## R35 — Files index fairness ve scoped-storage sözleşmesi

1. Her collection için ayrı izin/availability state.
2. “Any permission” yerine hangi media türlerinin erişilebilir olduğu raporlanmalı.
3. 1000 global images-first sınırı kaldırılmalı:
   - per-source quota + global modified-time merge, veya
   - provider limitli ayrı sorgular + fair merge.
4. Downloads kapsamı Android sürümüne göre dürüstçe belirtilmeli; gerekirse SAF.
5. Dokümantasyon ile runtime permission davranışı eşleşmeli.
6. Partial permission UI “tam hazır” göstermemeli.
7. 5000 images + video/audio/downloads fixture fairness testi.

**Durum:** ⏳ Bekliyor

---

# 7. Faz D — Bildirim, yükleme ve zaman doğruluğu

## R13 — Notification summary state ve seri DB yazımı

- Listener tek immutable summary yayınlasın: counts, lastPostedAt, previews, texts.
- Reconnect active notifications üzerinden bütün alanları yeniden kursun.
- ViewModel nested launch yerine `collectLatest`/actor ile seri yazsın.
- Moving 24h window timer/period flow ile yenilensin.
- Read-state ve active-system-notification ayrımı korunsun.

**Durum:** ⏳ Bekliyor

---

## R14 — CatalogLoadState, reconcile ve güvenli page migration

```kotlin
sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState
    data object Empty : CatalogLoadState
    data object Ready : CatalogLoadState
    data class Failed(val code: String) : CatalogLoadState
}
```

1. Gerçek DAO first emission ile state üretilmeli.
2. `allAppsSource` initial empty value Ready sayılmamalı.
3. Reconcile suspend use-case olarak gerçek tamamlanmayı döndürmeli.
4. `delay(50)` kaldırılmalı.
5. HomePage migration folder catalog Ready olmadan çalışmamalı.
6. Empty input migration flag yazmamalı; `Deferred` dönmeli.
7. Cold start empty initial state→Room data testi.

**Durum:** ⏳ Bekliyor

---

## R36 — Dinamik timezone, gün ve hafta rollover

1. `ZoneId.systemDefault()` singleton snapshot yerine dinamik provider.
2. `TIMEZONE_CHANGED`, `TIME_SET`, `DATE_CHANGED` event'lerinde period state yenilensin.
3. Weekly goals `periodKeyFlow.flatMapLatest(dao::observeGoals)` kullansın.
4. Notification 24h, mission deadlines, daily limits ve report boundaries aynı period kaynağını kullansın.
5. Pazartesi 00:00, DST 23/25 saat, timezone değişimi ve process-alive testleri.
6. WorkManager görevleri timezone değişiminde yeniden planlansın.

**Durum:** ⏳ Bekliyor

---

# 8. Faz E — Remote katalog, FCM ve backup/import

## R15 — Güvenli remote katalog

1. Mutable `main` URL yerine immutable release asset veya signed manifest.
2. Connect/read timeout, HTTP status, max payload, max entry count.
3. Monotonic version; downgrade reddi.
4. Package name ve category ID allowlist/schema doğrulaması.
5. Remote category bundled/android sinyalini yalnız doğrulanmış policy ile override etmeli.
6. Kayıt sayısına dayalı merge heuristic kaldırılmalı; full snapshot/delta semantiği açık olmalı.
7. Stage→validate→atomic commit→last-known-good rollback.
8. Hash/signature mismatch ve unknown category testleri.

**Durum:** ⏳ Bekliyor

---

## R26 — Güvenli manuel backup/import

1. Bütün JSON önce size/schema/version/item-count doğrulamasından geçmeli.
2. Immutable `RestorePlan` oluşturulmadan mevcut veri temizlenmemeli.
3. Missing field “preserve” veya version sözleşmesine göre explicit reset olmalı.
4. Transaction/compensation ve section-level sonuç raporu.
5. SAF URI, FCM token, biometric enabled, permissions/device IDs restore edilmemeli.
6. Standard JSON + authenticated encrypted backup seçeneği.
7. Notification text/contact/file path/secrets export'a girmemeli.
8. Partial/corrupt v3-v6 backup mevcut veriyi silmeme testleri.

**Durum:** ⏳ Bekliyor

---

## R37 — FCM ve remote update teslimat sözleşmesi

**Sorun:** FCM token saklanıyor, `db_update` mesajı işleniyor; ancak topic subscription veya authenticated backend registration görünmüyor.

**Karar seçenekleri:**

A. Consent sonrası sabit, privacy-reviewed topic subscription.  
B. Authenticated registration backend ve token rotation/delete lifecycle.  
C. FCM mekanizmasını kaldır; periodic verified catalog worker kullan.

**Zorunlu kurallar:**

- Token device-local backup-excluded store'da.
- Raw/partial token loglanmaz.
- `onNewToken`, logout/reset/uninstall lifecycle tanımlı.
- Message yalnız doğrulanmış katalog fetch'ini tetikler; payload kategori verisi doğrudan uygulanmaz.
- End-to-end test cihazına update teslim kanıtı.

**Durum:** ⏳ Bekliyor

---

# 9. Faz F — Mimari, performans, ürün anlamı ve yayın kanıtı

## R16 — Typed HomePreferences ve HomeUiState

- Onlarca SharedPreferences listener yerine repository `StateFlow`.
- `collectAsStateWithLifecycle`.
- `HomeUiState` immutable ve feature substate'lere ayrılmış.
- IO/permission/feature state UI'dan ayrılmış.

**Durum:** ⏳ Bekliyor

---

## R17 — HomeScreen orchestration dosyasına indir

- Gesture coordinator, overlay host, pager coordinator, layout state holder ayrılmalı.
- HomeScreen hedefi yalnız state bağlama ve top-level composition.
- Local state ownership matrisi dokümante edilmeli.

**Durum:** ⏳ Bekliyor

---

## R24 — Uygulama boyutunu doğru ölç veya doğru adlandır

- StorageStatsManager ile app/data/cache mümkünse.
- Aksi halde split APK toplamı `APK boyutu` olarak etiketlenmeli.
- Bilinmiyorsa `0 B` değil “Bilinmiyor”.

**Durum:** ⏳ Bekliyor

---

## R25 — XP, yıldız, Digital Life ve mission instance modeli

- XP = düzenleme/görev ilerleme puanı.
- Digital Life = 0–100 davranış kalitesi.
- Stars = görev başarısı.
- Ana okuma `mission_instances`; dual-write migration sonrası kaldırılmalı.
- Ödül yalnız başarılı DB transaction sonrası.
- Baseline/target instance oluşturulurken sabitlenmeli.

**Durum:** ⏳ Bekliyor

---

## R27 — Privacy-safe telemetri ve diagnostics

**Mevcut:** Event modelleri, validator, wrapper'lar ve home architecture diagnostics alanları var.

**Eksik:**

1. `homePageViewed`, `homePageSwiped`, `homeSearchOpened` production call-site'ları.
2. Settled page impression debounce ve navigation-source doğruluğu.
3. Runtime event testleri; yalnız wrapper testleri yeterli değil.
4. Consent off: remote gateway ve local aggregate write yok kanıtı.
5. Diagnostics'te category/package/query/contact/file path bulunmadığını fixture testi.
6. Dört cihaz health report karşılaştırması.
7. Rate-limit prefs backup-excluded device runtime store'a taşınmalı.

**Durum:** 🚧 Altyapı var, runtime call-site eksik

---

## R28 — Macrobenchmark, baseline profile ve jank kanıtı

**Mevcut:** folder search `remember` ve `spec.folders.size` mikro optimizasyonları var.

**Kalan:**

- Cold start.
- Dashboard first compose.
- Dashboard↔folder swipe.
- All Apps tablet panel.
- Search typing/results.
- 100/500/1000 apps.
- Baseline profile.
- JankStats/Macrobenchmark artifact.

**Durum:** 🚧 Devam ediyor — ölçüm altyapısı yok

---

## R29 — Manifest ve permission hardening

- Package receiver exported false.
- QUERY_ALL_PACKAGES Play declaration ve minimal queries review.
- Media permissions özellik kapalıysa istenmemeli.
- Notification listener ve contacts açıklaması gerçek davranışla eşleşmeli.
- FileProvider path en dar alana indirilmeli.
- Backup/service/exported component matrix testi.

**Durum:** ⏳ Bekliyor

---

## R32 — Nihai release evidence paketi

**Zorunlu artifact'ler:**

```text
compile/build report
unit test report
lint report
detekt/format report
runtime-consumer contract report
Room migration report
backup allowlist/secret test report
permission revoke test report
screenshot diff report
benchmark/baseline report
four-device diagnostics
upgrade test report
backup/restore matrix
Play permission declaration
privacy/data safety checklist
rollout/rollback evidence
```

**Yayın kapısı:**

- P0 açık bulgu: 0.
- P1 açık bulgu: 0 veya imzalı release exception.
- Crash/ANR kritik regresyon: 0.
- Upgrade sırasında kategori/favori/dock kaybı: 0.
- Backup çıktısında secret/private-history: 0.
- Visible no-op setting: 0.
- Permission revoke sonrası stale contact/file result: 0.
- Dashboard flag/safe-mode gerçek runtime testi: başarılı.
- GitHub Actions status/check: yeşil.

**Durum:** ⏳ Bekliyor

---

# 10. Dosya bazlı değişiklik haritası

| Dosya | Ana görev |
|---|---|
| `PackageChangeReceiver.kt` | replacing, eligibility, tek coordinator |
| `LauncherActivity.kt` | dynamic receiver kaldırma, tema |
| `AndroidManifest.xml` | receiver exported, izin hardening |
| `PackageManagerHelper.kt` | launchable eligibility, doğru app size |
| `AppDao.kt` | insert/update ayrımı, row count |
| `AppRepository.kt` | typed mutation results |
| `CategoryDao.kt` | transaction-safe category mutation |
| `SearchDocument.kt` | unique natural key, safe SourceType |
| `SearchDao.kt` | atomic upsert/rebuild |
| `SearchRepository.kt` | fairness, preference consumer, permission eligibility |
| `ContactsIndexer.kt` | lifecycle, SupervisorJob, batch query, revoke cleanup |
| `FilesIndexer.kt` | per-source permission/state, fair quotas |
| `AppNotificationListenerService.kt` | immutable summary, reconnect |
| `LauncherViewModel.kt` | load state, serial writes, query split |
| `HomeScreen.kt` | rollout/adaptive runtime bağlantısı, typed state |
| `HomeAdaptiveLayoutPolicy.kt` | üretim tek breakpoint kaynağı |
| `HomePagerRolloutPolicy.kt` | route-level production consumer |
| `HomePagePrefs.kt` | deferred migration on not-ready catalog |
| `HomeLayoutPrefs.kt` | tek reactive layout source |
| `HomeLayoutEditorScreen.kt` | renderer ile birebir contract |
| `SmartDashboardPage.kt` | item-level order/visibility |
| `SearchSettingsScreen.kt` | no-op ayarları implement/remove |
| `ContextualPermissionDialog.kt` | doğru disclosure ve partial grants |
| `AppDatabaseService.kt` | immutable/versioned/validated catalog |
| `AppFirebaseMessagingService.kt` | delivery contract/token lifecycle |
| `BackupManager.kt` | validate-plan-transaction restore |
| `backup_rules.xml` | allowlist/secret exclusion |
| `data_extraction_rules.xml` | modern parity |
| `AppPrefs.kt` | prefs partition/migration |
| `TelemetryManager.kt` | device-local limiter, consent tests |
| `DiagnosticsReportManager.kt` | privacy-safe evidence |
| workflows/Gradle | gerçek bloklayıcı quality gate |

---

# 11. Yasak desenler

1. `delay(...)` ile Room/UI refresh beklemek.
2. Exception loglayıp başarılı dönmek.
3. Aynı Android event'ini iki receiver ile işlemek.
4. Update için `INSERT IGNORE` kullanmak.
5. Aynı setting'i iki preference dosyasında aktif tutmak.
6. Görünür ayar ekleyip runtime consumer eklememek.
7. Policy/helper ekleyip yalnız testte kullanmak.
8. Event schema ekleyip production call-site eklememek.
9. Roadmap yorumunu CI/runtime kanıtı saymak.
10. Delete-all→insert-all işlemini transaction dışında yapmak.
11. Permission revoke edildiğinde stale private index tutmak.
12. Auto Backup'ta geniş include + eksik denylist kullanmak.
13. Secret'ı genel AppPrefs içinde saklamak.
14. Mutable branch remote dosyasını doğrulamasız uygulamak.
15. Source fairness olmadan tek global limit kullanmak.
16. Flow collector içinde kontrolsüz nested `viewModelScope.launch` açmak.
17. `Color.White` ile semantic tema kontratını bypass etmek.
18. Hardcode kullanıcı metni eklemek.
19. CI'da `continue-on-error` ile kalite kontrolü yapmak.
20. Gerçek cihaz kanıtı yokken `✅ Tamamlandı` yazmak.

---

# 12. Definition of Done

Bu roadmap yalnız aşağıdakilerin tamamında bitmiş sayılır:

1. Update sırasında kullanıcı verisi silinmez.
2. Uninstall bütün referansları temizler.
3. Package event tek, non-exported coordinator yolundan geçer.
4. Non-launchable/hidden package launcher kataloğuna girmez.
5. Repository hataları üst katmana ulaşır.
6. Category mutation transaction'dır.
7. DeepSeek/API secrets backup dışında ve Keystore-backed'tır.
8. Contact actions, location cache, crash logs, FCM token ve device state Auto Backup dışında kalır.
9. CI compile/test/lint/detekt/format/runtime-consumer adımlarını bloklar.
10. Dashboard toggle ve rollout flag gerçek page planını değiştirir.
11. Safe mode veri kaybetmeden legacy yola döner.
12. Adaptive policy HomeScreen, panel, dock/search ve drag tarafından gerçekten kullanılır.
13. Layout editor ve renderer birebirdir.
14. Search zone tek kaynaktır.
15. Bütün görünür search ayarları runtime'da çalışır veya kaldırılmıştır.
16. Search fairness kişi/dosya/ayar kaynaklarını aç bırakmaz.
17. Search rebuild atomik ve natural-key unique'tir.
18. Permission revoke sonrası stale contact/file result yoktur.
19. Contact observer restart ve hata sonrası çalışmaya devam eder.
20. File index partial permissions ve fair source quotas ile çalışır.
21. Notification reconnect/ordering/moving-window testleri geçer.
22. Catalog load typed state ve page migration readiness testleri geçer.
23. Timezone/midnight/Monday rollover doğru çalışır.
24. Remote catalog immutable, versioned, validated ve rollback'lidir.
25. FCM update delivery ya uçtan uca çalışır ya ürün kodundan kaldırılmıştır.
26. Backup import parse/validate tamamlanmadan veri silmez.
27. Home page telemetry production call-site'lara bağlıdır ve consent-off no-op kanıtlıdır.
28. Diagnostics kişisel identifier taşımaz.
29. Screenshot matrisi kritik fark üretmez.
30. Benchmark ve baseline profile artifact'i vardır.
31. App size doğru veya doğru şekilde `APK boyutu/Bilinmiyor` etiketlidir.
32. XP/yıldız/Digital Life kullanıcıya net anlatılır.
33. Dört cihaz upgrade, 24 saat kullanım ve rollout rollback testi geçer.
34. GitHub Actions sonucu yeşildir.
35. Release evidence paketi saklanmıştır.

---

# 13. Güncel uygulama sırası

```text
R00  Regresyon ve kanıt sözleşmesi
R01  Replacing + katalog eligibility
R02  Tek non-exported receiver/coordinator
R03  Insert/update ayrımı
R04  Repository typed error contract
R05  Category transaction
R06  Package reference cleanup
R07  Secret store + backup allowlist
R08  Bloklayıcı CI
R09  Dashboard runtime gate
R10  Tek HomeLayout source
R11  Editor-renderer birebirliği
R12  Search fairness + atomic unique index
R33  Search setting runtime contract
R34  Permission-aware source lifecycle
R35  File index fairness
R13  Notification summary/order/window
R14  CatalogLoadState + safe migration
R36  Timezone/period rollover
R15  Secure remote catalog
R37  FCM delivery contract
R16  Typed HomePreferences/HomeUiState
R17  HomeScreen parçalama
R18  Query ayrımı
R19  Türkçe normalize/rank
R20  Adaptive runtime integration
R21  Tema/semantic colors
R22  Lokalizasyon/a11y/disclosure
R23  Screenshot tests
R24  Doğru app size
R25  Mission/XP/star modeli
R26  Safe manual backup/import
R27  Telemetry/diagnostics runtime
R28  Benchmark/baseline
R29  Manifest/permission hardening
R30  Controlled rollout
R31  Legacy cleanup
R32  Release evidence
```

---

# 14. Hemen başlanacak görev

İlk uygulama commit'i **R00** olmalıdır. Ardından kullanıcı verisi ve güvenlik riski nedeniyle sırasıyla **R01–R08** tamamlanmalıdır.

P20–P25 roadmap notlarının mevcut “kısmen tamamlandı” ifadeleri bu belgedeki runtime kanıt kurallarına göre yeniden değerlendirilmelidir. Özellikle:

- P20: policy var, HomeScreen runtime bağlantısı yok.
- P21: event schema/wrapper var, page event production call-site yok.
- P22: diagnostics kodu var, gerçek cihaz ve privacy output kanıtı yok.
- P23: mikro optimizasyon var, benchmark yok.
- P24: preference/policy var, Dashboard runtime gate yok.
- P25: ham index write temizliği var, rollout sonrası legacy cleanup eksik.

**Net ürün kararı:** Şu anda Dashboard rollout açılmamalıdır. Önce temel veri bütünlüğü, backup güvenliği, no-op setting'ler ve permission revoke davranışı düzeltilmelidir.
