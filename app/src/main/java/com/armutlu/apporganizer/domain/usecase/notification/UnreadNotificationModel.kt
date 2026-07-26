package com.armutlu.apporganizer.domain.usecase.notification

/**
 * Aktif sistem bildirimi, launcher'da okunmamış rozet ve geçmiş istatistiği birbirinden ayırır.
 * Bu model yalnız rozet kararını üretir; notification_events geçmişini değiştirmez.
 */
object UnreadNotificationModel {

    /** Tek aktif bildirimin, paket son okunma zamanına göre okunmamış olup olmadığını belirler. */
    fun isUnread(postedAt: Long, lastReadAt: Long?): Boolean =
        lastReadAt == null || postedAt > lastReadAt

    fun unreadCountFor(activeCount: Int, lastPostedAt: Long?, lastReadAt: Long?): Int {
        if (activeCount <= 0) return 0
        val posted = lastPostedAt ?: return activeCount
        return if (isUnread(posted, lastReadAt)) activeCount else 0
    }

    fun computeUnreadCounts(
        activeCounts: Map<String, Int>,
        lastPostedAt: Map<String, Long>,
        lastReadAt: Map<String, Long>,
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        activeCounts.forEach { (pkg, active) ->
            val unread = unreadCountFor(active, lastPostedAt[pkg], lastReadAt[pkg])
            if (unread > 0) result[pkg] = unread
        }
        return result
    }
}
