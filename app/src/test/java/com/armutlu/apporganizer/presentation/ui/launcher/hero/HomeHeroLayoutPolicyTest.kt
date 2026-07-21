package com.armutlu.apporganizer.presentation.ui.launcher.hero

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeHeroLayoutPolicyTest {
    @Test fun `360x640 referans telefon olculerini korur`() {
        val spec = HomeHeroLayoutPolicy.resolve(360, 640, 1f)
        assertEquals(HomeHeroProfile.PHONE, spec.profile)
        assertEquals(304, spec.contentMaxWidthDp)
        assertEquals(114, spec.clockHeightDp)
        assertEquals(5, spec.appSlots)
    }

    @Test fun `320x568 kompakt profil tasma riskini azaltir`() {
        val spec = HomeHeroLayoutPolicy.resolve(320, 568, 1f)
        assertEquals(HomeHeroProfile.COMPACT_PHONE, spec.profile)
        assertTrue(spec.contentMaxWidthDp <= 288)
        assertTrue(spec.scrollEnabled)
    }

    @Test fun `412x915 buyuk telefon gereksiz buyumez`() {
        val spec = HomeHeroLayoutPolicy.resolve(412, 915, 1f)
        assertEquals(HomeHeroProfile.LARGE_PHONE, spec.profile)
        assertTrue(spec.contentMaxWidthDp < 412)
        assertEquals(5, spec.appSlots)
    }

    @Test fun `tablet icerigi merkezlenebilir tavanda tutar`() {
        val spec = HomeHeroLayoutPolicy.resolve(800, 1280, 1f)
        assertEquals(HomeHeroProfile.TABLET, spec.profile)
        assertEquals(420, spec.contentMaxWidthDp)
        assertTrue(spec.contentMaxWidthDp < 800)
    }

    @Test fun `landscape genis ama kaydirilabilir profil kullanir`() {
        val spec = HomeHeroLayoutPolicy.resolve(915, 412, 1f)
        assertEquals(HomeHeroProfile.LANDSCAPE, spec.profile)
        assertTrue(spec.scrollEnabled)
        assertTrue(spec.contentMaxWidthDp <= 720)
    }

    @Test fun `font scale 1_5 erisilebilir profile gecer`() {
        val spec = HomeHeroLayoutPolicy.resolve(360, 640, 1.5f)
        assertEquals(HomeHeroProfile.ACCESSIBLE, spec.profile)
        assertTrue(spec.smartAccessHeightDp > 162)
        assertTrue(spec.scrollEnabled)
    }

    @Test fun `tum profiller akilli erisim icerigini kirpmaz`() {
        val dimensions = listOf(
            Triple(320, 568, 1f),
            Triple(360, 640, 1f),
            Triple(412, 915, 1f),
            Triple(800, 1280, 1f),
            Triple(915, 412, 1f),
            Triple(360, 640, 1.5f),
        )
        dimensions.forEach { (width, height, fontScale) ->
            val spec = HomeHeroLayoutPolicy.resolve(width, height, fontScale)
            assertTrue("${spec.profile} içerik ekranı aşmamalı", spec.contentMaxWidthDp <= width)
            assertTrue("${spec.profile} smart access yüksekliği", spec.smartAccessHeightDp >= 162)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `gecersiz ekran olcusu reddedilir`() {
        HomeHeroLayoutPolicy.resolve(0, 640, 1f)
    }
}
