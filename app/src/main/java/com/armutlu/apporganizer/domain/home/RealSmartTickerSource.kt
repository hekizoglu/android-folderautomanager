package com.armutlu.apporganizer.domain.home

import android.content.Context
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.AppSnapshot
import com.armutlu.apporganizer.utils.FolderSnapshot
import com.armutlu.apporganizer.utils.InsightEngine
import com.armutlu.apporganizer.utils.InsightSnapshot
import com.armutlu.apporganizer.utils.TickerComposer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Döngü T01 — [SmartTickerEngine] için GERÇEK kaynak. [TickerComposer]'ın ürettiği
 * [SmartTickerItem] listesini [HomeIntelligenceCoordinator]'a taşır.
 *
 * NOT: Bu kaynak yalnızca TickerComposer'ın "tekil, koşulsuz" üreticilerini besler (bildirim
 * özeti, unutulan uygulama, içgörü, düşük güven uyarısı, özellik ipucu, haftalık özet).
 * `LauncherViewModel.tickerItems` ekranın gerçek kaynağıdır — kendi dock/klasör state'lerini
 * kullanarak reaktif biçimde aynı [TickerComposer.compose] fonksiyonunu çağırır ve ek olarak
 * arama istatistiği/Wrapped teaser haberlerini + oturum-bazlı dismiss filtresini uygular. Bu
 * sınıf [HomeIntelligenceCoordinator] üzerinden okunan koordinatör-seviyesi state için ayrı bir
 * (daha basit, DB'den taze) anlık görüntü üretir — görev/dijital-yaşam öğelerinin şeride
 * eklenmesi (T03) ve sıralama/limit (T02) burada henüz uygulanmaz.
 */
@Singleton
class RealSmartTickerSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val classifier: AppClassifier,
) : SmartTickerEngine {

    private val _state = MutableStateFlow(TickerSourceState())
    override val state: StateFlow<TickerSourceState> = _state.asStateFlow()

    override suspend fun refresh() {
        runCatching {
            val apps = appRepository.getAllApps().filter { !it.isHidden }
            val categories = Category.getDefaultCategories()
            val categoryNameById = categories.associate { it.categoryId to it.categoryName }
            val categoryEmojiById = categories.associate { it.categoryId to it.iconEmoji }

            val folderSnapshots = apps
                .groupBy { it.categoryId }
                .map { (categoryId, appsInCategory) ->
                    FolderSnapshot(
                        categoryId = categoryId,
                        categoryName = categoryNameById[categoryId] ?: categoryId,
                        emoji = categoryEmojiById[categoryId] ?: "📁",
                        appCount = appsInCategory.size,
                    )
                }

            val appSnapshots = apps.map { app ->
                AppSnapshot(
                    packageName = app.packageName,
                    appName = app.appName,
                    usageCount = app.usageCount,
                    lastUsedTimestamp = app.lastUsedTimestamp,
                )
            }

            val badgeCounts = AppNotificationListenerService.badgeCounts.value
            val badgeTotal = badgeCounts.values.sum()

            val insightSnapshots = InsightEngine.generate(
                context = context,
                apps = apps,
                categories = categories,
                badgeCounts = badgeCounts,
            ).map { card ->
                InsightSnapshot(
                    id = card.id,
                    message = card.message,
                    categoryId = card.categoryId,
                    packageName = card.packageName,
                )
            }

            val showSystemApps = AppPrefs.isShowSystemApps(context)
            val lowConfidenceCount = apps.count { app ->
                (showSystemApps || !app.isSystemApp) && classifier.isLowConfidence(app, app.categoryId)
            }

            TickerComposer.compose(
                folders = folderSnapshots,
                apps = appSnapshots,
                badgeTotal = badgeTotal,
                insights = insightSnapshots,
                lowConfidenceCount = lowConfidenceCount,
                nowMillis = System.currentTimeMillis(),
            )
        }.fold(
            onSuccess = { items -> _state.value = TickerSourceState(items = items) },
            onFailure = { error ->
                Timber.w(error, "RealSmartTickerSource: refresh hatasi")
                // Hata durumunda onceki state korunur (coordinator Stale/Failed sinifladirmasini
                // digerleri gibi kendi refreshSource try/catch'inde yapar).
            },
        )
    }
}
