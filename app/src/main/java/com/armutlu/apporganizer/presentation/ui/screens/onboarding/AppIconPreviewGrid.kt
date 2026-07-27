package com.armutlu.apporganizer.presentation.ui.screens.onboarding

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Onboarding kartları içinde kullanılmak üzere tasarlanmış sade, 2x2 performanslı uygulama ikon grid'i.
 */
@Composable
fun AppIconPreviewGrid(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    iconSizeDp: Dp = 26.dp,
    fallbackEmoji: String = "📁"
) {
    val displayApps = apps.take(4)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        if (displayApps.isEmpty()) {
            Text(text = fallbackEmoji, fontSize = 20.sp)
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PreviewIconItem(displayApps.getOrNull(0), iconSizeDp)
                    PreviewIconItem(displayApps.getOrNull(1), iconSizeDp)
                }
                if (displayApps.size > 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PreviewIconItem(displayApps.getOrNull(2), iconSizeDp)
                        PreviewIconItem(displayApps.getOrNull(3), iconSizeDp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewIconItem(app: AppInfo?, iconSizeDp: Dp) {
    if (app == null) {
        Box(
            modifier = Modifier
                .size(iconSizeDp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        return
    }

    val context = LocalContext.current
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = app.packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                context.packageManager.getApplicationIcon(app.packageName).toBitmap(48, 48).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = app.appName,
            modifier = Modifier
                .size(iconSizeDp)
                .clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(iconSizeDp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }
}
