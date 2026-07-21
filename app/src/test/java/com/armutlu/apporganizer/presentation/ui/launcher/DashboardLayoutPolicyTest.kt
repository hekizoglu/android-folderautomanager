package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P07 — `DashboardLayoutPolicy.mode()` saf devir/eşik mantığının birim testleri.
 * Compose/Android bağımlılığı yoktur (bkz. `DashboardLayoutPolicy.kt`).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P07
 * (satır 757-804) testler bölümü: "640dp altı ekran", "700dp ekran", "Tablet", "Widget açık/kapalı".
 */
class DashboardLayoutPolicyTest {

    @Test fun `640dp alti ekran ULTRA_COMPACT doner`() {
        assertEquals(
            DashboardDensity.ULTRA_COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 600, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `tam 640dp COMPACT doner (ust sinir haric)`() {
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 640, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `700dp altinda ama 640 ustunde COMPACT doner`() {
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 680, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `700dp ve uzeri az sectionla COMFORTABLE doner`() {
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 700, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `tablet buyuk ekran az sectionla COMFORTABLE doner`() {
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 1200, visibleSectionCount = 4, hasWidgets = false)
        )
    }

    @Test fun `buyuk ekranda cok section varsa COMPACT'a duser`() {
        // hasWidgets=false -> esik 5
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 4, hasWidgets = false)
        )
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 5, hasWidgets = false)
        )
    }

    @Test fun `widget acikken esik daha erken tetiklenir`() {
        // hasWidgets=true -> esik 4 (widget kapaliyken ayni sayida section COMFORTABLE kalirdi)
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 3, hasWidgets = true)
        )
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 4, hasWidgets = true)
        )
    }

    @Test fun `kucuk ekranda section sayisi ne olursa olsun ULTRA_COMPACT kazanir`() {
        assertEquals(
            DashboardDensity.ULTRA_COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 500, visibleSectionCount = 1, hasWidgets = false)
        )
    }

}
