package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HeroDock(
    packages: List<String>,
    appsByPackage: Map<String, AppInfo>,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    onEditDock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val apps = packages.asSequence()
        .mapNotNull(appsByPackage::get)
        .filter { it.isInstalled && !it.isHidden }
        .distinctBy { it.packageName }
        .take(5)
        .toList()
    PremiumGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeHeroTokens.DockHeight)
            .combinedClickable(onClick = {}, onLongClick = onEditDock),
        cornerRadius = HomeHeroTokens.DockCorner,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            apps.forEach { app ->
                AppIconView(
                    app = app,
                    onClick = { onLaunchApp(app.packageName) },
                    onLongClick = { onAppLongClick(app.packageName) },
                    showLabel = false,
                    iconSize = 48.dp,
                    newBadgeEnabled = false,
                )
            }
        }
    }
}
