package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.HomePagePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePageTelemetryPolicyTest {

    @Test fun `page type does not expose folder identity`() {
        val folderPage = HomePageSpec.FolderPage(
            pageIndex = 2,
            firstFolderCategoryId = "private-category-id",
            folders = emptyList(),
        )

        assertEquals(TelemetryEvent.HomePageType.DASHBOARD, HomePageTelemetryPolicy.pageType(HomePageSpec.Dashboard))
        assertEquals(TelemetryEvent.HomePageType.FOLDER, HomePageTelemetryPolicy.pageType(folderPage))
        assertEquals(TelemetryEvent.HomePageType.FOLDER, HomePageTelemetryPolicy.pageType(null))
    }

    @Test fun `position bucket hides exact page index`() {
        assertEquals(TelemetryEvent.HomePagePositionBucket.FIRST, HomePageTelemetryPolicy.positionBucket(0, 4))
        assertEquals(TelemetryEvent.HomePagePositionBucket.MIDDLE, HomePageTelemetryPolicy.positionBucket(1, 4))
        assertEquals(TelemetryEvent.HomePagePositionBucket.MIDDLE, HomePageTelemetryPolicy.positionBucket(2, 4))
        assertEquals(TelemetryEvent.HomePagePositionBucket.LAST, HomePageTelemetryPolicy.positionBucket(3, 4))
    }

    @Test fun `settings enums map to telemetry wire enums`() {
        assertEquals(TelemetryEvent.HomeSearchPosition.TOP, HomePageTelemetryPolicy.searchPosition(AppPrefs.SEARCH_BAR_POS_TOP))
        assertEquals(TelemetryEvent.HomeSearchPosition.BOTTOM, HomePageTelemetryPolicy.searchPosition(AppPrefs.SEARCH_BAR_POS_BOTTOM))
        assertEquals(TelemetryEvent.HomeStartMode.DASHBOARD, HomePageTelemetryPolicy.startMode(HomePagePrefs.StartPageMode.SMART_DASHBOARD))
        assertEquals(TelemetryEvent.HomeStartMode.FIRST_FOLDER, HomePageTelemetryPolicy.startMode(HomePagePrefs.StartPageMode.FIRST_FOLDER_PAGE))
        assertEquals(TelemetryEvent.HomeStartMode.LAST_VISITED, HomePageTelemetryPolicy.startMode(HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE))
    }

    @Test fun `device class maps to privacy safe buckets`() {
        assertEquals(TelemetryEvent.HomeTelemetryDeviceClass.PHONE, HomePageTelemetryPolicy.deviceClass(HomeDeviceClass.PHONE))
        assertEquals(TelemetryEvent.HomeTelemetryDeviceClass.COMPACT_TABLET, HomePageTelemetryPolicy.deviceClass(HomeDeviceClass.COMPACT_TABLET))
        assertEquals(TelemetryEvent.HomeTelemetryDeviceClass.EXPANDED_TABLET, HomePageTelemetryPolicy.deviceClass(HomeDeviceClass.EXPANDED_TABLET))
    }
}
