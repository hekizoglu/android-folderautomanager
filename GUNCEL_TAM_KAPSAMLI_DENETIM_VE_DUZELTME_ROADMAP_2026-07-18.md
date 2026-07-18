# AppOrganizer — Güncel Tam Kapsamlı Sistem Denetimi ve Düzeltme Roadmap'i

> **Denetim tarihi:** 2026-07-18  
> **Denetlenen dal:** `main`  
> **Denetlenen commit:** `7e8f49436e4cd4845108d3373e3416830b4c03c7`  
> **Denetlenen sürüm:** `1.3.88 (111)`  
> **Kapsam:** veri bütünlüğü, paket olayları, Room, repository sözleşmeleri, arama, bildirim, Dashboard geçişi, ana ekran yerleşimi, görev/skor sistemi, performans, görsel kalite, erişilebilirlik, güvenlik, yedekleme, CI ve yayın süreci.  
> **Ana karar:** Yeni özellik geliştirme, aşağıdaki yayın engelleyici döngüler tamamlanana kadar ikinci plandadır. Öncelik yeni özellik değil, mevcut sistemin doğru ve güvenilir çalışmasıdır.

---

# 1. Yönetici özeti

Repo ciddi biçimde ilerlemiştir. Tek pager mimarisi, semantic sayfa anchor'ı, global shell, Dashboard bileşeni, layout v2, Focus Mode politikası ve erişilebilirlik altyapısı önemli kazanımlardır.

Ancak güncel kodda hâlâ şu sınıflarda doğrulanmış riskler bulunmaktadır:

1. Uygulama güncellemesinin gerçek kaldırma gibi işlenebilmesi.
2. Aynı paket olayının iki receiver tarafından işlenmesi.
3. `PACKAGE_CHANGED` akışının `INSERT IGNORE` nedeniyle Room kaydını güncellememesi.
4. Repository mutasyonlarının hataları yutması ve üst katmanın işlemi başarılı sanması.
5. Kategori ve paket temizleme işlemlerinin atomik olmaması.
6. Dashboard ayarlarının kullanıcıya gösterilmesine rağmen pager'da bilinçli olarak etkisiz tutulması.
7. Ana ekran düzenleyicisinde gizlenen bölümlerin gerçek render state'ine uygulanmaması.
8. Arama çubuğu konumunun iki ayrı preference kaynağında tutulması.
9. Dashboard editörünün tekil bölüm sırası sunarken renderer'ın yalnız üç grup üzerinden sıralama yapması.
10. Arama sonuçlarında uygulamaların diğer kaynakları tek global limit içinde ezmesi.
11. Arama indeksinin sil-sonra-yaz yaklaşımıyla atomik olmayan şekilde yeniden kurulması.
12. Bildirim listener yeniden bağlandığında zaman/önem bilgisinin doğru yeniden üretilmemesi.
13. Bildirim DB yazımlarında iç içe coroutine nedeniyle eski emisyonun yeniyi ezebilmesi.
14. Son 24 saat bildirim sınırının ViewModel oluşturulurken bir kez hesaplanması.
15. İlk yükleme durumunun gerçek ilk Room emisyonundan önce tamamlandı sayılabilmesi.
16. CI'ın lint/detekt/ktlint hatalarını zorunlu yayın kapısı yapmaması.
17. Android 11 ve altı yedek kurallarında `deepseek_prefs.xml` istisnasının eksik olması.
18. Launcher'ın açık tema altyapısı varken zorla koyu tema ile açılması.
19. Gerçek uygulama boyutu yerine yalnız temel APK dosyasının ölçülmesi.
20. Uzak sınıflandırma verisinin mutable `main` dalından hash/imza doğrulaması olmadan indirilmesi.

## Yayın kararı

Aşağıdaki döngüler bitmeden:

- Dashboard üretimde varsayılan açılmamalı.
- `KEY_HOME_PAGER_V2_ENABLED` tüm kullanıcılara açılmamalı.
- Play Store üretim sürümü hazırlanmış sayılmamalı.
- Yeni büyük özellik eklenmemeli.

**Yayın engelleyici sıra:** `R00 → R01 → R02 → R03 → R04 → R05 → R06 → R07 → R08 → R09 → R10 → R11 → R12 → R13 → R14 → R15`.

---

# 2. Yapay zekâ çalışma protokolü

Her döngü tek başına uygulanacaktır. Bir görev tamamlanmadan sonraki göreve geçilmeyecektir.

## 2.1 Zorunlu çalışma sırası

1. Görevde belirtilen dosyaları güncel `main` üzerinden yeniden oku.
2. Roadmap ile kod farklıysa güncel kodu esas al; farkı görev raporuna yaz.
3. Yalnız döngü kapsamındaki dosyaları değiştir.
4. Davranış değişikliği varsa önce saf politika veya use-case testini yaz.
5. Room değişikliği varsa migration/schema testi ekle.
6. Kullanıcı metni ekleniyorsa hem `values` hem `values-en` kaynaklarını güncelle.
7. Kod değişikliğinden sonra en az ilgili testleri çalıştır.
8. Döngü sonunda tek ve anlamlı commit oluştur.
9. Test geçmeden `✅ Tamamlandı` yazma.
10. Gerçek cihaz kriteri varsa cihaz kanıtı olmadan `✅ Tamamlandı` yazma; `🟡 Kısmen tamamlandı` kullan.
11. Hata yakalayıp sessizce yutma. Başarı/başarısızlık üst katmana açıkça taşınmalıdır.
12. Zamanlama için `delay(...)` ekleyerek veri tutarlılığı çözmeye çalışma.
13. Aynı veri için ikinci preference veya ikinci state kaynağı oluşturma.
14. İlgisiz formatlama, toplu isim değiştirme ve geniş refactor yapma.

## 2.2 Her görev sonunda yazılacak rapor

```text
Döngü:
Commit:
Değişen dosyalar:
Kök neden:
Uygulanan çözüm:
Eklenen testler:
Çalıştırılan komutlar:
Sonuç:
Cihaz kanıtı:
Kalan risk:
Sonraki bağımlı döngü:
```

## 2.3 Durum değerleri

- `⏳ Bekliyor`: Kodlama başlamadı.
- `🚧 Devam ediyor`: Kod yazıldı; test veya doğrulama sürüyor.
- `🟡 Kısmen tamamlandı`: Otomatik testler geçti; gerçek cihaz veya yayın kanıtı eksik.
- `✅ Tamamlandı`: Kod, otomatik testler ve gerekli cihaz kanıtı tamamlandı.
- `⛔ Bloke`: Harici bağımlılık, izin, cihaz veya mağaza işlemi bekleniyor.

## 2.4 Temel doğrulama komutları

```bash
./gradlew assembleDebug -PskipGoogleServices=true
./gradlew testDebugUnitTest -PskipGoogleServices=true
./gradlew lintDebug -PskipGoogleServices=true
./gradlew detekt -PskipGoogleServices=true
./gradlew ktlintCheck -PskipGoogleServices=true
```

Room değişikliği olan döngüler:

```bash
./gradlew connectedDebugAndroidTest -PskipGoogleServices=true
```

Release/R8 doğrulaması:

```bash
./gradlew assembleRelease -PskipGoogleServices=true -PallowDebugReleaseSigning=true
```

---

# 3. Doğrulanmış hata envanteri

| ID | Seviye | Sorun | Kök dosyalar | Hedef döngü |
|---|---|---|---|---|
| DATA-01 | P0 | Güncelleme sırasında `PACKAGE_REMOVED + EXTRA_REPLACING=true` gerçek kaldırma gibi silme yapıyor | `PackageChangeReceiver.kt` | R01 |
| DATA-02 | P0 | Statik receiver ve `LauncherActivity` receiver aynı olayı işliyor | `PackageChangeReceiver.kt`, `LauncherActivity.kt` | R02 |
| DATA-03 | P0 | `PACKAGE_CHANGED` sonrası `insertApps(IGNORE)` metadata güncellemesini yok sayıyor | `PackageChangeReceiver.kt`, `AppDao.kt` | R03 |
| DATA-04 | P0 | Repository mutasyonları exception yutuyor | `AppRepository.kt`, `SearchRepository.kt` | R04 |
| DATA-05 | P0 | Kategori silme ve uygulamaları taşıma atomik değil | `AppListViewModel.kt`, DAO'lar | R05 |
| DATA-06 | P0 | Paket kaldırılınca dock/favori/manual override/index temizliği farklı yerlerde ve eksik | receiver, ViewModel, prefs | R06 |
| SEC-01 | P0 | Android 11 ve altı backup kuralında `deepseek_prefs.xml` hariç değil | `backup_rules.xml` | R07 |
| CI-01 | P0 | Ana CI lint/detekt/ktlint çalıştırmıyor; QA bunları bloklamıyor | workflow'lar, Gradle lint | R08 |
| HOME-01 | P0 | Dashboard ayarı görünür fakat pager bayrağı sabit `false` | `HomeScreen.kt`, `SettingsHomeScreenSection.kt` | R09 |
| HOME-02 | P0 | Layout editöründeki visibility ayarı gerçek Dashboard render'ına uygulanmıyor | layout editor, Dashboard state/renderer | R10-R11 |
| HOME-03 | P1 | Search zone iki ayrı kaynaktan yönetiliyor | `AppPrefs`, `HomeLayoutPrefs` | R10 |
| HOME-04 | P1 | Editör tekil section sıralıyor, renderer üç temsilci grup sıralıyor | editor, `SmartDashboardPage.kt` | R11 |
| HOME-05 | P1 | Boş Dashboard gerçek içerik yerine yalnız feature flag'e bakıyor | `SmartDashboardPage.kt` | R11 |
| SEARCH-01 | P1 | Tek global limit ve app-first order diğer kaynakları aç bırakıyor | `SearchRepository.kt` | R12 |
| SEARCH-02 | P1 | Bootstrap `deleteAll` ardından `insertAll`; süreç kesilirse indeks boş kalabilir | `SearchRepository.kt`, `SearchDao.kt` | R12 |
| NOTIF-01 | P1 | Listener reconnect `lastPostedAt` ve importance bilgisini yeniden kurmuyor | listener service | R13 |
| NOTIF-02 | P1 | Nested coroutine yazımları emisyon sırasını bozabilir | `LauncherViewModel.kt` | R13 |
| NOTIF-03 | P1 | 24 saat sınırı ViewModel ömrü boyunca sabit | `LauncherViewModel.kt` | R13 |
| START-01 | P1 | `initialLoadDone` gerçek ilk Room verisini ayırmıyor | `LauncherViewModel.kt` | R14 |
| START-02 | P1 | Reconcile başka coroutine başlatıp gerçek tamamlanmayı beklemiyor | `LauncherViewModel.kt` | R14 |
| START-03 | P1 | Kategori değişiminden sonra `delay(50)` ile UI tetiklenmeye çalışılıyor | `LauncherViewModel.kt` | R14 |
| REMOTE-01 | P1 | Remote katalog mutable `main` dalı, timeout/hash/imza yok | `AppDatabaseService.kt` | R15 |
| ARCH-01 | P1 | `HomeScreen` çok sayıda Flow ve preference state'ini doğrudan taşıyor | `HomeScreen.kt` | R16-R17 |
| SEARCH-03 | P1 | Global search ve All Apps query aynı state'i paylaşıyor | ViewModel, drawer, global search | R18 |
| SEARCH-04 | P1 | Türkçe normalizasyon/alias/typo modeli kaynaklar arasında tutarlı değil | indexer, entity, repository | R19 |
| UI-01 | P1 | Launcher `darkTheme=true` ile zorlanıyor | `LauncherActivity.kt` | R21 |
| UI-02 | P1 | Dashboard/Home bileşenlerinde sabit `Color.White` kullanımları var | launcher UI dosyaları | R21 |
| UI-03 | P1 | Kullanıcı metinlerinin önemli bölümü hardcode | Settings, drawer, launcher UI | R22 |
| A11Y-01 | P1 | Pager custom accessibility action, search/modal kilidini bypass edebilir | `HomePagerHost.kt` | R22 |
| TEST-01 | P1 | Gerçek screenshot/golden test altyapısı yok | Gradle, androidTest/screenshotTest | R23 |
| SIZE-01 | P2 | `sourceDir.length()` gerçek uygulama boyutu değil | `PackageManagerHelper.kt` | R24 |
| MISSION-01 | P2 | XP, dijital yaşam skoru ve yıldız anlatımı kullanıcı açısından karışık | görev/skor ekranları | R25 |
| MISSION-02 | P2 | `mission_instances` yazılıyor fakat ana okuma yolu tamamen taşınmamış | missions repository/ViewModel | R25 |
| BACKUP-01 | P2 | Manuel JSON yedeği kullanım profili içeriyor ve şifresiz | `BackupManager.kt` | R26 |
| PERF-01 | P2 | Dashboard rollout öncesi macrobenchmark/baseline profile kanıtı yok | benchmark altyapısı | R28 |
| PERM-01 | P2 | Manifest izinleri özellik bazında yeniden doğrulanmalı | manifest, permission UI | R29 |
| GOV-01 | P2 | Bazı eski roadmap döngüleri kabul kriteri eksikken tamamlandı işaretlenmiş | roadmap yönetimi | R00 |

---

# 4. Faz A — Yayın engelleyici veri ve davranış düzeltmeleri

## R00 — Denetim bazını kilitle ve regresyon testlerini hazırla

**Amaç:** Düzeltme öncesindeki kritik davranışları testle görünür hale getirmek.

**Değişecek/yeni dosyalar:**

- `app/src/test/.../PackageEventPolicyTest.kt`
- `app/src/test/.../RepositoryMutationContractTest.kt`
- `app/src/test/.../HomeLayoutRenderContractTest.kt`
- `app/src/test/.../SearchSourceAllocationTest.kt`
- `app/src/test/.../NotificationSummaryPolicyTest.kt`
- `docs/internal/audit_baseline_2026-07-18.md`

**Nokta atışı yapılacaklar:**

1. Test fixture'larında kullanıcı tarafından düzeltilmiş kategori, favori, dock, gizli uygulama ve notification state bulunan örnek uygulama oluştur.
2. Güncelleme dizisini modelle: `REMOVED(replacing=true) → ADDED(replacing=true) → REPLACED`.
3. Gerçek kaldırma dizisini ayrı modelle: `REMOVED(replacing=false)`.
4. Dashboard toggle açık fakat pager kapalı olan mevcut durumu kontrat testiyle belgeleyip test adını `currentBehavior_...` şeklinde yaz; sonraki döngü bu testi bilinçli değiştirsin.
5. Eski roadmap durumlarını değiştirme; eksik kabul kriterlerini bu roadmap'te yeniden görev olarak aç.

**Kabul kriteri:** Kritik hata senaryoları test isimleri ve fixture'larla yeniden üretilebilir.

**Durum:** ⏳ Bekliyor

---

## R01 — Paket olaylarını semantik olarak ayır

**Amaç:** Uygulama güncellemesini gerçek kaldırmadan ayırmak ve veri kaybını durdurmak.

**Yeni dosya:**

- `domain/usecase/packages/PackageEventPolicy.kt`

**Model:**

```kotlin
sealed interface PackageEvent {
    data class Added(val packageName: String, val replacing: Boolean) : PackageEvent
    data class Removed(val packageName: String, val replacing: Boolean) : PackageEvent
    data class Changed(val packageName: String) : PackageEvent
    data class Replaced(val packageName: String) : PackageEvent
}

enum class PackageMutation {
    INSERT_NEW,
    REFRESH_METADATA,
    DELETE_REAL,
    IGNORE_TRANSIENT_REMOVE,
}
```

**Değişecek dosyalar:**

- `presentation/receivers/PackageChangeReceiver.kt`
- Manifest intent-filter testleri.

**Nokta atışı değişiklik:**

1. `EXTRA_REPLACING` hem `PACKAGE_ADDED` hem `PACKAGE_REMOVED` için policy'ye verilsin.
2. `PACKAGE_REMOVED && replacing=true` durumunda Room, search, favori veya dock mutasyonu yapılmasın.
3. `PACKAGE_ADDED && replacing=true` yeni uygulama bildirimi üretmesin; metadata refresh yapsın.
4. `PACKAGE_REPLACED` doğrudan metadata refresh olarak işlensin.
5. Gerçek kaldırma yalnız `replacing=false` ile mümkün olsun.
6. Receiver içinde business logic kalmasın; yalnız event üretip coordinator'a aktarsın.

**Testler:**

- Update remove → `IGNORE_TRANSIENT_REMOVE`.
- Real remove → `DELETE_REAL`.
- Added replacing → `REFRESH_METADATA`.
- Added fresh → `INSERT_NEW`.
- Replaced → `REFRESH_METADATA`.

**Kabul kriteri:** Play Store güncellemesi kullanıcı kategorisini, favoriyi, dock'u ve sayaçları silemez.

**Durum:** ⏳ Bekliyor

---

## R02 — Tek paket receiver ve tek coordinator oluştur

**Amaç:** Aynı olayın iki farklı akışta işlenmesini engellemek.

**Yeni dosya:**

- `domain/usecase/packages/PackageEventCoordinator.kt`

**Değişecek dosyalar:**

- `LauncherActivity.kt`
- `PackageChangeReceiver.kt`
- `AndroidManifest.xml`
- Hilt module/entry point dosyaları.

**Nokta atışı değişiklik:**

1. `LauncherActivity.packageReceiver` alanını tamamen kaldır.
2. `onStart()` içindeki `registerReceiver` ve `onStop()` içindeki `unregisterReceiver` kodunu kaldır.
3. Manifest receiver tek olay kaynağı olsun.
4. Receiver `android:exported="false"` yapılmalı.
5. `PackageEventCoordinator` şu sırayı yönetsin:

```text
policy resolve
→ PackageManager'dan taze metadata
→ Room mutation
→ search index mutation
→ user preference cleanup/notification
→ result log/telemetry
```

6. Coordinator tüm operasyonları idempotent tasarlasın; aynı event iki kez gelse de sonuç değişmesin.
7. UI, Room Flow üzerinden güncellensin; Activity ayrıca paket olayı dinlemesin.

**Testler:**

- Aynı Added event iki kez → tek uygulama kaydı.
- Aynı Removed event iki kez → crash yok.
- Activity oluşturulmadan paket olayı → katalog doğru güncellenir.

**Kabul kriteri:** Repo genelinde paket olayını işleyen yalnız bir koordinasyon yolu bulunur.

**Durum:** ⏳ Bekliyor

---

## R03 — Insert ve metadata update yollarını ayır

**Amaç:** `PACKAGE_CHANGED` ve güncelleme olaylarında Room metadata'sının gerçekten yenilenmesi.

**Değişecek dosyalar:**

- `AppDao.kt`
- `AppRepository.kt`
- `PackageEventCoordinator.kt`
- `PackageManagerHelper.kt`
- Search index entegrasyonu.

**Nokta atışı değişiklik:**

1. `insertApps(IGNORE)` yalnız gerçek yeni paket eklemek için kullanılsın.
2. Mevcut uygulama metadata'sı için DAO'ya alan bazlı query ekle:

```kotlin
@Query("""
UPDATE apps SET
    appName = :appName,
    isSystemApp = :isSystemApp,
    lastUpdated = :lastUpdated,
    firstInstalledTime = :firstInstalledTime,
    lastUpdatedTime = :lastUpdatedTime,
    targetSdkVersion = :targetSdkVersion,
    versionName = :versionName,
    appSizeBytes = :appSizeBytes
WHERE packageName = :packageName
""")
suspend fun updatePackageMetadata(...): Int
```

3. Query yalnız PackageManager alanlarını değiştirsin; kategori, sınıflandırma kilidi, usage, launch count, hidden, notes ve notification alanlarına dokunmasın.
4. Dönen satır sayısı `0` ise paket yeni kabul edilip insert yolu çalışsın.
5. `onPackageChanged()` içinde `repo.insertApps(listOf(merged))` çağrısını kaldır.
6. Search index yalnız Room mutation başarılı olduktan sonra güncellensin.
7. Uygulama adı değişmişse eski search document silinip yenisi yazılsın.

**Testler:**

- Versiyon güncelleme user category'yi korur.
- App adı değişir, search yeni adı bulur.
- Hidden/usage/launch count korunur.
- Update olmayan paket insert edilir.

**Kabul kriteri:** Room ve search index aynı uygulama adını/sürümünü gösterir.

**Durum:** ⏳ Bekliyor

---

## R04 — Repository mutasyon sözleşmesini düzelt

**Amaç:** Sessiz başarısızlıkları kaldırmak.

**Karar:** DAO/repository mutasyonları başarısızsa exception üst katmana çıkmalıdır. `runCatching` yalnız use-case/coordinator sınırında kullanılmalıdır.

**Değişecek dosyalar:**

- `AppRepository.kt`
- `SearchRepository.kt`
- `MissionsRepository.kt` mutasyonları kontrol edilecek.
- `AppListViewModel.kt`
- `LauncherViewModel.kt`
- `BackupManager.kt`
- Paket coordinator.

**Nokta atışı değişiklik:**

1. Şu desen mutasyon fonksiyonlarından kaldırılmalı:

```kotlin
try { dao.update(...) } catch (e: Exception) { Timber.e(e) }
```

2. Okuma fonksiyonlarında güvenli fallback gerekiyorsa korunabilir; yazma fonksiyonlarında sessiz fallback yasaktır.
3. DAO update/delete query'leri mümkünse `Int` döndürsün.
4. Beklenen satır sayısı `0` ise `MutationRejectedException` veya açık domain sonucu üret.
5. ViewModel şu yan etkileri yalnız Room başarısından sonra yapsın:
   - manual override yazma,
   - search reindex,
   - görev puanı verme,
   - başarı mesajı gösterme.
6. UI'ya tek kullanımlık hata event'i gönder.
7. Log mesajlarında paket adı release telemetrisine gönderilmesin; yerel debug log ile sınırlandır.

**Testler:**

- DAO hata atar → manual override yazılmaz.
- DAO hata atar → görev puanı artmaz.
- DAO update 0 row → başarı mesajı gösterilmez.
- Search index hatası → Room geri alınmayacaksa repair queue oluşturulur.

**Kabul kriteri:** Üst katman başarısız bir DB işlemini başarılı sanamaz.

**Durum:** ⏳ Bekliyor

---

## R05 — Kategori silme ve yeniden atamayı transaction yap

**Amaç:** Silinmiş kategoriye bağlı orphan uygulama bırakmamak.

**Yeni dosya/adayı:**

- `data/repository/CatalogTransactionRepository.kt`

**Değişecek dosyalar:**

- `AppDao.kt`
- `CategoryDao.kt`
- `AppDatabase.kt`
- `AppListViewModel.kt`
- Search repository.

**Nokta atışı değişiklik:**

1. `AppDatabase.withTransaction` kullan.
2. Transaction içinde:
   - kategorinin custom olduğunu doğrula,
   - bağlı uygulamaları `uncategorized` yap,
   - classification metadata'yı `FALLBACK_OTHER/PENDING` uyumlu hale getir,
   - kategoriyi sil.
3. Transaction başarılı olduktan sonra manual override kayıtlarını temizle.
4. Search index için `repairCategoryDeletion(categoryId, affectedPackages)` fonksiyonu oluştur.
5. Search update başarısızsa idempotent repair flag/worker bırak; DB transaction'ını sessizce başarılı sayıp indeksi sonsuza kadar bozuk bırakma.
6. Sistem kategorileri için transaction başlamadan reddet.

**Testler:**

- 0 uygulamalı custom kategori.
- 20 uygulamalı custom kategori.
- Sistem kategorisi silme reddi.
- Uygulama taşıma hatası → kategori silinmez.
- Process death sonrası orphan categoryId yok.

**Kabul kriteri:** `apps.categoryId` her zaman mevcut veya izin verilen özel fallback kategoriye işaret eder.

**Durum:** ⏳ Bekliyor

---

## R06 — Paket kaldırma referans temizliğini tek yerde topla

**Amaç:** Kaldırılmış uygulamanın dock, favori, search ve tercih kayıtlarında kalmaması.

**Yeni dosya:**

- `domain/usecase/packages/PackageReferenceCleaner.kt`

**Temizlenecek alanlar:**

- Room app row.
- Search document.
- `DockPrefs`.
- Favorites.
- Manual category override.
- Notification read timestamp.
- Folder suggestion accepted/dismissed patternlerinde paket bazlı kayıt varsa ilgili öğe.
- Icon cache.
- In-memory ViewModel state'i Room/Prefs flow üzerinden.

**Nokta atışı değişiklik:**

1. `LauncherViewModel.onPackageRemoved()` kaldırılmalı veya yalnız coordinator sonucunu gözlemleyen UI yardımcısına dönüşmeli.
2. Yalnız bellekte `_dockPackages.value = current - packageName` yapmak yasak; `DockPrefs.removeFromDock` kalıcı olarak çağrılmalı.
3. `AppPrefs.removeFavorite` ve manual override temizliği coordinator sonrası tek noktada yapılmalı.
4. Cleaner idempotent olmalı.
5. Kategori dock item'ı, kategori silme döngüsünde ayrıca temizlenmeli.

**Testler:**

- Dock'taki uygulama kaldırılır → yeniden açılışta geri gelmez.
- Favori kaldırılır.
- Manual override kaldırılır.
- İkinci cleanup çağrısı hata üretmez.

**Kabul kriteri:** Kaldırılan uygulama hiçbir kullanıcı yüzeyinde kırık referans bırakmaz.

**Durum:** ⏳ Bekliyor

---

## R07 — Backup ve API anahtarı güvenliğini düzelt

**Amaç:** Eski Android sürümlerinde hassas preference'ın yedeklenmesini ve düz metin secret saklanmasını engellemek.

**Değişecek dosyalar:**

- `res/xml/backup_rules.xml`
- `res/xml/data_extraction_rules.xml`
- DeepSeek/API anahtarı okuyan-yazan sınıf.
- Güvenlik testleri/dokümantasyon.

**Nokta atışı değişiklik:**

1. `backup_rules.xml` içine ekle:

```xml
<exclude domain="sharedpref" path="deepseek_prefs.xml" />
```

2. Android 12+ ve eski backup kurallarını aynı hassas dosya listesiyle senkron tutan test/script ekle.
3. API anahtarını normal SharedPreferences'tan çıkar.
4. `SecretStore` oluştur; Android Keystore AES/GCM veya güvenli eşdeğer kullan.
5. Secret verisini `noBackupFilesDir`/yedek dışı depoda tut.
6. Mevcut kullanıcı anahtarını tek seferlik migrate et; migration başarılıysa eski preference değerini sil.
7. Diagnostics/backup/export içinde anahtar, kısmi anahtar veya hash bulunmamalı.

**Testler:**

- Legacy preference → secure store migration.
- Migration sonrası eski key yok.
- Backup XML parity testi.
- Export JSON secret içermez.

**Kabul kriteri:** API anahtarı hiçbir Android sürümünde cloud backup veya manuel JSON yedeğine girmez.

**Durum:** ⏳ Bekliyor

---

## R08 — Tek ve bloklayıcı CI kalite kapısı oluştur

**Amaç:** Yeşil build'in gerçekten kalite kontrollerini geçtiği anlamına gelmesi.

**Değişecek dosyalar:**

- `.github/workflows/android-ci.yml`
- `.github/workflows/android-qa.yml`
- Gereksiz/duplicate workflow'lar.
- `app/build.gradle.kts`
- Detekt/ktlint config.

**Nokta atışı değişiklik:**

1. Tüm otomatik kontroller JDK 17 kullanmalı.
2. Ana PR/push workflow şu görevleri zorunlu çalıştırmalı:

```text
assembleDebug
unit tests
lintDebug
detekt
ktlintCheck
Room schema/migration verification
```

3. `continue-on-error: true` kaldırılmalı.
4. `lint.abortOnError = true` yapılmalı.
5. Raporlar `if: always()` ile artifact olarak yüklenmeli; fakat hata job'ı başarısız bırakmalı.
6. Duplicate workflow'lar ya kaldırılmalı ya yalnız release/device amacıyla açıkça ayrılmalı.
7. Gradle/JDK sürümü tek kaynakta belgelenmeli.
8. GitHub branch protection'ta bu workflow required check yapılmalı; bu adım repo ayarı olduğu için görev raporunda kanıt verilmeli.

**Kabul kriteri:** Lint/detekt/ktlint hatalı commit `main`e birleşemez.

**Durum:** ⏳ Bekliyor

---

## R09 — Dashboard ayarlarını gerçek davranışla eşleştir

**Amaç:** Kullanıcıya çalışan gibi görünen fakat etkisiz ayar sunmamak.

**Karar:** R01-R08 bitmeden Dashboard genel kullanıma açılmayacak. Bu aşamada internal feature flag ile dürüst geçiş yapılacak.

**Yeni anahtar:**

```kotlin
KEY_HOME_PAGER_V2_ENABLED
```

**Değişecek dosyalar:**

- `HomeScreen.kt`
- `SettingsHomeScreenSection.kt`
- `HomePagePrefs.kt`
- Geliştirici ayarları.
- Diagnostics.

**Nokta atışı değişiklik:**

1. `dashboardEnabledForPager = false` sabiti kaldırılmadan önce internal feature flag ekle.
2. Üretim kullanıcılarında flag kapalıysa Dashboard toggle ve Dashboard start mode seçeneği gizlensin veya açıkça “Deneysel — henüz etkin değil” olarak disabled gösterilsin.
3. Flag açıkken:

```kotlin
val dashboardEnabledForPager =
    homePagerV2Enabled && smartDashboardPrefEnabled
```

4. Start mode resolver gerçek page listesine göre normalize etsin.
5. Dashboard kapalıyken SMART_DASHBOARD saklanamasın.
6. Dashboard açılıp/kapanınca semantic anchor doğru sayfaya taşınsın.
7. Flag cihaz bazlı ve backup dışı olmalı; rollout kontrol ayarı kullanıcı yedeğiyle başka cihaza taşınmamalı.

**Testler:**

- Flag off → no-op kontrol görünmez/disabled.
- Flag on + Dashboard on → page 0 Dashboard.
- Dashboard off → ilk klasör page 0.
- Start mode normalization.

**Kabul kriteri:** Ayarlar ekranında seçilebilen her seçenek anında gerçek davranış üretir.

**Durum:** ⏳ Bekliyor

---

## R10 — HomeLayout için tek doğruluk kaynağı oluştur

**Amaç:** Search konumu, bölüm görünürlüğü ve sıra ayarlarının iki preference sistemine dağılmasını bitirmek.

**Yeni dosya:**

- `data/repository/HomeLayoutRepository.kt`

**Değişecek dosyalar:**

- `HomeLayoutPrefs.kt`
- `AppPrefs.kt`
- `HomeScreen.kt`
- `SettingsHomeScreenSection.kt`
- `HomeLayoutEditorScreen.kt`
- Backup/diagnostics.

**Nokta atışı değişiklik:**

1. `HomeLayoutRepository` şu state'i yayınlasın:

```kotlin
data class HomeLayoutState(
    val config: HomeLayoutConfig,
    val customized: Boolean,
)
```

2. `HomeLayoutPrefs` yeni düzen için tek disk kaynağı olsun.
3. `AppPrefs.KEY_SEARCH_BAR_POSITION` yalnız legacy migration kaynağı olarak kalsın; yeni yazma yapılmasın.
4. Settings'teki üst/alt search seçimi `HomeLayoutRepository.setSearchZone()` kullansın.
5. Editor `write()` yerine repository action kullansın.
6. `HomeScreen` `remember(context) { HomeLayoutPrefs.read(...) }` kullanmasın; reaktif Flow toplasın.
7. `.first { MAIN_SEARCH }` yerine sanitize edilmiş state ve `firstOrNull` + default kullan.
8. Editor kaydından sonra Launcher aynı process içinde anında yeni config'i alsın.
9. Backup restore tek kaynağa yazsın; AppPrefs eski key'ini yeniden canlandırmasın.

**Testler:**

- Settings top/bottom seçimi restart sonrası korunur.
- Editor search zone seçimi Settings'e yansır.
- Bozuk config sanitize edilir.
- Aynı ayar iki farklı değer taşıyamaz.

**Kabul kriteri:** Search zone ve layout için yalnız bir aktif persistence kaynağı vardır.

**Durum:** ⏳ Bekliyor

---

## R11 — Dashboard renderer'ı editörle birebir eşleştir

**Amaç:** Kullanıcının gizlediği/taşıdığı her bölümün gerçekten aynı şekilde render edilmesi.

**Yeni dosyalar:**

- `DashboardSectionRenderer.kt`
- `DashboardVisibilityPolicy.kt`

**Değişecek dosyalar:**

- `SmartDashboardPage.kt`
- `DashboardUiState.kt`
- `HomeLayoutEditorScreen.kt`
- `HomeFavoritesSection.kt`
- `HomeScreen.kt`
- İlgili testler.

**Nokta atışı değişiklik:**

1. `DashboardUiState` içine sanitize edilmiş `List<HomeLayoutItem>` ekle; yalnız `contentOrder` göndermek yeterli değildir.
2. Renderer `config.items.filter { zone == CONTENT && visible }` sırasını doğrudan kullansın.
3. `DashboardContentGroup` ve temsilci section sıralaması kaldırılmalı veya editör de yalnız aynı üç grubu göstermelidir. **Tercih edilen çözüm: gerçek tekil section renderer.**
4. Birleşik `HomeFavoritesSection` şu alt composable'lara ayrılmalı:
   - `HomeFavoritesRow`
   - `HomeSuggestionsRow`
   - `HomeRecentNotificationsRow`
   - `HomeRecentAppsRow`
5. `GOOGLE_SEARCH` ve `ANDROID_WIDGETS` bağımsız taşınabilsin.
6. `ASSISTANT_INSIGHTS` ve `TICKER_OR_STATS` bağımsız taşınabilsin.
7. Recent installs ürün yüzeyinde düzenlenebilir olacaksa `RECENT_INSTALLS` section modeli ekle; olmayacaksa editörde açıkça “sabit bölüm” olarak göster.
8. `DashboardVisibilityPolicy` gerçek render koşullarını tek yerde hesaplasın. Feature açık fakat liste boşsa görünür sayılmasın.
9. `hasAnyContent` ve `countVisibleSections` aynı policy sonucunu kullansın.
10. Boş Dashboard saat dışında içerik yoksa açıklayıcı boş durum gösterilsin.

**Testler:**

- Her section hide/show.
- Her section reorder.
- Favoriler açık ama liste boş → boş içerik sayılmaz.
- Widget açık ama widget yok → görünmez.
- Editor preview ve gerçek renderer aynı sıralama.

**Kabul kriteri:** Editörde görülen düzen ile gerçek Dashboard arasında birebir sözleşme vardır.

**Durum:** ⏳ Bekliyor

---

## R12 — Arama kaynak adaletini ve indeks atomikliğini düzelt

**Amaç:** Uygulama sonuçlarının kişi/dosya/ayar/kategori kaynaklarını ezmesini önlemek ve indeksin boş kalma riskini kaldırmak.

**Değişecek dosyalar:**

- `SearchRepository.kt`
- `SearchDao.kt`
- `SearchDocument.kt`
- `AppDatabase.kt`
- Search testleri.

**Nokta atışı değişiklik:**

1. Tek global `ORDER BY app first LIMIT N` yaklaşımını kaldır.
2. Kaynak başına varsayılan kota uygula:

```text
APP: 8
CATEGORY: 4
SETTING: 4
CONTACT: 4
FILE: 4
```

3. Sonuç map'i her kaynak için kendi relevance sırasını korusun.
4. Global toplam limit küçükse minimum çeşitlilik sağla; kullanılmayan kota diğer kaynaklara dağıtılabilir.
5. Exact/prefix eşleşme source order'dan önce puanlanmalı.
6. `bootstrapIndex()` şu transaction içinde çalışmalı:

```kotlin
db.withTransaction {
    searchDao.deleteBaseSources()
    searchDao.insertAll(baseDocs)
}
```

7. Contacts/files kendi source transaction'larında yenilenmeli; base index'i silmemeli.
8. `settingsSeeded` yalnız DB yazımı başarıyla bitince true olsun.
9. Search mutation fonksiyonları exception yutmasın; repair yolu oluştur.
10. Telemetri query metni olmadan kaynak bazlı impression/result-count ölçsün.

**Testler:**

- 50 app eşleşmesi + 1 contact → contact görünür.
- 50 app + 1 setting → setting görünür.
- Bootstrap insert hatası → eski indeks korunur veya transaction rollback olur.
- FTS ve LIKE aynı source allocation kontratına uyar.

**Kabul kriteri:** Açık her arama kaynağı uygun sorguda sonuç yüzeyine çıkabilir.

**Durum:** ⏳ Bekliyor

---

## R13 — Bildirim state modelini ve yazım sırasını düzelt

**Amaç:** Badge, son bildirim zamanı, preview ve 24 saat raporunu birbirinden doğru ayırmak.

**Yeni model:**

```kotlin
data class ActiveNotificationSummary(
    val activeCount: Int,
    val latestPostedAt: Long,
    val maxImportance: Int,
    val latestPreview: NotificationPreview?,
)
```

**Değişecek dosyalar:**

- `AppNotificationListenerService.kt`
- `UnreadNotificationModel.kt`
- `LauncherViewModel.kt`
- `AppInfo.kt` veya ayrı summary entity.
- DAO/repository batch fonksiyonları.

**Nokta atışı değişiklik:**

1. `System.currentTimeMillis()` yerine notification zamanı için `sbn.postTime` kullan.
2. Ranking importance bilgisini listener callback/ranking map üzerinden al.
3. `onListenerConnected()` aktif bildirimlerden count, latestPostedAt, importance ve preview'ı baştan oluştursun.
4. Disconnect sırasında eski `lastPostedAt` state'i bırakılmasın; reconnect kaynağı aktif bildirimler olsun.
5. Dört ayrı map yerine mümkünse tek `StateFlow<Map<String, ActiveNotificationSummary>>` yayınla.
6. ViewModel içindeki `onEach { viewModelScope.launch(IO) { ... } }` desenini kaldır.
7. `collectLatest` veya seri actor/channel kullan; eski emisyon yeniyi ezemesin.
8. Count/text/importance/timestamp DB yazımı tek transaction/batch olsun.
9. Son 24 saat query sınırını sabit oluşturma zamanına bağlama. Saatlik ticker veya repository refresh parametresi kullan.
10. “Aktif bildirim”, “launcher'da okunmamış”, “son 24 saat olay sayısı” ayrı modeller olarak kalmalı.

**Testler:**

- Listener reconnect sonrası eski aktif notification doğru timestamp taşır.
- Uygulama açıldıktan sonra launcher badge 0; sistem bildirimi silinmez.
- Hızlı iki emisyon → son emisyon DB'de kalır.
- 24 saat sınırı saat ilerleyince yenilenir.

**Kabul kriteri:** Bildirim rozeti ve rapor aynı sayacı farklı anlamlarda kullanmaz.

**Durum:** ⏳ Bekliyor

---

## R14 — İlk yükleme ve reconcile state machine oluştur

**Amaç:** Yükleme, boş katalog, hata ve hazır durumlarını açıkça ayırmak; `delay(50)` gibi zamanlama yamalarını kaldırmak.

**Yeni model:**

```kotlin
sealed interface CatalogLoadState {
    data object Loading : CatalogLoadState
    data object Empty : CatalogLoadState
    data class Ready(val appCount: Int) : CatalogLoadState
    data class Failed(val reason: CatalogLoadFailure) : CatalogLoadState
}
```

**Değişecek dosyalar:**

- `LauncherViewModel.kt`
- `HomeScreen.kt`
- `AppRepository.kt`
- Reconcile testleri.

**Nokta atışı değişiklik:**

1. `initialLoadDone = allAppsSource.map { true }` kaldır.
2. İlk gerçek DAO emisyonunu veya repository bootstrap sonucunu açık state'e çevir.
3. `reconcileIfNeeded()` başka bir `launch` başlatıp dönmesin; iç işlem suspend olarak aynı coroutine içinde tamamlansın.
4. `loadAppsIfEmpty()` adı `reconcileCatalog()` olsun; gerçek davranış yalnız boş DB değil.
5. Reconcile sonucu `Added/Updated/Removed/Failed` sayılarıyla dönsün.
6. Stale app silme `PackageReferenceCleaner` üzerinden yapılsın.
7. `updateAppCategory()` içindeki `delay(50)` ve `_folderOrder.update { it.toList() }` kaldır. Room Flow gerçek state'i tetiklemeli.
8. Loading ekranı yalnız gerçekten veri beklenirken gösterilsin; boş katalog ayrı açıklama ve retry aksiyonu sunsun.
9. Reconcile bitmeden schema/current/reconciled bayrakları yazılmasın.

**Testler:**

- StateIn initial empty, Room henüz emit yok → Loading.
- Gerçek boş DB → Empty.
- 100 app → Ready(100).
- Package scan error → Failed.
- Kategori update için delay kullanılmaz ve UI Flow ile değişir.

**Kabul kriteri:** UI zaman gecikmesine değil veri state'ine göre güncellenir.

**Durum:** ⏳ Bekliyor

---

## R15 — Uzak sınıflandırma kataloğunu güvenli ve sürümlü yap

**Amaç:** Mutable `main` dosyasının doğrudan üretim verisi olmasını bitirmek.

**Yeni format:**

```text
catalog-manifest.json
catalog-vNN.json
catalog-vNN.sha256
```

**Değişecek dosyalar:**

- `AppDatabaseService.kt`
- Remote katalog üretim script'i.
- Asset fallback.
- Unit/integration testleri.

**Nokta atışı değişiklik:**

1. Raw `main/app_database.json` URL'si kaldırılmalı.
2. GitHub Release asset veya immutable tag/commit tabanlı URL kullan.
3. Connection/read timeout açıkça ayarla.
4. Maksimum dosya boyutu sınırı koy.
5. SHA-256 doğrulaması yap.
6. Version monoton artmalı; daha eski remote katalog cache'i düşürmemeli.
7. Bilinmeyen categoryId değerlerini reddet veya `other` fallback'e normalize et.
8. JSON parse ve doğrulama tamamlanmadan aktif `cachedMap` değiştirilmesin.
9. Cache yazımı geçici dosya/atomic rename veya tek doğrulanmış payload ile yapılsın.
10. ETag/If-None-Match veya version manifest ile gereksiz indirmeyi önle.
11. Hata halinde son doğrulanmış cache/asset korunmalı.

**Testler:**

- Hash yanlış → yeni katalog aktive olmaz.
- JSON yarım → eski cache korunur.
- Version düşük → reddedilir.
- Timeout → asset/cache fallback.
- Bilinmeyen kategori → güvenli fallback.

**Kabul kriteri:** Repo hesabındaki yanlış/yarım bir commit kullanıcı sınıflandırmasını doğrudan bozamaz.

**Durum:** ⏳ Bekliyor

---

# 5. Faz B — Ana ekran mimarisi ve state sadeleştirme

## R16 — HomePreferencesRepository ve tek HomeUiState

**Amaç:** `HomeScreen` içindeki onlarca `remember + SharedPreferences listener` state'ini kaldırmak.

**Yeni dosyalar:**

- `data/repository/HomePreferencesRepository.kt`
- `presentation/ui/launcher/HomeUiState.kt`
- `presentation/ui/launcher/HomeUiAction.kt`

**Yapılacaklar:**

1. Arka plan, görünürlük, klasör görünümü, search, dock ve gesture ayarlarını typed modellere ayır.
2. Preferences/DataStore akışlarını tek `StateFlow<HomePreferences>` içinde birleştir.
3. `LauncherViewModel` HomeUiState üretirken katalog, layout, notification ve intelligence state'lerini birleştirsin.
4. Composable doğrudan `AppPrefs.get...` çağırmasın.
5. `collectAsStateWithLifecycle()` kullan.
6. One-off UI olaylarını state içinde kalıcı tutma; event flow kullan.

**Kabul kriteri:** Yeni bir ana ekran ayarı eklemek için HomeScreen'e listener anahtarı eklemek gerekmez.

**Durum:** ⏳ Bekliyor

---

## R17 — HomeScreen'i orchestration seviyesine indir

**Amaç:** Monolit Composable'ı sorumluluk bazlı parçalamak.

**Yeni/ayrılacak dosyalar:**

- `HomeRoute.kt`
- `HomeOverlayHost.kt`
- `HomeDockHost.kt`
- `HomeGestureHost.kt`
- `HomePagerStateHolder.kt`

**Yapılacaklar:**

1. HomeScreen yalnız state toplasın ve host'ları bağlasın.
2. Overlay local state'lerini tek `HomeOverlayState` modelinde topla.
3. Pager state/semantic anchor koordinasyonunu ayrı state holder'a taşı.
4. Gesture policy yalnız `HomeGestureArbiter` sonucunu uygulasın.
5. Intent oluşturma tekrarlarını `HomeNavigationRouter` içine taşı.
6. `homePagerState`/`homePages` gibi composition sırasına güvenen holder yazımlarını kaldır.

**Hedef:** `HomeScreen.kt` orchestration dahil yaklaşık 300-500 satır bandına indirilmeli; bu sayı tek başına kabul kriteri değil, sorumluluk ayrımının göstergesidir.

**Durum:** ⏳ Bekliyor

---

## R18 — Global search ve All Apps arama state'ini ayır

**Amaç:** Drawer kapanınca global query'nin temizlenmesi veya iki yüzeyin birbirini etkilemesi sorununu kaldırmak.

**Yeni state:**

```kotlin
data class SearchQueries(
    val globalQuery: String = "",
    val drawerQuery: String = "",
    val folderQuery: String = "",
)
```

**Değişecek dosyalar:**

- `LauncherViewModel.kt`
- `GlobalSearchHost.kt`
- `FullScreenSearchOverlayV2.kt`
- `AllAppsDrawer.kt`
- `HomeScreen.kt`

**Yapılacaklar:**

1. `_searchQuery` üç ayrı amaca hizmet etmesin.
2. Drawer kapanınca yalnız drawer query temizlensin.
3. Global overlay kapanınca global query ürün kararına göre korunur veya temizlenir; açık kural test edilir.
4. Search active gesture kilidi aktif yüzeye göre hesaplanır.
5. `focusSearchOnOpen` yalnız drawer search'e etki eder.

**Durum:** ⏳ Bekliyor

---

## R19 — Türkçe arama normalizasyonu, alias ve typo toleransı

**Amaç:** Tüm kaynaklarda aynı normalize/rank modelini kullanmak.

**Yeni yardımcılar:**

- `SearchNormalizer.kt`
- `SearchRanker.kt`
- `SearchAliases.kt`

**Yapılacaklar:**

1. Türkçe `İ/i`, `I/ı`, aksan ve noktalama normalizasyonu.
2. `normalizedTitle`, `normalizedSubtitle`, `aliases`, `keywords` indeks alanları.
3. Exact > prefix > token > fuzzy sırası.
4. Bilinen alias örnekleri: `insta`, `watsap/whatsapp`, `wp`, `wifi`, `bluetooth`.
5. Fuzzy yalnız minimum query uzunluğu ve kontrollü edit distance ile çalışsın.
6. Search query metni telemetriye gitmesin.
7. FTS ve LIKE fallback aynı normalizasyon kontratını kullansın.

**Durum:** ⏳ Bekliyor

---

## R20 — Telefon, tablet, landscape ve foldable adaptif politika

**Amaç:** Ekran sınıfı kararlarını tek helper'da toplamak.

**Yeni dosya:**

- `HomeAdaptiveLayoutPolicy.kt`

**Model:**

```kotlin
enum class HomeDeviceClass { PHONE, COMPACT_TABLET, EXPANDED_TABLET }
```

**Yapılacaklar:**

1. Sütun sayısı, Dashboard density, search max width, dock max width ve drawer panel genişliği aynı policy'den türesin.
2. `Resources.getSystem().displayMetrics` yerine mevcut window bounds kullan.
3. Landscape Dashboard iki kolon seçeneği değerlendirilip test edilir.
4. Font scale ve usable height policy girdisi olsun.
5. Foldable posture desteklenmiyorsa güvenli config-change davranışı doğrulansın.

**Durum:** ⏳ Bekliyor

---

# 6. Faz C — Görsel kalite, tema ve erişilebilirlik

## R21 — Tema sözleşmesi ve renk token'ları

**Amaç:** Zorla koyu tema ve sabit beyaz renk kullanımını kaldırmak.

**Değişecek dosyalar:**

- `LauncherActivity.kt`
- `Theme.kt`
- `SmartDashboardPage.kt`
- `HomeScreen.kt`
- `FolderTile.kt`
- `AllAppsDrawer.kt`
- Diğer launcher bileşenleri.

**Yapılacaklar:**

1. `AppOrganizerTheme(darkTheme = true)` kaldır; sistem/kullanıcı tema tercihini kullan.
2. Launcher için açık tema desteklenmeyecekse bu ürün kararı açık bir enum ve ayar olarak tanımlansın; yanlışlıkla açık tema altyapısı varmış gibi davranılmasın.
3. `Color.White.copy(...)` yerine semantic color token kullan.
4. Duvar kâğıdı üzerindeki etiket kontrastı ayrı `WallpaperContentColors` modeliyle çözülsün.
5. Minimum kontrast kontrolleri test helper'ı ile doğrulansın.
6. Glass/frosted yüzeylerde açık ve koyu tema snapshot'ları üretilsin.

**Durum:** ⏳ Bekliyor

---

## R22 — Lokalizasyon ve erişilebilirlik sözleşmesini tamamla

**Amaç:** Hardcode metinleri, küçük touch target'ları ve kilit bypass eden accessibility action'ları düzeltmek.

**Değişecek dosyalar:**

- `SettingsHomeScreenSection.kt`
- `AllAppsDrawer.kt`
- `CategoryEditorScreen.kt`
- `HomePagerHost.kt`
- Görev ve Digital Life kartları.
- `values/strings.xml`, `values-en/strings.xml`.

**Yapılacaklar:**

1. Kullanıcıya gösterilen tüm sabit Türkçe metinleri resource'a taşı.
2. Pseudolocale testini CI'a ekle.
3. Filter/sort chip touch target'ları minimum 48dp yap.
4. `HomePagerHost` custom next/previous action'larını yalnız `userScrollEnabled/navigationEnabled` true iken sun. Search/modal/reorder açıkken TalkBack action pager'ı değiştirmemeli.
5. `HomeMissionCard` ve `DigitalLifeCard` tek birleşik contentDescription üretmeli.
6. Dekoratif emoji/glyph'leri semantics tree'den çıkar.
7. Font scale 2.0'da temel aksiyonlar görünür kalmalı.
8. Reduce motion ayarı process içinde değişirse state yeniden değerlendirilmeli.

**Durum:** ⏳ Bekliyor

---

## R23 — Gerçek screenshot/golden regresyon altyapısı

**Amaç:** Yazılı görsel test kurallarını çalışan CI alarmına dönüştürmek.

**Araç kararı:** Compose Preview Screenshot Testing, Roborazzi veya Paparazzi seçeneklerinden repo/AGP ile en uyumlu tek araç seçilecek; birden fazla araç aynı anda eklenmeyecek.

**Test matrisi:**

```text
360dp phone
411dp phone
600dp tablet
840dp tablet
portrait / landscape
light / dark / dynamic
font 1.0 / 1.3 / 2.0
TR / EN / pseudolocale
Dashboard empty / normal / dense
1 / 8 / 20 / 42 folder
All Apps empty / search / long list
```

**Yapılacaklar:**

1. Golden dosya konvansiyonu tanımla.
2. CI verify task ekle.
3. Golden güncelleme ayrı manuel komut olsun; CI otomatik golden yazmasın.
4. Görsel fark artifact olarak yüklensin.
5. En az HomeScreen, SmartDashboard, FolderTile, AllAppsDrawer, LayoutEditor ve Settings test edilsin.

**Durum:** ⏳ Bekliyor

---

# 7. Faz D — Veri doğruluğu ve ürün anlamı

## R24 — Uygulama boyutu verisini doğru adlandır veya doğru ölç

**Amaç:** Base APK boyutunu gerçek uygulama depolaması gibi göstermemek.

**Değişecek dosyalar:**

- `PackageManagerHelper.kt`
- App size worker/repository.
- Boyut kullanan rapor ve sort ekranları.

**Yapılacaklar:**

1. Usage access mevcutsa `StorageStatsManager` ile app+data+cache kırılımı al.
2. Permission yoksa yalnız base/split APK toplamı hesaplanabiliyorsa alanı `apkSizeBytes` olarak adlandır.
3. Gerçek total bilinmiyorsa “Boyut bilinmiyor” göster; `0 B` gösterme.
4. Sort mode yalnız karşılaştırılabilir doğru değerleri sıralasın.
5. Split APK dosyalarını hesaba kat.

**Durum:** ⏳ Bekliyor

---

## R25 — Görev XP, yıldız ve dijital yaşam skorunu ayır

**Amaç:** Kullanıcı açısından üç farklı puan sistemini anlaşılır hale getirmek ve görev instance geçişini tamamlamak.

**Yapılacaklar:**

1. `TaskScore` kullanıcı metninde `XP` veya `Düzenleme puanı` olarak adlandırılsın.
2. Digital Life score davranış kalitesi olarak 0-100 ölçeğinde kalsın.
3. Stars yalnız tamamlanan görev başarısı olarak gösterilsin.
4. Aynı kartta üç değeri açıklamasız yan yana koyma.
5. `mission_instances` ana okuma kaynağına geçirilsin; dual-write geçici kodu migration sonrası kaldır.
6. Dönem hedefi/baseline instance atanırken sabitlensin; görev ekranı her açılışta hedefi yeniden üretmesin.
7. XP verme yalnız DB işlemi gerçekten başarıyla tamamlandıktan sonra olsun.

**Durum:** ⏳ Bekliyor

---

## R26 — Manuel yedeği güvenli hale getir

**Amaç:** Paket/kullanım profilini içeren JSON yedeğinin riskini azaltmak.

**Yapılacaklar:**

1. Export öncesi açık gizlilik uyarısı göster.
2. Kullanıcıya iki seçenek sun:
   - standart JSON,
   - parola ile şifreli yedek.
3. Şifreli formatta authenticated encryption kullan; salt/nonce dosyada, parola saklanmaz.
4. Import önce schema/version/size doğrulaması yapsın.
5. Import transaction planı oluştur; yarım restore durumunda rollback veya ayrıntılı sonuç ver.
6. Notification text, contact, file path ve secrets export'a kesinlikle girmesin.
7. Backup test fixture'ları v1-v6 ve bozuk dosya senaryolarını kapsasın.

**Durum:** ⏳ Bekliyor

---

# 8. Faz E — Telemetri, performans ve güvenlik sertleştirme

## R27 — Privacy-safe telemetri ve sağlık raporu

**Amaç:** Yeni mimarinin çalışmasını kişisel veri toplamadan ölçmek.

**Güvenli alanlar:**

```text
page_type
device_class
start_mode
search_source_type
result_count_bucket
navigation_source
layout_version
catalog_load_state
package_event_outcome
```

**Gönderilmeyecekler:**

- Paket adı.
- Uygulama/klasör/kategori adı veya ID.
- Search query.
- Kişi/file bilgisi.
- Bildirim metni.

**Yapılacaklar:**

1. Recomposition duplicate event üretmesin.
2. Package event success/failure yalnız outcome ve event type ile ölçülsün.
3. Diagnostics'e Dashboard flag/pref/gerçek page planı ayrı yazılsın; no-op ayar kolay görülsün.
4. Layout hidden sections raporda isim yerine güvenli sayısal özet veya enum listesi ürün kararına göre yazılsın.
5. Consent off iken hiçbir event gönderilmesin.

**Durum:** ⏳ Bekliyor

---

## R28 — Macrobenchmark, baseline profile ve jank bütçesi

**Amaç:** Dashboard açılırken launcher akıcılığını ölçmek.

**Senaryolar:**

1. Cold start → home ready.
2. Dashboard → folder swipe.
3. Folder → Dashboard swipe.
4. All Apps aç/kapat.
5. Global search aç ve 5 karakter yaz.
6. Tablet side panel.
7. 40+ klasör.
8. 4 widget.

**Hedef metrikler:**

- Startup baseline'a göre anlamlı kötüleşme yok.
- Frame time/jank ölçülür.
- Search P95 ölçülür.
- Dashboard ilk compose ve tekrar dönüş ayrı ölçülür.

**Yapılacaklar:**

1. Benchmark modülü veya mevcut altyapıya Macrobenchmark ekle.
2. Baseline profile üret.
3. CI'da emulator benchmark nightly/manual çalışabilir; PR'da saf performans regression testleri çalışsın.
4. `HomeScreen` recomposition sayısı debug ölçümünde kaydedilsin.

**Durum:** ⏳ Bekliyor

---

## R29 — Manifest izin ve exported yüzey denetimi

**Amaç:** Yalnız gerekli izinleri ve minimum dışa açık bileşeni bırakmak.

**Yapılacaklar:**

1. Package receiver `exported=false` doğrula.
2. `QUERY_ALL_PACKAGES` kullanımını launcher temel işleviyle belgeleyen Play deklarasyon metni hazırla.
3. READ_MEDIA izinlerinin gerçekten dosya arama akışında kullanılıp kullanılmadığını doğrula. SAF kullanılıyorsa gereksiz izinleri kaldır.
4. `BIND_APPWIDGET` deklarasyonunun gerekli/etkili olup olmadığını doğrula; gereksizse kaldır.
5. `REQUEST_DELETE_PACKAGES` yalnız uninstall özelliği varsa kalsın.
6. Exported Activity intent input'ları sanitize edilsin.
7. FileProvider path'leri yalnız gereken dizinleri açsın.
8. Network security config yalnız kullanılan domainleri içersin; remote katalog domaini yeni mimariye göre güncellensin.

**Durum:** ⏳ Bekliyor

---

# 9. Faz F — Kontrollü Dashboard rollout ve temizlik

## R30 — Dört cihaz kanıtlı kontrollü Dashboard rollout

**Ön koşul:** R00-R29 içinden P0/P1 olarak işaretli tüm görevler tamamlanmış olmalı.

**Rollout sırası:**

1. Developer-only flag.
2. İç test kullanıcıları.
3. Dört cihaz matrisi.
4. Küçük yüzde rollout veya beta.
5. Default on.

**Cihaz matrisi:**

- Küçük telefon.
- Standart günlük telefon.
- Büyük telefon/izinleri eksik cihaz.
- 7-10 inç tablet.

**Zorunlu test:**

- Temiz kurulum.
- Mevcut sürümden upgrade.
- Dashboard aç/kapat.
- Start mode üç seçenek.
- Search top/bottom.
- Folder reorder/page size.
- Widget ekle/sil.
- Update olan üçüncü taraf uygulamanın kategori/favori/dock bilgisini koruma.
- Gerçek app kaldırma cleanup.
- TalkBack/font %200.
- Açık/koyu tema.
- 24 saat kullanım.

**Rollback:** Safe mode yalnız render flag'ini eski yola almalı; kullanıcı verisi veya yeni layout preference'ı silinmemeli.

**Durum:** ⏳ Bekliyor

---

## R31 — Legacy ana ekran ve preference temizliği

**Amaç:** Rollout doğrulandıktan sonra iki paralel kod yolu taşımamak.

**Silinecek/deprecate edilecekler:**

- Legacy folder-only render bloğu.
- `dashboardEnabledForPager=false` geçiş kodu.
- Ham `lastHomePage` yazımı.
- Legacy search position write yolu.
- Duplicate Dashboard component çağrıları.
- Kullanılmayan `homePagerPageCount` holder'ları.
- Eski focus bypass branch'leri.
- `delay(50)` ve yapay state refresh kodları.
- Migration süresi dolmuş preference okuma köprüleri.

**Kabul kriteri:** Üretimde tek ana ekran render yolu, tek layout kaynağı ve tek semantic page persistence bulunur.

**Durum:** ⏳ Bekliyor

---

## R32 — Nihai release evidence paketi

**Amaç:** “Derlendi” yerine kanıtlanmış yayın kararı üretmek.

**Çıktılar:**

```text
build/test reports
lint report
detekt report
ktlint report
Room migration report
screenshot diff report
benchmark report
four-device diagnostics
upgrade test report
backup/restore matrix
Play permission declaration
privacy/data safety checklist
```

**Yayın kapısı:**

- P0 açık bulgu: 0.
- P1 açık bulgu: 0 veya belgelenmiş release exception.
- Crash/ANR kritik regresyon: 0.
- Upgrade sırasında kategori/favori/dock kaybı: 0.
- Görsel kritik kırılma: 0.
- Secret backup testi: başarılı.
- Search source fairness testleri: başarılı.
- Dashboard feature flag rollback: başarılı.

**Durum:** ⏳ Bekliyor

---

# 10. Dosya bazlı değişiklik haritası

| Dosya | Ana görev |
|---|---|
| `PackageChangeReceiver.kt` | Yalnız event adapter; replacing policy ve coordinator çağrısı |
| `LauncherActivity.kt` | Dynamic package receiver kaldırma; tema kaynağı düzeltme |
| `PackageEventCoordinator.kt` | Tek paket mutation akışı |
| `AppDao.kt` | Insert-only ve metadata update ayrımı; row count |
| `AppRepository.kt` | Yazma hatalarını üst katmana taşıma |
| `CategoryDao.kt` | Transaction uyumlu kategori silme |
| `SearchRepository.kt` | Kaynak kotası, atomik bootstrap, açık hata sözleşmesi |
| `AppNotificationListenerService.kt` | Tek summary state, reconnect rebuild, postTime/importance |
| `LauncherViewModel.kt` | Seri notification collector, gerçek load state, suspend reconcile, query ayrımı |
| `HomeScreen.kt` | Reaktif layout/preferences, orchestration sadeleştirme, gerçek feature flag |
| `SmartDashboardPage.kt` | Tekil section renderer, gerçek visibility policy |
| `HomeLayoutEditorScreen.kt` | Renderer ile birebir section sözleşmesi, repository save |
| `HomeLayoutPrefs.kt` | Tek layout kaynağı ve migration |
| `SettingsHomeScreenSection.kt` | No-op kontrolü engelleme, layout repository kullanımı, string kaynakları |
| `HomePagerHost.kt` | Navigation lock'a uyan accessibility actions |
| `PackageManagerHelper.kt` | Gerçek/etiketlenmiş app size |
| `AppDatabaseService.kt` | Immutable, hash doğrulamalı ve timeout'lu katalog |
| `backup_rules.xml` | Legacy Android secret exclusion |
| `data_extraction_rules.xml` | Modern backup parity |
| `AndroidManifest.xml` | Exported/permission minimizasyonu |
| `android-ci.yml` | Bloklayıcı kalite kapısı |
| `android-qa.yml` | JDK standardı; non-blocking kalite kontrolünü kaldırma |
| `build.gradle.kts` | `abortOnError=true`, screenshot/benchmark bağımlılıkları |

---

# 11. Yasak desenler

Aşağıdaki desenler yeni kodda kullanılmayacaktır:

1. `delay(...)` ile Room Flow veya UI refresh beklemek.
2. Mutasyon exception'ını loglayıp başarılı dönmek.
3. Aynı Android olayını iki receiver ile işlemek.
4. Mevcut satırı güncellemek için `INSERT IGNORE` kullanmak.
5. Aynı ayarı AppPrefs ve HomeLayoutPrefs içinde aktif olarak birlikte tutmak.
6. Composable içinde onlarca SharedPreferences listener yönetmek.
7. Kullanıcıya no-op toggle göstermek.
8. Editörde sunulan bir ayarı renderer'da yok saymak.
9. Tek global search limitinde app-first sıralama ile kaynakları aç bırakmak.
10. `deleteAll → insertAll` işlemini transaction dışı yapmak.
11. Flow `onEach` içinde kontrolsüz yeni `viewModelScope.launch` açmak.
12. Tema token'ı yerine rastgele `Color.White` kullanmak.
13. UI metnini Kotlin dosyasında hardcode etmek.
14. CI'da `continue-on-error` ile kalite kontrolü yapmak.
15. Release lint için `abortOnError=false` kullanmak.
16. Mutable branch dosyasını doğrulamasız üretim kataloğu yapmak.
17. Base APK boyutunu gerçek uygulama boyutu diye sunmak.
18. Gerçek cihaz kriteri eksikken roadmap maddesini tamamlandı işaretlemek.

---

# 12. Definition of Done

Bu roadmap yalnız aşağıdaki koşulların tamamı sağlandığında bitmiş sayılır:

1. Uygulama update sırasında kullanıcı verisi silinmez.
2. Gerçek uninstall bütün referansları temizler.
3. Paket event'i tek coordinator tarafından işlenir.
4. Repository mutasyon hataları üst katmana ulaşır.
5. Kategori silme atomiktir.
6. Room ve search index tutarlılığı repair edilebilir ve testlidir.
7. Dashboard ayarı gerçek page planını anında değiştirir veya kullanıcıya sunulmaz.
8. Layout editor visibility/order gerçek renderer ile birebirdir.
9. Search zone tek preference kaynağına sahiptir.
10. Arama kişi/dosya/ayar/kategori kaynaklarını app sonuçları altında kaybetmez.
11. Bildirim reconnect ve hızlı emisyon testleri geçer.
12. İlk yükleme gerçek Loading/Empty/Ready/Failed state'ine sahiptir.
13. CI assemble, test, lint, detekt ve ktlint için bloklayıcıdır.
14. Android 11 ve altı dahil secret backup dışıdır.
15. Remote katalog immutable ve hash doğrulamalıdır.
16. HomeScreen typed state üzerinden çalışır ve lifecycle-aware toplama kullanır.
17. Global search ve drawer query bağımsızdır.
18. Açık/koyu tema ve semantic renkler tutarlıdır.
19. TR/EN/pseudolocale testleri geçer.
20. Screenshot matrix kritik fark üretmez.
21. App size doğru veya doğru biçimde “APK boyutu/bilinmiyor” olarak etiketlidir.
22. XP, yıldız ve dijital yaşam skoru kullanıcıya net anlatılır.
23. Privacy-safe telemetri query/paket/kişi/file verisi göndermez.
24. Dört cihaz upgrade ve 24 saat kullanım testi geçer.
25. Dashboard rollout geri alınabilir.
26. Legacy paralel render yolu temizlenmiştir.
27. Release evidence paketi repoda veya workflow artifact'lerinde mevcuttur.

---

# 13. Uygulama sırası — kısa liste

```text
R00  Regresyon bazını kilitle
R01  Replacing paket olayını düzelt
R02  Tek receiver/coordinator
R03  Metadata update yolunu düzelt
R04  Repository hata sözleşmesi
R05  Kategori transaction
R06  Paket referans cleanup
R07  Secret/backup güvenliği
R08  Bloklayıcı CI
R09  Dashboard no-op kontrolünü kaldır
R10  Tek HomeLayout kaynağı
R11  Editor-renderer birebirliği
R12  Search fairness + atomic index
R13  Notification summary + seri yazım
R14  Catalog load/reconcile state machine
R15  Güvenli remote katalog
R16  HomePreferences + HomeUiState
R17  HomeScreen parçalama
R18  Search query ayrımı
R19  Türkçe normalize/rank
R20  Adaptive layout
R21  Tema ve renk token'ları
R22  Lokalizasyon + erişilebilirlik
R23  Screenshot testleri
R24  Doğru app size
R25  Görev/XP/yıldız modeli
R26  Güvenli manuel backup
R27  Telemetri/diagnostics
R28  Benchmark/baseline profile
R29  Manifest/izin sertleştirme
R30  Kontrollü Dashboard rollout
R31  Legacy temizlik
R32  Nihai release evidence
```

**Son ürün kararı:** AppOrganizer'ın avantajı özellik sayısı değil; kullanıcı düzenini ve alışkanlık verisini bozmadan çalışan akıllı launcher olmasıdır. Veri kaybı veya etkisiz ayar varken yeni kart, yeni animasyon veya yeni öneri sistemi eklemek ürünü büyütmez; yalnız teknik borcu büyütür.
