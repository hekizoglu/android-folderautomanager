package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseAction
import com.armutlu.apporganizer.domain.home.TodayCardSpec

/**
 * Hero Dashboard migration — Commit 1 (bkz. YENI_HERO_DASHBOARD roadmap).
 *
 * Eski `SmartDashboardPage` onlarca alt-state taşıyordu (recentInstalls/insights/ticker/
 * secondarySections/favorites/contentOrder). Bu commit ile Dashboard, Hero tasarımına geçiş
 * için sadeleştirildi: yalnızca saat + "BUGÜN" bağlamsal kart state'i kalır. Kaldırılan
 * state'ler (`DashboardRecentInstallsState`, `DashboardInsightsState`, `DashboardTickerState`,
 * `DashboardSecondarySectionsState`, `DashboardFavoritesState`, `contentOrder`) Hero tasarımı
 * netleştikçe (TODO: HeroDashboardPage) yeniden eklenecek — bilinçli geçici kayıp.
 *
 * Compose/Android bağımlılığı yoktur (yalnızca domain tipleri) — saf veri taşıyıcıdır.
 */
data class DashboardClockState(
    val compact: Boolean,
)

data class DashboardIntelligenceState(
    val missionsEnabled: Boolean,
    val mission: HomeMissionSummary?,
    val digitalLifeCardVisible: Boolean,
    val pulse: HomePulseSummary?,
    // Görev S1 — açıkken bu bölümün (HomeMissionCard+DigitalLifeCard) yerine tek TodayCard çizilir.
    // [todayCardSpec] null ise (TodayCardSelector hiçbir önceliği seçemedi) kart hiç gösterilmez.
    val todayCardEnabled: Boolean = false,
    val todayCardSpec: TodayCardSpec? = null,
)

/**
 * `SmartDashboardPage`'e verilen state — Hero migration Commit 1 sonrası yalnızca saat ve
 * bağlamsal kart bilgisini taşır. Diğer bölümler (arama/widget/ticker/favoriler) Hero tasarımı
 * netleşene kadar geçici olarak kaldırıldı.
 */
data class DashboardUiState(
    val clock: DashboardClockState,
    val intelligence: DashboardIntelligenceState,
)

/** `SmartDashboardPage` içindeki tıklama/eylem callback'leri — tek yerde toplanır. */
data class DashboardActions(
    val onOpenWeeklyReport: () -> Unit,
    val onOpenScoreDetails: () -> Unit,
    val onClockLongPress: () -> Unit,
    val onMissionClick: () -> Unit,
    val onPulseClick: () -> Unit,
    val onPulseReasonAction: (PulseAction) -> Unit,
    val onOpenFolderStats: () -> Unit,
)
