package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.components.ColorPickerDialog
import com.armutlu.apporganizer.presentation.ui.launcher.HomeObjectStylePrefs

private data class HomeObjectColorOption(
    val mode: String,
    @StringRes val labelRes: Int,
    val preview: Color,
)

/** Ayarlar > Görünüm: ana ekran cam yüzeyleri ve klasör zemini için ortak renk seçimi. */
@Composable
internal fun HomeObjectColorSettingsCard() {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(HomeObjectStylePrefs.getMode(context)) }
    var customColorInt by remember { mutableStateOf(HomeObjectStylePrefs.getCustomColor(context)) }
    var showColorPicker by remember { mutableStateOf(false) }

    val options = remember(customColorInt) {
        listOf(
            HomeObjectColorOption(HomeObjectStylePrefs.MODE_AUTO, R.string.home_object_color_auto, Color(0xFF0D918A)),
            HomeObjectColorOption(HomeObjectStylePrefs.MODE_DARK, R.string.home_object_color_dark, Color(0xFF111820)),
            HomeObjectColorOption(HomeObjectStylePrefs.MODE_LIGHT, R.string.home_object_color_light_glass, Color(0xFFF3F5F7)),
            HomeObjectColorOption(HomeObjectStylePrefs.MODE_CUSTOM, R.string.home_object_color_custom, Color(customColorInt)),
        )
    }

    SettingsCard {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_object_color_title),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                text = stringResource(R.string.home_object_color_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.mode }) { option ->
                    val selected = selectedMode == option.mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                selectedMode = option.mode
                                HomeObjectStylePrefs.setMode(context, option.mode)
                            }
                            .padding(vertical = 2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(option.preview)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape,
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedMode = option.mode
                                HomeObjectStylePrefs.setMode(context, option.mode)
                            },
                            label = { Text(stringResource(option.labelRes), fontSize = 11.sp) },
                        )
                    }
                }
            }

            if (selectedMode == HomeObjectStylePrefs.MODE_AUTO) {
                Text(
                    text = stringResource(R.string.home_object_color_auto_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }

            if (selectedMode == HomeObjectStylePrefs.MODE_LIGHT) {
                Text(
                    text = stringResource(R.string.home_object_color_light_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }

            if (selectedMode == HomeObjectStylePrefs.MODE_CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(customColorInt))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    )
                    OutlinedButton(
                        onClick = { showColorPicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.home_object_color_pick_custom), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = Color(customColorInt),
            onColorSelected = { color ->
                customColorInt = android.graphics.Color.argb(
                    255,
                    (color.red * 255).toInt().coerceIn(0, 255),
                    (color.green * 255).toInt().coerceIn(0, 255),
                    (color.blue * 255).toInt().coerceIn(0, 255),
                )
                HomeObjectStylePrefs.setCustomColor(context, customColorInt)
                HomeObjectStylePrefs.setMode(context, HomeObjectStylePrefs.MODE_CUSTOM)
                selectedMode = HomeObjectStylePrefs.MODE_CUSTOM
            },
            onDismiss = { showColorPicker = false },
        )
    }
}
