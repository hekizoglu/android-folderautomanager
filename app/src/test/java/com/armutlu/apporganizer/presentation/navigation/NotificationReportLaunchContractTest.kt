package com.armutlu.apporganizer.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationReportLaunchContractTest {

    @Test
    fun `history request opens history tab`() {
        assertEquals(
            NotificationReportLaunchContract.TAB_HISTORY,
            NotificationReportLaunchContract.normalizeTab(
                NotificationReportLaunchContract.TAB_HISTORY,
            ),
        )
    }

    @Test
    fun `invalid low tab falls back to report`() {
        assertEquals(
            NotificationReportLaunchContract.TAB_REPORT,
            NotificationReportLaunchContract.normalizeTab(-1),
        )
    }

    @Test
    fun `invalid high tab is capped at history`() {
        assertEquals(
            NotificationReportLaunchContract.TAB_HISTORY,
            NotificationReportLaunchContract.normalizeTab(99),
        )
    }
}
