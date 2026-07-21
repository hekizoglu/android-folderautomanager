package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessTab
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState

@Composable
internal fun HeroDashboardPage(
    pulse: HomePulseSummary?,
    smartAccess: SmartAccessUiState,
    onOpenWeeklyReport: () -> Unit,
    onClockLongPress: () -> Unit,
    onOpenPulse: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSearchSettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onAppLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val spec = HomeHeroLayoutPolicy.resolve(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        fontScale = configuration.fontScale,
    )
    var selectedTab by rememberSaveable { mutableStateOf(SmartAccessTab.NOW) }
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val contentWidth = (maxWidth - (spec.horizontalPaddingDp * 2).dp)
            .coerceAtMost(spec.contentMaxWidthDp.dp)
            .coerceAtLeast(0.dp)
        Column(
            modifier = Modifier
                .width(contentWidth)
                .then(if (spec.scrollEnabled) Modifier.verticalScroll(scrollState) else Modifier)
                .padding(top = if (spec.profile == HomeHeroProfile.COMPACT_PHONE) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(HomeHeroTokens.SectionGap),
        ) {
            HeroClockCard(
                spec = spec,
                onClick = onOpenWeeklyReport,
                onLongClick = onClockLongPress,
            )
            HeroDigitalLifeCard(
                summary = pulse,
                spec = spec,
                onClick = onOpenPulse,
            )
            HeroSearchCard(
                spec = spec,
                onOpenSearch = onOpenSearch,
                onOpenSources = onOpenSearchSettings,
            )
            SmartAccessCard(
                state = smartAccess,
                spec = spec,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onOpenUsageSettings = onOpenUsageAccessSettings,
                onOpenNotificationSettings = onOpenNotificationAccessSettings,
                onLaunchApp = onLaunchApp,
                onAppLongClick = onAppLongClick,
            )
        }
    }
}
