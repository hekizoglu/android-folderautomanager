package com.armutlu.apporganizer.domain.usecase.folder

/**
 * R4.2 madde 6: TaskScore bağlantısını yalnız başarıdan sonra tetikle.
 * Merge başarılı → score event oluştur; undo → score reversal işaretleme.
 */
data class FolderMergeScoreEvent(
    val eventKey: String,
    val label: String,
    val delta: Int,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * Başarılı merge → "tidy.merge" event, +3 puan
         * (klasörleme işlemi başarıyla tamamlandı)
         */
        fun onMergeSuccess(sourceCategoryId: String, targetCategoryId: String): FolderMergeScoreEvent {
            return FolderMergeScoreEvent(
                eventKey = "tidy.merge.$sourceCategoryId",
                label = "Klasörler başarıyla birleştirildi",
                delta = 3,
            )
        }

        /**
         * Başarılı undo → "tidy.merge.undo" event, -3 puan
         * (merge işlemi geri alındı; skoru düşür)
         */
        fun onUndoSuccess(operationId: String): FolderMergeScoreEvent {
            return FolderMergeScoreEvent(
                eventKey = "tidy.merge.undo.$operationId",
                label = "Klasör birleştirmesi geri alındı",
                delta = -3,
            )
        }
    }
}
