package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessTab
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState
import com.armutlu.apporganizer.domain.models.AppInfo

@Composable
internal fun SmartAccessCard(
    state: SmartAccessUiState,
    spec: HomeHeroLayoutSpec,
    selectedTab: SmartAccessTab,
    onTabSelected: (SmartAccessTab) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PremiumGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(spec.smartAccessHeightDp.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF85BCFF),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.hero_smart_access_title),
                    color = Color.White.copy(alpha = .94f),
                    fontSize = HomeHeroTokens.CardTitleTextSize,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 7.dp),
                )
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.hero_smart_access_settings),
                    tint = Color.White.copy(alpha = .7f),
                    modifier = Modifier
                        .testTag("hero_smart_access_settings")
                        .size(32.dp)
                        .clickable(
                            onClick = if (selectedTab == SmartAccessTab.NOTIFICATIONS) {
                                onOpenNotificationSettings
                            } else {
                                onOpenUsageSettings
                            }
                        )
                        .padding(5.dp),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmartAccessTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    PremiumGlassSurface(
                        modifier = Modifier
                            .testTag("hero_smart_tab_${tab.name.lowercase()}")
                            .semantics { this.selected = selected }
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onTabSelected(tab) },
                        cornerRadius = 16.dp,
                        emphasis = if (selected) PremiumGlassEmphasis.ACTIVE else PremiumGlassEmphasis.SUBTLE,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(tab.labelRes()),
                                color = Color.White.copy(alpha = if (selected) .96f else .60f),
                                fontSize = HomeHeroTokens.BodyTextSize,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            SmartAccessContent(
                state = state,
                selectedTab = selectedTab,
                onOpenUsageSettings = onOpenUsageSettings,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onLaunchApp = onLaunchApp,
                onAppLongClick = onAppLongClick,
            )
        }
    }
}

@Composable
private fun SmartAccessContent(
    state: SmartAccessUiState,
    selectedTab: SmartAccessTab,
    onOpenUsageSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
) {
    val apps: List<Pair<AppInfo, Int?>> = when (selectedTab) {
        SmartAccessTab.NOW -> state.nowApps.map { it to null }
        SmartAccessTab.RECENT -> state.recentApps.map { it to null }
        SmartAccessTab.NOTIFICATIONS -> state.notificationApps.map { it.app to it.count }
    }
    if (apps.isEmpty()) {
        val permissionAction = when {
            selectedTab == SmartAccessTab.NOTIFICATIONS && !state.notificationPermissionGranted ->
                onOpenNotificationSettings
            selectedTab != SmartAccessTab.NOTIFICATIONS && !state.usagePermissionGranted ->
                onOpenUsageSettings
            else -> null
        }
        Text(
            text = stringResource(emptyMessageRes(state, selectedTab)),
            color = Color.White.copy(alpha = .58f),
            fontSize = HomeHeroTokens.BodyTextSize,
            modifier = Modifier
                .testTag("hero_smart_access_empty_action")
                .then(
                    if (permissionAction == null) Modifier
                    else Modifier.clickable(onClick = permissionAction)
                )
                .padding(horizontal = 4.dp, vertical = 10.dp),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.take(5).forEach { (app, count) ->
            SmartAccessAppItem(
                app = app,
                notificationCount = count,
                onClick = { onLaunchApp(app.packageName) },
                onLongClick = { onAppLongClick(app.packageName) },
            )
        }
    }
}

private fun SmartAccessTab.labelRes(): Int = when (this) {
    SmartAccessTab.NOW -> R.string.hero_smart_access_now
    SmartAccessTab.RECENT -> R.string.hero_smart_access_recent
    SmartAccessTab.NOTIFICATIONS -> R.string.hero_smart_access_notifications
}

private fun emptyMessageRes(state: SmartAccessUiState, tab: SmartAccessTab): Int = when {
    tab != SmartAccessTab.NOTIFICATIONS && !state.usagePermissionGranted ->
        R.string.hero_smart_access_usage_permission
    tab == SmartAccessTab.NOTIFICATIONS && !state.notificationPermissionGranted ->
        R.string.hero_smart_access_notification_permission
    else -> R.string.hero_smart_access_empty
}
