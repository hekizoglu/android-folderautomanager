package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.utils.InsightCard
import com.armutlu.apporganizer.utils.InsightType

@Composable
fun AssistantInsightRow(
    cards: List<InsightCard>,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.forEach { card ->
            InsightChip(card = card, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InsightChip(card: InsightCard, modifier: Modifier = Modifier) {
    val (icon, tint) = when (card.type) {
        InsightType.MORNING_HABIT        -> Icons.Default.Star to Color(0xFFFFD54F)
        InsightType.UNREAD_NOTIFICATIONS -> Icons.Default.Notifications to Color(0xFF4FC3F7)
        InsightType.UNUSED_APPS          -> Icons.Default.Warning to Color(0xFFFFB74D)
        InsightType.TOP_IN_FOLDER        -> Icons.Default.Info to Color(0xFF80CBC4)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = card.message,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
