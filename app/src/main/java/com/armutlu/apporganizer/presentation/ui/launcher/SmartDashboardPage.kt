package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.HomeSectionId

/**
 * Döngü P06 — Akıllı Ana Ekran / Dashboard sayfası. Roadmap bölüm 1.2 "Sayfa 0" içerik
 * listesinde yer alan zeka bileşenlerini (saat, görev/dijital yaşam kartları, arama/widget
 * alanı, içgörü kartları, akıllı nabız şeridi, favoriler) TEK bileşende toplar.
 *
 * Bu composable "yeniden yazma" değil, mevcut alt bileşenlerin (PulseClockWidget,
 * HomeIntelligenceCardsRow, GoogleSearchBar, WidgetArea, AssistantInsightRow, HomeTickerRow,
 * FolderStatsRow, HomeFavoritesSection) doğrudan çağrılmasıdır — görsel/mantıksal davranışları
 * HomeScreen.kt'deki eski konumlarıyla birebir aynıdır.
 *
 * ÖNEMLİ (P06/P25 güncel durum): Bu composable `HomePagerHost`'un `dashboardContent` slotundan
 * çağrılır; görünürlüğü HomeScreen.kt:1391 `HomePagerRolloutPolicy.dashboardEnabled` ile gerçek
 * kullanıcı tercihinden hesaplanır. HomeScreen.kt'deki eski pager-dışı kopya (Dashboard/ticker/
 * favoriler blokları) P25/F7'de kaldırıldı — artık tek doğruluk kaynağı bu composable ve
 * HomePagerHost'un ilgili slotlarıdır.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P06 (satır 676-756),
 * Bölüm 1.2 "Sayfa 0" içerik listesi (satır 90-105).
 */
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    // P06 madde 8: Dashboard boş kalırsa yalnız saat + öneri açıklaması gösterilir.
    // Görev S1 — todayCardEnabled açıkken intelligence/recentInstalls/insight koşulları yerine
    // tek todayCardSpec varlığı kontrol edilir (bu üç bölüm todayCard açıkken hiç çizilmez).
    val hasAnyContent = if (state.intelligence.todayCardEnabled) {
        state.intelligence.todayCardSpec != null ||
            state.secondarySections.googleSearchEnabled ||
            (state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty()) ||
            (state.ticker.tickerEnabled && !state.ticker.tickerMuted) ||
            (!state.ticker.tickerEnabled && state.ticker.folders.isNotEmpty()) ||
            state.favorites.favoritesEnabled ||
            state.favorites.suggestionsEnabled ||
            state.favorites.recentNotificationAppsEnabled ||
            state.favorites.recentAppsEnabled
    } else {
        state.intelligence.missionsEnabled ||
            (state.intelligence.digitalLifeCardVisible && state.intelligence.pulse != null) ||
            state.recentInstalls.let { it.enabled && it.apps.isNotEmpty() } ||
            state.secondarySections.googleSearchEnabled ||
            (state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty()) ||
            (state.insights.assistantCardsEnabled && !state.insights.tickerEnabled && state.insights.insightCards.isNotEmpty()) ||
            (state.ticker.tickerEnabled && !state.ticker.tickerMuted) ||
            (!state.ticker.tickerEnabled && state.ticker.folders.isNotEmpty()) ||
            state.favorites.favoritesEnabled ||
            state.favorites.suggestionsEnabled ||
            state.favorites.recentNotificationAppsEnabled ||
            state.favorites.recentAppsEnabled
    }

    // Döngü P07 madde 1-3: kullanılabilir Dashboard yüksekliği `BoxWithConstraints` ile ölçülür
    // (arama çubuğu/indicator/dock zaten HomeShell seviyesinde bu Box'ın dışında kaldığı için
    // `maxHeight` çıkarım gerektirmez — bkz. HomeScreen.kt BoxWithConstraints, satır ~1177).
    // `DashboardLayoutPolicy.mode()` görünür section sayısına göre kompakt varyant seçer; bu
    // yalnızca iç boşlukları daraltır, scroll ihtiyacını azaltmak içindir — scroll hâlâ mevcutsa
    // (madde 7: root swipe-up yalnız gesture arbitration sonucunda açılır) alttaki
    // `verticalScroll` taşan miktarı Compose'un doğal nested-scroll teslimiyle HomeScreen'in
    // swipe-up `NestedScrollConnection`'ına iletir (bkz. DashboardLayoutPolicy.kt dosya başı notu).
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableHeightDp = maxHeight.value.toInt()
        val visibleSectionCount = remember(state) { countVisibleSections(state) }
        val density = remember(availableHeightDp, visibleSectionCount, state.secondarySections.widgetAreaEnabled) {
            DashboardLayoutPolicy.mode(
                screenHeightDp = availableHeightDp,
                visibleSectionCount = visibleSectionCount,
                hasWidgets = state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty(),
            )
        }
        val clockTopPadding = when (density) {
            DashboardDensity.COMFORTABLE -> 32.dp
            DashboardDensity.COMPACT -> 20.dp
            DashboardDensity.ULTRA_COMPACT -> 12.dp
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Dashboard içeriği kendi kaydırma alanında kalır; üçüncü parti widget çizimi
            // pager dışına taşarak arama/dock katmanlarının üzerine gelemez.
            .clipToBounds()
            .verticalScroll(scrollState)
    ) {
        val compactClock = state.clock.compact || density != DashboardDensity.COMFORTABLE
        PulseClockWidget(
            compact = compactClock,
            onOpenWeeklyReport = actions.onOpenWeeklyReport,
            onOpenScoreDetails = actions.onOpenScoreDetails,
            onLongPress = actions.onClockLongPress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compactClock) 16.dp else clockTopPadding, bottom = 8.dp)
        )

        // Görev S1 — todayCardEnabled açıkken tek bağlamsal "BUGÜN" kartı, HomeIntelligenceCardsRow
        // (HomeMissionCard+DigitalLifeCard) yerine geçer. spec null ise (TodayCardSelector hiçbir
        // önceliği seçemedi) HİÇBİR ŞEY çizilmez — kapalıyken eski davranış AYNEN korunur.
        if (state.intelligence.todayCardEnabled) {
            state.intelligence.todayCardSpec?.let { spec ->
                TodayCard(
                    spec = spec,
                    onMissionClick = actions.onMissionClick,
                    onPulseClick = actions.onPulseClick,
                    onFolderReviewClick = actions.onOpenFolderStats,
                    onReportReadyClick = actions.onOpenWeeklyReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        } else {
            HomeIntelligenceCardsRow(
                missionsEnabled = state.intelligence.missionsEnabled,
                mission = state.intelligence.mission,
                digitalLifeCardVisible = state.intelligence.digitalLifeCardVisible,
                pulse = state.intelligence.pulse,
                onMissionClick = actions.onMissionClick,
                onPulseClick = actions.onPulseClick,
                onPulseReasonAction = actions.onPulseReasonAction,
            )
        }

        if (!state.intelligence.todayCardEnabled && state.recentInstalls.enabled && state.recentInstalls.apps.isNotEmpty()) {
            val recentInstallsTitle = stringResource(R.string.recent_installs_home_chip_title)
            val recentInstallsCount = state.recentInstalls.apps.size
            val recentInstallsSubtitle = if (recentInstallsCount == 1) {
                stringResource(R.string.recent_installs_home_chip_subtitle_one)
            } else {
                stringResource(R.string.recent_installs_home_chip_subtitle_other, recentInstallsCount)
            }
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clickable { actions.onOpenRecentInstalls() }
                    // Döngü P19 madde 4 — kart tek bir açıklayıcı contentDescription üretir;
                    // içindeki dekoratif "📥"/"›" glyph'leri TalkBack'e AYRI okunmaz
                    // (mergeDescendants=true altındaki Text node'ları kendi contentDescription'ını
                    // taşımadığından otomatik birleşir — HomeTickerRow.kt/FolderTile.kt aynı desen).
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$recentInstallsTitle, $recentInstallsSubtitle"
                        role = Role.Button
                    },
                cornerRadius = 18.dp,
                backgroundAlpha = 0.10f,
                borderAlpha = 0.18f,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.material3.Text("📥", fontSize = 15.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        androidx.compose.material3.Text(
                            text = recentInstallsTitle,
                            color = Color.White.copy(alpha = 0.90f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        androidx.compose.material3.Text(
                            text = recentInstallsSubtitle,
                            color = Color.White.copy(alpha = 0.52f),
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                    androidx.compose.material3.Text("›", color = Color.White.copy(alpha = 0.45f), fontSize = 18.sp)
                }
            }
        }

        val hideSecondaryRows = state.hideSecondaryRowsForIme
        // P16 — bu üç blok (arama+widget, içgörü+ticker, favoriler) CONTENT zone'un reorderable
        // gruplarıdır; sıraları editörden (Ayarlar > Ana Ekranı Düzenle) gelen
        // `state.contentOrder`'a göre belirlenir. Grup İÇİ alt bileşen sırası (örn. GOOGLE_SEARCH
        // her zaman ANDROID_WIDGETS'tan önce) sabit kalır — editör yalnız grupları birbirine göre
        // taşır, gruplar içindeki alt-bileşen sırasını değiştirmez (bkz. dashboardGroupOrder()).
        dashboardGroupOrder(state.contentOrder).forEach { group ->
            when (group) {
                DashboardContentGroup.SEARCH_AND_WIDGETS -> {
                    com.armutlu.apporganizer.presentation.ui.launcher.HomeSectionRenderer(
                        items = listOf(
                            com.armutlu.apporganizer.domain.models.HomeLayoutItem(
                                HomeSectionId.GOOGLE_SEARCH,
                                HomeSectionId.GOOGLE_SEARCH.defaultZone,
                                0,
                                !hideSecondaryRows && state.secondarySections.googleSearchEnabled,
                            ),
                            com.armutlu.apporganizer.domain.models.HomeLayoutItem(
                                HomeSectionId.ANDROID_WIDGETS,
                                HomeSectionId.ANDROID_WIDGETS.defaultZone,
                                1,
                                !hideSecondaryRows && state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty(),
                            ),
                        ),
                    ) { sectionId ->
                        when (sectionId) {
                            HomeSectionId.GOOGLE_SEARCH -> GoogleSearchBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            HomeSectionId.ANDROID_WIDGETS -> WidgetArea(
                                widgetIds = state.secondarySections.widgetIds,
                                onRemoveWidget = actions.onRemoveWidget,
                                onReorderWidgets = actions.onReorderWidgets,
                                autoResize = state.secondarySections.widgetAutoResize,
                                screenHeightDp = state.secondarySections.screenHeightDp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            else -> Unit
                        }
                    }
                }

                DashboardContentGroup.INSIGHTS_AND_TICKER -> {
                    // Görev S1 — todayCardEnabled açıkken assistant insight satırı TodayCard'a
                    // devredilir, ayrıca çizilmez (ticker satırı bu kuralın dışında kalır — S1
                    // görev tanımı yalnız intelligence+recentInstalls+insight satırlarını kapsar).
                    if (!state.intelligence.todayCardEnabled &&
                        state.insights.assistantCardsEnabled && !state.insights.tickerEnabled
                    ) {
                        if (state.insights.insightCards.isNotEmpty()) {
                            AssistantInsightRow(
                                cards = state.insights.insightCards,
                                onCardClick = actions.onInsightCardClick,
                                onOpenDashboard = actions.onOpenDashboardShortcut,
                            )
                        }
                    }

                    if (state.ticker.tickerEnabled && !state.ticker.tickerMuted) {
                        HomeTickerRow(
                            items = state.ticker.tickerItems,
                            visible = state.ticker.homeTickerVisible,
                            onMute = actions.onTickerMute,
                            onDismissItem = actions.onTickerDismissItem,
                            onHideType = actions.onTickerHideType,
                            onOpenTickerSettings = actions.onOpenTickerSettings,
                            onDisableTicker = actions.onDisableTicker,
                            autoAdvanceEnabled = state.ticker.tickerAutoAdvance,
                            autoAdvanceIntervalMs = state.ticker.tickerIntervalSeconds * 1000L,
                            onItemClick = actions.onTickerItemClick,
                        )
                    } else if (!state.ticker.tickerEnabled) {
                        FolderStatsRow(
                            folders = state.ticker.folders,
                            onOpenFolderStats = actions.onOpenFolderStats,
                            onOpenAppStats = actions.onOpenAppStats,
                            onOpenDashboard = actions.onOpenDashboard,
                            onOpenUsageReport = actions.onOpenUsageReport,
                        )
                    }
                }

                DashboardContentGroup.FAVORITES -> {
                    if (!state.hideSecondaryRowsForIme) {
                        HomeFavoritesSection(
                            favoritesEnabled = state.favorites.favoritesEnabled,
                            favoriteApps = state.favorites.favoriteApps,
                            suggestionsEnabled = state.favorites.suggestionsEnabled,
                            suggestedApps = state.favorites.suggestedApps,
                            suggestionsIconSizeDp = state.favorites.suggestionsIconSizeDp,
                            recentNotificationAppsEnabled = state.favorites.recentNotificationAppsEnabled,
                            recentNotificationApps = state.favorites.recentNotificationApps,
                            recentNotificationCounts = state.favorites.recentNotificationCounts,
                            recentAppsEnabled = state.favorites.recentAppsEnabled,
                            recentApps = state.favorites.recentApps,
                            dockPackages = state.favorites.dockPackages,
                            iconPackPkg = state.favorites.iconPackPkg,
                            haptic = androidx.compose.ui.platform.LocalHapticFeedback.current,
                            onLaunchApp = actions.onLaunchApp,
                            onAppLongClick = actions.onAppLongClick,
                            screenHeightDp = state.favorites.screenHeightDp,
                            showSecondaryRowsInCompactMode = false,
                        )
                    }
                }
            }
        }

        if (!hasAnyContent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.dashboard_empty_hint),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    }
}

/**
 * P16 — `SmartDashboardPage`'in CONTENT zone içinde birlikte taşınan (reorderable) blok grupları.
 * CLOCK/MISSIONS_AND_SCORE ikilisi ve RECENT_INSTALLS chip'i editörde de her zaman en üstte kalır
 * (roadmap madde 7 yalnız "Dashboard section reorder" der; sayaç/görev kartları sayfanın kimliğidir,
 * taşınmaz) — bu yüzden burada yer almaz. Boş durum mesajı (`hasAnyContent` false) her zaman en
 * altta sabit kalır.
 */
internal enum class DashboardContentGroup(val representativeSection: HomeSectionId) {
    SEARCH_AND_WIDGETS(HomeSectionId.GOOGLE_SEARCH),
    INSIGHTS_AND_TICKER(HomeSectionId.ASSISTANT_INSIGHTS),
    FAVORITES(HomeSectionId.FAVORITES),
}

/**
 * `contentOrder` (editörden gelen CONTENT zone bölüm sırası) içindeki her grubun temsilci
 * section'ının ilk göründüğü konuma göre grupları sıralar. `contentOrder` boşsa veya temsilci
 * section'lardan hiçbiri içinde yoksa `DashboardContentGroup.entries`'in doğal (varsayılan) sırası
 * korunur — geriye dönük uyumluluk, eski çağrı yerleri (testler) bozulmaz.
 *
 * Saf fonksiyon — Compose bağımlılığı yok, doğrudan test edilir.
 */
internal fun dashboardGroupOrder(contentOrder: List<HomeSectionId>): List<DashboardContentGroup> {
    if (contentOrder.isEmpty()) return DashboardContentGroup.entries
    return DashboardContentGroup.entries.sortedBy { group ->
        contentOrder.indexOf(group.representativeSection).let { if (it < 0) Int.MAX_VALUE else it }
    }
}

/**
 * Döngü P07 madde 3-4 — saf yardımcı: `state`'e göre şu an render edilecek (görünür) Dashboard
 * section sayısını sayar. `DashboardLayoutPolicy.mode()`'a girdi sağlar; Compose bağımlılığı
 * yoktur, `SmartDashboardPage` içindeki `if`/`when` görünürlük koşullarıyla birebir eşleşir —
 * ikisi ayrıştığında testler kırılır (bkz. SmartDashboardPageLogicTest).
 */
internal fun countVisibleSections(state: DashboardUiState): Int {
    var count = 1 // Saat (PulseClockWidget) her zaman render edilir.
    val hideSecondaryRows = state.hideSecondaryRowsForIme
    if (state.intelligence.todayCardEnabled) {
        // Görev S1 — intelligence/recentInstalls/insight üç ayrı bölüm yerine tek TodayCard.
        if (state.intelligence.todayCardSpec != null) count++
    } else {
        if (state.intelligence.missionsEnabled) count++
        if (state.intelligence.digitalLifeCardVisible && state.intelligence.pulse != null) count++
        if (state.recentInstalls.enabled && state.recentInstalls.apps.isNotEmpty()) count++
        if (state.insights.assistantCardsEnabled && !state.insights.tickerEnabled && state.insights.insightCards.isNotEmpty()) count++
    }
    if (!hideSecondaryRows && state.secondarySections.googleSearchEnabled) count++
    if (!hideSecondaryRows && state.secondarySections.widgetAreaEnabled && state.secondarySections.widgetIds.isNotEmpty()) count++
    if (state.ticker.tickerEnabled && !state.ticker.tickerMuted) {
        count++
    } else if (!state.ticker.tickerEnabled && state.ticker.folders.isNotEmpty()) {
        count++
    }
    if (!state.hideSecondaryRowsForIme &&
        (state.favorites.favoritesEnabled ||
            state.favorites.suggestionsEnabled ||
            state.favorites.recentNotificationAppsEnabled ||
            state.favorites.recentAppsEnabled)
    ) {
        count++
    }
    return count
}
