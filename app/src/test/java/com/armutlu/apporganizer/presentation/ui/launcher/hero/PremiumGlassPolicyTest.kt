package com.armutlu.apporganizer.presentation.ui.launcher.hero

import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumGlassPolicyTest {
    @Test fun `active yuzey standard yuzeyden daha belirgindir`() {
        val active = PremiumGlassPolicy.palette(PremiumGlassEmphasis.ACTIVE)
        val standard = PremiumGlassPolicy.palette(PremiumGlassEmphasis.STANDARD)
        assertTrue(active.fillAlpha > standard.fillAlpha)
        assertTrue(active.borderAlpha > standard.borderAlpha)
    }

    @Test fun `subtle yuzey standard yuzeyden daha hafiftir`() {
        val subtle = PremiumGlassPolicy.palette(PremiumGlassEmphasis.SUBTLE)
        val standard = PremiumGlassPolicy.palette(PremiumGlassEmphasis.STANDARD)
        assertTrue(subtle.fillAlpha < standard.fillAlpha)
        assertTrue(subtle.borderAlpha < standard.borderAlpha)
    }

    @Test fun `tum alpha degerleri gecerli araliktadir`() {
        PremiumGlassEmphasis.entries.forEach { emphasis ->
            val palette = PremiumGlassPolicy.palette(emphasis)
            listOf(
                palette.fillAlpha,
                palette.coolLayerAlpha,
                palette.borderAlpha,
                palette.highlightAlpha,
            ).forEach { assertTrue(it in 0f..1f) }
        }
    }
}
