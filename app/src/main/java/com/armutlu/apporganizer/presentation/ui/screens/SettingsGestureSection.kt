package com.armutlu.apporganizer.presentation.ui.screens

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
import com.armutlu.apporganizer.utils.AppPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsGestureSection() {
    val context = LocalContext.current

    var doubleTapAction by remember { mutableStateOf(AppPrefs.getGestureDoubleTap(context)) }
    var longPressAction  by remember { mutableStateOf(AppPrefs.getGestureLongPress(context)) }
    var swipeUpAction    by remember { mutableStateOf(AppPrefs.getGestureSwipeUp(context)) }

    SettingsSectionTitle("Gesture Aksiyonları")
    SettingsCard {
        GestureActionRow(
            icon = Icons.Default.TouchApp,
            label = "Çift Tık",
            selected = doubleTapAction,
            onSelect = {
                doubleTapAction = it
                AppPrefs.setGestureDoubleTap(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        GestureActionRow(
            icon = Icons.Default.PanTool,
            label = "Uzun Bas (Boş Alan)",
            selected = longPressAction,
            onSelect = {
                longPressAction = it
                AppPrefs.setGestureLongPress(context, it)
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        GestureActionRow(
            icon = Icons.Default.SwipeUp,
            label = "Yukarı Kaydır",
            selected = swipeUpAction,
            onSelect = {
                swipeUpAction = it
                AppPrefs.setGestureSwipeUp(context, it)
            }
        )
    }
}

@Composable
private fun GestureActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: AppPrefs.GestureAction,
    onSelect: (AppPrefs.GestureAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(selected.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("Değiştir")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AppPrefs.GestureAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        onClick = { onSelect(action); expanded = false },
                        leadingIcon = {
                            if (selected == action)
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
        }
    }
}
