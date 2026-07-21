package com.armutlu.apporganizer.domain.home.smartaccess

object SmartAccessNotificationPolicy {
    fun isWithinWindow(
        lastPostedAt: Long,
        nowMillis: Long,
        windowMillis: Long,
    ): Boolean {
        if (lastPostedAt <= 0L || windowMillis <= 0L) return false
        return lastPostedAt >= nowMillis - windowMillis
    }
}
