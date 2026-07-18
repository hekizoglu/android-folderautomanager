package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageSpec
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.HomePagePrefs

object HomePageTelemetryPolicy {

    fun pageType(page: HomePageSpec?): TelemetryEvent.HomePageType = when (page) {
        HomePageSpec.Dashboard -> TelemetryEvent.HomePageType.DASHBOARD
        is HomePageSpec.FolderPage,
        null -> TelemetryEvent.HomePageType.FOLDER
    }

    fun positionBucket(pageIndex: Int, pageCount: Int): TelemetryEvent.HomePagePositionBucket = when {
        pageIndex <= 0 -> TelemetryEvent.HomePagePositionBucket.FIRST
        pageIndex >= pageCount - 1 -> TelemetryEvent.HomePagePositionBucket.LAST
        else -> TelemetryEvent.HomePagePositionBucket.MIDDLE
    }

    fun searchPosition(value: String): TelemetryEvent.HomeSearchPosition =
        if (value == AppPrefs.SEARCH_BAR_POS_BOTTOM) {
            TelemetryEvent.HomeSearchPosition.BOTTOM
        } else {
            TelemetryEvent.HomeSearchPosition.TOP
        }

    fun startMode(mode: HomePagePrefs.StartPageMode): TelemetryEvent.HomeStartMode = when (mode) {
        HomePagePrefs.StartPageMode.SMART_DASHBOARD -> TelemetryEvent.HomeStartMode.DASHBOARD
        HomePagePrefs.StartPageMode.FIRST_FOLDER_PAGE -> TelemetryEvent.HomeStartMode.FIRST_FOLDER
        HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE -> TelemetryEvent.HomeStartMode.LAST_VISITED
    }

    fun deviceClass(deviceClass: HomeDeviceClass): TelemetryEvent.HomeTelemetryDeviceClass = when (deviceClass) {
        HomeDeviceClass.PHONE -> TelemetryEvent.HomeTelemetryDeviceClass.PHONE
        HomeDeviceClass.COMPACT_TABLET -> TelemetryEvent.HomeTelemetryDeviceClass.COMPACT_TABLET
        HomeDeviceClass.EXPANDED_TABLET -> TelemetryEvent.HomeTelemetryDeviceClass.EXPANDED_TABLET
    }
}
