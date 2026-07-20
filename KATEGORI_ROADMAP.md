# KATEGORİ ROADMAP — Kontrol Bekleyenler A Tasarımı

> **Tarih:** 2026-07-21  
> **Repo:** `hekizoglu/android-folderautomanager`  
> **Hedef ekran:** `Kontrol Bekleyenler`  
> **Seçilen tasarım:** **A — Odaklı Tek Uygulama İncelemesi**  
> **Durum:** Uygulamaya hazır teknik yol haritası  
> **Zorluk:** 7/10 — Çok dosyalı UI + state refactor  
> **Puan:** Kullanıcı Değeri 5 + Uygulanabilirlik 4 + Bağımlılık Riski 4 + Etki Alanı 4 = **17/20**

---

# 1. Amaç

Mevcut `Kontrol Bekleyenler` ekranını aşağıdaki yapıya dönüştür:

1. Kullanıcı aynı anda yalnızca **bir uygulamayı** incelesin.
2. Uygulamanın neden inceleme beklediği açık şekilde gösterilsin.
3. Sistem önerisi, güven oranı ve kaynak bilgisi anlaşılır biçimde sunulsun.
4. Yatay kategori butonları tamamen kaldırılsın.
5. Kategori seçimi arama destekli `ModalBottomSheet` üzerinden yapılsın.
6. Kategoriler `displayOrder` yerine **Türkçe alfabetik sırada** gösterilsin.
7. Uygulama kategorileri ile marka klasörleri ayrı bölümlerde gösterilsin.
8. Kategori seçimi Compose yerel state yerine `AppListViewModel` içinde yönetilsin.
9. Onay veya erteleme sonrası sıradaki uygulama otomatik açılarak hızlı akış sağlansın.
10. Ekran telefon, tablet, küçük ekran ve büyük yazı ayarlarında taşmadan çalışsın.

---

# 2. Mevcut Sorunlar

## 2.1. Kategori sırası yanlış kullanıcı deneyimi oluşturuyor

**Mevcut kod:**

`app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationReviewScreen.kt`

```kotlin
val categories = screenState.categories
    .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
    .sortedBy { it.displayOrder }
```

**Sorun:**

- Kullanıcı kategori adını alfabetik olarak arıyor.
- `displayOrder` kategorinin yaratılma/sistem sırasına bağlı.
- Türkçe karakterler için düz `sortedBy { categoryName }` de güvenli değil.
- `I`, `İ`, `Ç`, `Ş`, `Ö`, `Ü`, `Ğ` sıralaması cihaz diline göre bozulabilir.

**Karar:**

- Review ekranında `displayOrder` kullanılmayacak.
- `Locale("tr", "TR")` tabanlı `Collator` kullanılacak.

---

## 2.2. Yatay kategori butonları ölçeklenmiyor

**Mevcut yapı:**

```kotlin
LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(categories, key = { it.categoryId }) { category ->
        // Button / OutlinedButton
    }
}
```

**Sorun:**

- 30–40 kategori yatay kaydırma içine sıkışıyor.
- Kullanıcı aradığı kategorinin nerede olduğunu göremiyor.
- Kategori sayısı büyüdükçe karar süresi uzuyor.
- Büyük yazı kullanımında butonlar daha da genişliyor.
- Erişilebilirlik açısından zayıf.

**Karar:**

- `LazyRow` tamamen kaldırılacak.
- Yerine tek bir kategori seçim alanı kullanılacak.
- Alana basınca `ModalBottomSheet` açılacak.

---

## 2.3. İç içe kaydırma ve sabit yükseklik kaldırılmalı

**Mevcut yapı:**

```kotlin
LazyColumn(
    modifier = Modifier
        .fillMaxWidth()
        .height(560.dp),
)
```

**Sorun:**

- `SettingsSubScreenScaffold` içinde ikinci bir kaydırma alanı oluşuyor.
- Sabit `560.dp`, küçük telefonlarda içerik kesebilir.
- Tabletlerde gereksiz boşluk oluşturabilir.
- Büyük yazı ve ekran yakınlaştırma ayarlarında butonlar alta taşabilir.

**Karar:**

- İçteki `LazyColumn` kaldırılacak.
- Ana scaffold tek kaydırma kaynağı olacak.
- Ekranda yalnızca aktif uygulamanın detay kartı ve sıradaki uygulamaların kompakt önizlemesi bulunacak.

---

## 2.4. Seçim state'i yanlış katmanda

**Mevcut yapı:**

```kotlin
val selectedCategoryByPackage = remember {
    mutableStateMapOf<String, String>()
}
```

**Sorun:**

- State yalnızca composable yaşam döngüsünde tutuluyor.
- Ekran yeniden yaratılırsa seçim kaybolabilir.
- UI iş mantığıyla doğrudan ilgileniyor.
- Test etmek zorlaşıyor.

**Karar:**

- Seçim state'i `AppListViewModel` içine taşınacak.
- Ekran yalnızca state okuyacak ve event gönderecek.

---

# 3. Nihai Kullanıcı Akışı

## 3.1. Ekran açılışı

1. `classificationAttentionApps` akışı okunur.
2. Liste öncelik kurallarına göre sıralanır.
3. İlk uygulama aktif inceleme uygulaması olur.
4. Uygulamanın mevcut kategori önerisi hesaplanır.
5. Dikkat nedeni hesaplanır.
6. Kullanıcıya tek büyük inceleme kartı gösterilir.

## 3.2. Kullanıcı kategori seçer

1. `Seçilecek kategori` alanına dokunur.
2. `ModalBottomSheet` açılır.
3. En üstte önerilen kategori görünür.
4. Genel kategoriler Türkçe alfabetik listelenir.
5. Marka klasörleri ayrı bölümde Türkçe alfabetik listelenir.
6. Kullanıcı kategoriye dokunur.
7. Seçim ViewModel state'ine yazılır.
8. Sheet kapanır.
9. Ana kartta seçilen kategori görünür.
10. `Seçimi onayla` butonu aktif olur.

## 3.3. Kullanıcı onaylar

1. Seçilen kategori uygulamanın mevcut kategorisiyle aynıysa:
   - `confirmPendingClassification(packageName)` çağrılır.
2. Seçilen kategori farklıysa:
   - `correctPendingClassification(packageName, categoryId)` çağrılır.
3. İşlem sırasında aynı butona tekrar basılması engellenir.
4. Başarılı işlemden sonra aktif uygulama listeden düşer.
5. Sıradaki uygulama otomatik aktif olur.
6. Sheet state'i ve arama metni temizlenir.

## 3.4. Kullanıcı erteler

1. `7 gün ertele` seçilir.
2. `skipPendingClassification(packageName)` çağrılır.
3. Aktif uygulama kuyruktan çıkar.
4. Sıradaki uygulama otomatik aktif olur.

## 3.5. Liste biter

1. Aktif uygulama `null` olur.
2. Başarı boş ekranı gösterilir.
3. Metin:
   - `Tüm uygulamalar düzenli`
   - `Yeni bir uygulama belirsiz kaldığında burada göstereceğiz.`

---

# 4. Görsel Yapı — A Tasarımı

Ekran yukarıdan aşağı şu sırada kurulacak:

1. **Üst başlık**
   - Geri butonu
   - `Kontrol Bekleyenler`
   - Bekleyen sayı rozeti

2. **Özet kartı**
   - `12 uygulama inceleme bekliyor`
   - `Önce en belirsiz uygulamalar gösteriliyor`
   - ilerleme göstergesi
   - tamamlanan / toplam bilgisi

3. **Filtre chip'leri — Faz 2**
   - Tümü
   - Düşük güven
   - Çelişkili
   - Kategorisiz

4. **Aktif uygulama kartı**
   - uygulama simgesi
   - uygulama adı
   - paket adı
   - güven rozeti
   - `Neden burada?`
   - sistem önerisi
   - güven göstergesi
   - kategori seçim alanı
   - `Seçimi onayla`
   - `7 gün ertele`

5. **Sıradaki uygulamalar**
   - en fazla 3 kompakt satır
   - uygulama simgesi
   - ad
   - dikkat nedeni etiketi
   - önerilen kategori
   - dokununca aktif uygulama yap

6. **Kural kartı**
   - kategori seçilmeden onay verilemez
   - manuel karar otomatik sınıflandırma tarafından ezilmez

---

# 5. Mimari Kararlar

## 5.1. Veritabanı migration yapılmayacak

`AppInfo` içinde ihtiyaç duyulan alanlar zaten var:

- `classificationSource`
- `classificationConfidence`
- `classificationReason`
- `classificationReviewState`
- `isCategoryLocked`
- `lastReviewedAt`
- `reviewSnoozedUntil`

Bu nedenle ilk sürümde Room şeması değiştirilmeyecek.

## 5.2. Mevcut sınıflandırma motoru korunacak

Aşağıdaki yapılar aynen kullanılacak:

- `ClassificationAttentionPolicy`
- `CategorySuggestionEngine`
- `confirmPendingClassification`
- `correctPendingClassification`
- `skipPendingClassification`
- `TaskScoreManager`
- repository içindeki manuel kategori kilidi / review işlemleri

## 5.3. Yeni ayar toggle'ı eklenmeyecek

Bu çalışma yeni bir opsiyonel özellik değildir; mevcut ekranın yerine geçen zorunlu UX düzeltmesidir. Bu nedenle `AppPrefs` içine yeni toggle eklenmeyecek.

## 5.4. İş mantığı ViewModel'de olacak

Composable'lar:

- state okuyacak,
- kullanıcı event'i gönderecek,
- repository çağrısı yapmayacak,
- manuel kategori karşılaştırması yapmayacak.

---

# 6. Dosya Değişiklik Listesi

## 6.1. Değiştirilecek dosyalar

- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationReviewScreen.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt`
- [ ] `app/src/main/res/values/classification_review_strings.xml`
- [ ] `app/src/main/res/values-en/classification_review_strings.xml` — varsa güncelle
- [ ] `app/src/main/java/com/armutlu/apporganizer/telemetry/TelemetryEvent.kt` — telemetry aktifse

## 6.2. Yeni oluşturulacak dosyalar

- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationReviewUiState.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/CategoryPickerSection.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/TurkishCategorySorter.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationCategoryGrouping.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationReviewSummaryCard.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ActiveClassificationReviewCard.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationReviewQueue.kt`
- [ ] `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationCategoryPickerSheet.kt`
- [ ] `app/src/test/java/com/armutlu/apporganizer/presentation/ui/screens/TurkishCategorySorterTest.kt`
- [ ] `app/src/test/java/com/armutlu/apporganizer/presentation/ui/screens/ClassificationCategoryGroupingTest.kt`
- [ ] `app/src/test/java/com/armutlu/apporganizer/presentation/viewmodel/ClassificationReviewViewModelTest.kt`

> Not: Projede ekran yardımcıları için farklı bir paket standardı varsa yeni dosyalar o pakete taşınabilir. Ancak mevcut `ClassificationReviewScreen.kt` dosyasının yolu değiştirilmemeli; navigation importlarını gereksiz yere kırmamak gerekir.

---

# 7. Aşama 1 — Türkçe Alfabetik Sıralama

## 7.1. Yeni dosya

`TurkishCategorySorter.kt`

```kotlin
package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.Category
import java.text.Collator
import java.util.Locale

internal object TurkishCategorySorter {

    private val collator: Collator = Collator
        .getInstance(Locale("tr", "TR"))
        .apply {
            strength = Collator.PRIMARY
        }

    fun sort(categories: List<Category>): List<Category> {
        return categories.sortedWith { first, second ->
            collator.compare(first.categoryName, second.categoryName)
        }
    }
}
```

## 7.2. Kurallar

- [ ] `Category.CAT_UNCATEGORIZED` seçim listesinden çıkarılacak.
- [ ] `displayOrder` review ekranında kullanılmayacak.
- [ ] Sıralama kategori ID'sine göre değil kullanıcıya gösterilen `categoryName` üzerinden yapılacak.
- [ ] Büyük/küçük harf farkı sıralamayı bozmamalı.
- [ ] Türkçe karakter testleri yazılmalı.

## 7.3. Test örnekleri

```kotlin
@Test
fun `categories are sorted using Turkish alphabet`() {
    val input = listOf(
        category("Üretkenlik"),
        category("İletişim"),
        category("Çizgi Roman"),
        category("Alışveriş"),
        category("Spor"),
    )

    val result = TurkishCategorySorter.sort(input)
        .map { it.categoryName }

    assertEquals(
        listOf("Alışveriş", "Çizgi Roman", "İletişim", "Spor", "Üretkenlik"),
        result,
    )
}
```

---

# 8. Aşama 2 — Kategori Gruplama

## 8.1. Yeni model

`CategoryPickerSection.kt`

```kotlin
package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.Category

internal data class CategoryPickerSection(
    val type: CategoryPickerSectionType,
    val categories: List<Category>,
)

internal enum class CategoryPickerSectionType {
    SUGGESTED,
    APPLICATION,
    BRAND,
}
```

## 8.2. Yeni grouping sınıfı

`ClassificationCategoryGrouping.kt`

```kotlin
package com.armutlu.apporganizer.presentation.ui.screens

import com.armutlu.apporganizer.domain.models.Category

internal object ClassificationCategoryGrouping {

    private val brandCategoryIds = setOf(
        Category.CAT_AMAZON,
        Category.CAT_APPLE,
        Category.CAT_GOOGLE,
        Category.CAT_HUAWEI,
        Category.CAT_META,
        Category.CAT_MICROSOFT,
        Category.CAT_SAMSUNG,
        Category.CAT_SPOTIFY,
        Category.CAT_XIAOMI,
    )

    fun buildSections(
        categories: List<Category>,
        suggestedCategoryIds: List<String>,
        query: String,
    ): List<CategoryPickerSection> {
        val normalizedQuery = query.trim()

        val selectable = categories
            .asSequence()
            .filter { it.categoryId != Category.CAT_UNCATEGORIZED }
            .filter { category ->
                normalizedQuery.isBlank() ||
                    category.categoryName.contains(normalizedQuery, ignoreCase = true)
            }
            .toList()

        val suggestedIdSet = suggestedCategoryIds.toSet()

        val suggested = TurkishCategorySorter.sort(
            selectable.filter { it.categoryId in suggestedIdSet }
        )

        val remaining = selectable.filterNot { it.categoryId in suggestedIdSet }

        val applicationCategories = TurkishCategorySorter.sort(
            remaining.filterNot { it.categoryId in brandCategoryIds }
        )

        val brandCategories = TurkishCategorySorter.sort(
            remaining.filter { it.categoryId in brandCategoryIds }
        )

        return buildList {
            if (suggested.isNotEmpty()) {
                add(CategoryPickerSection(CategoryPickerSectionType.SUGGESTED, suggested))
            }
            if (applicationCategories.isNotEmpty()) {
                add(CategoryPickerSection(CategoryPickerSectionType.APPLICATION, applicationCategories))
            }
            if (brandCategories.isNotEmpty()) {
                add(CategoryPickerSection(CategoryPickerSectionType.BRAND, brandCategories))
            }
        }
    }
}
```

## 8.3. Kurallar

- [ ] Önerilen kategori normal listelerde ikinci kez görünmeyecek.
- [ ] Genel kategori ve marka klasörü birbirine karışmayacak.
- [ ] Kullanıcının oluşturduğu özel kategoriler `Uygulama Kategorileri` bölümünde gösterilecek.
- [ ] Arama sonuçlarında boş section gösterilmeyecek.
- [ ] Arama sonucu sıfırsa açıklayıcı boş durum gösterilecek.

---

# 9. Aşama 3 — Review UI State

## 9.1. Yeni state modeli

`ClassificationReviewUiState.kt`

```kotlin
package com.armutlu.apporganizer.presentation.ui.screens

internal data class ClassificationReviewUiState(
    val activePackageName: String? = null,
    val selectedCategoryByPackage: Map<String, String> = emptyMap(),
    val categoryPickerOpen: Boolean = false,
    val categorySearchQuery: String = "",
    val processingPackageName: String? = null,
    val completedInSession: Int = 0,
    val filter: ClassificationReviewFilter = ClassificationReviewFilter.ALL,
)

internal enum class ClassificationReviewFilter {
    ALL,
    LOW_CONFIDENCE,
    CONFLICT,
    UNCATEGORIZED,
}
```

## 9.2. Neden map tutuluyor?

Kullanıcı aktif uygulamayı değiştirip kuyruktaki başka uygulamaya dokunursa önceki uygulamanın seçimi kaybolmamalı. Bu nedenle tek `selectedCategoryId` yerine `packageName -> categoryId` map'i kullanılacak.

## 9.3. State güvenlik kuralları

- [ ] Aktif paket artık pending listede değilse otomatik ilk pending uygulamaya geç.
- [ ] Pending liste boşsa `activePackageName = null` yap.
- [ ] Onaylanan/ertelenen paketin seçimini map'ten sil.
- [ ] Sheet kapanınca arama sorgusunu temizle.
- [ ] İşlem sürerken onay ve ertele butonlarını disable et.
- [ ] Aynı package için çift işlem başlatma.

---

# 10. Aşama 4 — AppListViewModel Değişiklikleri

## 10.1. Eklenecek state

`AppListViewModel.kt` içinde `classificationAttentionApps` tanımından sonra ekle:

```kotlin
private val _classificationReviewUiState = MutableStateFlow(
    ClassificationReviewUiState()
)

val classificationReviewUiState: StateFlow<ClassificationReviewUiState> =
    _classificationReviewUiState.asStateFlow()
```

## 10.2. Pending listeyi önceliklendir

Mevcut `classificationAttentionApps` doğrudan policy sonucunu yayımlıyor. Review ekranına özel sıralama ekle:

```kotlin
val classificationAttentionApps: StateFlow<List<AppInfo>> =
    repository.getAllAppsFlow()
        .map { apps ->
            ClassificationAttentionPolicy
                .attentionList(apps)
                .sortedWith(classificationReviewComparator)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            emptyList(),
        )
```

Comparator:

```kotlin
private val classificationReviewComparator =
    compareBy<AppInfo> { app -> reviewPriority(app) }
        .thenBy { app -> app.classificationConfidence }
        .thenBy { app -> app.appName.lowercase(Locale("tr", "TR")) }

private fun reviewPriority(app: AppInfo): Int {
    return when (app.classificationReason) {
        ClassificationReason.CLASSIFIER_CONFLICT.name -> 0
        ClassificationReason.LOW_CONFIDENCE.name -> 1
        ClassificationReason.NO_RELIABLE_MATCH.name -> 2
        else -> 3
    }
}
```

> `ClassificationReason` enum sabitlerinin gerçek isimlerini dosyadan kontrol et. String alanla enum karşılaştırması yapılırken projedeki mevcut serialization biçimini bozma.

## 10.3. Pending liste değişimini izle

`init` içinde ayrı coroutine başlat:

```kotlin
viewModelScope.launch {
    classificationAttentionApps.collect { pendingApps ->
        syncClassificationReviewActiveApp(pendingApps)
    }
}
```

Fonksiyon:

```kotlin
private fun syncClassificationReviewActiveApp(pendingApps: List<AppInfo>) {
    _classificationReviewUiState.update { state ->
        val currentStillExists = pendingApps.any {
            it.packageName == state.activePackageName
        }

        state.copy(
            activePackageName = when {
                pendingApps.isEmpty() -> null
                currentStillExists -> state.activePackageName
                else -> pendingApps.first().packageName
            },
            selectedCategoryByPackage = state.selectedCategoryByPackage
                .filterKeys { packageName ->
                    pendingApps.any { it.packageName == packageName }
                },
        )
    }
}
```

## 10.4. Eklenecek event fonksiyonları

```kotlin
fun setActiveClassificationReviewApp(packageName: String)
fun openClassificationCategoryPicker()
fun closeClassificationCategoryPicker()
fun setClassificationCategorySearchQuery(query: String)
fun selectClassificationReviewCategory(packageName: String, categoryId: String)
fun confirmActiveClassificationReview()
fun snoozeActiveClassificationReview()
fun setClassificationReviewFilter(filter: ClassificationReviewFilter)
```

## 10.5. Aktif uygulama değiştirme

```kotlin
fun setActiveClassificationReviewApp(packageName: String) {
    if (classificationAttentionApps.value.none { it.packageName == packageName }) return

    _classificationReviewUiState.update {
        it.copy(
            activePackageName = packageName,
            categoryPickerOpen = false,
            categorySearchQuery = "",
        )
    }
}
```

## 10.6. Kategori seçme

```kotlin
fun selectClassificationReviewCategory(
    packageName: String,
    categoryId: String,
) {
    val categoryExists = screenState.value.categories.any {
        it.categoryId == categoryId &&
            it.categoryId != Category.CAT_UNCATEGORIZED
    }
    if (!categoryExists) return

    _classificationReviewUiState.update { state ->
        state.copy(
            selectedCategoryByPackage =
                state.selectedCategoryByPackage + (packageName to categoryId),
            categoryPickerOpen = false,
            categorySearchQuery = "",
        )
    }
}
```

## 10.7. Aktif uygulamayı onaylama

```kotlin
fun confirmActiveClassificationReview() {
    val uiState = _classificationReviewUiState.value
    val packageName = uiState.activePackageName ?: return
    val selectedCategoryId = uiState.selectedCategoryByPackage[packageName] ?: return
    val app = classificationAttentionApps.value
        .firstOrNull { it.packageName == packageName }
        ?: return

    if (uiState.processingPackageName != null) return

    _classificationReviewUiState.update {
        it.copy(processingPackageName = packageName)
    }

    viewModelScope.launch {
        runCatching {
            if (selectedCategoryId == app.categoryId) {
                repository.confirmClassification(packageName)
                TaskScoreManager.record(
                    getApplication(),
                    TaskScoreManager.EventType.ClassificationApproved,
                )
            } else {
                val oldCategoryId = app.categoryId
                repository.updateAppCategory(packageName, selectedCategoryId)
                AppPrefs.setManualCategoryOverride(
                    getApplication(),
                    packageName,
                    selectedCategoryId,
                )
                repository.getAppByPackageName(packageName)
                    ?.let { searchRepository.indexApp(it) }
                prepareSimilarCategorySuggestions(
                    packageName,
                    oldCategoryId,
                    selectedCategoryId,
                )
                TaskScoreManager.record(
                    getApplication(),
                    TaskScoreManager.EventType.ClassificationCorrected,
                )
            }
        }.onFailure { error ->
            Timber.e(error, "Classification review confirm failed")
            _screenState.update {
                it.copy(error = "Kategori kararı kaydedilemedi")
            }
        }

        _classificationReviewUiState.update { state ->
            state.copy(
                processingPackageName = null,
                selectedCategoryByPackage =
                    state.selectedCategoryByPackage - packageName,
                categoryPickerOpen = false,
                categorySearchQuery = "",
                completedInSession = state.completedInSession + 1,
            )
        }
    }
}
```

### Daha düşük riskli alternatif

İlk uygulamada mevcut public fonksiyonları kullan:

```kotlin
if (selectedCategoryId == app.categoryId) {
    confirmPendingClassification(packageName)
} else {
    correctPendingClassification(packageName, selectedCategoryId)
}
```

Ancak bu fonksiyonlar fire-and-forget olduğu için işlem bitişini kesin izleyemez. En doğru çözüm, repository işlemini tek coroutine içinde yapan yeni `confirmActiveClassificationReview()` fonksiyonudur.

## 10.8. Erteleme

```kotlin
fun snoozeActiveClassificationReview() {
    val packageName = _classificationReviewUiState.value.activePackageName ?: return
    if (_classificationReviewUiState.value.processingPackageName != null) return

    _classificationReviewUiState.update {
        it.copy(processingPackageName = packageName)
    }

    viewModelScope.launch {
        runCatching {
            repository.skipClassificationReview(packageName, days = 7)
            TaskScoreManager.record(
                getApplication(),
                TaskScoreManager.EventType.ClassificationSnoozed,
            )
        }.onFailure { error ->
            Timber.e(error, "Classification review snooze failed")
            _screenState.update {
                it.copy(error = "Uygulama ertelenemedi")
            }
        }

        _classificationReviewUiState.update { state ->
            state.copy(
                processingPackageName = null,
                selectedCategoryByPackage =
                    state.selectedCategoryByPackage - packageName,
                categoryPickerOpen = false,
                categorySearchQuery = "",
            )
        }
    }
}
```

---

# 11. Aşama 5 — ClassificationReviewScreen Refactor

## 11.1. Kaldırılacak kodlar

- [ ] `remember { mutableStateMapOf<String, String>() }`
- [ ] Her uygulama için tam boy kart oluşturan `items(pendingApps)` bloğu
- [ ] Sabit `.height(560.dp)`
- [ ] Yatay kategori `LazyRow`
- [ ] Kart içindeki `Button / OutlinedButton` kategori listesi
- [ ] UI içindeki kategori aynı mı farklı mı karşılaştırması

## 11.2. Yeni ana state okumaları

```kotlin
val pendingApps by viewModel.classificationAttentionApps.collectAsState()
val screenState by viewModel.screenState.collectAsState()
val reviewUiState by viewModel.classificationReviewUiState.collectAsState()

val activeApp = pendingApps.firstOrNull {
    it.packageName == reviewUiState.activePackageName
}

val queuedApps = pendingApps
    .filterNot { it.packageName == reviewUiState.activePackageName }
    .take(3)

val selectedCategoryId = activeApp?.packageName?.let {
    reviewUiState.selectedCategoryByPackage[it]
}

val selectedCategory = screenState.categories.firstOrNull {
    it.categoryId == selectedCategoryId
}
```

## 11.3. Öneri ve attention reason

```kotlin
val suggestion = activeApp?.let { app ->
    remember(app.packageName, screenState.apps.size) {
        CategorySuggestionEngine.suggestFor(app, screenState.apps)
    }
}

val attentionReason = activeApp?.let { app ->
    remember(app) {
        ClassificationAttentionPolicy.evaluate(app)
    }
}
```

## 11.4. Ana ekran iskeleti

```kotlin
SettingsSubScreenScaffold(
    title = stringResource(R.string.classification_review_title),
    onNavigateBack = onNavigateBack,
) {
    item {
        ClassificationReviewSummaryCard(
            pendingCount = pendingApps.size,
            completedInSession = reviewUiState.completedInSession,
        )
    }

    if (activeApp == null) {
        item {
            ClassificationReviewEmptyState()
        }
    } else {
        item {
            ActiveClassificationReviewCard(
                app = activeApp,
                selectedCategory = selectedCategory,
                suggestedCategory = suggestion?.categoryId?.let(viewModel::getCategoryById),
                suggestion = suggestion,
                attentionReason = attentionReason,
                processing = reviewUiState.processingPackageName == activeApp.packageName,
                onOpenCategoryPicker = viewModel::openClassificationCategoryPicker,
                onConfirm = viewModel::confirmActiveClassificationReview,
                onSnooze = viewModel::snoozeActiveClassificationReview,
            )
        }

        if (queuedApps.isNotEmpty()) {
            item {
                ClassificationReviewQueue(
                    apps = queuedApps,
                    categoryNames = screenState.categories.associate {
                        it.categoryId to it.categoryName
                    },
                    onAppClick = viewModel::setActiveClassificationReviewApp,
                )
            }
        }
    }

    item {
        ClassificationReviewRuleCard()
    }
}
```

## 11.5. Sheet çağrısı

Scaffold içeriği bittikten sonra composable scope içinde:

```kotlin
if (reviewUiState.categoryPickerOpen && activeApp != null) {
    val suggestedIds = listOfNotNull(suggestion?.categoryId)

    val sections = remember(
        screenState.categories,
        reviewUiState.categorySearchQuery,
        suggestedIds,
    ) {
        ClassificationCategoryGrouping.buildSections(
            categories = screenState.categories,
            suggestedCategoryIds = suggestedIds,
            query = reviewUiState.categorySearchQuery,
        )
    }

    ClassificationCategoryPickerSheet(
        sections = sections,
        selectedCategoryId = selectedCategoryId,
        query = reviewUiState.categorySearchQuery,
        onQueryChange = viewModel::setClassificationCategorySearchQuery,
        onCategorySelected = { categoryId ->
            viewModel.selectClassificationReviewCategory(
                packageName = activeApp.packageName,
                categoryId = categoryId,
            )
        },
        onDismiss = viewModel::closeClassificationCategoryPicker,
    )
}
```

---

# 12. Aşama 6 — Özet Kartı

## 12.1. Yeni dosya

`ClassificationReviewSummaryCard.kt`

## 12.2. Parametreler

```kotlin
@Composable
internal fun ClassificationReviewSummaryCard(
    pendingCount: Int,
    completedInSession: Int,
    modifier: Modifier = Modifier,
)
```

## 12.3. İçerik

- uyarı / review ikonu
- pending sayı
- açıklama
- oturum ilerlemesi
- progress indicator

## 12.4. Kurallar

- [ ] Progress sıfıra bölünmemeli.
- [ ] Pending sayı canlı güncellenmeli.
- [ ] Tamamlanan sayı yalnızca mevcut ekran oturumunu göstermeli.
- [ ] Renkler sabit hex yerine `MaterialTheme.colorScheme` kullanmalı.
- [ ] Dark theme ve Pixel görünümüyle çalışmalı.

---

# 13. Aşama 7 — Aktif İnceleme Kartı

## 13.1. Yeni dosya

`ActiveClassificationReviewCard.kt`

## 13.2. Parametreler

```kotlin
@Composable
internal fun ActiveClassificationReviewCard(
    app: AppInfo,
    selectedCategory: Category?,
    suggestedCategory: Category?,
    suggestion: CategorySuggestionEngine.Suggestion?,
    attentionReason: ClassificationAttentionPolicy.AttentionReason?,
    processing: Boolean,
    onOpenCategoryPicker: () -> Unit,
    onConfirm: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
)
```

> Gerçek nested type isimlerini ilgili dosyalardan kontrol et. Tip public değilse UI için basit mapper model oluştur.

## 13.3. Kart bölümleri

### Başlık

- uygulama simgesi
- uygulama adı
- paket adı
- güven rozeti

### Neden burada?

- `ClassificationAttentionPolicy.reasonStringRes(...)` kullan
- kullanıcıya teknik enum gösterme

### Sistem önerisi

- kategori emoji/icon
- kategori adı
- signal label
- confidence gösterimi

### Kategori seçim alanı

- `OutlinedCard` veya tıklanabilir `Surface`
- seçilmediyse `Kategori seç`
- seçildiyse kategori emoji + kategori adı
- sağda aşağı ok

### Aksiyonlar

- ana buton: `Seçimi onayla`
- ikinci buton: `7 gün ertele`

## 13.4. Buton kuralları

```kotlin
Button(
    enabled = selectedCategory != null && !processing,
    onClick = onConfirm,
)
```

- [ ] Seçili kategori yoksa onay disabled.
- [ ] İşlem sürerken iki buton da disabled.
- [ ] İşlem sürerken ana butonda progress indicator göster.
- [ ] Erteleme butonu ikincil stil kullanmalı.
- [ ] En az 48.dp dokunma alanı olmalı.

---

# 14. Aşama 8 — Uygulama Simgesi

## 14.1. İlk tercih

Projede kullanılan mevcut uygulama ikon composable'ını bul ve yeniden kullan.

Aranacak terimler:

- `AppIcon`
- `packageManager.getApplicationIcon`
- `rememberDrawablePainter`
- `iconUrl`
- `ApplicationInfo.loadIcon`

## 14.2. Mevcut bileşen yoksa

Yeni yardımcı composable oluştur:

```kotlin
@Composable
internal fun ClassificationReviewAppIcon(
    app: AppInfo,
    modifier: Modifier = Modifier,
)
```

Fallback sırası:

1. PackageManager yerel uygulama ikonu
2. `app.iconUrl`
3. Uygulama adının ilk harfini içeren renkli daire

## 14.3. Performans

- [ ] Icon her recomposition'da PackageManager'dan tekrar okunmamalı.
- [ ] `remember(app.packageName)` veya image loader cache kullan.
- [ ] Hatalı/missing icon ekranı çökertmemeli.

---

# 15. Aşama 9 — Kategori Bottom Sheet

## 15.1. Yeni dosya

`ClassificationCategoryPickerSheet.kt`

## 15.2. Ana yapı

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClassificationCategoryPickerSheet(
    sections: List<CategoryPickerSection>,
    selectedCategoryId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        // Header
        // Search
        // LazyColumn sections
    }
}
```

## 15.3. Header

- başlık: `Kategori seç`
- açıklama: `Kategoriler Türkçe alfabetik sıradadır.`
- sürükleme tutamacı Material 3 varsayılanı olabilir

## 15.4. Search

Material 3 `SearchBar` ağır kalırsa sade `OutlinedTextField` kullan:

```kotlin
OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    placeholder = {
        Text(stringResource(R.string.classification_review_picker_search_hint))
    },
    leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = null)
    },
)
```

## 15.5. Section başlıkları

`CategoryPickerSectionType` resource'a map edilmeli:

```kotlin
private fun CategoryPickerSectionType.titleRes(): Int = when (this) {
    CategoryPickerSectionType.SUGGESTED ->
        R.string.classification_review_section_suggested
    CategoryPickerSectionType.APPLICATION ->
        R.string.classification_review_section_app_categories
    CategoryPickerSectionType.BRAND ->
        R.string.classification_review_section_brand_categories
}
```

## 15.6. Kategori satırı

Her satır:

- emoji/icon
- kategori adı
- seçiliyse check ikonu
- tüm satır tıklanabilir

```kotlin
@Composable
private fun ClassificationCategoryRow(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
)
```

## 15.7. Alfabe hızlı çubuğu

**Faz 2 olarak uygulanabilir.**

Önce temel alfabetik liste ve arama tamamlanacak. Alfabe çubuğu eklenirse:

- section içinde A–Z indeksleri çıkarılacak,
- `LazyListState.animateScrollToItem(index)` kullanılacak,
- Türkçe harf dizisi sabit olacak:
  `A B C Ç D E F G Ğ H I İ J K L M N O Ö P R S Ş T U Ü V Y Z`

Alfabe çubuğu ilk commit'in zorunlu parçası değildir. Arama kutusu temel gereksinimi karşılar.

---

# 16. Aşama 10 — Sıradaki Uygulamalar

## 16.1. Yeni dosya

`ClassificationReviewQueue.kt`

## 16.2. Kurallar

- en fazla 3 uygulama göster
- ana kart kadar detay verme
- uygulama simgesi + isim + durum etiketi göster
- paket adını yalnız ihtiyaç varsa küçük metin olarak göster
- satıra dokununca aktif uygulama değişsin

## 16.3. Örnek API

```kotlin
@Composable
internal fun ClassificationReviewQueue(
    apps: List<AppInfo>,
    categoryNames: Map<String, String>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

## 16.4. Durum etiketi mapper

```kotlin
private fun AppInfo.reviewBadgeText(): String {
    return when (classificationReason) {
        ClassificationReason.CLASSIFIER_CONFLICT.name -> "Çelişkili"
        ClassificationReason.LOW_CONFIDENCE.name -> "Düşük güven"
        ClassificationReason.NO_RELIABLE_MATCH.name -> "Kategorisiz"
        else -> "İnceleme"
    }
}
```

Teknik enum string'leri UI'a doğrudan basılmamalı; resource kullanılmalı.

---

# 17. String Kaynakları

## 17.1. Güncellenecek dosya

`app/src/main/res/values/classification_review_strings.xml`

## 17.2. Eklenecek Türkçe metinler

```xml
<string name="classification_review_why_here">Neden burada?</string>
<string name="classification_review_system_suggestion">Sistem önerisi</string>
<string name="classification_review_selected_category">Seçilecek kategori</string>
<string name="classification_review_open_category_picker">Kategori seç</string>
<string name="classification_review_picker_title">Kategori seç</string>
<string name="classification_review_picker_subtitle">Kategoriler Türkçe alfabetik sıradadır.</string>
<string name="classification_review_picker_search_hint">Kategori ara</string>
<string name="classification_review_section_suggested">Önerilen</string>
<string name="classification_review_section_app_categories">Uygulama Kategorileri</string>
<string name="classification_review_section_brand_categories">Marka Klasörleri</string>
<string name="classification_review_queue_title">Sıradaki uygulamalar</string>
<string name="classification_review_view_all">Tümünü gör</string>
<string name="classification_review_progress">%1$d / %2$d tamamlandı</string>
<string name="classification_review_no_match">Aramaya uygun kategori bulunamadı</string>
<string name="classification_review_all_clear_title">Tüm uygulamalar düzenli</string>
<string name="classification_review_all_clear_subtitle">Yeni bir uygulama belirsiz kaldığında burada göstereceğiz.</string>
<string name="classification_review_badge_low_confidence">Düşük güven</string>
<string name="classification_review_badge_conflict">Çelişkili</string>
<string name="classification_review_badge_uncategorized">Kategorisiz</string>
<string name="classification_review_processing">Kaydediliyor…</string>
<string name="classification_review_save_error">Kategori kararı kaydedilemedi.</string>
<string name="classification_review_snooze_error">Uygulama ertelenemedi.</string>
```

## 17.3. İngilizce kaynak

`values-en` mevcutsa eş değer İngilizce metinler eklenmeli. Yoksa bu görev sırasında ayrı dosya açılması zorunlu değildir; ancak mevcut locale standardı korunmalıdır.

---

# 18. Renk ve Tema Kuralları

- [ ] Sabit beyaz/siyah yerine `MaterialTheme.colorScheme` kullan.
- [ ] Ana buton `primary`.
- [ ] Öneri etiketi `secondaryContainer` veya `tertiaryContainer`.
- [ ] Düşük güven etiketi `errorContainer` kullanmamalı; hata değil uyarıdır.
- [ ] Kartlar açık ve koyu temada okunmalı.
- [ ] Pixel görünüm ve dynamic color bozulmamalı.
- [ ] `Category.colorHex` yalnız emoji yanında küçük vurgu olarak kullanılabilir; büyük kart zemini yapılmamalı.

---

# 19. Erişilebilirlik

- [ ] Tüm tıklanabilir alanlar en az 48.dp.
- [ ] İkonların anlamlı olanlarında `contentDescription` kullan.
- [ ] Dekoratif ikonlarda `contentDescription = null`.
- [ ] Renk tek başına bilgi taşımamalı; metin etiketi de bulunmalı.
- [ ] Font scale 1.3 ve 1.5 test edilmeli.
- [ ] TalkBack sırası: uygulama → neden → öneri → seçilen kategori → onay → ertele.
- [ ] Paket adı düşük öncelikli olsa da kontrastı erişilebilir olmalı.
- [ ] Bottom sheet açıldığında klavye içerik üstüne binmemeli.

---

# 20. Telemetry

Telemetry altyapısı aktifse aşağıdaki event'ler eklenmeli:

- `classification_review_screen_opened`
- `classification_review_app_changed`
- `classification_category_picker_opened`
- `classification_category_search_used`
- `classification_category_selected`
- `classification_review_confirmed`
- `classification_review_corrected`
- `classification_review_snoozed`
- `classification_review_queue_completed`

Event parametreleri kişisel veri içermemeli:

- package name göndermek yerine gerekiyorsa hash / category id
- confidence bucket: `0_39`, `40_59`, `60_79`, `80_100`
- reason code
- selected category type: application / brand / custom
- suggestion accepted: true / false

---

# 21. Test Planı

## 21.1. Unit test — Türkçe sıralama

- [ ] A, Ç, İ, Ö, Ş, Ü sırası doğru.
- [ ] Büyük/küçük harf sıralamayı bozmuyor.
- [ ] Aynı isimli iki kategori crash oluşturmuyor.
- [ ] Boş liste düzgün dönüyor.

## 21.2. Unit test — grouping

- [ ] `Kategorisiz` dışlanıyor.
- [ ] Amazon/Apple/Google/Huawei/Meta/Microsoft/Samsung/Spotify/Xiaomi marka bölümüne gidiyor.
- [ ] Önerilen kategori ilk section'da.
- [ ] Önerilen kategori genel listede tekrar etmiyor.
- [ ] Özel kategori uygulama section'ında.
- [ ] Arama section'ları doğru filtreliyor.
- [ ] Sonuç olmayan section gizleniyor.

## 21.3. ViewModel test

- [ ] Pending liste geldiğinde ilk app aktif oluyor.
- [ ] Aktif app listeden düşünce sıradaki app seçiliyor.
- [ ] Kategori seçimi package map'ine yazılıyor.
- [ ] Kategori sheet açılıp kapanıyor.
- [ ] Aynı kategori seçilirse confirm çağrılıyor.
- [ ] Farklı kategori seçilirse correction çağrılıyor.
- [ ] Onay sonrası seçim temizleniyor.
- [ ] Erteleme sonrası seçim temizleniyor.
- [ ] İşlem sürerken ikinci event reddediliyor.
- [ ] Repository hata verirse processing state sıfırlanıyor.
- [ ] Liste bitince aktif app null oluyor.

## 21.4. Compose UI test

- [ ] Ekran başlığı görünüyor.
- [ ] Pending sayı doğru.
- [ ] Aktif uygulama adı görünüyor.
- [ ] Kategori seçilmeden onay disabled.
- [ ] Kategori alanına tıklayınca sheet açılıyor.
- [ ] Arama sonucu doğru kategori gösteriyor.
- [ ] Kategori seçince sheet kapanıyor.
- [ ] Seçilen kategori ana kartta görünüyor.
- [ ] Onay butonu aktif oluyor.
- [ ] Erteleme butonu çalışıyor.
- [ ] Empty state doğru gösteriliyor.

## 21.5. Manuel test cihazları

- [ ] küçük telefon: 360 × 640 dp
- [ ] normal telefon: Pixel 6 / API 33+
- [ ] büyük telefon
- [ ] tablet
- [ ] açık tema
- [ ] koyu tema
- [ ] Pixel görünümü açık
- [ ] font scale 1.0
- [ ] font scale 1.3
- [ ] font scale 1.5
- [ ] ekran döndürme
- [ ] sheet açıkken geri tuşu
- [ ] arama açıkken klavye

---

# 22. Derleme ve Kalite Kontrolleri

Uygulama sırası:

1. [ ] `./gradlew testDebugUnitTest`
2. [ ] `./gradlew lintDebug`
3. [ ] `./gradlew detekt`
4. [ ] `./gradlew assembleDebug`
5. [ ] ADB cihaz varsa `./gradlew connectedDebugAndroidTest`
6. [ ] Debug APK açılış smoke testi
7. [ ] Ayarlar → Uygulamalar → Kontrol Bekleyenler navigasyon testi
8. [ ] Onay / düzeltme / ertele persistence testi

Windows karşılığı gerekiyorsa `gradlew.bat` kullanılacak.

---

# 23. Uygulama Sırası — AI İçin Kesin Görev Listesi

## Döngü 1 — Altyapı

1. [ ] `TurkishCategorySorter.kt` oluştur.
2. [ ] `CategoryPickerSection.kt` oluştur.
3. [ ] `ClassificationCategoryGrouping.kt` oluştur.
4. [ ] İlgili unit testleri yaz.
5. [ ] Unit test çalıştır.

## Döngü 2 — ViewModel state

1. [ ] `ClassificationReviewUiState.kt` oluştur.
2. [ ] `AppListViewModel` içine state flow ekle.
3. [ ] Aktif app senkronizasyonunu ekle.
4. [ ] Sheet event fonksiyonlarını ekle.
5. [ ] Kategori seçim fonksiyonunu ekle.
6. [ ] Onay ve erteleme wrapper'larını ekle.
7. [ ] ViewModel testlerini yaz.

## Döngü 3 — Bottom sheet

1. [ ] `ClassificationCategoryPickerSheet.kt` oluştur.
2. [ ] Header ekle.
3. [ ] Search ekle.
4. [ ] Section listesi ekle.
5. [ ] Seçili kategori tikini ekle.
6. [ ] Boş arama sonucu ekle.
7. [ ] Dark theme preview/test yap.

## Döngü 4 — Aktif kart

1. [ ] `ActiveClassificationReviewCard.kt` oluştur.
2. [ ] App icon ekle.
3. [ ] Güven badge'i ekle.
4. [ ] `Neden burada?` kutusu ekle.
5. [ ] Sistem önerisini ekle.
6. [ ] Kategori seçim alanını ekle.
7. [ ] Onay / ertele butonlarını ekle.
8. [ ] Processing state ekle.

## Döngü 5 — Ekran refactor

1. [ ] Eski yatay kategori `LazyRow` kodunu sil.
2. [ ] Nested `LazyColumn` kodunu sil.
3. [ ] Local `selectedCategoryByPackage` state'ini sil.
4. [ ] Özet kartını bağla.
5. [ ] Aktif kartı bağla.
6. [ ] Queue bileşenini bağla.
7. [ ] Bottom sheet'i bağla.
8. [ ] Empty state'i bağla.

## Döngü 6 — Test ve temizlik

1. [ ] Tüm string'leri resource'a taşı.
2. [ ] Türkçe/İngilizce locale kontrolü yap.
3. [ ] Kullanılmayan importları sil.
4. [ ] Detekt uyarılarını temizle.
5. [ ] Unit test çalıştır.
6. [ ] Lint çalıştır.
7. [ ] Debug build al.
8. [ ] Manuel cihaz testi yap.
9. [ ] HISTORY.md güncelle.
10. [ ] Tamamlanan maddeleri ROADMAP'ten HISTORY'ye taşı.

---

# 24. Kabul Kriterleri

Bu çalışma aşağıdaki şartların tamamı sağlanmadan tamamlanmış sayılmaz:

- [ ] Ekranda aynı anda yalnızca bir büyük inceleme kartı var.
- [ ] Yatay kategori buton listesi tamamen kaldırıldı.
- [ ] Kategori seçimi bottom sheet üzerinden yapılıyor.
- [ ] Kategoriler Türkçe alfabetik sıralanıyor.
- [ ] Marka klasörleri ayrı bölümde.
- [ ] Kategorisiz seçim listesinde yok.
- [ ] Önerilen kategori üstte ve tekrar etmiyor.
- [ ] Kategori araması çalışıyor.
- [ ] Kategori seçilmeden onay verilemiyor.
- [ ] Onay sonrası sıradaki uygulama açılıyor.
- [ ] Erteleme sonrası sıradaki uygulama açılıyor.
- [ ] Ekran rotate/recreate sonrası geçerli aktif app'i koruyor veya güvenli şekilde ilk app'e dönüyor.
- [ ] Repository hatasında loading state takılı kalmıyor.
- [ ] Küçük telefonda taşma yok.
- [ ] Tablet görünümü bozulmuyor.
- [ ] Açık/koyu tema okunabilir.
- [ ] Pixel görünümü bozulmuyor.
- [ ] Unit testler geçiyor.
- [ ] Lint ve detekt kritik hata vermiyor.
- [ ] Debug APK build oluyor.

---

# 25. Riskler ve Önlemler

## Risk 1 — Review işlemi tamamlanmadan sıradaki app'e geçme

**Önlem:**

- `processingPackageName` kullan.
- State'i yalnız repository işlemi tamamlandıktan sonra temizle.

## Risk 2 — Mevcut public ViewModel fonksiyonları fire-and-forget

**Önlem:**

- Yeni wrapper içinde repository çağrılarını tek coroutine içinde yap.
- Alternatif olarak mevcut fonksiyonları `suspend private` çekirdeğe ayır.

Önerilen refactor:

```kotlin
private suspend fun confirmPendingClassificationInternal(packageName: String)
private suspend fun correctPendingClassificationInternal(packageName: String, categoryId: String)
private suspend fun skipPendingClassificationInternal(packageName: String)
```

Public eski fonksiyonlar geriye uyumluluk için bu internal fonksiyonları `viewModelScope.launch` içinde çağırabilir.

## Risk 3 — Enum/string eşleşmesi

**Önlem:**

- `classificationReason` veritabanında String.
- Gerçek enum `name` değerleri koddan doğrulanmalı.
- Tahmini string yazılmamalı.

## Risk 4 — Uygulama ikonu performansı

**Önlem:**

- PackageManager çağrısını cache et.
- Her recomposition'da drawable yükleme.

## Risk 5 — Türkçe arama

`contains(ignoreCase = true)` bazı `I/İ` durumlarında tam kullanıcı beklentisini karşılamayabilir.

**Faz 2 çözümü:**

- query ve kategori adını `Locale("tr", "TR")` ile normalize et.
- Gerekirse diakritik toleranslı arama helper'ı yaz.

---

# 26. Commit Planı

Repo kuralına göre tüm değişiklikler `main` üzerinde yapılacak.

Önerilen commit sırası:

1. `Türkçe kategori sıralama ve gruplama altyapısı`
2. `Kontrol Bekleyenler ViewModel state akışı`
3. `Alfabetik kategori seçim bottom sheet`
4. `Kontrol Bekleyenler A tasarım refactor`
5. `Kategori review testleri ve erişilebilirlik düzeltmeleri`

Her commit sonrası:

- ilgili test çalıştır,
- build sonucu kaydet,
- HISTORY.md güncelle,
- yalnız ilgili dosyaları commit et.

---

# 27. Yapılmayacaklar

Bu roadmap kapsamında aşağıdakiler yapılmayacak:

- [ ] Kategori veritabanını tamamen değiştirme
- [ ] Mevcut classification engine'i yeniden yazma
- [ ] Otomatik sınıflandırma eşiklerini değiştirme
- [ ] Yeni LLM sağlayıcısı ekleme
- [ ] Toplu kategori onayını ana akışa ekleme
- [ ] Kullanıcı özel kategori yönetimini yeniden tasarlama
- [ ] Yeni onboarding adımı ekleme
- [ ] Ana ekran klasör düzenini değiştirme

Toplu işlem tasarımı daha sonra ayrı Faz 2 özelliği olabilir. İlk sürümün amacı karar doğruluğu ve sadeliktir.

---

# 28. Nihai Teknik Karar Özeti

1. `ClassificationReviewScreen.kt` korunacak ancak içeriği refactor edilecek.
2. İç içe `LazyColumn` ve `560.dp` sabit yükseklik kaldırılacak.
3. Yatay kategori `LazyRow` tamamen kaldırılacak.
4. Kategori seçimi `ModalBottomSheet` üzerinden yapılacak.
5. `displayOrder` yerine Türkçe `Collator` kullanılacak.
6. Marka klasörleri ayrı section'a alınacak.
7. `CAT_UNCATEGORIZED` seçilebilir listeden çıkarılacak.
8. UI seçim state'i `AppListViewModel` içine taşınacak.
9. Aktif uygulama + kuyruk modeli uygulanacak.
10. Mevcut `ClassificationAttentionPolicy` ve `CategorySuggestionEngine` korunacak.
11. İlk sürümde DB migration yapılmayacak.
12. Tüm işlem durumları test edilecek.

---

# 29. Definition of Done

**Kategori Roadmap tamamlandı** denebilmesi için:

- A tasarımı kodda birebir uygulanmış,
- tüm kabul kriterleri geçilmiş,
- testler başarılı,
- debug APK oluşturulmuş,
- telefon ve tablet smoke testi yapılmış,
- HISTORY.md güncellenmiş,
- eski yatay kategori kodu tamamen kaldırılmış,
- kullanıcı tek uygulama üzerinden hızlı ve açık karar verebiliyor olmalıdır.
