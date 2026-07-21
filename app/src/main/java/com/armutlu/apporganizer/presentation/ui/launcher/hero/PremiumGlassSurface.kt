package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class PremiumGlassEmphasis { STANDARD, ACTIVE, SUBTLE }

internal data class PremiumGlassPalette(
    val fillAlpha: Float,
    val coolLayerAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float,
)

internal object PremiumGlassPolicy {
    fun palette(emphasis: PremiumGlassEmphasis): PremiumGlassPalette = when (emphasis) {
        PremiumGlassEmphasis.STANDARD -> PremiumGlassPalette(.065f, .035f, .28f, .10f)
        PremiumGlassEmphasis.ACTIVE -> PremiumGlassPalette(.095f, .07f, .52f, .16f)
        PremiumGlassEmphasis.SUBTLE -> PremiumGlassPalette(.045f, .025f, .18f, .07f)
    }
}

/**
 * Hero yüzeylerine özel API 26 uyumlu cam katmanı. Global GlassCard değiştirilmez. Gerçek
 * RenderEffect blur kullanılmadığı için API 26-30'da görsel/performans davranışı ayrışmaz.
 */
@Composable
internal fun PremiumGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = HomeHeroTokens.CardCorner,
    emphasis: PremiumGlassEmphasis = PremiumGlassEmphasis.STANDARD,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = PremiumGlassPolicy.palette(emphasis)
    val shape = RoundedCornerShape(cornerRadius)
    val fill = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = palette.fillAlpha + palette.highlightAlpha),
            Color(0xFF7CB7FF).copy(alpha = palette.coolLayerAlpha),
            Color.White.copy(alpha = palette.fillAlpha),
        )
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, Color.White.copy(alpha = palette.borderAlpha), shape),
        content = content,
    )
}
