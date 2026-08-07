package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.common.rememberBooleanPreferenceState
import com.armutlu.apporganizer.presentation.ui.common.rememberIntPreferenceState
import com.armutlu.apporganizer.utils.AppPrefs

@Composable
fun SettingsDrawerScreen(onNavigateBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var favorites by rememberBooleanPreferenceState(context, AppPrefs.KEY_FAVORITES_ENABLED_ALLAPPS) {
        AppPrefs.isFavoritesEnabledAllApps(context)
    }
    var recent by rememberBooleanPreferenceState(context, AppPrefs.KEY_RECENT_APPS_ENABLED_ALLAPPS) {
        AppPrefs.isRecentAppsEnabledAllApps(context)
    }
    var notifications by rememberBooleanPreferenceState(context, AppPrefs.KEY_RECENT_NOTIFICATION_APPS_ROW) {
        AppPrefs.isRecentNotificationAppsRowEnabled(context)
    }
    var today by rememberBooleanPreferenceState(context, AppPrefs.KEY_RECENT_INSTALLS_ENABLED) {
        AppPrefs.isRecentInstallsEnabled(context)
    }
    var systemApps by rememberBooleanPreferenceState(context, AppPrefs.KEY_SHOW_SYSTEM_APPS) {
        AppPrefs.isShowSystemApps(context)
    }
    var chips by rememberBooleanPreferenceState(context, AppPrefs.KEY_DRAWER_CHIP_ROWS_ENABLED) {
        AppPrefs.isDrawerChipRowsEnabled(context)
    }
    var notificationText by rememberBooleanPreferenceState(context, AppPrefs.KEY_NOTIFICATION_TEXT_ENABLED) {
        AppPrefs.isNotificationTextEnabled(context)
    }
    var pixelLook by rememberBooleanPreferenceState(context, AppPrefs.KEY_PIXEL_LOOK_ENABLED) {
        AppPrefs.isPixelLookEnabled(context)
    }
    var shownAppsCount by rememberIntPreferenceState(context, AppPrefs.KEY_SHOWN_APPS_COUNT) {
        AppPrefs.getShownAppsCount(context)
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.settings_drawer_title),
        onNavigateBack = onNavigateBack,
    ) {
        item { SettingsSectionTitle(stringResource(R.string.settings_drawer_title)) }
        item {
            SettingsCard {
                SettingsSwitchRow(Icons.Default.Star, stringResource(R.string.settings_drawer_favorites_title), stringResource(R.string.settings_drawer_favorites_desc), favorites, onCheckedChange = {
                    favorites = it
                    AppPrefs.setFavoritesEnabledAllApps(context, it)
                })
                SettingsSwitchRow(Icons.Default.Schedule, stringResource(R.string.settings_drawer_recent_title), stringResource(R.string.settings_drawer_recent_desc), recent, onCheckedChange = {
                    recent = it
                    AppPrefs.setRecentAppsEnabledAllApps(context, it)
                })
                SettingsSwitchRow(Icons.Default.Notifications, stringResource(R.string.settings_drawer_notifications_title), stringResource(R.string.settings_drawer_notifications_desc), notifications, onCheckedChange = {
                    notifications = it
                    AppPrefs.setRecentNotificationAppsRowEnabled(context, it)
                })
                SettingsSwitchRow(Icons.Default.Apps, stringResource(R.string.settings_drawer_today_title), stringResource(R.string.settings_drawer_today_desc), today, onCheckedChange = {
                    today = it
                    AppPrefs.setRecentInstallsEnabled(context, it)
                })
            }
        }
        item { SettingsSectionTitle("Liste ve görünüm") }
        item {
            SettingsCard {
                SettingsSwitchRow(Icons.Default.Apps, stringResource(R.string.settings_drawer_system_title), stringResource(R.string.settings_drawer_system_desc), systemApps, onCheckedChange = {
                    systemApps = it
                    AppPrefs.setShowSystemApps(context, it)
                })
                SettingsSwitchRow(Icons.Default.Tune, stringResource(R.string.settings_drawer_chips_title), stringResource(R.string.settings_drawer_chips_desc), chips, onCheckedChange = {
                    chips = it
                    AppPrefs.setDrawerChipRowsEnabled(context, it)
                })
                SettingsSwitchRow(Icons.Default.Notifications, stringResource(R.string.settings_drawer_text_title), stringResource(R.string.settings_drawer_text_desc), notificationText, onCheckedChange = {
                    notificationText = it
                    AppPrefs.setNotificationTextEnabled(context, it)
                })
                SettingsSwitchRow(Icons.Default.Palette, stringResource(R.string.settings_drawer_pixel_title), stringResource(R.string.settings_drawer_pixel_desc), pixelLook, onCheckedChange = {
                    pixelLook = it
                    AppPrefs.setPixelLookEnabled(context, it)
                })
                SettingsListPreferenceRow(
                    icon = Icons.Default.Apps,
                    title = stringResource(R.string.settings_shown_apps_count_title),
                    subtitle = stringResource(R.string.settings_shown_apps_count_desc, shownAppsCount),
                    currentValue = shownAppsCount.toString(),
                    options = listOf(
                        "4" to stringResource(R.string.settings_shown_apps_count_option_4),
                        "8" to stringResource(R.string.settings_shown_apps_count_option_8),
                        "12" to stringResource(R.string.settings_shown_apps_count_option_12)
                    ),
                    onValueSelected = { value ->
                        val count = value.toIntOrNull() ?: 4
                        shownAppsCount = count
                        AppPrefs.setShownAppsCount(context, count)
                    }
                )
            }
        }
    }
}
