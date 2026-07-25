package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppIconView
import com.armutlu.apporganizer.presentation.ui.launcher.HomeAdaptiveLayoutPolicy

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
    val configuration = LocalConfiguration.current
    // Döngü P20 — HomeScreen.kt:805-810 yorumuyla birebir aynı sözleşme: yalnız EXPANDED_TABLET
    // (840dp+) genişlik tavanına çarpar (HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp()).
    // Telefon/küçük tablette null döner → fillMaxWidth() davranışı hiç değişmez.
    val deviceClass = HomeAdaptiveLayoutPolicy.deviceClass(configuration.screenWidthDp)
    val maxContentWidth = HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(deviceClass)

    PremiumGlassSurface(
        modifier = modifier
            .testTag("hero_dock")
            .fillMaxWidth(if (maxContentWidth != null) 0.95f else 1f)
            .widthIn(max = maxContentWidth?.dp ?: Dp.Unspecified)
            .height(HomeHeroTokens.DockHeight)
            .combinedClickable(onClick = onEditDock, onLongClick = onEditDock),
        cornerRadius = HomeHeroTokens.DockCorner,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
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
