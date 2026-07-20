# KLASÖR BİRLEŞTİRME ROADMAP — A TASARIMI

> **Tarih:** 2026-07-21  
> **Repo:** `hekizoglu/android-folderautomanager`  
> **Nihai karar:** **A — Akıllı Birleştirme Önizlemesi**  
> **Durum:** Onaylandı — uygulanmaya hazır teknik yol haritası  
> **Zorluk:** 8/10 — Domain, Room transaction, geri alma, ViewModel ve Compose UI refactor  
> **Temel ilke:** Kullanıcı görmeden hiçbir klasör birleşmeyecek; hiçbir uygulama silinmeyecek; işlem geri alınabilir olacak.

---

# 1. Amaç

Mevcut `Klasör Önerileri` ekranındaki basit “Kabul et” akışını güvenli, açıklanabilir ve kullanıcı kontrollü klasör birleştirme sistemine dönüştür.

1. Kaynak klasör ve hedef klasör aynı ekranda gösterilecek.
2. Taşınacak uygulamalar ikonlarıyla görülecek.
3. Kullanıcı taşınacak uygulamaları seçebilecek.
4. Kullanıcı önerilen hedef klasörü değiştirebilecek.
5. Birleştirme öncesi ve sonrası uygulama sayıları gösterilecek.
6. Sistem birleşme nedenlerini açıklayacak.
7. Manuel kategori kilidi bulunan uygulamalar açıkça işaretlenecek.
8. Birleştirme tek Room transaction içinde yapılacak.
9. İşlem geçmişi saklanacak ve gerçek geri alma sağlanacak.
10. Birleştirme sonrası search index, manuel override ve görev puanı birlikte güncellenecek.
11. Telefon, küçük ekran, tablet, büyük yazı ve karanlık tema desteklenecek.
12. `SPLIT_LARGE_FOLDER` ve `CLEAN_UNUSED_APPS` davranışları bozulmayacak.

---

# 2. Mevcut Sistem ve Sorunlar

## 2.1. Mevcut öneri motoru

Dosya:

`app/src/main/java/com/armutlu/apporganizer/domain/usecase/folder/FolderSuggestionEngine.kt`

Mevcut sistem:

1. Klasörde 1–2 uygulama varsa küçük klasör kabul ediyor.
2. Birleştirme hedefini sabit `mergeTargets` tablosundan seçiyor.
3. Kaynak klasördeki tüm uygulamaları hedef kategoriye taşımayı öneriyor.
4. Kullanıcı hedef kategoriyi değiştiremiyor.
5. Kullanıcı taşınacak uygulamaları seçemiyor.
6. Önce/sonra klasör görünümü üretmiyor.
7. Öneri nedeni yalnız başlık ve açıklama içine gömülü.
8. Güven yüzdesinin hangi sinyallerden oluştuğu bilinmiyor.

Mevcut sabit eşleşmeler:

- Video → Eğlence
- Müzik → Eğlence
- Arkadaşlık → Sosyal Medya
- Harita & Navigasyon → Seyahat
- Ev & Yaşam → Yaşam Tarzı
- Güzellik → Yaşam Tarzı
- Etkinlikler → Yaşam Tarzı
- Çizgi Roman → Kitap & Referans

Bu tablo ilk güvenilir sinyal olarak korunacak ancak tek karar kaynağı olmayacak.

## 2.2. Mevcut ekran

Dosya:

`app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/FolderSuggestionsScreen.kt`

Mevcut ekran yalnızca şunları gösteriyor:

- öneri başlığı
- kısa açıklama
- hedef klasör
- güven yüzdesi
- Kabul et
- 7 gün ertele
- Gizle

Eksikler:

- kaynak klasör görünmüyor
- hedef klasör görseli yok
- uygulama ikonları yok
- hangi uygulamaların taşınacağı görünmüyor
- hedef klasör değiştirilemiyor
- kısmi seçim yapılamıyor
- işlem sonrası klasör boyutu bilinmiyor
- geri alma görünür değil
- önerinin nedenleri ayrıştırılmıyor
- nested `LazyColumn(height = 560.dp)` küçük ekran ve büyük yazı için kırılgan

## 2.3. Mevcut kabul davranışı

Dosya:

`app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt`

Mevcut `acceptFolderSuggestion()`:

1. Önerideki tüm paketleri hedef kategoriye taşır.
2. Her paket için manuel kategori override yazar.
3. Search index günceller.
4. Öneriyi dismissed yapar.
5. Görev puanı yazar.
6. Özel birleşme geçmişi oluşturmaz.
7. Tek işlem kimliği üretmez.
8. Kalıcı geri alma sağlamaz.

---

# 3. Sabitlenen Ürün Kararları

1. **A — Akıllı Birleştirme Önizlemesi uygulanacak.**
2. Birleştirme kartındaki ana eylem `Kabul et` değil `İncele` olacak.
3. Hiçbir birleşme otomatik veya sessiz uygulanmayacak.
4. Kullanıcı uygulamaları görmeden işlem yapamayacak.
5. Kullanıcı hedef klasörü değiştirebilecek.
6. Kullanıcı taşınacak uygulamaları seçebilecek.
7. Manuel kilitli uygulamalar varsayılan seçim dışında tutulacak.
8. İlk sürümde küçük klasör eşiği 1–2 uygulama olarak korunacak.
9. Birleşme sonrası hedef klasör 20 uygulamayı geçerse uyarı gösterilecek.
10. Sistem kategorisi boş kalırsa veri tabanından silinmeyecek; görünür klasör listesinden düşecek.
11. Kullanıcı tarafından oluşturulan özel klasör boş kalırsa silme için ayrıca karar istenecek.
12. Yalnız Snackbar süresine bağlı geçici undo kullanılmayacak; Room tabanlı işlem geçmişi yazılacak.
13. Kaynak ve hedef aynı kategori olamayacak.
14. `Kategorisiz` hedef kategori olamayacak.
15. Gizli uygulamalar birleştirme planına alınmayacak.
16. Sistem uygulamaları varsayılan olarak birleştirmeye dahil edilmeyecek.
17. Birleştirme sırasında uygulama silme veya gizleme yapılmayacak.

---

# 4. Nihai Kullanıcı Akışı

## 4.1. Klasör Önerileri ana ekranı

1. Üstte toplam öneri sayısını göster.
2. Filtreler ekle:
   - Tümü
   - Birleştirme
   - Bölme
   - Temizlik
3. En yüksek öncelikli öneriyi büyük kart olarak göster.
4. Birleştirme kartında kaynak ve hedef klasör yan yana göster.
5. Kaynak ve hedef uygulama sayılarını göster.
6. Taşınacak uygulamaların en fazla 6 ikonunu göster.
7. Fazla uygulama varsa `+N` rozeti kullan.
8. Kart üzerinde `İncele` ve `7 gün ertele` eylemleri göster.
9. Altta sıradaki önerileri kompakt kartlarla göster.

## 4.2. Birleştirme inceleme ekranı

Yeni ekran:

`FolderMergeReviewScreen`

Ekran sırası:

1. Başlık: `Klasörleri Birleştir`
2. Güven rozeti
3. Öneri özeti
4. Kaynak klasör kartı
5. Birleşme yön oku
6. Hedef klasör kartı
7. Taşınacak uygulamalar bölümü
8. `Uygulama seçimini düzenle`
9. `Hedef klasörü değiştir`
10. `Neden bu hedef?` bölümü
11. Önce / sonra uygulama sayıları
12. Geri alınabilir bilgisi
13. Ana buton: `%1$d uygulamayı %2$s ile birleştir`
14. İkincil buton: `7 gün ertele`
15. Metin butonu: `Bu öneriyi gösterme`

## 4.3. Onay akışı

Birleştirme butonuna basılınca:

1. `isApplying=true` yap.
2. Butonu loading durumuna al.
3. İkinci tıklamayı engelle.
4. Planı yeniden doğrula.
5. İşlem snapshot’ını oluştur.
6. Room transaction başlat.
7. Operation kaydını yaz.
8. Operation item kayıtlarını yaz.
9. Seçilen uygulamaları hedef kategoriye taşı.
10. Manuel override kayıtlarını güncelle.
11. Search index’i güncelle.
12. Öneriyi dismissed yap.
13. TaskScore kaydı oluştur.
14. Telemetry event’i gönder.
15. Başarı Snackbar göster:
    - `Müzik, Eğlence ile birleştirildi`
    - `GERİ AL`
16. Sonraki öneriyi otomatik aç.

## 4.4. Geri alma akışı

1. Kullanıcı `GERİ AL` seçer.
2. Operation kaydı bulunur.
3. Operation item’lar okunur.
4. Her uygulama önceki kategorisine döner.
5. Manuel override eski kategoriye göre güncellenir.
6. Search index yeniden oluşturulur.
7. Operation `undoneAt` ile işaretlenir.
8. Aynı operation ikinci kez geri alınamaz.
9. Başarı mesajı gösterilir.

---

# 5. Domain Model Değişiklikleri

## 5.1. `FolderSuggestion` modelini genişlet

Dosya:

`domain/usecase/folder/FolderSuggestionEngine.kt`

Yeni model:

```kotlin
data class FolderSuggestion(
    val id: String,
    val type: FolderSuggestionType,
    val title: String,
    val description: String,
    val sourceCategoryId: String?,
    val targetCategoryId: String,
    val packageNames: List<String>,
    val confidence: Int,
    val reasonCodes: List<FolderSuggestionReason>,
    val sourceAppCount: Int,
    val targetAppCount: Int,
)
```

Kurallar:

1. `sourceCategoryId`, merge önerisinde zorunlu olacak.
2. Split ve cleanup önerilerinde nullable kalabilir.
3. `packageNames` yalnız varsayılan taşınabilir uygulamaları içerecek.
4. `sourceAppCount` ve `targetAppCount` UI hesaplamasını kolaylaştıracak.
5. Confidence 0–100 aralığına zorlanacak.

## 5.2. Yeni reason enum

Yeni dosya:

`domain/usecase/folder/FolderSuggestionReason.kt`

```kotlin
enum class FolderSuggestionReason {
    SMALL_SOURCE_FOLDER,
    SEMANTIC_CATEGORY_MATCH,
    TARGET_FOLDER_EXISTS,
    SIMILAR_USAGE_PATTERN,
    USER_CORRECTION_PATTERN,
    FIXED_RULE_MATCH,
    TARGET_SIZE_BALANCED,
}
```

UI mapping örneği:

```kotlin
fun FolderSuggestionReason.toStringRes(): Int = when (this) {
    SMALL_SOURCE_FOLDER -> R.string.folder_merge_reason_small_folder
    SEMANTIC_CATEGORY_MATCH -> R.string.folder_merge_reason_semantic_match
    TARGET_FOLDER_EXISTS -> R.string.folder_merge_reason_target_exists
    SIMILAR_USAGE_PATTERN -> R.string.folder_merge_reason_usage_pattern
    USER_CORRECTION_PATTERN -> R.string.folder_merge_reason_user_pattern
    FIXED_RULE_MATCH -> R.string.folder_merge_reason_fixed_rule
    TARGET_SIZE_BALANCED -> R.string.folder_merge_reason_balanced_target
}
```

## 5.3. Yeni merge plan modeli

Yeni dosya:

`domain/usecase/folder/FolderMergePlan.kt`

```kotlin
data class FolderMergePlan(
    val suggestionId: String,
    val sourceCategoryId: String,
    val targetCategoryId: String,
    val selectedPackageNames: Set<String>,
    val sourcePackageNames: Set<String>,
    val sourceAppCountBefore: Int,
    val targetAppCountBefore: Int,
    val confidence: Int,
    val reasonCodes: List<FolderSuggestionReason>,
) {
    val sourceAppCountAfter: Int
        get() = sourceAppCountBefore - selectedPackageNames.size

    val targetAppCountAfter: Int
        get() = targetAppCountBefore + selectedPackageNames.size

    val isSourceEmptyAfter: Boolean
        get() = sourceAppCountAfter == 0

    val isTargetCrowdedAfter: Boolean
        get() = targetAppCountAfter > 20

    val isValid: Boolean
        get() = sourceCategoryId != targetCategoryId &&
            selectedPackageNames.isNotEmpty() &&
            selectedPackageNames.all { it in sourcePackageNames }
}
```

---

# 6. Öneri Motoru Refactor

Dosya:

`domain/usecase/folder/FolderSuggestionEngine.kt`

## 6.1. Mevcut sabit eşleştirmeyi koru

`mergeTargets` ilk sinyal olarak kalacak.

Ancak hedef seçimi yeni scorer üzerinden yapılacak.

## 6.2. Yeni aday modeli

```kotlin
data class MergeCandidate(
    val sourceCategoryId: String,
    val targetCategoryId: String,
    val score: Int,
    val reasons: List<FolderSuggestionReason>,
)
```

## 6.3. Yeni scorer

Yeni dosya:

`domain/usecase/folder/FolderMergeCandidateScorer.kt`

```kotlin
object FolderMergeCandidateScorer {
    fun score(
        sourceCategoryId: String,
        targetCategoryId: String,
        sourceApps: List<AppInfo>,
        targetApps: List<AppInfo>,
        fixedTargetCategoryId: String?,
        userPatternMatch: Boolean,
    ): MergeCandidate
}
```

Önerilen puanlama:

- sabit semantik eşleşme: +40
- kaynak klasör 1–2 uygulama: +15
- hedef klasör mevcut: +10
- hedef klasör 3–15 uygulama: +10
- uygulama/paket adı sinyali: +10
- kullanıcı düzeltme örüntüsü: +10
- benzer kullanım zamanı: +5

Toplam 100.

Eşikler:

- 80–100: Güçlü öneri
- 65–79: İncelemeye değer
- 50–64: Düşük öncelik
- 0–49: Gösterme

## 6.4. Aday üretimi

```kotlin
private fun findMergeCandidates(
    sourceCategoryId: String,
    sourceApps: List<AppInfo>,
    groupedApps: Map<String, List<AppInfo>>,
    categories: List<Category>,
): List<MergeCandidate>
```

Filtreler:

```kotlin
if (sourceApps.isEmpty()) return emptyList()
if (sourceApps.size > SMALL_FOLDER_THRESHOLD) return emptyList()
if (targetCategoryId == sourceCategoryId) return null
if (targetCategoryId == Category.CAT_UNCATEGORIZED) return null
if (targetApps.isEmpty()) return null
```

## 6.5. Kilitli uygulamalar

```kotlin
val defaultSelected = sourceApps
    .filterNot { it.isCategoryLocked }
    .map { it.packageName }
    .toSet()
```

Kurallar:

1. Kilitli uygulama varsayılan seçili gelmez.
2. Tüm kaynak uygulamalar kilitliyse öneri düşük öncelikli olur veya gösterilmez.
3. Kullanıcı kilitli uygulamayı seçerse uyarı gösterilir.
4. Kullanıcı onaylarsa önceki manuel kararın değişeceği açıkça belirtilir.

## 6.6. Sıralama

Öneriler:

1. confidence yüksekten düşüğe
2. merge önerileri önce
3. başlık Türkçe alfabetik
4. maksimum 12 öneri

---

# 7. UI State

Yeni dosya:

`presentation/ui/screens/foldermerge/FolderMergeUiState.kt`

```kotlin
data class FolderMergeUiState(
    val activeSuggestionId: String? = null,
    val activePlan: FolderMergePlan? = null,
    val selectedPackageNames: Set<String> = emptySet(),
    val targetCategoryId: String? = null,
    val isTargetPickerOpen: Boolean = false,
    val isAppPickerOpen: Boolean = false,
    val isApplying: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val lastUndoOperationId: String? = null,
)
```

State kuralları:

1. UI içinde bağımsız `remember` state kullanılmayacak.
2. Seçilen uygulamalar ViewModel’de tutulacak.
3. Hedef kategori ViewModel’de tutulacak.
4. Process death sonrasında ekran güvenli varsayılan state’e dönecek.
5. İşlem devam ederken hedef veya seçim değiştirilemeyecek.

---

# 8. ViewModel Değişiklikleri

Dosya:

`presentation/viewmodel/AppListViewModel.kt`

## 8.1. Eklenecek state

```kotlin
private val _folderMergeUiState = MutableStateFlow(FolderMergeUiState())
val folderMergeUiState: StateFlow<FolderMergeUiState> =
    _folderMergeUiState.asStateFlow()
```

## 8.2. Eklenecek fonksiyonlar

```kotlin
fun openMergeSuggestion(suggestionId: String)
fun closeMergeSuggestion()
fun toggleMergeApp(packageName: String)
fun selectAllMergeApps()
fun clearMergeApps()
fun openMergeTargetPicker()
fun closeMergeTargetPicker()
fun selectMergeTarget(categoryId: String)
fun applyFolderMerge()
fun undoLastFolderMerge()
fun snoozeActiveMerge()
fun dismissActiveMerge()
fun clearFolderMergeError()
```

## 8.3. `openMergeSuggestion()`

1. `folderSuggestions` içinde öneriyi bul.
2. Türün `MERGE_SMALL_FOLDER` olduğunu doğrula.
3. Kaynak ve hedef kategoriyi doğrula.
4. Kaynak uygulamaları yükle.
5. Gizli ve sistem uygulamalarını filtrele.
6. Kilitli olmayanları varsayılan seç.
7. `FolderMergePlan` oluştur.
8. State’i güncelle.
9. Telemetry `folder_merge_suggestion_opened` gönder.

## 8.4. `selectMergeTarget()`

1. Kaynak kategorinin seçilmesini engelle.
2. `Kategorisiz` seçimini engelle.
3. Hedef kategori mevcut mu kontrol et.
4. Hedef uygulama sayısını yeniden hesapla.
5. Planı `copy(targetCategoryId=...)` ile güncelle.
6. Target picker’ı kapat.
7. `targetChanged=true` telemetry parametresi tut.

## 8.5. `toggleMergeApp()`

1. Uygulama kaynak klasörde mi kontrol et.
2. İşlem devam ediyorsa değişiklik yapma.
3. Seçiliyse kaldır, değilse ekle.
4. Kilitli uygulama ekleniyorsa UI uyarı state’i üret.
5. Önce/sonra sayılarını yeniden hesapla.

## 8.6. `applyFolderMerge()`

```kotlin
fun applyFolderMerge() {
    val state = _folderMergeUiState.value
    val plan = state.activePlan ?: return
    if (!plan.isValid || state.isApplying) return

    viewModelScope.launch {
        _folderMergeUiState.update {
            it.copy(isApplying = true, errorMessage = null)
        }

        runCatching {
            repository.mergeFolders(plan)
        }.onSuccess { operationId ->
            plan.selectedPackageNames.forEach { packageName ->
                AppPrefs.setManualCategoryOverride(
                    getApplication(),
                    packageName,
                    plan.targetCategoryId,
                )
                repository.getAppByPackageName(packageName)?.let {
                    searchRepository.indexApp(it)
                }
            }

            AppPrefs.dismissFolderSuggestion(
                getApplication(),
                plan.suggestionId,
            )

            TaskScoreManager.recordBulk(
                context = getApplication(),
                eventType = TaskScoreManager.EventType.FolderSuggestionAccepted,
                itemCount = plan.selectedPackageNames.size,
            )

            _folderMergeUiState.update {
                it.copy(
                    isApplying = false,
                    activeSuggestionId = null,
                    activePlan = null,
                    lastUndoOperationId = operationId,
                    successMessage = "Klasörler birleştirildi",
                )
            }
            _folderSuggestionRefresh.value += 1
        }.onFailure { error ->
            _folderMergeUiState.update {
                it.copy(
                    isApplying = false,
                    errorMessage = error.message
                        ?: "Klasörler birleştirilemedi",
                )
            }
        }
    }
}
```

## 8.7. Eski fonksiyonun geçişi

Mevcut `acceptFolderSuggestion()` hemen silinmeyecek.

Geçiş kuralı:

```kotlin
fun acceptFolderSuggestion(suggestionId: String) {
    val suggestion = folderSuggestions.value
        .firstOrNull { it.id == suggestionId }
        ?: return

    if (suggestion.type == FolderSuggestionType.MERGE_SMALL_FOLDER) {
        openMergeSuggestion(suggestionId)
        return
    }

    // Split ve cleanup için mevcut davranış geçici olarak korunur.
    applyLegacyFolderSuggestion(suggestion)
}
```

---

# 9. Room İşlem Geçmişi ve Transaction

## 9.1. Yeni operation entity

Yeni dosya:

`data/local/FolderOperationEntity.kt`

```kotlin
@Entity(tableName = "folder_operations")
data class FolderOperationEntity(
    @PrimaryKey val operationId: String,
    val type: String,
    val sourceCategoryId: String,
    val targetCategoryId: String,
    val createdAt: Long,
    val undoneAt: Long? = null,
)
```

## 9.2. Yeni operation item entity

Yeni dosya:

`data/local/FolderOperationItemEntity.kt`

```kotlin
@Entity(
    tableName = "folder_operation_items",
    primaryKeys = ["operationId", "packageName"],
    indices = [Index("operationId"), Index("packageName")],
)
data class FolderOperationItemEntity(
    val operationId: String,
    val packageName: String,
    val previousCategoryId: String,
    val newCategoryId: String,
)
```

## 9.3. Yeni DAO

Yeni dosya:

`data/local/FolderOperationDao.kt`

Gerekli fonksiyonlar:

```kotlin
@Insert
suspend fun insertOperation(operation: FolderOperationEntity)

@Insert
suspend fun insertItems(items: List<FolderOperationItemEntity>)

@Query("SELECT * FROM folder_operations WHERE operationId = :id")
suspend fun getOperation(id: String): FolderOperationEntity?

@Query("SELECT * FROM folder_operation_items WHERE operationId = :id")
suspend fun getOperationItems(id: String): List<FolderOperationItemEntity>

@Query("UPDATE folder_operations SET undoneAt = :undoneAt WHERE operationId = :id")
suspend fun markUndone(id: String, undoneAt: Long)
```

## 9.4. Transaction katmanı

Önerilen yeni sınıf:

`data/local/FolderOperationTransaction.kt`

veya repository içinde `AppDatabase.withTransaction` kullan.

```kotlin
suspend fun mergeFolders(plan: FolderMergePlan): String =
    database.withTransaction {
        val operationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val apps = plan.selectedPackageNames.mapNotNull {
            appDao.getAppByPackageName(it)
        }

        require(apps.size == plan.selectedPackageNames.size)

        folderOperationDao.insertOperation(
            FolderOperationEntity(
                operationId = operationId,
                type = "MERGE",
                sourceCategoryId = plan.sourceCategoryId,
                targetCategoryId = plan.targetCategoryId,
                createdAt = now,
            )
        )

        folderOperationDao.insertItems(
            apps.map { app ->
                FolderOperationItemEntity(
                    operationId = operationId,
                    packageName = app.packageName,
                    previousCategoryId = app.categoryId,
                    newCategoryId = plan.targetCategoryId,
                )
            }
        )

        appDao.updateAppsCategoryWithClassification(
            packageNames = plan.selectedPackageNames.toList(),
            categoryId = plan.targetCategoryId,
            source = ClassificationSource.USER_CORRECTED.name,
            confidence = 100,
            reason = ClassificationReason.USER_SELECTION.name,
            reviewState = ClassificationReviewState.CORRECTED.name,
            locked = true,
            version = CLASSIFICATION_ENGINE_VERSION,
            classifiedAt = now,
            reviewedAt = now,
            snoozedUntil = 0L,
        )

        operationId
    }
```

## 9.5. Undo transaction

```kotlin
suspend fun undoFolderMerge(operationId: String) =
    database.withTransaction {
        val operation = folderOperationDao.getOperation(operationId)
            ?: error("Operation bulunamadı")

        check(operation.undoneAt == null) {
            "Operation daha önce geri alınmış"
        }

        val items = folderOperationDao.getOperationItems(operationId)
        require(items.isNotEmpty())

        items.groupBy { it.previousCategoryId }
            .forEach { (categoryId, categoryItems) ->
                appDao.updateAppsCategoryWithClassification(
                    packageNames = categoryItems.map { it.packageName },
                    categoryId = categoryId,
                    source = ClassificationSource.USER_CORRECTED.name,
                    confidence = 100,
                    reason = ClassificationReason.USER_SELECTION.name,
                    reviewState = ClassificationReviewState.CORRECTED.name,
                    locked = true,
                    version = CLASSIFICATION_ENGINE_VERSION,
                    classifiedAt = System.currentTimeMillis(),
                    reviewedAt = System.currentTimeMillis(),
                    snoozedUntil = 0L,
                )
            }

        folderOperationDao.markUndone(
            operationId,
            System.currentTimeMillis(),
        )
    }
```

## 9.6. Database migration

`AppDatabase.kt`:

1. Version bir artırılacak.
2. Yeni entity’ler `entities` listesine eklenecek.
3. `folderOperationDao()` eklenecek.
4. Migration SQL yazılacak.
5. Schema JSON commit edilecek.
6. Migration test edilecek.

Örnek SQL:

```sql
CREATE TABLE IF NOT EXISTS folder_operations (
    operationId TEXT NOT NULL PRIMARY KEY,
    type TEXT NOT NULL,
    sourceCategoryId TEXT NOT NULL,
    targetCategoryId TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    undoneAt INTEGER
)
```

```sql
CREATE TABLE IF NOT EXISTS folder_operation_items (
    operationId TEXT NOT NULL,
    packageName TEXT NOT NULL,
    previousCategoryId TEXT NOT NULL,
    newCategoryId TEXT NOT NULL,
    PRIMARY KEY(operationId, packageName)
)
```

---

# 10. Repository Değişiklikleri

Dosya:

`data/repository/AppRepository.kt`

Eklenecek fonksiyonlar:

```kotlin
suspend fun mergeFolders(plan: FolderMergePlan): String
suspend fun undoFolderMerge(operationId: String)
suspend fun getFolderOperation(operationId: String): FolderOperationEntity?
```

Kurallar:

1. Repository planı yeniden doğrulayacak.
2. Seçilen paketlerin kaynak kategoride olduğunu kontrol edecek.
3. Hedef kategori mevcut mu kontrol edecek.
4. Transaction dışında kısmi veri yazmayacak.
5. Hata yutulmayacak; ViewModel’e iletilecek.
6. `updateAppsCategory()` doğrudan merge işleminin yerine kullanılmayacak.
7. İşlem kimliği başarıyla tamamlanmadan döndürülmeyecek.

---

# 11. Yeni UI Dosyaları

Yeni klasör:

`presentation/ui/screens/foldermerge/`

Oluşturulacak dosyalar:

1. `FolderMergeUiState.kt`
2. `FolderMergeReviewScreen.kt`
3. `FolderMergePreviewCard.kt`
4. `FolderMergeReasonCard.kt`
5. `FolderMergeAppGrid.kt`
6. `FolderMergeAppPickerBottomSheet.kt`
7. `FolderMergeTargetPickerBottomSheet.kt`
8. `FolderMergeBeforeAfterCard.kt`
9. `FolderMergeLockedAppWarningDialog.kt`

## 11.1. `FolderMergeReviewScreen`

Parametreler:

```kotlin
@Composable
fun FolderMergeReviewScreen(
    viewModel: AppListViewModel,
    onNavigateBack: () -> Unit,
)
```

Ekran state’i:

```kotlin
val mergeState by viewModel.folderMergeUiState.collectAsState()
val screenState by viewModel.screenState.collectAsState()
```

Ekran yapısı:

```kotlin
Scaffold(
    topBar = { FolderMergeTopBar(...) },
    snackbarHost = { SnackbarHost(snackbarHostState) },
) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { FolderMergeSummaryCard(...) }
        item { FolderMergePreviewCard(...) }
        item { FolderMergeAppGrid(...) }
        item { FolderMergeTargetSelector(...) }
        item { FolderMergeReasonCard(...) }
        item { FolderMergeBeforeAfterCard(...) }
        item { FolderMergeActionSection(...) }
    }
}
```

Nested sabit yükseklikli `LazyColumn` kullanılmayacak.

## 11.2. Kaynak ve hedef önizleme

`FolderMergePreviewCard`:

1. Kaynak klasör solda.
2. Hedef klasör sağda.
3. Ortada yön oku.
4. Her kartta emoji, isim, uygulama sayısı.
5. Hedef primary container ile vurgulanır.
6. Kaynak surface variant kullanır.
7. Altında `Sonra: 14 uygulama` rozeti gösterilir.
8. Kaynak boş kalacaksa bilgi metni gösterilir.

## 11.3. Uygulama grid’i

`FolderMergeAppGrid`:

1. En fazla 6 uygulama ikonu göster.
2. Her uygulamada seçili durum görünür olsun.
3. Kilitli uygulamaya küçük kilit rozeti ekle.
4. `N / toplam seçili` göster.
5. `Uygulama seçimini düzenle` butonu ekle.
6. Uygulama simgesi yüklenemezse harfli placeholder göster.

## 11.4. Önce / sonra kartı

Örnek:

```text
ÖNCE
Müzik       2
Eğlence    12

SONRA
Müzik       0
Eğlence    14
```

Kurallar:

1. Sayılar state’ten hesaplanacak.
2. Kaynakta seçilmeyen uygulama kalıyorsa doğru sayı gösterilecek.
3. Hedef 20’yi geçiyorsa uyarı rozeti çıkacak.
4. `Geri alınabilir` bilgisi gösterilecek.

---

# 12. `FolderSuggestionsScreen.kt` Refactor

Mevcut `LazyColumn(height = 560.dp)` kaldırılacak.

Yeni yapı:

```kotlin
SettingsSubScreenScaffold(...) {
    item { FolderSuggestionSummaryCard(...) }
    item { FolderSuggestionFilterRow(...) }

    activeSuggestion?.let { suggestion ->
        item {
            FeaturedFolderSuggestionCard(
                suggestion = suggestion,
                categoryNames = categoryNames,
                onReview = {
                    if (suggestion.type == FolderSuggestionType.MERGE_SMALL_FOLDER) {
                        viewModel.openMergeSuggestion(suggestion.id)
                    } else {
                        viewModel.acceptFolderSuggestion(suggestion.id)
                    }
                },
                onSnooze = {
                    viewModel.snoozeFolderSuggestion(suggestion.id)
                },
            )
        }
    }

    if (queuedSuggestions.isNotEmpty()) {
        item {
            FolderSuggestionQueue(
                suggestions = queuedSuggestions,
                onSelect = viewModel::openMergeSuggestion,
            )
        }
    }
}
```

Birleştirme önerisi buton metni:

- Eski: `Kabul et`
- Yeni: `İncele`

Split ve cleanup için mevcut kabul akışı ayrı tutulacak.

---

# 13. Hedef Klasör Seçici

Yeni dosya:

`FolderMergeTargetPickerBottomSheet.kt`

`ModalBottomSheet` kullanılacak.

Bölümler:

1. Önerilen hedef
2. Benzer klasörler
3. Tüm klasörler — Türkçe A–Z

Kategori satırı:

```text
🎬 Eğlence
12 uygulama → birleşince 14
Önerilen hedef
```

Kurallar:

1. Kaynak kategori disabled olacak.
2. `Kategorisiz` gösterilmeyecek.
3. Mevcut hedef en üstte seçili olacak.
4. Hedef uygulama sayısı gösterilecek.
5. Birleşme sonrası sayı gösterilecek.
6. 20+ hedeflerde `Kalabalıklaşabilir` uyarısı gösterilecek.
7. Türkçe alfabetik sıralama için `Collator(Locale("tr", "TR"))` kullanılacak.
8. Arama kutusu eklenecek.

---

# 14. Uygulama Seçici

Yeni dosya:

`FolderMergeAppPickerBottomSheet.kt`

İçerik:

1. uygulama ikonu
2. uygulama adı
3. paket adı
4. mevcut kilit durumu
5. checkbox
6. tümünü seç
7. seçimi temizle
8. seçilen uygulama sayısı

Kilitli uygulama görünümü:

```text
Spotify
Manuel kategori kararı mevcut
[ ] Taşı
```

Kilitli uygulama seçilirse dialog:

```text
Bu uygulamanın daha önce verdiğiniz manuel kategori kararı değiştirilecek.

[İptal] [Yine de taşı]
```

En az bir uygulama seçilmeden onay butonu aktif olmayacak.

---

# 15. Navigasyon

Dosya:

`presentation/navigation/AppNavigation.kt`

Yeni route:

```kotlin
const val FOLDER_MERGE_REVIEW = "folder_merge_review"
```

Öneri ID route argümanı olarak verilebilir:

```kotlin
"folder_merge_review/{suggestionId}"
```

Ancak öneri state’i aynı ViewModel’de tutuluyorsa yalnız route kullanımı da yeterlidir.

Tercih:

1. `openMergeSuggestion(id)` state’i hazırlar.
2. Ardından review route’una geçilir.
3. Ekran doğrudan açılırsa aktif plan yoksa geri döner veya güvenli empty state gösterir.

---

# 16. String Kaynakları

Yeni dosya:

`app/src/main/res/values/folder_merge_strings.xml`

```xml
<string name="folder_merge_title">Klasörleri Birleştir</string>
<string name="folder_merge_source">Kaynak klasör</string>
<string name="folder_merge_target">Hedef klasör</string>
<string name="folder_merge_why">Neden öneriliyor?</string>
<string name="folder_merge_edit_apps">Taşınacak uygulamaları düzenle</string>
<string name="folder_merge_change_target">Hedef klasörü değiştir</string>
<string name="folder_merge_before">Önce</string>
<string name="folder_merge_after">Sonra</string>
<string name="folder_merge_confirm">%1$d uygulamayı %2$s ile birleştir</string>
<string name="folder_merge_success">%1$s, %2$s ile birleştirildi</string>
<string name="folder_merge_undo">Geri al</string>
<string name="folder_merge_empty_source_info">%1$s klasörü boş kalacağı için ana ekrandan gizlenecek.</string>
<string name="folder_merge_locked_app">Manuel kategori kararı mevcut</string>
<string name="folder_merge_no_apps_selected">Birleştirmek için en az bir uygulama seç.</string>
<string name="folder_merge_target_crowded">Bu klasör birleşme sonrası kalabalıklaşabilir.</string>
<string name="folder_merge_review_action">İncele</string>
<string name="folder_merge_snooze">7 gün ertele</string>
<string name="folder_merge_dismiss">Bu öneriyi gösterme</string>
<string name="folder_merge_reversible">Geri alınabilir</string>
```

İngilizce karşılıkları `values-en/folder_merge_strings.xml` içine eklenecek.

Kod içine sabit Türkçe metin yazılmayacak.

---

# 17. Görsel Tasarım Kuralları

1. Kaynak ve hedef klasör aynı görsel ağırlıkta başlamalı.
2. Birleşme yönü merkezde ok ile gösterilmeli.
3. Hedef kart primary container ile vurgulanmalı.
4. Kaynak kart surface variant kullanılmalı.
5. Güven oranı neden kartlarıyla desteklenmeli.
6. En fazla 6 uygulama ikonu gösterilmeli.
7. Ana eylem tam genişlikte olmalı.
8. Erteleme ikincil outline buton olmalı.
9. Gizleme text button olmalı.
10. Kırmızı yalnız hata veya geri döndürülemez silme için kullanılmalı.
11. Material 3 dynamic color ile uyumlu olmalı.
12. Pixel görünümü açıkken tema bozulmamalı.
13. Dark mode ayrıca test edilmeli.
14. Dokunma hedefleri minimum 48 dp olmalı.
15. Büyük yazıda buton metni iki satıra çıkabilmeli.
16. TalkBack açıklamaları klasör adı ve uygulama sayısını içermeli.

---

# 18. Telemetry

Dosya:

`telemetry/TelemetryEvent.kt`

Eklenecek event’ler:

- `folder_merge_suggestion_opened`
- `folder_merge_target_changed`
- `folder_merge_app_selection_changed`
- `folder_merge_applied`
- `folder_merge_undone`
- `folder_merge_snoozed`
- `folder_merge_dismissed`
- `folder_merge_failed`

Parametreler:

- sourceCategoryId
- targetCategoryId
- suggestedTargetCategoryId
- selectedAppCount
- sourceAppCount
- targetAppCount
- confidence
- targetChanged
- lockedAppIncluded
- durationMs
- undoUsed

Gizlilik:

1. Uygulama adları telemetry’ye yazılmamalı.
2. Package name gönderilmemeli.
3. Yalnız sayısal ve kategori seviyesinde aggregate veri kullanılmalı.

---

# 19. Test Planı

## 19.1. Unit test

Yeni testler:

- `FolderMergeCandidateScorerTest.kt`
- `FolderMergePlanTest.kt`
- `FolderSuggestionEngineMergeTest.kt`

Senaryolar:

1. 2 uygulamalı Müzik → Eğlence önerisi oluşur.
2. Hedef klasör yoksa öneri oluşmaz.
3. Kaynak ve hedef aynıysa plan geçersizdir.
4. Kategorisiz hedef olamaz.
5. Kilitli uygulama varsayılan seçilmez.
6. Kaynakta seçilmeyen uygulama varsa kaynak sayısı doğru kalır.
7. Güven eşiği altındaki aday gösterilmez.
8. Dismissed öneri yeniden gösterilmez.
9. Snoozed öneri süresi dolmadan gösterilmez.
10. Stable ID aynı veri için değişmez.
11. Hedef 20 uygulamayı geçerse crowded flag true olur.
12. Seçilen paket kaynak paket listesinde değilse plan geçersiz olur.

## 19.2. Repository test

Yeni test:

`FolderOperationRepositoryTest.kt`

Senaryolar:

1. Merge transaction başarılıdır.
2. Operation history oluşur.
3. Operation item sayısı taşınan uygulama sayısına eşittir.
4. Transaction hata verirse hiçbir uygulama taşınmaz.
5. Undo eski kategorileri geri getirir.
6. İkinci undo engellenir.
7. Kısmi uygulama seçimi doğru taşınır.
8. Farklı eski kategoriler gerektiğinde doğru restore edilir.
9. Operation bulunamazsa kontrollü hata verir.
10. Migration sonrası tablolar mevcuttur.

## 19.3. ViewModel test

Yeni test:

`FolderMergeViewModelTest.kt`

Senaryolar:

1. Öneri açıldığında plan oluşur.
2. Varsayılan hedef önerilen hedeftir.
3. Kilitli uygulama seçili değildir.
4. Hedef değişince sayılar güncellenir.
5. Uygulama seçimi değişince buton metni güncellenir.
6. Boş seçimde onay disabled olur.
7. Çift onay engellenir.
8. Başarılı işlem sonrası state temizlenir.
9. Hata durumunda plan ekranda kalır.
10. Undo operation ID doğru saklanır.
11. Erteleme öneriyi kapatır.
12. Dismiss öneriyi kalıcı gizler.

## 19.4. Compose UI test

Yeni test:

`FolderMergeReviewScreenTest.kt`

Senaryolar:

1. Kaynak ve hedef kartları görünür.
2. Uygulama ikonları görünür.
3. Hedef picker açılır.
4. App picker açılır.
5. Kilitli uygulama uyarısı görünür.
6. Önce/sonra sayıları doğru görünür.
7. Ana buton doğru metni gösterir.
8. Empty selection durumunda buton disabled olur.
9. Loading sırasında buton ikinci tıklamayı kabul etmez.
10. Başarı Snackbar’ında Geri Al vardır.
11. Büyük yazıda taşma olmaz.
12. Dark mode okunabilir.
13. TalkBack semantics doğru metinleri içerir.

## 19.5. Manuel cihaz testi

1. 360 dp küçük telefon
2. Pixel 6 boyutu
3. büyük ekranlı telefon
4. tablet
5. font scale 1.3
6. font scale 1.5
7. dark mode
8. dynamic color açık
9. Pixel görünümü açık
10. en az 10 klasörlü gerçek veri
11. tüm uygulamaları kilitli kaynak klasör
12. 20+ uygulamalı hedef klasör
13. işlem sırasında ekran döndürme
14. uygulamayı arka plana gönderip geri dönme
15. merge sonrası launcher klasör görünümü
16. undo sonrası launcher klasör görünümü

---

# 20. Uygulama Fazları

## Faz 1 — Domain ve öneri motoru

1. `FolderSuggestion` modelini genişlet.
2. `FolderSuggestionReason` oluştur.
3. `FolderMergePlan` oluştur.
4. `FolderMergeCandidateScorer` yaz.
5. Mevcut sabit eşleşmeleri scorer’a taşı.
6. Kilitli app varsayılan seçimini uygula.
7. Confidence eşiklerini uygula.
8. Engine unit testlerini yaz.

**Çıkış kriteri:** Merge önerileri kaynak, hedef, neden ve sayılarıyla üretilebilmeli.

## Faz 2 — UI state ve ViewModel

1. `FolderMergeUiState` oluştur.
2. ViewModel state flow ekle.
3. `openMergeSuggestion()` yaz.
4. Uygulama seçme fonksiyonlarını yaz.
5. Hedef değiştirme fonksiyonunu yaz.
6. Loading, success ve error state ekle.
7. Legacy accept fonksiyonunu route edecek şekilde düzenle.
8. ViewModel testlerini yaz.

**Çıkış kriteri:** Repository’ye yazmadan tam merge planı UI state içinde yönetilebilmeli.

## Faz 3 — A tasarımı UI

1. `FolderSuggestionsScreen` refactor et.
2. Featured merge card ekle.
3. `FolderMergeReviewScreen` oluştur.
4. Kaynak/hedef preview oluştur.
5. App grid oluştur.
6. App picker bottom sheet oluştur.
7. Target picker bottom sheet oluştur.
8. Before/after kartı oluştur.
9. Loading ve hata görünümü ekle.
10. Dark mode ve erişilebilirlik düzelt.
11. Compose UI testlerini yaz.

**Çıkış kriteri:** Kullanıcı merge planını eksiksiz inceleyebilmeli fakat henüz kalıcı uygulama yapılmayabilir.

## Faz 4 — Room transaction ve gerçek undo

1. Operation entity oluştur.
2. Operation item entity oluştur.
3. DAO oluştur.
4. AppDatabase version artır.
5. Migration yaz.
6. Schema JSON üret.
7. Repository `mergeFolders()` yaz.
8. Repository `undoFolderMerge()` yaz.
9. Transaction rollback testleri yaz.
10. Search index ve manual override senkronizasyonunu bağla.

**Çıkış kriteri:** Merge atomik olarak uygulanmalı ve uygulama yeniden başlatıldıktan sonra dahi geri alınabilmeli.

## Faz 5 — Telemetry ve QA

1. Telemetry event’lerini ekle.
2. TaskScore bağlantısını doğrula.
3. Unit testleri çalıştır.
4. Repository testlerini çalıştır.
5. Compose UI testlerini çalıştır.
6. Telefon smoke testi yap.
7. Tablet testi yap.
8. Büyük yazı testi yap.
9. Dark mode testi yap.
10. Launcher görünümünü merge ve undo sonrası doğrula.

**Çıkış kriteri:** Tüm kabul kriterleri sağlanmış, build ve testler geçmiş olmalı.

---

# 21. Değiştirilecek Dosyalar

1. `app/src/main/java/com/armutlu/apporganizer/domain/usecase/folder/FolderSuggestionEngine.kt`
2. `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/FolderSuggestionsScreen.kt`
3. `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt`
4. `app/src/main/java/com/armutlu/apporganizer/data/repository/AppRepository.kt`
5. `app/src/main/java/com/armutlu/apporganizer/data/local/AppDatabase.kt`
6. `app/src/main/java/com/armutlu/apporganizer/presentation/navigation/AppNavigation.kt`
7. `app/src/main/java/com/armutlu/apporganizer/utils/TaskScoreManager.kt`
8. `app/src/main/java/com/armutlu/apporganizer/telemetry/TelemetryEvent.kt`
9. `app/src/main/res/values/folder_merge_strings.xml`
10. `app/src/main/res/values-en/folder_merge_strings.xml`
11. Room schema JSON dosyası

---

# 22. Yeni Oluşturulacak Dosyalar

1. `domain/usecase/folder/FolderMergePlan.kt`
2. `domain/usecase/folder/FolderSuggestionReason.kt`
3. `domain/usecase/folder/FolderMergeCandidateScorer.kt`
4. `data/local/FolderOperationEntity.kt`
5. `data/local/FolderOperationItemEntity.kt`
6. `data/local/FolderOperationDao.kt`
7. `presentation/ui/screens/foldermerge/FolderMergeUiState.kt`
8. `presentation/ui/screens/foldermerge/FolderMergeReviewScreen.kt`
9. `presentation/ui/screens/foldermerge/FolderMergePreviewCard.kt`
10. `presentation/ui/screens/foldermerge/FolderMergeReasonCard.kt`
11. `presentation/ui/screens/foldermerge/FolderMergeAppGrid.kt`
12. `presentation/ui/screens/foldermerge/FolderMergeAppPickerBottomSheet.kt`
13. `presentation/ui/screens/foldermerge/FolderMergeTargetPickerBottomSheet.kt`
14. `presentation/ui/screens/foldermerge/FolderMergeBeforeAfterCard.kt`
15. `presentation/ui/screens/foldermerge/FolderMergeLockedAppWarningDialog.kt`
16. ilgili unit, repository, ViewModel ve Compose test dosyaları

---

# 23. Kabul Kriterleri

- [ ] A tasarımı uygulanmıştır.
- [ ] Kullanıcı uygulamaları görmeden merge uygulayamaz.
- [ ] Kaynak ve hedef klasör yan yana görünür.
- [ ] Hedef klasör değiştirilebilir.
- [ ] Taşınacak uygulamalar seçilebilir.
- [ ] Kilitli uygulamalar açıkça işaretlenir.
- [ ] Kilitli uygulamalar varsayılan seçim dışında kalır.
- [ ] Önce/sonra uygulama sayıları doğrudur.
- [ ] Hedef 20+ olacaksa uyarı görünür.
- [ ] En az bir uygulama seçilmeden onay verilemez.
- [ ] İşlem tek transaction içinde yapılır.
- [ ] Hata durumunda kısmi taşıma olmaz.
- [ ] Birleştirme operation geçmişine yazılır.
- [ ] Birleştirme uygulama yeniden başlatıldıktan sonra geri alınabilir.
- [ ] Geri alma eski kategori bilgilerini doğru geri getirir.
- [ ] Aynı operation ikinci kez geri alınamaz.
- [ ] Search index güncellenir.
- [ ] Manuel override kayıtları güncellenir.
- [ ] Öneri başarı sonrası yeniden görünmez.
- [ ] Ertelenen öneri 7 gün görünmez.
- [ ] Kaynak sistem klasörü boşsa görünür listeden düşer.
- [ ] Hiçbir uygulama silinmez.
- [ ] Split ve cleanup davranışları bozulmaz.
- [ ] Türkçe ve İngilizce string kaynakları tamamlanır.
- [ ] Kod içinde sabit UI metni kalmaz.
- [ ] Telefon ve tablette UI taşmaz.
- [ ] Büyük yazıda ana buton erişilebilir kalır.
- [ ] Dark mode kontrastı yeterlidir.
- [ ] TalkBack açıklamaları yeterlidir.
- [ ] Unit testler geçer.
- [ ] Repository transaction testleri geçer.
- [ ] ViewModel testleri geçer.
- [ ] Compose UI testleri geçer.
- [ ] Gerçek cihaz smoke testi geçer.

---

# 24. Bu Çalışmada Yapılmayacaklar

1. Otomatik sessiz birleştirme
2. Çoklu klasörü tek seferde birleştirme
3. Sürükle-bırak ana akışı
4. Yapay zekâ API’siyle hedef belirleme
5. Klasör adını otomatik değiştirme
6. Bulut üzerinden cihazlar arası merge geçmişi
7. Boş sistem kategorisini veri tabanından silme
8. Uygulama silme veya kaldırma
9. Split ve cleanup akışını aynı anda büyük refactor’a sokma

Bunlar ilk sürüm kullanım verileri toplandıktan sonra ayrıca değerlendirilecektir.

---

# 25. Uygulama Ekibine Kesin Talimat

1. Bu roadmap geliştirme görev sırası olarak kullanılacaktır.
2. Mevcut `acceptFolderSuggestion()` güvenli merge akışı tamamlanmadan silinmeyecektir.
3. `MERGE_SMALL_FOLDER` doğrudan kategori taşıma yapmayacak; review ekranına yönlenecektir.
4. `SPLIT_LARGE_FOLDER` ve `CLEAN_UNUSED_APPS` davranışları korunacaktır.
5. Room migration ve schema JSON commit edilmeden işlem tamamlanmış sayılmayacaktır.
6. Her transaction hatası kullanıcıya görünür şekilde bildirilecektir.
7. Birleştirmenin başarılı sayılması için Room, manuel override ve search index aynı sonucu göstermelidir.
8. Testler geçmeden özellik tamamlandı olarak işaretlenmeyecektir.
9. Telefon testi tek başına yeterli değildir; tablet, küçük ekran, büyük font ve dark mode doğrulanacaktır.
10. Çalışma tamamlandığında bu dosyadaki kabul kriterleri tek tek işaretlenecek ve sonuç `HISTORY.md` içine taşınacaktır.
