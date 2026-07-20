package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Görev S2 — Usta (100⭐) ödülü kilit mantığı. Saf Kotlin, Android bağımlılığı yok,
 * unit test edilebilir. StarLevelSystem.Level.MASTER'a ulaşılmadan hiçbir görsel ödül
 * (altın saat aksanı, altın klasör rengi) açılmaz — tek doğruluk kaynağı burasıdır.
 */
object MasterRewardPolicy {

    /** Kullanıcı MASTER seviyesine ulaştı mı (toplam ⭐ üzerinden). */
    fun isMasterUnlocked(totalStars: Int): Boolean =
        StarLevelSystem.levelFor(totalStars) == StarLevelSystem.Level.MASTER

    /**
     * Altın saat aksanının fiilen gösterilip gösterilmeyeceği — hem MASTER kilidi HEM kullanıcının
     * Ayarlar'dan açtığı tercih (varsayılan kapalı) birlikte gerekir.
     */
    fun isGoldClockAccentActive(totalStars: Int, prefEnabled: Boolean): Boolean =
        prefEnabled && isMasterUnlocked(totalStars)

    /** Altın klasör rengi seçeneği yalnızca MASTER seviyesinde palette'te görünür. */
    fun isGoldFolderColorVisible(totalStars: Int): Boolean = isMasterUnlocked(totalStars)
}
