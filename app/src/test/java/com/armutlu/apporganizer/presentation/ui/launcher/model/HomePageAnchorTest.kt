package com.armutlu.apporganizer.presentation.ui.launcher.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Döngü P02 — HomePageAnchor serileştirme round-trip testleri.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P02 (satır 449-501).
 */
class HomePageAnchorTest {

    @Test fun `dashboard round-trip`() {
        val serialized = HomePageAnchor.serialize(HomePageAnchor.Dashboard)
        assertEquals("dashboard", serialized)
        assertEquals(HomePageAnchor.Dashboard, HomePageAnchor.deserialize(serialized))
    }

    @Test fun `folder round-trip`() {
        val anchor = HomePageAnchor.Folder("productivity")
        val serialized = HomePageAnchor.serialize(anchor)
        assertEquals("folder:productivity", serialized)
        assertEquals(anchor, HomePageAnchor.deserialize(serialized))
    }

    @Test fun `page index round-trip`() {
        val anchor = HomePageAnchor.PageIndex(4)
        val serialized = HomePageAnchor.serialize(anchor)
        assertEquals("index:4", serialized)
        assertEquals(anchor, HomePageAnchor.deserialize(serialized))
    }

    @Test fun `null blank and unknown values deserialize to null`() {
        assertNull(HomePageAnchor.deserialize(null))
        assertNull(HomePageAnchor.deserialize(""))
        assertNull(HomePageAnchor.deserialize("   "))
        assertNull(HomePageAnchor.deserialize("garbage"))
        assertNull(HomePageAnchor.deserialize("folder:"))
        assertNull(HomePageAnchor.deserialize("index:not_a_number"))
    }

    @Test fun `folder categoryId with colon is preserved`() {
        // categoryId'ler pratikte ':' icermez ama sag tarafi tumu id kabul edilmeli (savunma testi).
        val anchor = HomePageAnchor.Folder("custom:sub")
        val serialized = HomePageAnchor.serialize(anchor)
        assertEquals(anchor, HomePageAnchor.deserialize(serialized))
    }
}
