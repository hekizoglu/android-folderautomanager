package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.utils.InsightCard
import com.armutlu.apporganizer.utils.InsightType

@Composable
fun AssistantInsightRow(
    cards: List<InsightCard>,
    modifier: Modifier = Modifier,
    onCardClick: (InsightCard) -> Unit = {},
) {
    if (cards.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        cards.forEach { card ->
            InsightChip(
                card = card,
                modifier = Modifier.weight(1f),
                onClick = { onCardClick(card) },
            )
        }
    }
}

@Composable
private fun InsightChip(
    card: InsightCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val (icon, tint) = card.type.iconAndTint()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = card.message,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun InsightType.iconAndTint(): Pair<ImageVector, Color> = when (this) {
    InsightType.MORNING_HABIT        -> Icons.Default.Star         to Color(0xFFFFD54F)
    InsightType.UNREAD_NOTIFICATIONS -> Icons.Default.Notifications to Color(0xFF4FC3F7)
    InsightType.UNUSED_APPS          -> Icons.Default.Warning       to Color(0xFFFFB74D)
    InsightType.TOP_IN_FOLDER        -> Icons.Default.Info          to Color(0xFF80CBC4)
    InsightType.NEVER_OPENED         -> Icons.Default.QuestionMark  to Color(0xFFCE93D8)
    InsightType.NEW_INSTALL          -> Icons.Default.NewReleases   to Color(0xFF80DEEA)
    InsightType.LARGE_APP            -> Icons.Default.Storage       to Color(0xFFEF9A9A)
    InsightType.CATEGORY_SUMMARY     -> Icons.Default.AutoAwesome   to Color(0xFFA5D6A7)
    InsightType.MOTIVATIONAL         -> Icons.Default.Star          to Color(0xFFFFCC02)
    InsightType.LONG_UNUSED          -> Icons.Default.Delete        to Color(0xFFFF8A65)
}
