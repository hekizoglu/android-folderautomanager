package com.armutlu.apporganizer.domain.usecase.missions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Görev S2 — Usta (100⭐) ödülü kilit mantığı testleri. Saf Kotlin, StarLevelSystem üzerine
 * kurulu — MASTER eşiği (100⭐) altında hiçbir ödül aktif olmamalı.
 */
class MasterRewardPolicyTest {

    @Test
    fun isMasterUnlocked_falseBelowThreshold() {
        assertFalse(MasterRewardPolicy.isMasterUnlocked(0))
        assertFalse(MasterRewardPolicy.isMasterUnlocked(50))
        assertFalse(MasterRewardPolicy.isMasterUnlocked(99))
    }

    @Test
    fun isMasterUnlocked_trueAtAndAboveThreshold() {
        assertTrue(MasterRewardPolicy.isMasterUnlocked(100))
        assertTrue(MasterRewardPolicy.isMasterUnlocked(250))
    }

    @Test
    fun isGoldClockAccentActive_requiresBothMasterAndPref() {
        assertFalse(MasterRewardPolicy.isGoldClockAccentActive(totalStars = 100, prefEnabled = false))
        assertFalse(MasterRewardPolicy.isGoldClockAccentActive(totalStars = 99, prefEnabled = true))
        assertTrue(MasterRewardPolicy.isGoldClockAccentActive(totalStars = 100, prefEnabled = true))
    }

    @Test
    fun isGoldFolderColorVisible_matchesMasterUnlock() {
        assertFalse(MasterRewardPolicy.isGoldFolderColorVisible(99))
        assertTrue(MasterRewardPolicy.isGoldFolderColorVisible(100))
    }
}
