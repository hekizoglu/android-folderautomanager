package com.armutlu.apporganizer.presentation.ui.launcher

/** P24 rollout karari; Compose ve Android tercihlerinden bagimsiz saf politika. */
internal object HomePagerRolloutPolicy {
    fun isV2Active(flagEnabled: Boolean, safeMode: Boolean): Boolean =
        flagEnabled && !safeMode

    fun dashboardEnabled(
        flagEnabled: Boolean,
        safeMode: Boolean,
        dashboardPreferenceEnabled: Boolean,
    ): Boolean = isV2Active(flagEnabled, safeMode) && dashboardPreferenceEnabled
}
