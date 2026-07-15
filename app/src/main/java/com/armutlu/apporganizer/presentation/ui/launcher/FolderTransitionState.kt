package com.armutlu.apporganizer.presentation.ui.launcher

import kotlin.math.abs

internal enum class FolderTransitionMode {
    ANDROID_SMOOTH,
    IOS_ZOOM_FADE,
}

internal data class FolderDragSettleTarget(
    val commitDirection: Int,
    val settleToOffsetPx: Float,
)

internal fun mapFolderTransitionDirectionToNextFlag(direction: Int): Boolean = direction < 0

internal data class FolderTransitionFrame(
    val mode: FolderTransitionMode,
    val progress: Float,
    val direction: Int,
    val translationX: Float,
    val currentAlpha: Float,
    val currentScale: Float,
    val currentRotationY: Float,
    val previewAlpha: Float,
    val previewScale: Float,
    val previewTranslationX: Float,
)

internal fun resolveFolderTransitionMode(prefValue: String?): FolderTransitionMode = when (prefValue) {
    "ios_zoom_fade", "zoom_fade" -> FolderTransitionMode.IOS_ZOOM_FADE
    else -> FolderTransitionMode.ANDROID_SMOOTH
}

internal fun shouldCommitFolderTransition(
    dragOffsetPx: Float,
    velocityPxPerSecond: Float,
    settleDistancePx: Float,
    velocityThresholdPxPerSecond: Float,
): Int {
    if (settleDistancePx <= 0f) return 0
    if (abs(dragOffsetPx) >= settleDistancePx) {
        return if (dragOffsetPx > 0f) 1 else -1
    }
    if (abs(velocityPxPerSecond) >= velocityThresholdPxPerSecond) {
        return if (velocityPxPerSecond > 0f) 1 else -1
    }
    return 0
}

internal fun computeFolderSettleTarget(
    dragOffsetPx: Float,
    velocityPxPerSecond: Float,
    settleDistancePx: Float,
    velocityThresholdPxPerSecond: Float,
): FolderDragSettleTarget {
    val direction = shouldCommitFolderTransition(
        dragOffsetPx = dragOffsetPx,
        velocityPxPerSecond = velocityPxPerSecond,
        settleDistancePx = settleDistancePx,
        velocityThresholdPxPerSecond = velocityThresholdPxPerSecond,
    )
    val settleTo = if (direction == 0) 0f else settleDistancePx * direction
    return FolderDragSettleTarget(commitDirection = direction, settleToOffsetPx = settleTo)
}

internal fun buildFolderTransitionFrame(
    mode: FolderTransitionMode,
    rawOffsetPx: Float,
    settleDistancePx: Float,
    reduceMotionEnabled: Boolean,
): FolderTransitionFrame {
    val safeDistance = settleDistancePx.coerceAtLeast(1f)
    val clampedOffset = rawOffsetPx.coerceIn(-safeDistance, safeDistance)
    val signedProgress = (clampedOffset / safeDistance).coerceIn(-1f, 1f)
    val progress = abs(signedProgress)
    val direction = when {
        signedProgress > 0f -> 1
        signedProgress < 0f -> -1
        else -> 0
    }
    if (reduceMotionEnabled) {
        return FolderTransitionFrame(
            mode = mode,
            progress = progress,
            direction = direction,
            translationX = clampedOffset * 0.2f,
            currentAlpha = 1f - progress * 0.08f,
            currentScale = 1f,
            currentRotationY = 0f,
            previewAlpha = progress * 0.35f,
            previewScale = 1f,
            previewTranslationX = if (direction == 0) 0f else -direction * (1f - progress) * safeDistance * 0.08f,
        )
    }
    return when (mode) {
        FolderTransitionMode.ANDROID_SMOOTH -> FolderTransitionFrame(
            mode = mode,
            progress = progress,
            direction = direction,
            translationX = clampedOffset,
            currentAlpha = 1f - progress * 0.1f,
            currentScale = 1f,
            currentRotationY = 0f,
            previewAlpha = (0.18f + progress * 0.82f).coerceIn(0f, 1f),
            previewScale = 1f,
            previewTranslationX = if (direction == 0) 0f else -direction * (1f - progress) * safeDistance * 0.22f,
        )
        FolderTransitionMode.IOS_ZOOM_FADE -> FolderTransitionFrame(
            mode = mode,
            progress = progress,
            direction = direction,
            translationX = clampedOffset * 0.92f,
            currentAlpha = 1f - progress * 0.3f,
            currentScale = 1f - progress * 0.08f,
            currentRotationY = 0f,
            previewAlpha = (0.24f + progress * 0.76f).coerceIn(0f, 1f),
            previewScale = 0.94f + progress * 0.06f,
            previewTranslationX = if (direction == 0) 0f else -direction * (1f - progress) * safeDistance * 0.12f,
        )
    }
}
