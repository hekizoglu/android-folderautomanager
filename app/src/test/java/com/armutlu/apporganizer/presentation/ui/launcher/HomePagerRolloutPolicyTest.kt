package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePagerRolloutPolicyTest {
    @Test fun flagKapaliykenV2Pasif() {
        assertFalse(HomePagerRolloutPolicy.isV2Active(flagEnabled = false, safeMode = false))
    }

    @Test fun safeModeFlagAcikOlsaBileV2yiKapatir() {
        assertFalse(HomePagerRolloutPolicy.isV2Active(flagEnabled = true, safeMode = true))
    }

    @Test fun dashboardYalnizcaIkiKapidanDaGecerseAcilir() {
        assertTrue(HomePagerRolloutPolicy.dashboardEnabled(true, false, true))
        assertFalse(HomePagerRolloutPolicy.dashboardEnabled(true, false, false))
        assertFalse(HomePagerRolloutPolicy.dashboardEnabled(false, false, true))
    }
}
