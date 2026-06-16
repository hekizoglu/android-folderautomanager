package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.ShortcutHelper
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onHideApp: ((Boolean) -> Unit)? = null,
    onSaveNote: ((String) -> Unit)? = null,
    onToggleFavorite: ((isFavNow: Boolean) -> Unit)? = null,
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    var showNoteDialog by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(com.armutlu.apporganizer.utils.AppPrefs.isFavorite(context, app.packageName)) }

    val icon by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(app.packageName).toBitmap(128, 128).asImageBitmap()
            }.getOrNull()
        }
    }

    // Uygulama kısayolları — LauncherApps API (API 25+, launcher rolü gerekir)
    val shortcuts by produceState<List<ShortcutInfo>>(emptyList(), app.packageName) {
        value = withContext(Dispatchers.IO) {
            ShortcutHelper.getShortcuts(context, app.packageName).take(4)
        }
    }

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
                icon?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    )
                } ?: run {
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
                    value = formatUsageTime(app.usageCount),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Kısayollar ────────────────────────────────────────────────────
            if (shortcuts.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Kısayollar",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    shortcuts.forEach { shortcut ->
                        ShortcutItem(
                            shortcut = shortcut,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                ShortcutHelper.launchShortcut(context, shortcut)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            // ── Eylemler ──────────────────────────────────────────────────────
            ContextAction(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                label = "Uygulamayı Aç",
                color = TealColor,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLaunch()
                    onDismiss()
                }
            )

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            if (isDocked) {
                ContextAction(
                    icon = Icons.Default.DesktopAccessDisabled,
                    label = "Dock'tan Kaldır",
                    onClick = { onRemoveFromDock(); onDismiss() }
                )
            } else {
                ContextAction(
                    icon = Icons.AutoMirrored.Filled.AddToHomeScreen,
                    label = "Dock'a Ekle",
                    onClick = { onAddToDock(); onDismiss() }
                )
            }

            ContextAction(
                icon = Icons.Default.Category,
                label = "Kategori Değiştir",
                onClick = { onChangeCategory(); onDismiss() }
            )

            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

            ContextAction(
                icon = Icons.Default.Info,
                label = "Uygulama Bilgisi",
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                    onDismiss()
                }
            )

            ContextAction(
                icon = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                label = if (isFav) "Favorilerden Çıkar" else "Favorilere Ekle",
                onClick = {
                    val newFav = !isFav
                    isFav = newFav
                    onToggleFavorite?.invoke(newFav)
                    onDismiss()
                }
            )

            onHideApp?.let { hideCallback ->
                ContextAction(
                    icon = if (app.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = if (app.isHidden) "Gizlemeyi Kaldır" else "Gizle",
                    onClick = { hideCallback(!app.isHidden); onDismiss() }
                )
            }

            onSaveNote?.let {
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                // Mevcut not varsa göster
                if (app.customNotes.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.StickyNote2,
                            null,
                            tint = TealColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = app.customNotes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ContextAction(
                    icon = Icons.Default.EditNote,
                    label = if (app.customNotes.isBlank()) "Not Ekle" else "Notu Düzenle",
                    color = TealColor,
                    onClick = { showNoteDialog = true }
                )
            }

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
                        runCatching { context.startActivity(intent) }
                        onDismiss()
                    }
                )
            }
        }
    }

    // Not düzenleme dialogu
    if (showNoteDialog) {
        AppNoteDialog(
            initialNote = app.customNotes,
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                onSaveNote?.invoke(note)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun AppNoteDialog(
    initialNote: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2A),
        title = {
            Text("Uygulama Notu", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Not girin...", color = Color.White.copy(0.4f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = TealColor,
                    unfocusedBorderColor = Color.White.copy(0.3f),
                    cursorColor = TealColor
                ),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                singleLine = false
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(noteText) }) {
                Text("Kaydet", color = TealColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = Color.White.copy(0.6f))
            }
        }
    )
}

// usageCount = toplam ön plan süresi (ms) — 30 günlük
private fun formatUsageTime(ms: Long): String = when {
    ms <= 0L          -> "—"
    ms < 60_000L      -> "${ms / 1000} sn"
    ms < 3_600_000L   -> "${ms / 60_000} dk"
    ms < 86_400_000L  -> "${"%.1f".format(ms / 3_600_000.0)} sa"
    else              -> "${ms / 86_400_000} gün"
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

@Composable
private fun ShortcutItem(shortcut: ShortcutInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val iconBmp by produceState<ImageBitmap?>(null, shortcut.id) {
        value = withContext(Dispatchers.IO) {
            ShortcutHelper.getShortcutIcon(context, shortcut, 96)
        }
    }
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            iconBmp?.let { bmp ->
                Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(32.dp))
            } ?: Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = TealColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "",
            fontSize = 10.sp,
            color = TextSecondary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}
