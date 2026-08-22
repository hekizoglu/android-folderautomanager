package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditingCenterCard(
    state: EditingCenterState,
    onNavigateToClassificationReview: () -> Unit = {},
    onNavigateToFolderMerge: () -> Unit = {},
    onNavigateToAppCorrections: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToStaleApps: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!state.hasAnyAlert) {
        return // Hiç uyarı yoksa kartı gösterme
    }

    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBgColor)
            .padding(16.dp),
    ) {
        // Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.width(20.dp),
            )
            Text(
                text = "Düzenleme Merkezi",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            // Toplam uyarı sayısı badge
            if (state.totalAlerts > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFFF6B6B),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${state.totalAlerts}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Uyarı Listesi
        if (state.pendingClassificationCount > 0) {
            EditingCenterAlertItem(
                icon = "📋",
                title = "Sınıflandırma Onayı",
                count = state.pendingClassificationCount,
                onClick = onNavigateToClassificationReview,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.folderMergeCandidates > 0) {
            EditingCenterAlertItem(
                icon = "🔗",
                title = "Klasör Birleşimi",
                count = state.folderMergeCandidates,
                onClick = onNavigateToFolderMerge,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.appCorrectionsCount > 0) {
            EditingCenterAlertItem(
                icon = "✏️",
                title = "Yanlış Konumlandırma",
                count = state.appCorrectionsCount,
                onClick = onNavigateToAppCorrections,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.missingPermissionsCount > 0) {
            EditingCenterAlertItem(
                icon = "🔐",
                title = "Eksik İzinler",
                count = state.missingPermissionsCount,
                onClick = onNavigateToPermissions,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.staleAppsCount > 0) {
            EditingCenterAlertItem(
                icon = "⏰",
                title = "Kullanılmayan Uygulamalar",
                count = state.staleAppsCount,
                onClick = onNavigateToStaleApps,
            )
        }
    }
}

@Composable
private fun EditingCenterAlertItem(
    icon: String,
    title: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
