package com.armutlu.apporganizer.presentation.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay

/**
 * Elmas parlaması — arama çubuğu üzerinde ~900 ms'lik çapraz beyaz gradient süpürmesi.
 * [trigger] değiştiğinde (ör. ana ekrana her gelişte artan bir sayaç) BİR KEZ oynar —
 * sürekli tekrarlayan zamanlayıcı değildir (D210: "15 saniyede bir" rahatsız edici tekrar kaldırıldı).
 *
 * Animasyon state'i bu modifier'ın içinde yaşar; yalnızca süpürme sırasında
 * çizim yapılır (progress 0..1 dışında hiçbir ek maliyet yok).
 */
fun Modifier.diamondShine(enabled: Boolean, shape: Shape, trigger: Any = Unit): Modifier = composed {
    if (!enabled) return@composed this

    val progress = remember { Animatable(-1f) }
    LaunchedEffect(trigger) {
        delay(600)
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 900))
        progress.snapTo(-1f)
    }

    this
        .clip(shape)
        .drawWithContent {
            drawContent()
            val p = progress.value
            if (p in 0f..1f) {
                val w = size.width
                val h = size.height
                val band = w * 0.22f
                // Süpürme çapraz ilerler: sol-dışından sağ-dışına
                val x = -band + (w + band * 2) * p
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.32f),
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        start = Offset(x, 0f),
                        end = Offset(x + band, h)
                    )
                )
            }
        }
}
