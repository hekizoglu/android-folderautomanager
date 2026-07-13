package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism kart — blur simülasyonu: yarı saydam beyaz arka plan + ince beyaz border.
 * Gerçek blur (RenderEffect) API 31+ gerektirir; bu composable API 21+'da çalışır.
 */
@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundAlpha: Float = 0.15f,
    borderAlpha: Float = 0.30f,
    borderColor: Color = Color.White,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = backgroundAlpha))
            .border(1.dp, borderColor.copy(alpha = borderAlpha), shape),
        content = content
    )
}
