package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.presentation.viewmodel.AppListViewModel

@Composable
fun WidgetSuggestionSection(viewModel: AppListViewModel) {
    val suggestions by viewModel.widgetSuggestions.collectAsState()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    if (suggestions.isEmpty()) return

    SettingsSectionTitle("Widget Önerileri")
    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${suggestions.size} uygulamanın widget'ı var",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Sık kullandığın uygulamalar için widget ekleyebilirsin",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Gizle" else "Göster"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(suggestion.appName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${suggestion.widgetCount} widget • ${suggestion.launchCount} kez açıldı",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                runCatching {
                                    val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                                        .setData(android.net.Uri.parse("package:${suggestion.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Text("Detay", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
