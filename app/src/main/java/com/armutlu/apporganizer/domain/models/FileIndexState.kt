package com.armutlu.apporganizer.domain.models

/**
 * P0.3: Dosya arama kaynağının izin + indeks durumu.
 *
 * Sorun: Dosya kaynağında izin yokken indeksleme sessizce atlanıyordu
 * (FilesIndexer.indexAll() erken return), kullanıcı "sonuç yok" ile
 * "izin yok"u ayırt edemiyordu. Bu sealed model gerçek durumu taşır;
 * SearchSettingsScreen ve HomeAppSearchBar/AllAppsDrawer bu duruma göre
 * doğru mesajı gösterir.
 */
sealed class FileIndexState {
    /** Kullanıcı Ayarlar > Arama > "Dosya Adları" kaynağını hiç açmadı. */
    data object Disabled : FileIndexState()

    /** Kaynak açık ama Android izni (READ_MEDIA_* / READ_EXTERNAL_STORAGE) verilmemiş. */
    data object PermissionRequired : FileIndexState()

    /** İndeksleme sürüyor. progress bilinmiyorsa null (belirsiz ilerleme çubuğu). */
    data class Indexing(val progress: Float? = null) : FileIndexState()

    /** İndeks hazır — itemCount indekslenen dosya sayısı, lastIndexedAt epoch millis. */
    data class Ready(val itemCount: Int, val lastIndexedAt: Long) : FileIndexState()

    /** İndeksleme hata ile sonuçlandı — "Yeniden indeksle" aksiyonu sunulmalı. */
    data class Failed(val reason: String) : FileIndexState()
}

/**
 * Saf durum eşleme fonksiyonu — Android bağımlılığı yok, unit test edilebilir.
 *
 * @param sourceEnabled Kullanıcı dosya kaynağını Ayarlar'dan açtı mı (AppPrefs.isSearchSourceFilesEnabled)
 * @param hasPermission Gerekli medya/depolama izni verilmiş mi
 * @param isIndexing Şu anda arka planda tarama sürüyor mu
 * @param lastFailureReason Son indeksleme denemesi hata ile bittiyse sebep metni, yoksa null
 * @param itemCount Son başarılı indekste yazılan döküman sayısı
 * @param lastIndexedAt Son başarılı indeksleme epoch millis (hiç indekslenmediyse 0)
 */
fun computeFileIndexState(
    sourceEnabled: Boolean,
    hasPermission: Boolean,
    isIndexing: Boolean,
    lastFailureReason: String?,
    itemCount: Int,
    lastIndexedAt: Long,
): FileIndexState = when {
    !sourceEnabled -> FileIndexState.Disabled
    !hasPermission -> FileIndexState.PermissionRequired
    isIndexing -> FileIndexState.Indexing()
    lastFailureReason != null && lastIndexedAt <= 0L -> FileIndexState.Failed(lastFailureReason)
    lastIndexedAt > 0L -> FileIndexState.Ready(itemCount, lastIndexedAt)
    lastFailureReason != null -> FileIndexState.Failed(lastFailureReason)
    else -> FileIndexState.Indexing()
}
