package com.armutlu.apporganizer.launcher

import com.armutlu.apporganizer.presentation.ui.launcher.FolderTransitionMode
import com.armutlu.apporganizer.presentation.ui.launcher.buildFolderTransitionFrame
import com.armutlu.apporganizer.presentation.ui.launcher.computeFolderSettleTarget
import com.armutlu.apporganizer.presentation.ui.launcher.mapFolderTransitionDirectionToNextFlag
import com.armutlu.apporganizer.presentation.ui.launcher.resolveFolderTransitionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderTransitionStateTest {

    @Test
    fun `transition mode legacy values migrate to new defaults`() {
        assertEquals(FolderTransitionMode.ANDROID_SMOOTH, resolveFolderTransitionMode("page_turn"))
        assertEquals(FolderTransitionMode.ANDROID_SMOOTH, resolveFolderTransitionMode("slide_parallax"))
        assertEquals(FolderTransitionMode.IOS_ZOOM_FADE, resolveFolderTransitionMode("zoom_fade"))
    }

    @Test
    fun `settle target does not commit before threshold or velocity`() {
        val result = computeFolderSettleTarget(
            dragOffsetPx = 40f,
            velocityPxPerSecond = 120f,
            settleDistancePx = 100f,
            velocityThresholdPxPerSecond = 600f,
        )

        assertEquals(0, result.commitDirection)
        assertEquals(0f, result.settleToOffsetPx)
    }

    @Test
    fun `settle target commits after threshold in swipe direction`() {
        val result = computeFolderSettleTarget(
            dragOffsetPx = -130f,
            velocityPxPerSecond = 80f,
            settleDistancePx = 100f,
            velocityThresholdPxPerSecond = 600f,
        )

        assertEquals(-1, result.commitDirection)
        assertEquals(-100f, result.settleToOffsetPx)
    }

    @Test
    fun `positive drag direction opens previous folder and negative opens next`() {
        assertFalse(mapFolderTransitionDirectionToNextFlag(1))
        assertTrue(mapFolderTransitionDirectionToNextFlag(-1))
    }

    @Test
    fun `settle target commits for fast fling even if distance is short`() {
        val result = computeFolderSettleTarget(
            dragOffsetPx = 20f,
            velocityPxPerSecond = 920f,
            settleDistancePx = 100f,
            velocityThresholdPxPerSecond = 600f,
        )

        assertEquals(1, result.commitDirection)
        assertEquals(100f, result.settleToOffsetPx)
    }

    @Test
    fun `reduce motion frame removes scale and strong translation`() {
        val frame = buildFolderTransitionFrame(
            mode = FolderTransitionMode.IOS_ZOOM_FADE,
            rawOffsetPx = 50f,
            settleDistancePx = 100f,
            reduceMotionEnabled = true,
        )

        assertEquals(1f, frame.currentScale)
        assertEquals(1f, frame.previewScale)
        assertTrue(frame.translationX < 20f)
        assertTrue(frame.currentAlpha > 0.9f)
    }

    @Test
    fun `ios zoom fade frame scales current and preview content`() {
        val frame = buildFolderTransitionFrame(
            mode = FolderTransitionMode.IOS_ZOOM_FADE,
            rawOffsetPx = -70f,
            settleDistancePx = 100f,
            reduceMotionEnabled = false,
        )

        assertEquals(-1, frame.direction)
        assertTrue(frame.currentScale < 1f)
        assertTrue(frame.previewScale > 0.94f)
        assertTrue(frame.previewAlpha > 0.24f)
    }
}
