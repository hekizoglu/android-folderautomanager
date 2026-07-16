package com.armutlu.apporganizer.presentation.navigation

import org.junit.Assert.assertTrue
import org.junit.Test

class UsageDataRouteTest {
    @Test fun `usage data route is whitelisted`() {
        assertTrue(Routes.isValid(Routes.SETTINGS_USAGE_DATA))
    }
}
