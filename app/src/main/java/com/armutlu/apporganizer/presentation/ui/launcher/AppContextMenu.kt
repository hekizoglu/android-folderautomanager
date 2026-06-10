package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SheetBg      = Color(0xFF1A1A2A)
private val TealColor    = Color(0xFF00897B)
private val DangerColor  = Color(0xFFE53935)
private val TextPrimary  = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.55f)
private val DividerColor = Color.White.copy(alpha = 0.08f)
private val RowHover     = Color.White.copy(alpha = 0.08f)

private val dateFmt = SimpleDateFormat("d MMM yyyy", Locale("tr"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContextMenu(
    app: AppInfo,
    isDocked: Boolean,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onAddToDock: () -> Unit,
    onRemoveFromDock: () -> Unit,
    onChangeCategory: () -> Unit,
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    val icon = produceState<android.graphics.drawable.Drawable?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull()
        }
    }.value

    // APK versiyonu
    val version = remember(app.packageName) {
        runCatching {
            context.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
        ) {
            // ── Uygulama başlığı ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    androidx.compose.foundation.Image(
                        painter = rememberDrawablePainter(icon),
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(TealColor.copy(0.3f)),
                        contentAlignment = Alignment.Center) {
                        Text(app.appName.take(1), fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.appName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    Text("v$version", fontSize = 12.sp, color = TextSecondary)
                }
            }

            // ── Bilgi satırları ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    label = "Boyut",
                    value = if (app.appSizeBytes > 0) formatBytes(app.appSizeBytes) else "—",
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label = "Yükleme",
                    value = if (app.installTime > 0) dateFmt.format(Date(app.installTime)) else "—",
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label = "Kullanım",
                    value = if (app.usageCount > 0) "${app.usageCount}×" else "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            // ── Eylemler ──────────────────────────────────────────────────────
            ContextAction(
                icon = Icons.Default.OpenInNew,
                label = "Uygulamayı Aç",
                color = TealColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunch()
                    onDismiss()
                }
            )

            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            if (isDocked) {
                ContextAction(
                    icon = Icons.Default.DesktopAccessDisabled,
                    label = "Dock'tan Kaldır",
                    onClick = { onRemoveFromDock(); onDismiss() }
                )
            } else {
                ContextAction(
                    icon = Icons.Default.AddToHomeScreen,
                    label = "Dock'a Ekle",
                    onClick = { onAddToDock(); onDismiss() }
                )
            }

            ContextAction(
                icon = Icons.Default.Category,
                label = "Kategori Değiştir",
                onClick = { onChangeCategory(); onDismiss() }
            )

            Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            ContextAction(
                icon = Icons.Default.Info,
                label = "Uygulama Bilgisi",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    onDismiss()
                }
            )

            if (!app.isSystemApp) {
                ContextAction(
                    icon = Icons.Default.Delete,
                    label = "Kaldır",
                    color = DangerColor,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )
            }
        }
    }
}

// ── Yardımcı composable'lar ───────────────────────────────────────────────────

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(0.07f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
        Text(label, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ContextAction(
    icon: ImageVector,
    label: String,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 15.sp, color = color)
    }
}
