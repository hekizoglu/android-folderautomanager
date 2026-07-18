package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P07 — `DashboardLayoutPolicy.mode()` saf devir/eşik mantığının birim testleri.
 * Compose/Android bağımlılığı yoktur (bkz. `DashboardLayoutPolicy.kt`).
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P07
 * (satır 757-804) testler bölümü: "640dp altı ekran", "700dp ekran", "Tablet", "Widget açık/kapalı".
 */
class DashboardLayoutPolicyTest {

    @Test fun `640dp alti ekran ULTRA_COMPACT doner`() {
        assertEquals(
            DashboardDensity.ULTRA_COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 600, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `tam 640dp COMPACT doner (ust sinir haric)`() {
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 640, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `700dp altinda ama 640 ustunde COMPACT doner`() {
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 680, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `700dp ve uzeri az sectionla COMFORTABLE doner`() {
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 700, visibleSectionCount = 3, hasWidgets = false)
        )
    }

    @Test fun `tablet buyuk ekran az sectionla COMFORTABLE doner`() {
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 1200, visibleSectionCount = 4, hasWidgets = false)
        )
    }

    @Test fun `buyuk ekranda cok section varsa COMPACT'a duser`() {
        // hasWidgets=false -> esik 5
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 4, hasWidgets = false)
        )
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 5, hasWidgets = false)
        )
    }

    @Test fun `widget acikken esik daha erken tetiklenir`() {
        // hasWidgets=true -> esik 4 (widget kapaliyken ayni sayida section COMFORTABLE kalirdi)
        assertEquals(
            DashboardDensity.COMFORTABLE,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 3, hasWidgets = true)
        )
        assertEquals(
            DashboardDensity.COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 900, visibleSectionCount = 4, hasWidgets = true)
        )
    }

    @Test fun `kucuk ekranda section sayisi ne olursa olsun ULTRA_COMPACT kazanir`() {
        assertEquals(
            DashboardDensity.ULTRA_COMPACT,
            DashboardLayoutPolicy.mode(screenHeightDp = 500, visibleSectionCount = 1, hasWidgets = false)
        )
    }

    // Döngü P18 — `DashboardLayoutPolicy.applyFocusMode()` testleri: Focus Mode artık ayrı bir
    // paralel ana ekran değil, Dashboard'un sade bir preset'idir (roadmap Döngü P18, satır 1339-1382).

    private fun fullyEnabledState(): DashboardUiState = DashboardUiState(
        clock = DashboardClockState(compact = false),
        intelligence = DashboardIntelligenceState(
            missionsEnabled = true,
            mission = null,
            digitalLifeCardVisible = true,
            pulse = null,
        ),
        recentInstalls = DashboardRecentInstallsState(enabled = true, apps = emptyList()),
        secondarySections = DashboardSecondarySectionsState(
            googleSearchEnabled = true,
            widgetAreaEnabled = true,
            widgetIds = listOf(1),
            widgetAutoResize = false,
            screenHeightDp = 800,
        ),
        insights = DashboardInsightsState(
            assistantCardsEnabled = true,
            tickerEnabled = true,
            insightCards = emptyList(),
        ),
        ticker = DashboardTickerState(
            tickerEnabled = true,
            tickerMuted = false,
            tickerItems = emptyList(),
            homeTickerVisible = true,
            tickerAutoAdvance = true,
            tickerIntervalSeconds = 6,
            folders = emptyList(),
        ),
        favorites = DashboardFavoritesState(
            favoritesEnabled = true,
            favoriteApps = emptyList(),
            suggestionsEnabled = true,
            suggestedApps = emptyList(),
            suggestionsIconSizeDp = 48,
            recentNotificationAppsEnabled = true,
            recentNotificationApps = emptyList(),
            recentNotificationCounts = emptyMap(),
            recentAppsEnabled = true,
            recentApps = emptyList(),
            dockPackages = emptyList(),
            iconPackPkg = "",
            screenHeightDp = 800,
        ),
        hideSecondaryRowsForIme = false,
    )

    @Test fun `applyFocusMode saati kompaklastirir`() {
        val result = DashboardLayoutPolicy.applyFocusMode(fullyEnabledState())
        assertEquals(true, result.clock.compact)
    }

    @Test fun `applyFocusMode gorev ve dijital yasam kartlarini kapatir`() {
        val result = DashboardLayoutPolicy.applyFocusMode(fullyEnabledState())
        assertEquals(false, result.intelligence.missionsEnabled)
        assertEquals(false, result.intelligence.digitalLifeCardVisible)
    }

    @Test fun `applyFocusMode global aramaya dokunmaz`() {
        val result = DashboardLayoutPolicy.applyFocusMode(fullyEnabledState())
        assertEquals(true, result.secondarySections.googleSearchEnabled)
    }

    @Test fun `applyFocusMode oneri ve son kullanilan satirlarini kapatir ama favorileri korur`() {
        val result = DashboardLayoutPolicy.applyFocusMode(fullyEnabledState())
        assertEquals(false, result.favorites.suggestionsEnabled)
        assertEquals(false, result.favorites.recentNotificationAppsEnabled)
        assertEquals(false, result.favorites.recentAppsEnabled)
        assertEquals(true, result.favorites.favoritesEnabled)
    }

    @Test fun `applyFocusMode icgorulerini ve ticker'i kapatir`() {
        val result = DashboardLayoutPolicy.applyFocusMode(fullyEnabledState())
        assertEquals(false, result.insights.assistantCardsEnabled)
        assertEquals(false, result.ticker.tickerEnabled)
    }

    @Test fun `applyFocusMode her seyi kapali state uzerinde no-op gibi davranir`() {
        val allOff = DashboardUiState(
            clock = DashboardClockState(compact = false),
            intelligence = DashboardIntelligenceState(
                missionsEnabled = false,
                mission = null,
                digitalLifeCardVisible = false,
                pulse = null,
            ),
            recentInstalls = DashboardRecentInstallsState(enabled = false, apps = emptyList()),
            secondarySections = DashboardSecondarySectionsState(
                googleSearchEnabled = false,
                widgetAreaEnabled = false,
                widgetIds = emptyList(),
                widgetAutoResize = false,
                screenHeightDp = 800,
            ),
            insights = DashboardInsightsState(
                assistantCardsEnabled = false,
                tickerEnabled = false,
                insightCards = emptyList(),
            ),
            ticker = DashboardTickerState(
                tickerEnabled = false,
                tickerMuted = false,
                tickerItems = emptyList(),
                homeTickerVisible = true,
                tickerAutoAdvance = true,
                tickerIntervalSeconds = 6,
                folders = emptyList(),
            ),
            favorites = DashboardFavoritesState(
                favoritesEnabled = false,
                favoriteApps = emptyList(),
                suggestionsEnabled = false,
                suggestedApps = emptyList(),
                suggestionsIconSizeDp = 48,
                recentNotificationAppsEnabled = false,
                recentNotificationApps = emptyList(),
                recentNotificationCounts = emptyMap(),
                recentAppsEnabled = false,
                recentApps = emptyList(),
                dockPackages = emptyList(),
                iconPackPkg = "",
                screenHeightDp = 800,
            ),
            hideSecondaryRowsForIme = false,
        )
        val result = DashboardLayoutPolicy.applyFocusMode(allOff)
        // clock.compact tek istisna — Focus Mode her zaman kompakt saat zorlar.
        assertEquals(true, result.clock.compact)
        assertEquals(false, result.intelligence.missionsEnabled)
        assertEquals(false, result.favorites.suggestionsEnabled)
    }
}
