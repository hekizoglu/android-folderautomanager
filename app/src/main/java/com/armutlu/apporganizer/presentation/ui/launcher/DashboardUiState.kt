package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState

/**
 * Hero Dashboard migration — Commit 1 (bkz. YENI_HERO_DASHBOARD roadmap).
 *
 * Eski section tabanlı dashboard state'i kaldırılmıştır. Hero yalnız saat, tek Dijital Yaşam
 * özeti ve Akıllı Erişim state'ini taşır; widget/ticker/favori satırları Sayfa 0'a dönmez.
 *
 * Compose/Android bağımlılığı yoktur (yalnızca domain tipleri) — saf veri taşıyıcıdır.
 */
/**
 * `SmartDashboardPage` için tamamlanmış Hero state sözleşmesi.
 */
data class DashboardUiState(
    val pulse: HomePulseSummary?,
    val smartAccess: SmartAccessUiState,
)

/** `SmartDashboardPage` içindeki tıklama/eylem callback'leri — tek yerde toplanır. */
data class DashboardActions(
    val onOpenWeeklyReport: () -> Unit,
    val onClockLongPress: () -> Unit,
    val onPulseClick: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenSearchSettings: () -> Unit,
    val onOpenSmartAccessSettings: () -> Unit,
    val onLaunchApp: (String) -> Unit,
    val onAppLongClick: (String) -> Unit,
)
