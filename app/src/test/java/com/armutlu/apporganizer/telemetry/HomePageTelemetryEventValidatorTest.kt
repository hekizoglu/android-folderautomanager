package com.armutlu.apporganizer.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePageTelemetryEventValidatorTest {

    @Test fun `home page telemetry events contain only bounded values`() {
        val events = listOf(
            TelemetryEvent.HomePageViewed(
                pageType = TelemetryEvent.HomePageType.DASHBOARD,
                pagePosition = TelemetryEvent.HomePagePositionBucket.FIRST,
                navigationSource = TelemetryEvent.HomeNavigationSource.RESTORE,
                searchPosition = TelemetryEvent.HomeSearchPosition.TOP,
                startMode = TelemetryEvent.HomeStartMode.DASHBOARD,
                deviceClass = TelemetryEvent.HomeTelemetryDeviceClass.PHONE,
            ),
            TelemetryEvent.HomePageSwiped(
                pageType = TelemetryEvent.HomePageType.FOLDER,
                pagePosition = TelemetryEvent.HomePagePositionBucket.LAST,
                deviceClass = TelemetryEvent.HomeTelemetryDeviceClass.EXPANDED_TABLET,
            ),
            TelemetryEvent.HomeSearchOpened(TelemetryEvent.HomeSearchPosition.BOTTOM, TelemetryEvent.HomePageType.FOLDER),
            TelemetryEvent.HomeStartModeChanged(TelemetryEvent.HomeStartMode.LAST_VISITED),
            TelemetryEvent.SmartDashboardToggled(TelemetryEvent.ToggleState.ENABLED),
            TelemetryEvent.AllAppsOpenedFromPage(
                TelemetryEvent.HomePageType.FOLDER,
                TelemetryEvent.HomePagePositionBucket.MIDDLE,
                TelemetryEvent.HomeTelemetryDeviceClass.COMPACT_TABLET,
            ),
            TelemetryEvent.HomeButtonNavigation(
                TelemetryEvent.HomeStartMode.FIRST_FOLDER,
                TelemetryEvent.HomePageType.FOLDER,
            ),
        )

        assertTrue(events.all(TelemetryEventValidator::isValid))
    }

    @Test fun `home page telemetry rejects folder identity and exact page index`() {
        assertFalse(
            TelemetryEventValidator.isValidPayload(
                "home_page_viewed",
                mapOf(
                    "page_type" to "folder",
                    "page_position_bucket" to "first",
                    "navigation_source" to "swipe",
                    "search_position" to "top",
                    "start_mode" to "dashboard",
                    "device_class" to "phone",
                    "category_name" to "Finance",
                ),
            )
        )
        assertFalse(
            TelemetryEventValidator.isValidPayload(
                "home_page_swiped",
                mapOf(
                    "page_type" to "folder",
                    "page_position_bucket" to "3",
                    "device_class" to "phone",
                ),
            )
        )
    }
}
