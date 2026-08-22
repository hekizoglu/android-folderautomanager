package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home V2 saat başlığı: büyük saat, tam Türkçe tarih ve (veri varsa) nabız/görev şeridi.
 * 30 saniyede bir kendini tazeler; biçimlendirme ThreadLocal ile thread-safe'tir.
 */
@Composable
internal fun ClockHeaderV2(
    pulse: PulseStripState?,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }
    val timeFmt = remember { ThreadLocal.withInitial { SimpleDateFormat("HH:mm", Locale("tr")) } }
    val dateFmt = remember { ThreadLocal.withInitial { SimpleDateFormat("d MMMM EEEE", Locale("tr")) } }
    val date = remember(now) { Date(now) }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = timeFmt.get()!!.format(date),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = dateFmt.get()!!.format(date),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (pulse != null) {
            Spacer(Modifier.height(10.dp))
            PulseStripV2(pulse)
        }
    }
}

@Composable
private fun PulseStripV2(pulse: PulseStripState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pulse.pulseScoreText != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("💠", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Nabız ${pulse.pulseScoreText}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (pulse.missionTitle != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (pulse.missionStreak >= 2) {
                        Text("🔥", fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = pulse.missionTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    val fraction = pulse.missionProgressFraction
                    if (fraction != null) {
                        Spacer(Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { fraction.coerceIn(0f, 1f) },
                            modifier = Modifier.width(48.dp).height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
