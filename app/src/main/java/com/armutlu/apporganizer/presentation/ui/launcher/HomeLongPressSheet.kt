package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLongPressSheet(
    onDismiss: () -> Unit,
    onWallpaper: () -> Unit,
    onSettings: () -> Unit,
    onDockEdit: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1A1A2A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(0.2f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "Ana Ekran",
                color = Color.White.copy(0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))

            HomeLongPressAction(
                icon = Icons.Default.Image,
                label = "Duvar Kağıdı",
                subtitle = "Arka plan resmini değiştir",
                onClick = onWallpaper
            )
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))

            HomeLongPressAction(
                icon = Icons.Default.ViewModule,
                label = "Dock Düzenle",
                subtitle = "Alt kısımdaki uygulamaları seç",
                onClick = onDockEdit
            )
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(0.08f)))

            HomeLongPressAction(
                icon = Icons.Default.Settings,
                label = "Launcher Ayarları",
                subtitle = "Görünüm, bildirim ve daha fazlası",
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun HomeLongPressAction(
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00897B).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
        }
        Column {
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.White.copy(0.5f), fontSize = 12.sp)
        }
    }
}
