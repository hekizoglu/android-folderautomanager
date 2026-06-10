package com.armutlu.apporganizer.presentation.ui.launcher

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SheetBg    = Color(0xFF1A1A2E)
private val TealColor  = Color(0xFF00897B)
private val SearchBg   = Color.White.copy(alpha = 0.09f)
private val TextPrimary   = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.55f)

@Composable
private fun rememberIcon(packageName: String): Drawable? {
    val context = LocalContext.current
    return produceState<Drawable?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
        }
    }.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockEditSheet(
    allApps: List<AppInfo>,
    dockPackages: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
    maxDock: Int = 4
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allApps, query) {
        if (query.isBlank()) allApps
        else allApps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBg,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(0.2f)))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Başlık
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dock Düzenle", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("${dockPackages.size}/$maxDock", fontSize = 13.sp, color = TealColor, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Kapat", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            // Mevcut dock uygulamaları
            if (dockPackages.isNotEmpty()) {
                Text("Mevcut", fontSize = 12.sp, color = TealColor, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dockPackages.forEach { pkg ->
                        val icon = rememberIcon(pkg)
                        Box(contentAlignment = Alignment.TopEnd) {
                            if (icon != null) {
                                androidx.compose.foundation.Image(
                                    painter = rememberDrawablePainter(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(TealColor.copy(0.3f)))
                            }
                            Box(
                                modifier = Modifier.size(18.dp).clip(CircleShape)
                                    .background(Color(0xFFE53935))
                                    .clickable { onRemove(pkg) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Divider(color = Color.White.copy(0.08f), modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
            }

            // Arama
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .height(42.dp).clip(RoundedCornerShape(21.dp)).background(SearchBg)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) Text("Uygulama ara...", color = TextSecondary, fontSize = 13.sp)
                        BasicTextField(
                            value = query, onValueChange = { query = it },
                            singleLine = true, cursorBrush = SolidColor(TealColor),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Uygulama listesi
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items = filtered, key = { it.packageName }) { app ->
                    val inDock = app.packageName in dockPackages
                    val full = dockPackages.size >= maxDock && !inDock
                    val icon = rememberIcon(app.packageName)

                    Row(
                        Modifier.fillMaxWidth().height(52.dp)
                            .clickable(enabled = !full) {
                                if (inDock) onRemove(app.packageName) else onAdd(app.packageName)
                            }
                            .background(if (inDock) TealColor.copy(0.10f) else Color.Transparent)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            androidx.compose.foundation.Image(
                                painter = rememberDrawablePainter(icon),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(TealColor.copy(0.2f)))
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            app.appName, fontSize = 15.sp, color = if (full) TextSecondary else TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                        if (inDock) {
                            Icon(Icons.Default.Check, null, tint = TealColor, modifier = Modifier.size(18.dp))
                        } else if (!full) {
                            Icon(Icons.Default.Add, null, tint = TextSecondary.copy(0.5f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
