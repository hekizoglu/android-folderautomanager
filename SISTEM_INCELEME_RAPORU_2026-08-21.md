# AppOrganizer Sistem İnceleme Raporu

**Tarih:** 2026-08-21  
**İncelenen dal:** `arena/01a022ef-android-folderautomanager`  
**İncelenen commit:** `cecb1b3feffe803bcc1c18a0328abc5f6b4cec5f`  
**Kapsam:** Mimari, veri akışları, launcher yaşam döngüsü, sınıflandırma/arama, izinler, test ve release hazırlığı.

## 1. Yönetici özeti

Proje küçük bir launcher prototipinden çıkmış, kapsamlı bir ürüne dönüşmüş durumda. Ana özelliklerin çoğu kodlanmış: Room tabanlı katalog, otomatik kategori, Hero Dashboard, klasör/pager, All Apps, global arama, bildirim sistemi, widget, yedekleme, görevler ve telemetri.

Buna karşılık sistemin riski artık yeni özellik eksikliğinden çok **veri bütünlüğü, yaşam döngüsü ve doğrulama kanıtı** tarafında. Kod tabanı yaklaşık **379 production Kotlin dosyası / 70.029 satır**, **158 unit test / 21.976 satır** ve **10 Android test** içeriyor. Bu büyüklükte doğrudan yeni özellik eklemek yerine önce ölçülebilir bir kalite tabanı ve P0 veri güvenliği kapısı oluşturulmalı.

### Genel değerlendirme

| Alan | Durum | Değerlendirme |
|---|---|---|
| Ürün kapsamı | İyi | Özellik seti geniş, ancak bazı eski/çift akışlar bulunuyor |
| Mimari | Orta | Katmanlar mevcut; `LauncherViewModel` ve `HomeScreen` çok büyümüş |
| Veri güvenliği | Riskli | Bazı repository metotları hatayı loglayıp yutuyor |
| Room/migration | Orta-riskli | 28 sürüm ve zincir var; destructive fallback ve 21→22 tablo düşürme dikkat istiyor |
| Test altyapısı | İyi ama kanıt eksik | Çok sayıda test var; bu ortamda çalıştırılamadı |
| UI/launcher | Orta | Home akışı karmaşık, gerçek cihaz/responsive kanıtı gerekiyor |
| Release hazırlığı | Bloke | Keystore, Play Console ve cihaz matrisi dış aksiyon bekliyor |

## 2. Mimari harita

### Uygulama girişleri

- `MainActivity`: onboarding, uygulama yönetimi, ayarlar ve rapor navigasyonu.
- `LauncherActivity`: HOME/DEFAULT launcher rolü, katalog yükleme, Home UI ve widget picker.
- `AppOrganizerApp`: Firebase/telemetri kurulumu ve arka plan worker planlaması.

Bu iki Activity ayrımı ürün kararına uygun olabilir; ancak onboarding sonrası geçiş, `singleTask`, `onNewIntent`, `onResume` ve varsayılan launcher değişimi mutlaka cihaz matrisiyle doğrulanmalı.

### Veri akışı

`PackageManagerHelper → AppClassifier → AppRepository → Room(AppDao/CategoryDao) → ViewModel → Compose UI`

Arama tarafında ayrıca:

`SearchIndexer / ContactsIndexer / FilesIndexer → SearchDao/FTS5 → SearchRepository → Home/AllApps UI`

Bu akış genel olarak anlaşılır; fakat repository katmanında tüm metotlar aynı hata sözleşmesini kullanmıyor. Bazıları exception rethrow ediyor, bazıları yalnız loglayıp başarı gibi dönüyor.

## 3. Doğrulanan güçlü taraflar

1. **Room migration zinciri mevcut:** Veritabanı sürümü 28 ve 1→28 arasında migration tanımları bulunuyor.
2. **Kullanıcı kategori kararları korunuyor:** Paket değişiminde mevcut kategori, kilit ve sınıflandırma metadata'sını korumaya yönelik merge kodu var.
3. **Yeni paket race koşulu ele alınmış:** `PackageChangeReceiver.onPackageAdded` içinde sınırlı retry/backoff ve `goAsync()` kullanılıyor.
4. **Arama tasarımı olgunlaşmış:** instant/debounced akışlar, LIKE escape, FTS5 fallback ve skor modeli bulunuyor.
5. **Gizlilik yönelimi mevcut:** Firebase koleksiyonları varsayılan kapalı, telemetri için rıza yönetimi ve kişisel veri filtreleme niyeti var.
6. **Test kapsamı geniş:** Domain, ViewModel, Room migration, arama, bildirim, görev, telemetri ve Compose mantığı için testler eklenmiş.
7. **Launcher özellikleri tek akışta toplanmış:** Hero dashboard, pager, klasörler, dock, All Apps ve arama bağlantıları kodda mevcut.

## 4. Öncelikli teknik bulgular

### P0 — Veri ve davranış bütünlüğü

#### P0.1 `ACTION_PACKAGE_REPLACED` işlenmiyor olabilir

Manifest `ACTION_PACKAGE_REPLACED` olayını dinliyor; ancak `PackageChangeReceiver.onReceive()` yalnızca `ADDED`, `REMOVED` ve `CHANGED` dallarını işliyor. `REPLACED` geldiğinde `else` dalında yalnız `finish()` çağrılıyor. Bazı cihazlarda güncelleme metadata'sı bu event üzerinden gelirse isim/ikon/uygulama bilgisi yenilenmeyebilir.

**Öneri:** `PACKAGE_REPLACED` olayını güvenli biçimde `onPackageChanged` ile aynı işleme bağlamak; `ADDED + EXTRA_REPLACING` ile çift işleme riskini idempotent test etmek.

#### P0.2 Repository hata sözleşmesi tutarsız

`AppRepository.updateApp()` ve `updateApps()` exception'ı loglayıp yutuyor. Buna karşılık `updateAppCategory()` ve toplu kategori güncellemesi exception'ı yeniden fırlatıyor. Örneğin paket güncelleme akışında Room update başarısız olsa bile sonraki arama indeksleme adımı çalışabilir.

**Etki:** Kullanıcı arayüzü/arama indeksi ile Room arasında tutarsızlık, sahte başarı ve sessiz veri kaybı.

**Öneri:** Yazma işlemleri için tek sözleşme: hata rethrow + çağıran katmanda kullanıcıya hata. Yan etkileri yalnız başarılı transaction sonrasında çalıştırmak.

#### P0.3 Kategori silme ve yan etkiler transaction sınırında incelenmeli

`deleteCategoryWithFallback` transaction kullanıyor; bu olumlu. Ancak Room değişikliğinden sonra arama indeksi, SharedPreferences, dock ve telemetri gibi yan etkilerin nasıl sıralandığı ayrıca doğrulanmalı. Transaction rollback olduğunda dış indeksler geri alınmıyorsa tutarsızlık oluşabilir.

#### P0.4 Migration 21→22 operasyon tablosunu düşürüyor

`MIGRATION_21_22` içinde `DROP TABLE IF EXISTS operations` var. Bu, eski sürümden yükselten kullanıcıların undo/operation geçmişini silebilir. Bilinçli bir şema düzeltmesi olsa bile release öncesi veri kaybı kararı ve migration test kanıtı gerekiyor.

#### P0.5 `fallbackToDestructiveMigration()` release veri güvenliği açısından riskli

Tüm bilinen ardışık migration'lar mevcut olsa da gelecekte eksik bir migration veya beklenmeyen sürüm atlaması uygulama verisini silebilir.

**Öneri:** Production build'de destructive fallback kaldırılmalı veya yalnız açık bir debug/test seçeneği yapılmalı; migration failure kullanıcıya güvenli hata ve backup/restore yolu sunmalı.

### P1 — Karmaşıklık ve bakım riski

#### P1.1 Home/Launcher katmanı aşırı büyümüş

`app/src/main/java` yaklaşık 70 bin satır. `HomeScreen.kt`, `LauncherViewModel.kt` ve ilgili Home bileşenleri çok sayıda state, izin, arama, widget, bildirim, görev ve navigasyon davranışını aynı akışta taşıyor.

**Öneri:** Yeni özellik eklemeden önce şu sınırlar çıkarılmalı:

- `HomeStateAssembler` veya selector katmanı
- `LauncherPackageSyncUseCase`
- `LauncherSearchCoordinator`
- `PermissionPromptCoordinator`
- UI state'lerini feature bazlı immutable modeller

Bu refactor tek seferde değil, test korunarak küçük fazlarda yapılmalı.

#### P1.2 Dokümantasyon ile kodun güncelliği uyuşmuyor

`MANAGEMENT/ROADMAP.md` içinde tamamlanmış görünen işler hâlâ “Bekliyor” olarak duruyor; ayrıca aynı durum tablosu iki kez bulunuyor. `COZULEMEYEN_SORUNLAR.md` Gradle 8.7 sorunundan söz ediyor, fakat wrapper şu anda Gradle 8.13 istiyor. Eski `CODE_AUDIT_FINDINGS.md` bulgularının bir bölümü kodda düzeltilmiş olmasına rağmen açık görünüyor.

**Öneri:** Tek bir “current status” bölümü oluşturulmalı; eski raporlar arşivlenmeli; her açık bulgu kod satırı + test kanıtı + tarih ile güncellenmeli.

#### P1.3 Çift/legacy ürün izleri temizlenmeli

`SmartDashboardPage`, eski ayar anahtarları ve geçmiş roadmap kararları hâlâ referans/kod yorumlarında görünüyor. Bunların bir kısmı gerçek kullanımda, bir kısmı yalnız uyumluluk için olabilir. Kullanılmayan kodu kaldırmadan önce `rg` caller matrisi çıkarılmalı.

#### P1.4 Test sayısı kalite kanıtına eşit değil

Test sayısı yüksek olsa da bu çalışma ortamında test çalıştırılamadı. Gerçek kalite kapısı olarak unit test + lint + detekt + debug build + Android smoke test birlikte çalışmalı.

## 5. Ortam/doğrulama sonucu

Çalıştırılan komut:

```text
./gradlew testDebugUnitTest --no-daemon -PskipGoogleServices
```

Sonuç: **Çalıştırılamadı.** Ortamda `JAVA_HOME` tanımlı değil ve PATH üzerinde `java` bulunmuyor. Bu nedenle şu anda “testler yeşil” sonucu üretilemez. Bu bir kod başarısızlığı değil, doğrulama ortamı engelidir.

İlk teknik altyapı gereksinimleri:

- JDK 17 kurulumu ve `JAVA_HOME` ayarı
- Gradle wrapper 8.13 erişimi
- `./gradlew testDebugUnitTest -PskipGoogleServices`
- `./gradlew lintDebug detekt -PskipGoogleServices`
- `./gradlew assembleDebug -PskipGoogleServices`
- Uygun emülatör/cihaz ile `connectedDebugAndroidTest`

## 6. Önerilen faz planı

### Faz 0 — Baseline ve proje gerçeğini sabitleme

**Amaç:** Hangi kodun gerçekten çalıştığını ve hangi iddianın kanıtlı olduğunu ayırmak.

- JDK/Gradle build ortamını düzelt.
- Unit test, lint, detekt ve debug build sonuçlarını kaydet.
- ROADMAP/HISTORY/CODE_AUDIT raporlarını tek güncel durumla konsolide et.
- Test/coverage ve build süresi baseline'ı oluştur.
- Cihaz olmadan yapılamayan maddeleri ayrı listele.

**Çıkış kriteri:** Tekrarlanabilir build + test komutları ve temiz bir durum tablosu.

### Faz 1 — Veri bütünlüğü ve paket yaşam döngüsü

**İlk gerçek geliştirme fazı budur.**

- `ACTION_PACKAGE_REPLACED` işleme.
- Receiver event'lerini idempotent yapma.
- Repository yazma metotlarında hata sözleşmesini birleştirme.
- Room migration 21→22 veri kaybı kararını test/backup ile netleştirme.
- Destructive fallback'i production'dan çıkarma veya sınırlama.
- Kategori silme, toplu taşıma, undo ve restart senaryoları.

**Çıkış kriteri:** Paket yükleme/güncelleme/kaldırma ve kategori işlemleri Room, arama ve dock'ta tutarlı; hata durumunda sahte başarı yok.

### Faz 2 — Launcher çekirdeği ve yaşam döngüsü

- Home state'lerini feature bazlı ayırma.
- Onboarding → Launcher geçişi.
- Home tuşu, `onNewIntent`, cold start, rotation, process death.
- Pager/semantic anchor/dock/folder davranışları.
- Telefon/tablet/landscape/font scale smoke testleri.

**Çıkış kriteri:** Launcher temel akışlarında crash/ANR yok ve görsel taşma kanıtı var.

### Faz 3 — Sınıflandırma ve arama doğruluğu

- `AppClassifier` öncelik sırası, vendor prefix sınırları ve çakışan sinyaller.
- Manual review queue restart sonrası durum.
- Search scoring ve Google fallback davranışı.
- FTS5 olmayan cihaz fallback'i.
- 1.000+ uygulama ve 5.000+ indeks öğesi performansı.

### Faz 4 — İzin, gizlilik ve arka plan işler

- Bildirim, kişi, konum, kullanım istatistiği ve medya izin lifecycle testleri.
- Worker duplicate/constraint ve reboot testleri.
- Telemetri rıza false iken sıfır event doğrulaması.
- Backup/restore ve hassas veri kapsamı.

### Faz 5 — Release ve beta kapısı

- İmzalı AAB, Play Console beyanları, QUERY_ALL_PACKAGES açıklaması.
- Dört cihaz profili ve OEM testi.
- Kapalı beta, crash-free/session ve ANR takibi.
- İlk production sonrası özellikler R9 backlog'unda tutulmalı.

## 7. Nereden başlamalıyız?

Önerdiğim sıra:

1. **Önce Faz 0:** JDK 17 ile build/test ortamını ayağa kaldırıp mevcut durumun gerçek fotoğrafını çekelim.
2. **Sonra Faz 1 P0.1–P0.2:** `PACKAGE_REPLACED` akışını ve repository hata sözleşmesini düzeltelim. Bunlar yeni UI özelliğinden daha yüksek risk taşıyor.
3. Her değişiklik için önce regression test yazalım; sonra kodu değiştirelim.
4. Faz kapısında test + lint + detekt + assemble + cihaz smoke birlikte geçmeden yeni faza geçmeyelim.
5. Ancak veri güvenliği kapandıktan sonra Home/Launcher refactor ve görsel iyileştirmelere geçelim.

**İlk çalışma paketi:** `PackageChangeReceiver`, `AppRepository`, `AppDao`, `AppDatabaseMigrationTest` ve ilgili testler. Bu paket küçük, etkisi yüksek ve sonraki tüm fazların veri tabanını güvenceye alır.

## Sonuç

Sistem işlev bakımından zengin ve önemli altyapı parçaları zaten mevcut. Şu an ihtiyaç “daha fazla özellik” değil; **kanıtlanabilir build, tutarlı veri işlemleri, temiz roadmap ve kontrollü faz kapıları**. İlk kodlama hedefi paket yaşam döngüsü + repository hata yönetimi olmalı; ilk operasyonel hedef ise JDK/Gradle doğrulama ortamını çalışır hale getirmek.
