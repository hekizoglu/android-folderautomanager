package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import kotlinx.coroutines.delay

/**
 * Ana ekran haber şeridi maddesi.
 * @param categoryId dolu ise dokununca ilgili klasör açılır
 * @param route dolu ise dokununca MainActivity içinde ilgili rota açılır (Dashboard, Bildirim Raporu...)
 */
data class TickerItem(
    val text: String,
    val emoji: String = "📰",
    val categoryId: String? = null,
    val route: String? = null,
    val packageName: String? = null,
) {
    /** Dismiss/rotasyon takibi için kararlı kimlik — aynı haber tekrar tekrar dönmesin (D226). */
    val key: String get() = "${packageName ?: ""}|${categoryId ?: ""}|${route ?: ""}|$text"
}

/**
 * Etkileşimli haber şeridi — haber bülteni tarzı:
 * - Her seferinde 1 haber; ~6 sn'de bir otomatik sonrakine geçer (slide animasyonu)
 * - Dokunma → haberin hedefi açılır (klasör / rapor / dashboard)
 * - Yatay kaydırma → önceki/sonraki haber
 * - Uzun metin `basicMarquee` ile kayar
 * Animasyon state'i bu composable'ın içinde kalır — HomeScreen recompose olmaz.
 */
@Composable
internal fun HomeTickerRow(
    items: List<TickerItem>,
    onItemClick: (TickerItem) -> Unit,
    modifier: Modifier = Modifier,
    onMute: ((durationMillis: Long) -> Unit)? = null,
) {
    if (items.isEmpty()) return
    var index by remember(items.size) { mutableStateOf(0) }
    var direction by remember { mutableStateOf(1) } // 1 = ileri, -1 = geri
    // Basili tut -> sessize alma menusu (8 saat / 1 gun / 7 gun) (D233)
    var muteMenuOpen by remember { mutableStateOf(false) }
    val current = items[index.coerceIn(0, items.lastIndex)]

    // Otomatik ilerleme — index veya liste değişince sayaç sıfırlanır
    LaunchedEffect(index, items.size) {
        delay(6_000)
        direction = 1
        index = (index + 1) % items.size
    }

    // "CANLI" noktası — yumuşak nabız animasyonu
    val pulse by rememberInfiniteTransition(label = "ticker_pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ticker_pulse_alpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .border(0.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
            .pointerInputTicker(
                onTap = { onItemClick(current) },
                onLongPress = { if (onMute != null) muteMenuOpen = true },
                onSwipe = { forward ->
                    direction = if (forward) 1 else -1
                    index = if (forward) (index + 1) % items.size
                            else (index - 1 + items.size) % items.size
                }
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(pulse)
                .background(Color(0xFF26C6DA), CircleShape)
        )
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                (slideInHorizontally { it * direction } + fadeIn(tween(200)))
                    .togetherWith(slideOutHorizontally { -it * direction } + fadeOut(tween(150)))
            },
            label = "ticker_content",
            modifier = Modifier.weight(1f)
        ) { i ->
            val item = items[i.coerceIn(0, items.lastIndex)]
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.emoji, fontSize = 12.sp)
                Text(
                    text = item.text,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 36.dp)
                )
            }
        }
        Text(
            text = "${index + 1}/${items.size}",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 10.sp
        )

        // Sessize alma menusu — basili tutunca acilir; secilen sure kadar serit gizlenir (D233)
        if (onMute != null) {
            DropdownMenu(expanded = muteMenuOpen, onDismissRequest = { muteMenuOpen = false }) {
                listOf(
                    stringResource(R.string.ticker_mute_8h) to 8L * 60 * 60 * 1000,
                    stringResource(R.string.ticker_mute_1d) to 24L * 60 * 60 * 1000,
                    stringResource(R.string.ticker_mute_7d) to 7L * 24 * 60 * 60 * 1000,
                ).forEach { (label, duration) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            muteMenuOpen = false
                            onMute(duration)
                        }
                    )
                }
            }
        }
    }
}

/** Tap + basili tutma + yatay swipe'ı tek modifier'da birleştirir — grid gesture'larıyla çakışmaz. */
private fun Modifier.pointerInputTicker(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipe: (forward: Boolean) -> Unit,
): Modifier = this
    .pointerInput("ticker_tap") {
        detectTapGestures(onTap = { onTap() }, onLongPress = { onLongPress() })
    }
    .pointerInput("ticker_swipe") {
        var accumulated = 0f
        detectHorizontalDragGestures(
            onDragStart = { accumulated = 0f },
            onDragEnd = {
                // Sola kaydırma = sonraki haber (haber şeridi alışkanlığı), sağa = önceki
                if (accumulated < -48f) onSwipe(true)
                else if (accumulated > 48f) onSwipe(false)
                accumulated = 0f
            },
            onDragCancel = { accumulated = 0f },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                accumulated += dragAmount
            }
        )
    }
