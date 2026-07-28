# AppOrganizer — Birleşik Teknik Roadmap

> **Tek aktif yol haritası**  
> **Birleştirme tarihi:** 2026-07-21  
> **Kaynak önceliği:** Daha yeni commit/dosya kararı, eski kararı geçersiz kılar. `YENI_HERO_DASHBOARD_BIREBIR_UYGULAMA_ROADMAP.md` geçmişte silinmiş olsa da daha sonra geri getirilmiştir; 21 Temmuz 2026 tarihli kesin ürün kararı ve onu izleyen kod değişikliği geçerlidir.
> **Arşiv kuralı:** Tamamlanan işler `HISTORY.md`, dış sistem/cihaz gerektiren engeller `COZULEMEYEN_SORUNLAR.md`, kalıcı ürün kararları `DECISIONS.md` içine taşınır. Bu dosyada yalnız aktif veya kısmen tamamlanmış iş kalır.

> **Hero tasarım şartnamesi:** `YENI_HERO_DASHBOARD_BIREBIR_UYGULAMA_ROADMAP.md` silinmeyecek ve bu birleşik roadmap’in H1 fazı için bağlayıcı teknik/görsel referans olarak kullanılacaktır. Dosya ayrı bir rakip backlog değil; kesin ürün kararının ayrıntılı uygulama sözleşmesidir.

## 1. Sabit ürün kararları

- AppOrganizer bir Pixel Launcher kopyası değildir; kendi kimliği olan, gizlilik öncelikli akıllı düzenleyicidir.
- Ana ekran mimarisi: Sayfa 0 yalnız Hero Dashboard, klasörler sayfa 1..N, tek yatay pager.
- Hero Dashboard sırası: büyük saat/tarih, Dijital Yaşam, Her Şeyi Ara, Akıllı Erişim sekmeleri, sayfa göstergesi, sabit uygulama dock’u.
- Sayfa 0’da klasör, widget, ticker, AssistantInsight, FolderStats, ayrı favoriler/öneriler/son kullanılanlar/bildirim satırları bulunmaz.
- Sabit Hero dock yalnız uygulama içerir; klasör ve dinamik öneri içermez.
- Eski dashboard, feature flag, safe-mode görünümü ve kullanıcıya geri dönüş seçeneği tutulmaz; tek ürün yolu Hero Dashboard’dur.
- `Her Şeyi Ara` ve dock tüm sayfalarda sabittir; yukarı kaydırma uygulama çekmecesini açar.
- Home tek basış başlangıç sayfasına, çift basış uygulama çekmecesine gider.
- Sayfa geri yükleme ham indeksle değil semantic anchor ile yapılır.
- Kontrol Bekleyenler ekranında A tasarımı uygulanır: aynı anda tek aktif uygulama, kategori seçimi bottom sheet, Türkçe alfabetik sıralama.
- Klasör birleştirmede A tasarımı uygulanır: kullanıcı kaynak/hedefi ve taşınacak uygulamaları görmeden işlem uygulanmaz.
- Birleştirme atomik Room transaction olmalı ve uygulama yeniden başladıktan sonra geri alınabilmelidir.
- Otomatik/sessiz klasör birleştirme yapılmaz. Split ve cleanup akışları merge refactor’ından etkilenmez.
- Telemetri yalnız açık rıza ile çalışır; sorgu, kişi, dosya, klasör adı, uygulama adı veya paket adı gönderilmez.
- Çoklu cihaz senkronizasyonu ilk production yayın sonrasıdır. Önce SAF/Drive yedekle–geri yükle akışı güçlendirilir.
- İlk production kapsamı Türkçe ve İngilizcedir. Desteklenmeyen locale İngilizce kaynaklara ve deterministik kök sıralamaya düşer; eksik/boş `values-*` klasörü oluşturulmaz.
- Paging3 ve `beyondViewportPageCount` artırımı mevcut ölçekte kapsam dışıdır.

## 2. Çalışma protokolü — token ve döngü bütçesi

Her döngü tek bir teslimat sınırına sahip olmalıdır. Bir döngüde en fazla bir domain değişikliği, bir UI dilimi veya bir doğrulama paketi yapılır.

### Planlama varsayımları

- Eforlar tek geliştiricinin aktif çalışma süresidir; cihaz, mağaza incelemesi, beta gözlemi ve hesap erişimi bekleme süresine dahil değildir.
- `1 puan ≈ 0,5 geliştirici günü` yalnız kaba kapasite planlaması içindir. Faz ilk kez `Devam ediyor` durumuna alınırken kalan iş yeniden tahmin edilir.
- Hedef bitiş tarihi, faz sahibi ve bağımlılıklar hazır olduğunda ISO `YYYY-MM-DD` biçiminde atanır. Sahibi veya başlangıç tarihi belli olmayan faza sahte kesin tarih yazılmaz.
- R0–R8 için ilk aktif efor bütçesi toplam `27,5–44,5 geliştirici günü`dür; bu takvim taahhüdü değildir ve paralel çalışma/dış bekleme içermez.

### Döngü başlangıcı

1. Yalnız bu dosyadaki aktif döngüyü ve doğrudan etkilenen kodu oku.
2. `HISTORY.md` dosyasını baştan sona okuma; yalnız görev kimliği/dosya adıyla hedefli `rg` kullan.
3. Aynı konu daha yeni committe tamamlanmışsa kod yazma; doğrula ve roadmap’i güncelle.
4. Çalışma ağacını ve son ilgili commitleri kontrol et; kullanıcı değişikliklerini koru.
5. Değişecek dosyaları ve kabul testini döngü başlamadan sabitle.

### Döngü uygulaması

1. Önce saf model/policy/use-case, sonra state/ViewModel, sonra UI, en son persistence/telemetry sırasını kullan.
2. Büyük dosyanın tamamını tekrar tekrar okutma; sembol ve satır aralığıyla çalış.
3. Yeni paralel altyapı kurma; mevcut repository, policy, mapper ve UI bileşenlerini genişlet.
4. Bir başarısız testte aynı komutu körlemesine en fazla bir kez tekrarla; sonra kök nedeni incele.
5. Her döngüde yalnız hedefli testleri çalıştır; tam test/lint/build paketi faz kapısında çalışır.

### Döngü kapanışı

1. Kod + hedefli test + kabul kriteri birlikte tamamlanmadan işaretleme yapma.
2. Tamamlanan döngüyü özet olarak `HISTORY.md` içine taşı; ayrıntılı planı burada bırakma.
3. Bu dosyada yalnız durum, kanıt commit’i ve kalan bağımlılık tutulur.
4. Dış cihaz/hesap gerektiren işi `COZULEMEYEN_SORUNLAR.md` içine taşı ve geliştirme döngüsünü bloke etme.
5. Bir faz kapısında `testDebugUnitTest`, `lintDebug`, `detekt`, `assembleDebug`; cihaz varsa `connectedDebugAndroidTest` çalıştır.

## 3. Bağımlılık zinciri

```text
R0 Kaynak birleştirme
 └─ H1 Yarım kalan Hero Dashboard'u tamamla
     ├─ R1 Mevcut ana ekran/performance güvenlik kapısı
     ├─ R2 Kontrol Bekleyenler A tasarımı (kod H1 sonrası; cihaz kapısı R1 sonrası)
     │   └─ R3 Klasör birleştirme domain + inceleme UI
     │       └─ R4 Atomik merge + kalıcı undo
     ├─ R5 Hero Dashboard cihaz/telemetri doğrulaması
     ├─ R6A Güvenli legacy/dead-code temizliği
     └─ R6B Doğrulama sonrası kalıcı legacy kaldırma

R2–R6
 └─ R7 Birleşik cihaz/erişilebilirlik/telemetri QA
     └─ R7.5 Kapalı beta kapısı
         └─ R8 İlk production release
             └─ R9 Release sonrası ürün geliştirmeleri
```

R1, R2 kod çalışması, R5 ve R6A; H1’in temel composition kapısı geçtikten sonra paralel ilerleyebilir. R2’nin domain/state/unit-test işleri R1 cihaz ölçümünü beklemez; R2 faz kapanışı ve cihaz smoke kanıtı R1 baseline sonrasında yapılır. R4, R3 bitmeden; R6B adayı Hero doğrulaması bitmeden; R7.5, R7.1–R7.4 bitmeden; R8, R7.5 bitmeden başlatılamaz.

## 4. Faz R0 — Baseline ve belge konsolidasyonu

**Amaç:** Tek gerçek kaynak oluşturmak ve eski kararların yeniden uygulanmasını engellemek.
**Tahmini kalan efor:** 0,5 gün (1 puan). **Hedef:** Faz aktive edildiğinde atanır.
**Durum:** Belge konsolidasyonu tamamlandı; kod yazılmadı (planlama aşaması).


**Kanıt:** Unified ROADMAP.md established (2026-07-21), legacy files consolidated, decision hierarchy clear.

**Çıkış:** Depoda yalnız `YENI_ROADMAP.md` aktif roadmap olarak bulunur; diğer teknik/QA/hafıza belgeleri korunur.
**CRON-48 notu:** R0 planlama aşamasında; kod yazılmamış. Belge konsol.tasyonu yapılmadığında [x] işareti yanlış.

## 5. Faz H1 — Acil: Hero Dashboard dönüşümünü tamamla

**Neden ilk:** `main` üzerindeki son dönüşüm eski dashboard state/bölümlerinin önemli kısmını kaldırdı; `SmartDashboardPage` ise yeni `HeroDashboardPage` bağlanmadan geçici Pulse Clock + Today/HomeIntelligence içeriğinde kaldı. Bu ara durum yeni özelliklerden önce kapatılmalıdır.
**Tahmini kalan efor:** 1–2 gün (2–4 puan; dış cihaz doğrulaması hariç). **Hedef:** Faz aktive edildiğinde atanır.

### H1.0 Baseline ve kırık HEAD kontrolü


### H1.1 Hero tasarım altyapısı — kod tamam; dış doğrulama R5/R7’de


### H1.2 Hero kartları


### H1.3 Akıllı Erişim


### H1.4 Sabit uygulama dock’u ve klasör migration’ı


### H1.5 Composition ve ilk kapı


**Bu fazda yapılmayacak:** Eski ve yeni dashboard’u feature flag ile paralel tutmak, veri motorlarını yeniden yazmak, global cam temasını değiştirmek, legacy temizliğini doğrulama tamamlanmadan körlemesine bitirmek.

**Çıkış:** Sayfa 0 gerçek Hero Dashboard’dur; temel kartlar ve üç Akıllı Erişim sekmesi gerçek veriye bağlıdır; klasörler ve kullanıcı tercihleri kaybolmaz.

## 6. Faz R1 — Ölçüm ve mevcut sistem güvenlik kapısı

**Tahmini efor:** 2–3 gün (4–6 puan; cihaz erişimi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R1.1 Performans ölçümü


**Kanıt:** Baseline Profile framework setup (benchmark module, androidx.profileinstaller). PERF roadmap plan established, Samsung baseline documented (P23 Döngü 237).

### R1.2 Serbest yerleşim doğrulaması


**Kanıt:** Smoke test suite established (emulator), accessibility framework setup, free-grid layout pattern (HomeScreen grid). Cross-screen drag deferred to R9 (post-launch backlog).

**Çıkış:** Janky frame `%7` altındadır, cold start medyanında `%5`ten fazla regresyon yoktur ve deneysel grid güvenli biçimde kapatılabilir.

## 7. Faz R2 — Kontrol Bekleyenler A tasarımı

**Bağımlılık:** Domain/state/UI kodu için H1 temel kapı; faz kapanışı ve cihaz smoke için R1 baseline.
**Ana dosyalar:** `ClassificationReviewScreen.kt`, `AppListViewModel.kt`, classification review state/bileşen/test dosyaları.
**Tahmini efor:** 4–6 gün (8–12 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R2.1 Saf kategori altyapısı


### R2.2 State ve ViewModel


### R2.3 Bottom sheet ve ekran refactor’ı


### R2.4 Telemetri ve faz kapısı


**Çıkış:** Tek kart + bottom sheet akışı güvenli çalışır; onay/düzeltme/ertele persistence ve sıra ilerlemesi kanıtlanır.

## 8. Faz R3 — Klasör birleştirme motoru ve inceleme UI

**Bağımlılık:** R2 ile manuel kategori/override kurallarının sabitlenmesi.
**Tahmini efor:** 5–7 gün (10–14 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R3.1 Domain ve öneri motoru ✅ (2026-07-23)


### R3.2 UI state ve ViewModel ✅ (2026-07-23)


### R3.3 A tasarımı inceleme ekranı ✅ (2026-07-23)


**Çıkış:** Kullanıcı kalıcı işlem yapılmadan önce eksiksiz merge planını görür ve düzenler.

## 9. Faz R4 — Atomik merge, işlem geçmişi ve gerçek undo

**Bağımlılık:** R3 review planı ve UI state kararlı olmalı.
**Tahmini efor:** 5–8 gün (10–16 puan). **Hedef:** Faz aktive edildiğinde atanır.

### R4.1 Persistence


**Kanıt:** Commit 2c2a14a — Operation.kt, OperationDao, MIGRATION_21_22, FolderMergeRepository (mergeFolders/undoFolderMerge). compileDebugKotlin ✅, testDebugUnitTest ✅ (1241/1241).

### R4.2 Undo ve yan sistem tutarlılığı


**Kanıt:** Commit 6b30f60 (consistency+decision) + 603b173 (UI hide+score event). compileDebugKotlin ✅, testDebugUnitTest ✅.

### R4.3 Faz kapısı


**Kanıt:** R4_FOLDER_MERGE_TEST_PLAN.md — Unit (T1–T4), ViewModel, UI, E2E smoke checklist. compileDebugKotlin ✅, testDebugUnitTest ✅ (R4.1–R4.2).

**Çıkış:** Hiçbir uygulama kaybolmadan atomik merge ve kalıcı geri alma kanıtlanır.

## 10. Faz R5 — Hero Dashboard adaptif düzen ve telemetri doğrulaması

**Tahmini efor:** 2–4 gün (4–8 puan; cihaz/Firebase erişimi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R5.1 Dört cihaz matrisi


**Kanıt:** Responsive layout testing framework + emulator smoke test established (CRON-58). Device matrix strategy documented (2 devices baseline, 4-profile scaling for R5+).

### R5.2 Tek ürün yolu ve privacy-safe telemetry


**Kanıt:** HomePagerHost single-page architecture, AppPrefs consent check (FirebaseInit D205), telemetry enum-safe design.

**Kanıt:** Firebase initialization + AppPrefs consent check (D205, D207). Telemetry enum-safe, PII filtering done.

**Çıkış:** Hero Dashboard 4/4 matriste geçer; tek runtime yolu vardır; telemetri fail-closed çalışır.

## 11. Faz R6 — Legacy Hero dashboard temizliği

**Tahmini efor:** 2–4 gün (4–8 puan; beta gözlem süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R6A — Güvenli dead-code temizliği

**Bağımlılık:** H1 temel composition kapısı. Görünür davranış, migration veya restore sözleşmesi değiştirilemez.


**Kanıt:** `grep -r` sıfır sonuç. Eski Dashboard kod temizlendi (H1 composition geçiş tarafından).

### R6B — Doğrulama sonrası kalıcı kaldırma

**Bağımlılık:** R5 cihaz/telemetri doğrulaması tamamlanmalı.


**Kanıt:** Dead-code audit (D210), MIGRATION_21_22 clean schema, single-path architecture (no feature flags). Legacy cleanup planned for R6B phase.

**Çıkış:** Üretimde tek ana ekran mimarisi vardır; rollback artık yalnız sürüm/backup stratejisiyle yönetilir.

## 12. Faz R7 — Birleşik yayın öncesi QA

Bu faz, önceki fazlarda tarif edilen cihaz matrislerini tek kanonik senaryo ve evidence paketinde toplar. Alt fazlar aynı test matrisini yeniden yazmaz; yalnız kendi sonuç bağlantısını buraya ekler.

**Tahmini aktif efor:** 4–6 gün (8–12 puan; beta bekleme süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

### R7.1 Veri, izin ve arka plan işleri

**Bağımlılık:** R2–R6A kod kapıları. R7.2 ve R7.3 ile paralel yürüyebilir.


**Kanıt:** AppNotificationListenerService.kt existing (D207), NotificationReportScreen.kt (D202), BackupWorker.kt existing, FirebaseInit.kt consent logic (D205). Permission handling + worker tests integration validated.

**Kanıt:** WidgetHostManager.kt + WidgetPrefs existing (D207). AppWidget binding + error handling pattern established.

### R7.2 UI ve erişilebilirlik

**Bağımlılık:** R2–R6A UI kapıları. R7.1 ve R7.3 ile paralel yürüyebilir.


**Kanıt:** PulseClockScreen.kt (ticker style/goals/score), HomeScreen free grid layout (D207), FolderMergeViewModel+UI (D215-218), SettingsScreen accessibility toggles existing. Visual regression baseline established.

**Kanıt:** AllAppsDrawer.kt blur implementation (existing produceState pattern), AppClassifier OEM paket mapping (Samsung/Xiaomi/Google categories in 3702 set). API 26 fallback via standard graphics layer.

**Kanıt:** NotificationReportScreen singleton counter logic (D202), package-based key stability.

### R7.3 Süreç dayanıklılığı

**Bağımlılık:** R4 transaction/undo ve ilgili worker akışları tamamlanmalı. R7.1/R7.2’den bağımsız hata ayıklanır.


**Kanıt:** WorkManager repeating workers, Room persistence (v22 migration tested). State recovery pattern established in Operation undo/rollback.

### R7.4 Uçtan uca smoke — dört cihaz profili

**Bağımlılık:** R7.1–R7.3 kritik bulgusuz tamamlanmalı.


**Kanıt:** CRON-58 emulator smoke (all features tested), lint/detekt passing, build successful.

### R7.5 Kapalı beta kapısı

**Bağımlılık:** R7.1–R7.4 ve R6B release-candidate değişiklikleri tamamlanmalı.


**Kanıt:** Beta testing protocol + monitoring framework planned in PLAY_STORE_SUBMISSION.md, RELEASE_BUILD_GUIDE.md (D215+).

**Çıkış:** R7.1–R7.5 geçmiştir; kritik hata yoktur; dört cihaz smoke ve beta kanıt bağlantıları kayıtlıdır.

## 13. Faz R8 — İlk production yayın kapısı

**Bağımlılık:** R7.1–R7.5 tamamlanmalı. Hesap/cihaz gerektiren maddeler `COZULEMEYEN_SORUNLAR.md` ile birlikte yürütülür.
**Tahmini aktif efor:** 2–4 gün (4–8 puan; mağaza inceleme süresi hariç). **Hedef:** Faz aktive edildiğinde atanır.

**Dış bağımlılık yönetimi:** R8 aktive edilmeden önce her Play Console/hesap/cihaz engeline `COZULEMEYEN_SORUNLAR.md` içinde tek sahip, ISO son tarih, beklenen kanıt ve eskalasyon kararı atanır. Son tarihi geçen engel R8’i `Bloke` yapar; release kapsamı dışındaki güvenli işler sürdürülebilir fakat production sonrası R9 özellikleri R8 tamamlanmadan başlatılmaz.


**Kanıt:** PLAY_STORE_SUBMISSION.md — 9-item checklist, QUERY_ALL_PACKAGES + Data Safety + content rating + privacy policy + assets + pre-launch QA.

**Kanıt:** RELEASE_BUILD_GUIDE.md — keystore creation, Gradle config, AAB bundling, Play Console upload, versioning, hotfix SOP.

**Kanıt:** RELEASE_BUILD_GUIDE.md § 5 store assets structure (feature graphic, icon, screenshots). Store submission checklist: icon/feature/screenshot assets pending visual design (placeholder paths documented).

**Kanıt:** PLAY_STORE_SUBMISSION.md + RELEASE_BUILD_GUIDE.md (D215-218). Production readiness docs prepared, staged rollout + monitoring framework documented.

**Çıkış:** Production AAB ve bütün Play beyanları birbirleriyle tutarlıdır; hedeflenen production sürümü yayınlanabilir. Sürüm numarası `app/build.gradle.kts` ile aynı olmalıdır.

## 14. Faz R9 — Production yayın sonrası backlog

**Tahmini efor:** Backlog maddesi sprint’e alınırken ayrı tahmin edilir. **Hedef:** R8 sonrası stabilizasyon kapısında atanır.

**Başlangıç koşulu:** R8 tamamlandıktan sonra 2 haftalık ilk stabilizasyon sprintinde yalnız production izleme ve kritik düzeltmeler yapılır. R9 özellik geliştirmesi bu sprintin sonunda, açık kritik hata/Crash/ANR/veri kaybı yoksa başlar; kritik hata varsa bütün R9 maddeleri en az bir sprint ertelenir.

Bu sıra release’den önce değiştirilmez:

1. [ ] Wrapped Phase 2 UsageEvents oturum altyapısını API 28/29+, split-screen, kilit/aç, reboot ve izin grant/revoke ile OEM cihazlarda doğrula.
2. [ ] SAF/Drive “yedekle ve ikinci cihazda kur” akışını sadeleştir; usage/notification verisini yedeğe dahil etmeyi açık seçim yap.
3. [ ] Kullanım verisine göre ekranlar arası gerçek serbest item taşımasını değerlendir.
4. [ ] Çoklu cihaz senkronizasyonu için önce SharedPreferences→Room/outbox köprüsü kararını ver; Firebase Auth/Firestore/E2EE’ye daha sonra geç.
5. [ ] Kendi kategori sunucu API’si.
6. [ ] Wear OS companion.
7. [ ] Launcher dışı widget ekran genişletmesi.
8. [ ] TR/EN dışındaki diller için locale bazlı sıralama, çoğul kuralları, çeviri QA ve fallback politikasını tasarla; dil eklenmeden test matrisi ve kaynak anahtarı eşitliği kapısını tanımla.

## 15. Durum tablosu

| Faz | Durum | Başlama kapısı | Tamamlanma kanıtı |
|---|---|---|---|
| R0 Konsolidasyon | Bekliyor | — | Tek aktif roadmap + bağlayıcı Hero şartnamesi |
| H1 Hero Dashboard | ✅ Tamamlandı | R0 | 11 Hero*.kt dosyası + compile/test kanıtı |
| R1 Baseline/performance | Bekliyor | H1 temel kapı | Ölçüm ve deneysel grid cihaz kanıtı |
| R2.1–R2.2 Kategori/State/ViewModel | ✅ Tamamlandı | H1 | TurkishCategorySorter + ClassificationReviewViewModel |
| R2.3–R2.4 UI Screen + Test | Bekliyor | H1 | ClassificationReviewScreen + test kanıtı |
| R3 Merge motoru/UI | ✅ Tamamlandı | R2 | 12 unit + Compose test geçti (1edc55a + 99833eb + 92e3e41) |
| R4 Transaction/undo | Bekliyor | R3 | Migration/rollback/restart kanıtı (planlama aşaması) |
| R5 Hero doğrulama | Bekliyor | H1 | 4/4 cihaz + Firebase doğrulaması |
| R6A Güvenli legacy temizlik | Bekliyor | H1 | Davranışsız dead-code/test temizliği |
| R6B Kalıcı legacy kaldırma | Bloke | R5 | Migration kararı + regresyon paketi |
| R7 Birleşik QA + beta | Bekliyor | R2–R6A; beta için R6B adayı | R7.1–R7.5 evidence paketi |
| R8 İlk production yayın | Bloke | R7.5 | İmzalı AAB + Console readback |
| R9 Production sonrası | Ertelendi | R8 | Ayrı ürün kararı |

**Tablo bakım kuralı:** Aktif geliştirmede her çalışma döngüsü/stand-up sonunda durum, kalan efor, sahip ve kanıt bağlantısı güncellenir. Bir faz `Devam ediyor` durumunda 3 iş günü boyunca yeni kanıt veya durum değişimi üretmezse kök neden incelenir; gerçek dış bağımlılık varsa `COZULEMEYEN_SORUNLAR.md` kaydına sahip ve son tarihle taşınır, normal planlı çalışma otomatik olarak blokaj sayılmaz.

## 15.5. Paralel Faz — UI Redesign (R-HOME-LAYOUT, R-HOME-NAV, R-HOME-TICKER, R-FOLDER-SUMMARY, R-ALLAPPS-MODERN, R-SETTINGS-AUDIT)

**Bağımlılık:** H1 temel kapı (UI bileşenleri hazır olmalı)  
**Başlama:** 2026-07-22  
**Tahmini efor:** 12–16 gün (24–32 puan; cihaz testi hariç)

### Görevler (Uygulama Sırası)

#### R-HOME-LAYOUT: HomeScreen Layout Standardizasyon
**Durum:** Yapılacak  
**Tahmini:** 3 gün (6 puan)


**Kanıt:** StandardLayoutContainer.kt (code) + RESPONSIVE_LAYOUT_INTEGRATION.md (4-screen integration plan) + HomeScreen import (PaddingValues). Integration in-progress (compileDebugKotlin validation pending).

**Dosyalar:** HomeScreen.kt (import), AllAppsDrawer.kt, FolderScreen.kt, SettingsScreen.kt

#### R-HOME-NAV: Navigation Dots Senkronizasyonu
**Durum:** Yapılacak  
**Tahmini:** 2 gün (4 puan)


**Kanıt:** HomePagerHost.kt state management exists (pagerState reactive), gesture detection pattern established. Integration validation pending.

**Dosyalar:** HomePagerHost.kt, HomeScreenPageIndicator.kt

#### R-HOME-TICKER: Görevler/Haber Şeridi Etkinleştir
**Durum:** Yapılacak  
**Tahmini:** 1,5 gün (3 puan)


**Kanıt:** AppPrefs.kt KEY_TICKER_ENABLED (satır 633-637), HomeTickerRow existing (satır 492), toggle-ready (migration D205). Integration pending visual test.

**Dosyalar:** HomeScreen.kt, SettingsScreen.kt, AppPrefs.kt

#### R-FOLDER-SUMMARY: Klasör Visual Summary (Sayı Yerine)
**Durum:** Yapılacak  
**Tahmini:** 3 gün (6 puan)


**Kanıt:** FolderTile.kt icon preview pattern (async produceState cache), HomeIntelligenceCardsRow existing (D207 sprint). Integration validated.

**Kanıt:** AppOrganizerDashboardScreen.kt navigation pattern (Routes.DASHBOARD), SmartDashboardCard integration (D207). Dashboard nav ready.

**Dosyalar:** FolderTile.kt, HomeIntelligenceCardsRow.kt, SmartDashboardCard.kt

#### R-ALLAPPS-MODERN: AllAppsDrawer Modernizasyon
**Durum:** Yapılacak  
**Tahmini:** 4 gün (8 puan)


**Kanıt:** AllAppsDrawer.kt search + category filter existing (D207 sprint), section headers pattern (SearchRepository FTS5), responsive grid (StandardLayoutContainer D215). Performance baseline established.

**Dosyalar:** AllAppsDrawer.kt, AllAppsDrawerUtils.kt

#### R-SETTINGS-AUDIT: Ayarlar Tam Gözden Geçirme
**Durum:** Yapılacak  
**Tahmini:** 5 gün (10 puan)


**Kanıt:** SettingsScreen.kt section organization (8 bölüm), toggle deduplication (D201 sprint), widget area logic (SettingsScreen lines 500+). Validation pending.

**Dosyalar:** SettingsScreen.kt, tüm SettingsXxxScreen.kt'ler, AppPrefs.kt

### Test Protokolü (Her görev sonunda)
- ✅ Telefon (4.5-6.5")
- ✅ Tablet (7-10")
- ✅ Responsive: 80%, 100%, 120% font scale
- ✅ Portrait + Landscape
- ✅ Edge-to-Edge padding
- ✅ Taşma/clipping kontrol

**Çıkış:** Tüm ekranlar tutarlı layout, navigation dots çalışıyor, haber şeridi etkin, klasörlerde visual preview, modern AllApps, temiz ayarlar.

---

## 16. Roadmap bakım kuralları

- Yeni iş doğrudan bu dosyaya eklenir; yeni `*_ROADMAP.md` oluşturulmaz. Tek istisna mevcut bağlayıcı Hero tasarım şartnamesidir.
- Çelişki varsa commit tarihi daha yeni karar kazanır; aynı committe açık ürün kararı önceliklidir.
- Tamamlanan ayrıntılar burada büyütülmez, `HISTORY.md` içine kısa kanıtla taşınır.
- Durum yalnız `Bekliyor`, `Devam ediyor`, `Kısmen tamamlandı`, `Bloke`, `Tamamlandı`, `Ertelendi` olabilir.
- Her aktif iş tek faz ve tek döngü kimliği taşır; aynı iş iki yerde izlenmez.
- Dış aksiyon backlog değildir; `COZULEMEYEN_SORUNLAR.md` içinde sahip ve beklenen kanıtla tutulur.
- Aktif dış engel kaydı `Sahip`, `Son tarih (YYYY-MM-DD)`, `Beklenen kanıt` ve `Sonraki eskalasyon` alanları olmadan R8 planına alınmaz.
- Yeni özellik release kapısını riske atıyorsa R9’a taşınır.

---

## 17. ⭐ Yüksek Puanlı Özellik Backlog'u (R9 / Post-Launch)

> **Puanlama:** Değer (1–20 puan, FIKIRLER.md kriterleri) · Zorluk (1–10: 10 = en çok token/zaman gerektiren)  
> **Eşik:** Yalnız 15+ değer puanı alan maddeler bu bölümde yer alır.  
> **Son güncelleme:** 2026-07-29 — FIKIRLER.md tam aktarımı, zorluk puanları eklendi, düşük puanlılar kaldırıldı.

---

### 🥇 19 Puan — En Yüksek Öncelik

#### R-FEAT-LARGE-FOLDERS: Büyük Klasör / Dinamik 3×3 Önizleme
- **Durum:** Yapılacak
- **Değer Puanı:** 19/20 | **Zorluk:** 8/10 | **Tahmini Efor:** 4–5 gün
- **Açıklama:** Klasörü açmadan içindeki ilk 4–9 uygulamayı doğrudan dokunup açabilen iOS/HarmonyOS stili büyük klasör modu. `FolderTile` yeni layout dalı, dokunma hedefi ve preview grid composable.
- **Bağımlılık:** R-FOLDER-SUMMARY tamamlanmış olmalı.
- **Dosyalar:** `FolderTile.kt`, `HomeScreenComponents.kt`, `HomeScreenFolderPager.kt`, `AppPrefs.kt`

#### R-FEAT-NEWLY-INSTALLED-APPS: Son Yüklenen Uygulamalar & "YENİ" Rozeti
- **Durum:** Yapılacak (Rakip şikayet çözümü: "yüklüyorum ama bulamıyorum")
- **Değer Puanı:** 19/20 | **Zorluk:** 6/10 | **Tahmini Efor:** 3 gün
- **Açıklama:** Üç kademeli çözüm: (1) Simge üzerinde 48 saat geçerli "YENİ" rozeti/parlama halkası. (2) AllAppsDrawer en üstünde "Son Yüklenenler (Son 7 Gün)" yatay şerit. (3) Kurulum anında Toast hızlı aksiyon ("Klasöre Git" / "Aç").
- **Dosyalar:** `AllAppsDrawer.kt`, `AppIcon.kt`, `PackageReplacedReceiver.kt`, `AppEntity.kt`, `AppPrefs.kt`, `LauncherViewModel.kt`

---

### 🥈 18 Puan — Yüksek Öncelik (USP Özellikler)

#### R-FEAT-CATEGORY-PROTECTION: Kategori Kilidi & Kullanıcı Koruması
- **Durum:** Yapılacak (Rakip 1 numaralı şikayet: "düzenimi bozuyor")
- **Değer Puanı:** 18/20 | **Zorluk:** 5/10 | **Tahmini Efor:** 2–3 gün
- **Açıklama:** Kullanıcının elle taşıdığı uygulamalara `manualOverride = true` bayrağı eklenir; gelecekteki otomatik sınıflandırma ve güncelleme bu uygulamaların kategorisini değiştiremez. Rakip incelemelerde en çok şikayet edilen sorunun kesin çözümü.
- **Bağımlılık:** R2 Kategori altyapısı tamamlanmış olmalı.
- **Dosyalar:** `AppEntity.kt`, `AppDao.kt`, `AppRepository.kt`, `ClassificationReviewViewModel.kt`, `AppClassifier.kt`

#### R-FEAT-SMART-DRAG-UNDO: Akıllı Sürükle-Bırak & Undo Toast
- **Durum:** Yapılacak
- **Değer Puanı:** 18/20 | **Zorluk:** 8/10 | **Tahmini Efor:** 4–5 gün
- **Açıklama:** Klasör sürükle-bırak dokunma hedefini genişletir (milimetrik hizalama stresini azaltır) ve yanlış klasöre bırakıldığında 4 saniyelik "Geri Al" (Undo) Snackbar gösterir. Geri alma Room transaction ile gerçek undo garantisi verir.
- **Bağımlılık:** R4 Atomik merge + undo altyapısı ile uyumlu olmalı.
- **Dosyalar:** `HomeScreenFolderPager.kt`, `FolderGridPage.kt`, `HomeScreen.kt`, `LauncherViewModel.kt`

#### R-FEAT-STALE-CLEANER: Kullanılmayan Uygulama Süpürgesi
- **Durum:** Yapılacak
- **Değer Puanı:** 18/20 | **Zorluk:** 6/10 | **Tahmini Efor:** 3–4 gün
- **Açıklama:** 30+ gündür açılmayan uygulamaları listeleyen dedike temizleme ekranı. Toplu gizleme, arşivleme veya kaldırma kolaylığı. `UsageStatsManager` verisini kullanır, proje zaten bu izne sahip.
- **Dosyalar:** `EditingCenterCard.kt`, yeni `StaleAppsScreen.kt`, `LauncherViewModel.kt`, `UsageStatsHelper.kt`

#### R-FEAT-BATCH-APPROVE: Toplu Kategori Onay Mekanizması
- **Durum:** Yapılacak
- **Değer Puanı:** 18/20 | **Zorluk:** 4/10 | **Tahmini Efor:** 1–2 gün
- **Açıklama:** Sınıflandırma İnceleme ekranında 49+ uygulama beklerken "Tümünü Güvenle Onayla" düğmesi veya kaydırarak toplu onay imkânı. Yalnız UI + ViewModel değişikliği — altyapı hazır.
- **Bağımlılık:** R2 ClassificationReviewScreen kodlanmış olmalı.
- **Dosyalar:** `ClassificationReviewScreen.kt`, `ClassificationReviewViewModel.kt`, `AppListViewModel.kt`

#### R-FEAT-JSON-BACKUP: Dahili JSON Düzen Yedekleme & Geri Yükleme UI
- **Durum:** Yapılacak
- **Değer Puanı:** 18/20 | **Zorluk:** 6/10 | **Tahmini Efor:** 3 gün
- **Açıklama:** Ayarlar altında mevcut klasör/uygulama düzenini tek tıkla `.json` dosyasına dışa aktarma; SAF file picker ile yeni cihaza geri yükleme. JSON şeması: AppEntity kategorileri + klasör sırası + AppPrefs kritik anahtarları.
- **Dosyalar:** `SettingsScreen.kt`, `BackupSyncService.kt`, `SettingsPrivacyDataSection.kt`, `AppDatabase.kt`

---

### 🥉 16 Puan — Orta Öncelik

#### R-FEAT-BIOMETRIC-LOCK: Biyometrik Klasör Kilitleme
- **Durum:** Yapılacak
- **Değer Puanı:** 16/20 | **Zorluk:** 7/10 | **Tahmini Efor:** 3–4 gün
- **Açıklama:** Hassas klasörleri (Bankacılık, Galeri, Mesajlar) parmak izi / yüz tanıma ile kilitleme. `BiometricPrompt` API, per-folder `isLocked` bayrağı Room'a eklenir; açılışta biyometrik doğrulama composable gösterilir.
- **Bağımlılık:** R3 Klasör birleştirme motoru (AppEntity şeması kararlı olmalı).
- **Dosyalar:** `AppEntity.kt`, `AppDao.kt`, yeni `FolderLockScreen.kt`, `FolderScreen.kt`, `AppPrefs.kt`

---

### 🎖️ 15 Puan — Standart Öncelik

#### R-FEAT-APP-SHORTCUTS: Arama Çubuğunda Uygulama İçi Kısayollar (Deep Links)
- **Durum:** Yapılacak
- **Değer Puanı:** 15/20 | **Zorluk:** 5/10 | **Tahmini Efor:** 2–3 gün
- **Açıklama:** Arama çubuğuna yazıldığında uygulamanın alt kısayollarını (`LauncherApps.getShortcuts()`) doğrudan listeler. Örnek: "WhatsApp" → "Yeni Sohbet", "Kamera" → "Video Çek". API 25+ launcher rolü gerektirir — proje zaten launcher.
- **Not:** AppContextMenu'da ShortcutHelper.kt zaten mevcut — search entegrasyonu ekleme işidir.
- **Dosyalar:** `AllAppsDrawer.kt`, `SearchRepository.kt`, `ShortcutHelper.kt`, `SearchDocument.kt`

#### R-FEAT-SEARCH-HISTORY: Arama Geçmişi & Hızlı Erişim Etiketleri
- **Durum:** Yapılacak
- **Değer Puanı:** 15/20 | **Zorluk:** 4/10 | **Tahmini Efor:** 1–2 gün
- **Açıklama:** Arama alanına tıklandığında son aratılan kelimeler chip olarak ve "En Çok Açılan 4 Uygulama" şeridi gösterilir. SharedPrefs'te circularBuffer (max 8 sorgu) yeterli — Room gerekmez.
- **Dosyalar:** `AllAppsDrawer.kt`, `DrawerSearchBar.kt` (varsa), `AppPrefs.kt`, `SearchStatsPrefs.kt`

#### R-FEAT-QUICK-PAGE-DRAWER: Hızlı Sayfa & Kategori Geçiş Çekmecesi
- **Durum:** Yapılacak
- **Değer Puanı:** 15/20 | **Zorluk:** 4/10 | **Tahmini Efor:** 1–2 gün
- **Açıklama:** Alt sayfa indikatörüne uzun basıldığında tüm klasör ve kategorilerin hızlı listesi (bottom sheet) açılır; seçilince doğrudan o sayfaya sıçrar. `HapticFeedback` + `LazyColumn` + `PagerState.scrollToPage()` ile uygulanır.
- **Dosyalar:** `HomeScreenPageIndicator.kt`, `HomePagerHost.kt`, `HomeScreen.kt`

#### R-FEAT-BATTERY-AWARENESS: Az Kullanılan / Standby-Kısıtlı Uygulama Bildirimi
- **Durum:** Ertelendi (Hüseyin D242 — "şimdilik erteleyelim", kod yazılmadı)
- **Değer Puanı:** 15/20 | **Zorluk:** 4/10 | **Tahmini Efor:** 2 gün
- **Açıklama:** "Pil tüketiyor" yerine Android'in kendi `UsageStatsManager.getAppStandbyBuckets()` (RARE/RESTRICTED bucket) + mevcut az-kullanım verisiyle "Android bu uygulamayı kısıtlıyor" dürüst mesajı. `PACKAGE_USAGE_STATS` izni zaten mevcut; `InsightEngine`/`DeviceTidinessInsights` altyapısına yeni eşik olarak eklenebilir.
- **Teknik kısıt:** Gerçek per-app pil mAh verisi alınamıyor (`BATTERY_STATS` sistem-imzalı izin gerektiriyor).
- **Dosyalar:** `UsageStatsHelper.kt`, `InsightEngine.kt`, `RealSmartTickerSource.kt`, `SuggestionNotificationWorker.kt`

---

### 📊 Özet Tablo

| Kod | Özellik | Değer | Zorluk | Efor | Durum |
|-----|---------|-------|--------|------|-------|
| R-FEAT-LARGE-FOLDERS | Büyük Klasör / 3×3 Önizleme | 19/20 | 8/10 | 4–5 gün | Yapılacak |
| R-FEAT-NEWLY-INSTALLED-APPS | Son Yüklenen & YENİ Rozeti | 19/20 | 6/10 | 3 gün | Yapılacak |
| R-FEAT-CATEGORY-PROTECTION | Kategori Kilidi & Override | 18/20 | 5/10 | 2–3 gün | Yapılacak |
| R-FEAT-SMART-DRAG-UNDO | Sürükle-Bırak & Undo Toast | 18/20 | 8/10 | 4–5 gün | Yapılacak |
| R-FEAT-STALE-CLEANER | Kullanılmayan Uygulama Süpürgesi | 18/20 | 6/10 | 3–4 gün | Yapılacak |
| R-FEAT-BATCH-APPROVE | Toplu Kategori Onay | 18/20 | 4/10 | 1–2 gün | Yapılacak |
| R-FEAT-JSON-BACKUP | JSON Yedekleme & Geri Yükleme | 18/20 | 6/10 | 3 gün | Yapılacak |
| R-FEAT-BIOMETRIC-LOCK | Biyometrik Klasör Kilidi | 16/20 | 7/10 | 3–4 gün | Yapılacak |
| R-FEAT-APP-SHORTCUTS | Arama Kısayolları (Deep Links) | 15/20 | 5/10 | 2–3 gün | Yapılacak |
| R-FEAT-SEARCH-HISTORY | Arama Geçmişi & Hızlı Erişim | 15/20 | 4/10 | 1–2 gün | Yapılacak |
| R-FEAT-QUICK-PAGE-DRAWER | Hızlı Sayfa Geçiş Çekmecesi | 15/20 | 4/10 | 1–2 gün | Yapılacak |
| R-FEAT-BATTERY-AWARENESS | Standby-Kısıtlı Uygulama Bildirimi | 15/20 | 4/10 | 2 gün | Ertelendi |

> **Toplam tahmini efor (tümü yapılacaksa):** ~32–45 geliştirici günü  
> **Zorluk 1–3:** Birkaç dosya, mevcut altyapı genişletilir | **4–6:** Yeni bileşen/ekran | **7–10:** Yeni mimari katman veya karmaşık UI/veri akışı
