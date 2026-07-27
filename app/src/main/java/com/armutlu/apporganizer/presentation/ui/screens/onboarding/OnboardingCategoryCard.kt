package com.armutlu.apporganizer.presentation.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R

/**
 * Modern, pastel tonlu, erişilebilir Onboarding Kategori Kartı.
 */
@Composable
fun OnboardingCategoryCard(
    model: OnboardingCategoryCardUiModel,
    modifier: Modifier = Modifier
) {
    val categoryColor = parseCategoryColor(model.colorHex)
    // Açık ve koyu temalarda okunabilir yumuşak pastel arka plan tonu (%12 alfa)
    val cardBackground = categoryColor.copy(alpha = 0.14f)
    val cardBorderColor = categoryColor.copy(alpha = 0.30f)

    val accessibilityLabel = "${model.title}, ${model.appCount} uygulama" +
        if (model.pendingCount > 0) ", ${model.pendingCount} uygulama onay bekliyor" else ""

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Başlık & İkon & Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = model.iconEmoji, fontSize = 18.sp)
                    Text(
                        text = model.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Uygulama Sayısı Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${model.appCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Kategori Açıklaması
            Text(
                text = model.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Alt Kısım: İkon Grid'i ve Varsa Düşük Güvenlik Uyarısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                AppIconPreviewGrid(
                    apps = model.previewApps,
                    fallbackEmoji = model.iconEmoji,
                    iconSizeDp = 24.dp
                )

                if (model.pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${model.pendingCount}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseCategoryColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = when (cleaned.length) {
            6 -> android.graphics.Color.parseColor("#FF$cleaned")
            8 -> android.graphics.Color.parseColor("#$cleaned")
            else -> android.graphics.Color.parseColor("#6C63FF")
        }
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFF6C63FF)
    }
}
