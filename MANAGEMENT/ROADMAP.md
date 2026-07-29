# AppOrganizer — Birleşik Teknik Roadmap

> **Tek aktif yol haritası** | **Son güncelleme:** 2026-07-29  
> **Bayat temizliği:** R0/H1/R2.1–R2.2/R3/R4.1–R4.2 tamamlandı; detay HISTORY.md'de.  
> **Kural:** Tamamlanan faz detayı bu dosyada tutulmaz — yalnız tek satır kanıt bırakılır.  
> **Öncelik:** 🔴 KRİTİK BUGLAR → aktif fazlar → feature backlog (kolaydan zora).

## 1. Sabit ürün kararları

- AppOrganizer bir Pixel Launcher kopyası değildir; kendi kimliği olan, gizlilik öncelikli akıllı düzenleyicidir.
- Ana ekran mimarisi: Sayfa 0 yalnız Hero Dashboard, klasörler sayfa 1..N, tek yatay pager.
- Hero Dashboard sırası: büyük saat/tarih, Dijital Yaşam, Her Şeyi Ara, Akıllı Erişim sekmeleri, sayfa göstergesi, sabit uygulama dock’u.
- Sayfa 0’da klasör, widget, ticker, AssistantInsight, FolderStats, ayrı favoriler/öneriler/son kullanılanlar/bildirim satırları bulunmaz.
- Sabit Hero dock yalnız uygulama içerir; klasör ve dinamik öneri içermez.
- Eski dashboard, feature flag, safe-mode görünümü ve kullanıcıya geri dönüş seçeneği tutulmaz; tek ürün yolu Hero Dashboard’dur.
- `Her Şeyi Ara` ve dock tüm sayfalarda sabittir; yukarı kaydırma uygulama çekmecesini açtır.
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

## 3. Bağımlılık Zinciri (Güncel)

```text
R1, R2 kod çalışması, R5 ve R6A; H1’in temel composition kapısı geçtikten sonra paralel ilerleyebilir. R2’nin domain/state/unit-test işleri R1 cihaz ölçümünü beklemez; R2 faz kapanışı ve cihaz smoke kanıtı R1 baseline sonrasında yapılır. R4, R3 bitmeden; R6B adayı Hero doğrulaması bitmeden; R7.5, R7.1–R7.4 bitmeden; R8, R7.5 bitmeden başlatılamaz.

## 4. 🚨 KRİTİK BUGLAR — Önce Bunlar Çözülmeli

> Kaynak: `MANAGEMENT/GUNLUK_DENETIM/2026-07-29.md`  
> Çözüm promptları: `MANAGEMENT/GUNLUK_DENETIM/PROMPTS/2026-07-29-FINDING-*.md`

---

### BUG-003 🔴 YÜKSEK — Toplu kategori taşıma sessiz başarısız olabilir

**Etki:** Uygulamalar taşınmış görünür ama Room'da eski kategoride kalır. Reboot'ta geri döner.  
**Güven:** %98 | **Dosya:** `AppRepository.kt` satır 319–321 ve 340–342

**Neden:** `updateAppsCategory()` ve `updateAppsCategoryAutomatically()` exception'ı yalnız logluyor (`Timber.e`). ViewModel başarı varsayarak AppPrefs, arama indeksi, öneri state ve TaskScore'u geri dönüşsüz yazıyor.

**Çözüm — 3 adım:**

**Adım 1 — `AppRepository.kt` satır 319–321 ve 340–342:**
```kotlin
// Her iki catch bloğuna throw e ekle:
} catch (e: Exception) {
    Timber.e(e, "Error updating multiple apps")
    throw e  // ← EKLE
}
```

**Adım 2 — `AppListViewModel.kt` satır 435–465:**  
`updateAppsCategory()` çağrısını try-catch içine al. `catch` bloğuna girilirse AppPrefs/indeks/TaskScore YAZma:
```kotlin
fun updateAppsCategory(packageNames: List<String>, categoryId: String) {
    viewModelScope.launch {
        try {
            repository.updateAppsCategory(packageNames, categoryId)
            // başarı: yan etkiler burada
            AppPrefs.setManualCategoryOverrides(...)
            _uiState.update { it.copy(selectionCleared = true) }
        } catch (e: Exception) {
            Timber.e(e, "Bulk category update failed")
            _uiState.update { it.copy(errorMessage = "Kategori güncellenemedi") }
            // yan etkiler YAZILMAZ
        }
    }
}
```

**Adım 3 — Testler:**  
`AppRepositoryTest.kt` → `updateAppsCategory_whenDaoThrows_propagatesException()` ekle.  
`AppListViewModelTest.kt` → `updateAppsCategory_onFailure_doesNotWriteSideEffects()` ekle.  
`./gradlew testDebugUnitTest --tests "*AppRepository*" --tests "*AppListViewModel*"`

---

### BUG-001 🟠 YÜKSEK-ORTA — Keyword sınıflandırması map sırasına bağlı

**Etki:** Katalog dışı uygulama yanlış klasöre gider. Ana sınıflandırma ile öneri uyuşmayabilir.  
**Güven:** %95 | **Dosyalar:** `AppClassifier.kt`, `CategorySuggestionEngine.kt`, `KeywordDatabase.kt`

**Neden:** `CategorySuggestionEngine` ilk `contains()` eşleşmesinde döner; `AppClassifier` en uzun keyword'ü seçer ama eşit uzunluktaki çakışmada map sırası kazanır. `amazon` hem PRODUCTIVITY hem SHOPPING'de; `payment` hem SHOPPING hem FINANCE'da.

**Çözüm — 2 adım:**

**Adım 1 — `KeywordMatchResult.kt` yeni data class ekle:**
```kotlin
data class KeywordMatchResult(val categoryId: String, val keyword: String, val score: Int)
// EXACT=100+len, STARTS_WITH=80+len, CONTAINS=60+len
```
`AppClassifier.bestKeywordCategory()` → tüm eşleşmeleri topla, score'a göre sırala, en yüksek kazanır.

**Adım 2 — `KeywordDatabase.kt`:**  
`amazon`→yalnız SHOPPING; `payment`→yalnız FINANCE; `video`/`feed`→SOCIAL; `nike`/`adidas`→SHOPPING.

`./gradlew testDebugUnitTest --tests "*AppClassifier*" --tests "*CategorySuggestion*"`

---

### BUG-002 🟡 ORTA — Vendor prefix sınır kontrolü yok

**Etki:** `com.amazonian.reader` Amazon; `MetaMask` Meta ilişkisi varmış gibi sınıflandırılabilir.  
**Güven:** %95 | **Dosya:** `AppClassifier.kt`

**Çözüm — 1 adım:**
```kotlin
// ÖNCE:
packageName.startsWith("com.amazon")
// SONRA (segment sınırı):
packageName.startsWith("com.amazon.") || packageName == "com.amazon"

// ÖNCE (uygulama adı):
appName.contains("meta", ignoreCase = true)
// SONRA (tam token):
appName.split(" ").any { it.equals("meta", ignoreCase = true) }
```
`./gradlew testDebugUnitTest --tests "*AppClassifier*"`

---

## 5. Tamamlananlar (Detay HISTORY.md'de)

| Faz | Kanıt |
|-----|-------|
| R0 Konsolidasyon | Unified ROADMAP.md (2026-07-21) |
| H1 Hero Dashboard | 11 Hero*.kt + compile/test (2026-07-21) |
| R2.1–R2.2 Kategori/ViewModel | TurkishCategorySorter + ClassificationReviewViewModel |
| R3 Merge motoru/UI | 12 unit + Compose test (1edc55a + 99833eb) |
| R4.1–R4.2 Transaction/model | commit 2c2a14a + 6b30f60 |

---

## 6. Aktif Faz İşleri

### R2.3–R2.4 — ClassificationReviewScreen + Test
**Bağımlılık:** R2.1–R2.2 ✅ | **Efor:** 3–5 gün | **Durum:** ⏳ Bekliyor  
- A tasarımı: tek kart, sola kaydır = reddet, sağa kaydır = onayla, bottom sheet kategori seçimi, Türkçe alfabetik
- `pendingQueue: StateFlow`, `approveApp()`, `rejectApp()`, `snoozeApp(duration)`
- Telemetri: rıza false ise loglanmaz
- Test: 10+ app → kaydır → restart → yeniden bekleyen çıkmıyor mu?

### R4.3 — Atomik Merge Senaryo Testleri
**Bağımlılık:** R4.1–R4.2 ✅ | **Efor:** 2–3 gün | **Durum:** ⏳ Bekliyor  
- `./gradlew testDebugUnitTest --tests "*FolderMerge*"`
- 2 klasör birleştir → hiçbir uygulama kaybolmasın → undo → orijinal durum geri gelsin → restart'ta kalıcı mı?

### R5 — Hero Dashboard Cihaz + Telemetri Doğrulaması
**Bağımlılık:** H1 ✅ | **Efor:** 2–4 gün | **Durum:** ⏳ Bekliyor  
- 4 cihaz/emülatör matrisi: küçük telefon (4.5"), büyük telefon (6.5"), katlanabilir, tablet
- Firebase telemetri fail-closed: rıza false → sıfır event gönderilmeli
- Janky frame < %7, cold start medyan regresyonu < %5

### R6A — Güvenli Dead-Code Temizliği
**Bağımlılık:** H1 ✅ | **Efor:** 2–4 gün | **Durum:** ⏳ Bekliyor  
- `AppClassifier.classifyApps()` — 2026-07-29 auditinde üretim caller'ı bulunamadı; tam kontrol yap, 0 caller ise sil
- `grep -r "SmartDashboardPage\|OldDashboard"` → sıfır sonuç olmalı

### R7 — Birleşik QA + Beta
**Bağımlılık:** R2–R6A | **Efor:** 4–6 gün | **Durum:** ⏳ Bekliyor  
- R7.1 veri/izin/arka plan · R7.2 UI/erişilebilirlik · R7.3 süreç dayanıklılığı · R7.4 4-cihaz smoke · R7.5 kapalı beta kapısı

### R8 — İlk Production Yayın
**Bağımlılık:** R7.5 | **Durum:** ⛔ Bloke  
Kanıt: PLAY_STORE_SUBMISSION.md + RELEASE_BUILD_GUIDE.md hazır (D215+)

### R9 — Post-Launch Backlog
**Başlangıç:** R8 + 2 hafta | **Durum:** ⏸️ Ertelendi  
1. Wrapped Phase 2 UsageEvents oturum altyapısı
2. SAF/Drive yedekle–geri yükle
3. Ekranlar arası serbest taşıma
4. Çoklu cihaz senkronizasyonu
5. Kendi kategori API'si
6. Wear OS companion
7. Widget genişletme
8. TR/EN dışı locale QA

---

## 15. Durum Tablosu

| Faz | Durum | Not |
|-----|-------|-----|
| R0 Konsolidasyon | ✅ | HISTORY.md |
| H1 Hero Dashboard | ✅ | 11 Hero*.kt |
| R2.1–R2.2 Kategori/ViewModel | ✅ | TurkishCategorySorter |
| R3 Merge motoru/UI | ✅ | 12 unit + Compose test |
| R4.1–R4.2 Transaction/model | ✅ | commit 2c2a14a + 6b30f60 |
| **BUG-003** Toplu taşıma sessiz hata | 🔴 Açık | AppRepository satır 319–321 |
| **BUG-001** Keyword map sırası | 🟠 Açık | AppClassifier + CategorySuggestionEngine |
| **BUG-002** Vendor prefix sınır | 🟡 Açık | AppClassifier |
| R2.3–R2.4 ClassificationReviewScreen | ⏳ Bekliyor | — |
| R4.3 Merge senaryo testleri | ⏳ Bekliyor | — |
| R5 Hero doğrulama | ⏳ Bekliyor | 4 cihaz + Firebase |
| R6A Dead-code temizlik | ⏳ Bekliyor | classifyApps() caller |
| R6B Kalıcı kaldırma | ⛔ Bloke | R5 bekleniyor |
| R7 Birleşik QA + beta | ⏳ Bekliyor | R2–R6A bitmeli |
| R8 İlk production | ⛔ Bloke | R7.5 bitmeli |
| R9 Post-launch | ⏸️ Ertelendi | R8 + 2 hafta |

---




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

