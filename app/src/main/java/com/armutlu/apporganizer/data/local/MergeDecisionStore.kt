package com.armutlu.apporganizer.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * R4.2: Başarılı merge sonrası aynı öneriyi tekrar göstermeyen + ertelenmiş önerileri 7 gün boyunca gizleyen store.
 * Key format: "merge_decision_{sourceCatId}_{targetCatId}" → json "{decision: 'approved', timestamp: ms}"
 */
class MergeDecisionStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "merge_decisions",
        Context.MODE_PRIVATE,
    )

    companion object {
        const val DECISION_APPROVED = "approved"
        const val DECISION_SNOOZED = "snoozed"
        const val SNOOZE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 gün
    }

    /**
     * Başarılı merge sonrası öneriyi "onaylandı" olarak işaretle.
     * Aynı source→target önerisi tekrar gösterilmeyecek.
     */
    fun recordApprovedMerge(sourceCategoryId: String, targetCategoryId: String) {
        val key = "merge_decision_${sourceCategoryId}_$targetCategoryId"
        prefs.edit().putLong("${key}_timestamp", System.currentTimeMillis()).apply()
        prefs.edit().putString("${key}_decision", DECISION_APPROVED).apply()
    }

    /**
     * Önerinin sonraki gösterilmesi için 7 gün ertele.
     */
    fun snoozeForSevenDays(sourceCategoryId: String, targetCategoryId: String) {
        val key = "merge_decision_${sourceCategoryId}_$targetCategoryId"
        prefs.edit().putLong("${key}_timestamp", System.currentTimeMillis()).apply()
        prefs.edit().putString("${key}_decision", DECISION_SNOOZED).apply()
    }

    /**
     * Öneri gösterilmeli mi kontrol et:
     * - Onaylanmışsa false (asla yeniden gösterme)
     * - Ertelenmiş ve 7 gün geçmişse true (göster)
     * - Ertelenmiş ve 7 gün geçmemişse false (gizle)
     * - Kaydı yoksa true (göster)
     */
    fun shouldShowSuggestion(sourceCategoryId: String, targetCategoryId: String): Boolean {
        val key = "merge_decision_${sourceCategoryId}_$targetCategoryId"
        val decision = prefs.getString("${key}_decision", null) ?: return true
        val timestamp = prefs.getLong("${key}_timestamp", 0)

        return when (decision) {
            DECISION_APPROVED -> false // Asla yeniden gösterme
            DECISION_SNOOZED -> {
                val elapsed = System.currentTimeMillis() - timestamp
                elapsed >= SNOOZE_DURATION_MS // 7 gün geçmişse göster
            }
            else -> true // Bilinmeyen durum → göster
        }
    }

    /**
     * Tüm kararları temizle (test/debug için).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
