package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState
import com.armutlu.apporganizer.domain.models.HomeSectionId

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
    val pendingClassificationCount: Int = 0, // P1.2: Badge için beklemede olan sınıflandırma sayısı
    // D240 — Ana Ekranı Düzenle editöründen (HomeLayoutEditorScreen) gelen gerçek CONTENT sırası/
    // görünürlüğü. HomeSectionRenderer.dashboardContentOrder(config) ile üretilir, HeroDashboardPage
    // bu listeye göre CLOCK/MISSIONS_AND_SCORE'u sıralar ve SmartAccessCard grubunu gizler/gösterir.
    val contentOrder: List<HomeSectionId> = HomeSectionId.entries,
    val missionSummary: HomeMissionSummary? = null,
    val notificationCount24h: Int = 0,
)

/** `SmartDashboardPage` içindeki tıklama/eylem callback'leri — tek yerde toplanır. */
data class DashboardActions(
    val onOpenWeeklyReport: () -> Unit,
    val onClockLongPress: () -> Unit,
    val onPulseClick: () -> Unit,
    val onOpenUsageAccessSettings: () -> Unit,
    val onOpenNotificationAccessSettings: () -> Unit,
    val onLaunchApp: (String) -> Unit,
    val onAppLongClick: (String) -> Unit,
    val onOpenClassificationReview: () -> Unit = {}, // P1.2: Sınıflandırma inceleme ekranı
    val onOpenMissions: () -> Unit = {},
    val onOpenFolderReview: () -> Unit = {},
    val onOpenNotificationHistory: () -> Unit = {},
)
