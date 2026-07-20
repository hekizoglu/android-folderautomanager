package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.HomeGridItemDao
import com.armutlu.apporganizer.data.local.HomeGridItemEntity
import com.armutlu.apporganizer.domain.home.GridPosition
import com.armutlu.apporganizer.domain.home.HomeGridItemType
import com.armutlu.apporganizer.domain.home.HomeGridItemTypeCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Faz S3 — [WidgetFreeGrid] için ince ViewModel. [FolderFreeGridViewModel] ile birebir aynı
 * desen: [HomeGridItemDao] üzerinden Dashboard widget pozisyonlarını okur/yazar. Tek fark
 * itemType filtresi — burada [HomeGridItemType.WIDGET] kullanılır (klasör içi uygulamalarla
 * ([HomeGridItemType.APP_SHORTCUT]) aynı `screenIndex` kullanılsa bile Room sorgusu tip bazlı
 * filtrelediği için çakışma olmaz; ayrıca [widgetGridScreenIndex] klasörlerden farklı sabit bir
 * değerdir).
 */
@HiltViewModel
class WidgetFreeGridViewModel @Inject constructor(
    private val dao: HomeGridItemDao,
) : ViewModel() {

    private val screenFlows = mutableMapOf<Int, StateFlow<Map<String, GridPosition>>>()

    /** Dashboard'daki tüm widget'ların pozisyon haritası. */
    fun observePositions(screenIndex: Int): StateFlow<Map<String, GridPosition>> {
        return screenFlows.getOrPut(screenIndex) {
            dao.observeByScreen(screenIndex)
                .map { entities ->
                    entities
                        .filter { HomeGridItemTypeCodec.decode(it.itemType) == HomeGridItemType.WIDGET }
                        .associate { entity ->
                            entity.itemId to GridPosition(
                                cellX = entity.cellX,
                                cellY = entity.cellY,
                                spanX = entity.spanX,
                                spanY = entity.spanY,
                            )
                        }
                }
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
        }
    }

    /** Verilen itemId -> GridPosition eşlemesini Room'a kalıcı yazar (REPLACE ile upsert). */
    fun persistPositions(screenIndex: Int, positions: Map<String, GridPosition>) {
        if (positions.isEmpty()) return
        viewModelScope.launch {
            val entities = positions.map { (itemId, pos) ->
                HomeGridItemEntity(
                    itemId = itemId,
                    itemType = HomeGridItemTypeCodec.encode(HomeGridItemType.WIDGET),
                    screenIndex = screenIndex,
                    cellX = pos.cellX,
                    cellY = pos.cellY,
                    spanX = pos.spanX,
                    spanY = pos.spanY,
                )
            }
            dao.insertAll(entities)
        }
    }

    /** Room'dan kaldırılan bir widget'ın grid konumunu da temizler. */
    fun removePosition(widgetId: Int) {
        viewModelScope.launch {
            dao.deleteById(widgetGridItemId(widgetId))
        }
    }
}
