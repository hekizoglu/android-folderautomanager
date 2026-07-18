package com.armutlu.apporganizer.presentation.ui.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Döngü P07 — `countVisibleSections()` saf yardımcısının birim testleri. Bu fonksiyon
 * `SmartDashboardPage`'in gövdesindeki görünürlük koşullarıyla (`if`/`when`) birebir eşleşmelidir
 * — `DashboardLayoutPolicy.mode()`'a doğru `visibleSectionCount` girdisini sağlamak için kritik.
 * Compose bağımlılığı yoktur.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P07 (satır 757-804).
 */
class SmartDashboardPageLogicTest {

    private fun emptyState(
        missionsEnabled: Boolean = false,
        digitalLifeCardVisible: Boolean = false,
        pulse: com.armutlu.apporganizer.domain.home.HomePulseSummary? = null,
        recentInstallsEnabled: Boolean = false,
        recentInstallsApps: List<com.armutlu.apporganizer.domain.models.AppInfo> = emptyList(),
        googleSearchEnabled: Boolean = false,
        widgetAreaEnabled: Boolean = false,
        widgetIds: List<Int> = emptyList(),
        assistantCardsEnabled: Boolean = false,
        tickerEnabled: Boolean = false,
        tickerMuted: Boolean = false,
        insightCards: List<com.armutlu.apporganizer.utils.InsightCard> = emptyList(),
        tickerFolders: List<AppFolder> = emptyList(),
        favoritesEnabled: Boolean = false,
        suggestionsEnabled: Boolean = false,
        recentNotificationAppsEnabled: Boolean = false,
        recentAppsEnabled: Boolean = false,
        hideSecondaryRowsForIme: Boolean = false,
    ): DashboardUiState = DashboardUiState(
        clock = DashboardClockState(compact = false),
        intelligence = DashboardIntelligenceState(
            missionsEnabled = missionsEnabled,
            mission = null,
            digitalLifeCardVisible = digitalLifeCardVisible,
            pulse = pulse,
        ),
        recentInstalls = DashboardRecentInstallsState(
            enabled = recentInstallsEnabled,
            apps = recentInstallsApps,
        ),
        secondarySections = DashboardSecondarySectionsState(
            googleSearchEnabled = googleSearchEnabled,
            widgetAreaEnabled = widgetAreaEnabled,
            widgetIds = widgetIds,
            widgetAutoResize = false,
            screenHeightDp = 800,
        ),
        insights = DashboardInsightsState(
            assistantCardsEnabled = assistantCardsEnabled,
            tickerEnabled = tickerEnabled,
            insightCards = insightCards,
        ),
        ticker = DashboardTickerState(
            tickerEnabled = tickerEnabled,
            tickerMuted = tickerMuted,
            tickerItems = emptyList(),
            homeTickerVisible = true,
            tickerAutoAdvance = true,
            tickerIntervalSeconds = 6,
            folders = tickerFolders,
        ),
        favorites = DashboardFavoritesState(
            favoritesEnabled = favoritesEnabled,
            favoriteApps = emptyList(),
            suggestionsEnabled = suggestionsEnabled,
            suggestedApps = emptyList(),
            suggestionsIconSizeDp = 48,
            recentNotificationAppsEnabled = recentNotificationAppsEnabled,
            recentNotificationApps = emptyList(),
            recentNotificationCounts = emptyMap(),
            recentAppsEnabled = recentAppsEnabled,
            recentApps = emptyList(),
            dockPackages = emptyList(),
            iconPackPkg = "",
            screenHeightDp = 800,
        ),
        hideSecondaryRowsForIme = hideSecondaryRowsForIme,
    )

    @Test fun `hicbir section acik degilken sadece saat sayilir`() {
        assertEquals(1, countVisibleSections(emptyState()))
    }

    @Test fun `tum secondary section'lar acikken hepsi sayilir`() {
        val state = emptyState(
            missionsEnabled = true,
            googleSearchEnabled = true,
            widgetAreaEnabled = true,
            widgetIds = listOf(1),
            tickerEnabled = false,
            tickerFolders = listOf(
                AppFolder(
                    category = com.armutlu.apporganizer.domain.models.Category(
                        categoryId = "a", categoryName = "a", displayOrder = 0
                    ),
                    apps = emptyList(),
                )
            ),
            favoritesEnabled = true,
        )
        // saat(1) + missions(1) + googleSearch(1) + widget(1) + folderStats(tickerFolders dolu)(1) + favorites(1) = 6
        assertEquals(6, countVisibleSections(state))
    }

    @Test fun `hideSecondaryRowsForIme acikken google widget ve favoriler sayilmaz`() {
        val state = emptyState(
            googleSearchEnabled = true,
            widgetAreaEnabled = true,
            widgetIds = listOf(1),
            favoritesEnabled = true,
            hideSecondaryRowsForIme = true,
        )
        // yalnız saat kalır
        assertEquals(1, countVisibleSections(state))
    }

    @Test fun `widget ids bos ise widgetAreaEnabled acik olsa da sayilmaz`() {
        val state = emptyState(widgetAreaEnabled = true, widgetIds = emptyList())
        assertEquals(1, countVisibleSections(state))
    }

    @Test fun `ticker acik ve mute degilken tek section sayilir, mute iken sayilmaz`() {
        val active = emptyState(tickerEnabled = true, tickerMuted = false)
        assertEquals(2, countVisibleSections(active)) // saat + ticker

        val muted = emptyState(tickerEnabled = true, tickerMuted = true)
        assertEquals(1, countVisibleSections(muted)) // saat only — ticker susturulmuş, folderStats da kapalı (tickerEnabled=true)
    }

    @Test fun `favoriler alt bayraklardan herhangi biri aciksa tek section sayilir`() {
        val state = emptyState(suggestionsEnabled = true, recentAppsEnabled = true, recentNotificationAppsEnabled = true)
        assertEquals(2, countVisibleSections(state)) // saat + favorites section (tek satır olarak sayılır)
    }
}
