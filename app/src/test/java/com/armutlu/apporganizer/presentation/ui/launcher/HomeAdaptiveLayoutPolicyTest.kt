package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Döngü P20 — `HomeAdaptiveLayoutPolicy` saf breakpoint/eşleme mantığının birim testleri.
 * Compose/Android bağımlılığı yoktur (bkz. `HomeAdaptiveLayoutPolicy.kt`).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P20 (satır 1432-1500).
 */
class HomeAdaptiveLayoutPolicyTest {

    // --- deviceClass: eski isTablet/screenColumns eşikleriyle birebir uyum (599/600/839/840) ---

    @Test fun `599dp telefon sinifidir`() {
        assertEquals(HomeDeviceClass.PHONE, HomeAdaptiveLayoutPolicy.deviceClass(599))
    }

    @Test fun `600dp kucuk tablet sinifina gecer`() {
        assertEquals(HomeDeviceClass.COMPACT_TABLET, HomeAdaptiveLayoutPolicy.deviceClass(600))
    }

    @Test fun `839dp hala kucuk tablettir`() {
        assertEquals(HomeDeviceClass.COMPACT_TABLET, HomeAdaptiveLayoutPolicy.deviceClass(839))
    }

    @Test fun `840dp buyuk tablet sinifina gecer`() {
        assertEquals(HomeDeviceClass.EXPANDED_TABLET, HomeAdaptiveLayoutPolicy.deviceClass(840))
    }

    @Test fun `1200dp buyuk tablettir`() {
        assertEquals(HomeDeviceClass.EXPANDED_TABLET, HomeAdaptiveLayoutPolicy.deviceClass(1200))
    }

    @Test fun `cok kucuk telefon genisligi de PHONE doner`() {
        assertEquals(HomeDeviceClass.PHONE, HomeAdaptiveLayoutPolicy.deviceClass(320))
    }

    // --- folderColumns: telefon 4, kucuk tablet 5, buyuk tablet 6 (roadmap P20 bolumleri) ---

    @Test fun `telefon 4 sutun`() {
        assertEquals(4, HomeAdaptiveLayoutPolicy.folderColumns(HomeDeviceClass.PHONE))
    }

    @Test fun `kucuk tablet 5 sutun`() {
        assertEquals(5, HomeAdaptiveLayoutPolicy.folderColumns(HomeDeviceClass.COMPACT_TABLET))
    }

    @Test fun `buyuk tablet 6 sutun`() {
        assertEquals(6, HomeAdaptiveLayoutPolicy.folderColumns(HomeDeviceClass.EXPANDED_TABLET))
    }

    // --- isTablet: PHONE haric hepsi tablet (side-panel/scrim kararinin dayandigi ikili sozlesme) ---

    @Test fun `telefon tablet degildir`() {
        assertEquals(false, HomeAdaptiveLayoutPolicy.isTablet(HomeDeviceClass.PHONE))
    }

    @Test fun `kucuk ve buyuk tablet tablet sayilir`() {
        assertEquals(true, HomeAdaptiveLayoutPolicy.isTablet(HomeDeviceClass.COMPACT_TABLET))
        assertEquals(true, HomeAdaptiveLayoutPolicy.isTablet(HomeDeviceClass.EXPANDED_TABLET))
    }

    // --- allAppsSidePanelWidthDp: kucuk tablette eski sabitle (380dp) birebir ayni, buyukte genisler ---

    @Test fun `kucuk tablet side panel eski 380dp sabitiyle ayni`() {
        assertEquals(380, HomeAdaptiveLayoutPolicy.allAppsSidePanelWidthDp(HomeDeviceClass.COMPACT_TABLET))
    }

    @Test fun `buyuk tablet side panel kucuk tabletten genistir ama ekrani kaplamaz`() {
        val expandedWidth = HomeAdaptiveLayoutPolicy.allAppsSidePanelWidthDp(HomeDeviceClass.EXPANDED_TABLET)
        val compactWidth = HomeAdaptiveLayoutPolicy.allAppsSidePanelWidthDp(HomeDeviceClass.COMPACT_TABLET)
        assert(expandedWidth > compactWidth)
        assert(expandedWidth < 900) // buyuk tablet genisliginin (840dp+) tamamini kaplamaz
    }

    // --- centeredContentMaxWidthDp: yalnizca EXPANDED_TABLET'te tavan uygulanir ---

    @Test fun `telefonda content max width sinir yoktur (null)`() {
        assertNull(HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(HomeDeviceClass.PHONE))
    }

    @Test fun `kucuk tablette content max width sinir yoktur (null)`() {
        assertNull(HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(HomeDeviceClass.COMPACT_TABLET))
    }

    @Test fun `buyuk tablette content max width tavan uygulanir`() {
        val maxWidth = HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(HomeDeviceClass.EXPANDED_TABLET)
        assertEquals(HomeAdaptiveLayoutPolicy.EXPANDED_CONTENT_MAX_WIDTH_DP, maxWidth)
        assert(maxWidth!! < 840) // 840dp+ ekranin tamamindan dar olmali, aksi halde tavan anlamsiz
    }
}
