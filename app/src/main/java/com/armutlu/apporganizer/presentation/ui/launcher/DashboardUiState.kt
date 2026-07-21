package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseAction
import com.armutlu.apporganizer.domain.home.TodayCardSpec
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState

/**
 * Hero Dashboard migration — Commit 1 (bkz. YENI_HERO_DASHBOARD roadmap).
 *
 * Eski section tabanlı dashboard state'i kaldırılmıştır. Hero yalnız saat, tek Dijital Yaşam
 * özeti ve Akıllı Erişim state'ini taşır; widget/ticker/favori satırları Sayfa 0'a dönmez.
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
 * `SmartDashboardPage` için tamamlanmış Hero state sözleşmesi.
 */
data class DashboardUiState(
    val clock: DashboardClockState,
    val intelligence: DashboardIntelligenceState,
    val smartAccess: SmartAccessUiState,
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
    val onOpenSearch: () -> Unit,
    val onOpenSearchSettings: () -> Unit,
    val onOpenSmartAccessSettings: () -> Unit,
    val onLaunchApp: (String) -> Unit,
    val onAppLongClick: (String) -> Unit,
)
