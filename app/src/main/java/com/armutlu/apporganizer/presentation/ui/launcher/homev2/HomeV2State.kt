package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.runtime.Immutable
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder

/**
 * Home V2 — tek immutable ekran state'i.
 *
 * HomeV2Screen bu modeli render eder; model [HomeV2Assembler] (saf fonksiyon) tarafından
 * ViewModel akışlarından üretilir ve birim testleriyle kilitlenir. UI'da ayrıca hesap yok.
 */
@Immutable
data class HomeV2State(
    val loading: Boolean,
    val folders: List<FolderTileState>,
    val banners: List<HomeBannerState>,
    val pulse: PulseStripState?,
    val dockPackages: List<String>,
    val pageSize: Int,
)

/** Bir klasör kartının render için ihtiyaç duyduğu her şey. */
@Immutable
data class FolderTileState(
    val categoryId: String,
    val title: String,
    val emoji: String,
    val colorHex: String,
    val appCount: Int,
    /** Kullanım miktarına göre sıralı ilk N uygulamanın paket adları (önizleme ikonları). */
    val previewPackages: List<String>,
    /** Klasördeki toplam bekleyen bildirim sayısı. */
    val notificationTotal: Int,
    /** Acil (yüksek önem) bildirim taşıyan uygulama var mı — kart halkasını belirler. */
    val hasUrgentNotification: Boolean,
    /** Hızlı başlatma hedefi: klasörün en sık açılan görünür uygulaması (yoksa null). */
    val quickLaunchPackage: String?,
)

/** Kompakt kapatılabilir uyarı şeridi. */
@Immutable
data class HomeBannerState(
    val id: String,
    val text: String,
    val actionLabel: String?,
)

/** Üst şerit: dijital nabız + görev özeti (yalnız veri varsa gösterilir). */
@Immutable
data class PulseStripState(
    val pulseScoreText: String?,
    val pulseBandLabel: String?,
    val missionTitle: String?,
    val missionProgressFraction: Float?,
    val missionStreak: Int,
)

/** Önem eşiği: rozet halkası ve "acil" vurgusu bu ve üzeri önemde tetiklenir. */
internal const val URGENT_IMPORTANCE_THRESHOLD = 2

/** Önizleme şeridindeki maksimum ikon sayısı. */
internal const val FOLDER_PREVIEW_LIMIT = 4

/**
 * Saf assembler — Android/Compose bağımlılığı yok, doğrudan birim testinden çağrılabilir.
 * ViewModel akış snapshot'larını tek [HomeV2State]'e indirger.
 */
object HomeV2Assembler {

    fun assemble(
        initialLoadDone: Boolean,
        folders: List<AppFolder>,
        dockPackages: List<String>,
        pageSize: Int,
        pendingClassificationsCount: Int,
        notificationPermissionMissing: Boolean,
        pulseSummary: HomePulseSummary?,
        missionSummary: HomeMissionSummary?,
        bannerDismissals: Set<String>,
    ): HomeV2State {
        return HomeV2State(
            loading = !initialLoadDone,
            folders = folders.map { buildFolderTile(it) },
            banners = buildBanners(pendingClassificationsCount, notificationPermissionMissing, bannerDismissals),
            pulse = buildPulseStrip(pulseSummary, missionSummary),
            dockPackages = dockPackages,
            pageSize = pageSize.coerceAtLeast(1),
        )
    }

    private fun buildFolderTile(folder: AppFolder): FolderTileState {
        val visibleApps = folder.apps.filter { !it.isHidden }
        val preview = visibleApps
            .sortedWith(compareByDescending<AppInfo> { it.usageCount }.thenByDescending { it.lastUsedTimestamp })
            .take(FOLDER_PREVIEW_LIMIT)
            .map { it.packageName }
        return FolderTileState(
            categoryId = folder.category.categoryId,
            title = folder.category.categoryName,
            emoji = folder.category.iconEmoji,
            colorHex = folder.category.colorHex,
            appCount = visibleApps.size,
            previewPackages = preview,
            notificationTotal = visibleApps.sumOf { it.notificationCount },
            hasUrgentNotification = visibleApps.any {
                it.notificationCount > 0 && it.notificationImportance >= URGENT_IMPORTANCE_THRESHOLD
            },
            quickLaunchPackage = FolderQuickLaunchResolver.resolve(visibleApps)?.packageName,
        )
    }

    private fun buildBanners(
        pendingClassificationsCount: Int,
        notificationPermissionMissing: Boolean,
        dismissals: Set<String>,
    ): List<HomeBannerState> {
        val banners = mutableListOf<HomeBannerState>()
        if (notificationPermissionMissing) {
            banners += HomeBannerState(
                id = BANNER_ID_NOTIFICATION_PERMISSION,
                text = "Bildirim rozetleri için bildirim izni gerekli",
                actionLabel = "İzin ver",
            )
        }
        if (pendingClassificationsCount > 0) {
            banners += HomeBannerState(
                id = BANNER_ID_PENDING_CLASSIFICATIONS,
                text = "$pendingClassificationsCount uygulama sınıflandırma bekliyor",
                actionLabel = "İncele",
            )
        }
        // En fazla tek banner gösterilir — öncelik: izin > sınıflandırma. Kapatılanlar elenir.
        return banners.filterNot { it.id in dismissals }.take(1)
    }

    private fun buildPulseStrip(
        pulseSummary: HomePulseSummary?,
        missionSummary: HomeMissionSummary?,
    ): PulseStripState? {
        val pulseText = pulseSummary
            ?.takeIf { !it.shouldHideScore && it.isActionable }
            ?.score
            ?.toString()
        val bandLabel = pulseSummary?.takeIf { !it.shouldHideScore }?.statusBand?.name
        val missionTitle = missionSummary?.primaryTitle
        val hasPulse = pulseText != null
        val hasMission = missionSummary != null && missionSummary.totalCount > 0
        if (!hasPulse && !hasMission) return null
        return PulseStripState(
            pulseScoreText = pulseText,
            pulseBandLabel = bandLabel,
            missionTitle = missionTitle,
            missionProgressFraction = missionSummary?.primaryProgressFraction,
            missionStreak = missionSummary?.currentStreak ?: 0,
        )
    }

    const val BANNER_ID_NOTIFICATION_PERMISSION = "notif_permission"
    const val BANNER_ID_PENDING_CLASSIFICATIONS = "pending_classifications"
}
