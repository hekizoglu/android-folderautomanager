package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.HomeGridItemDao
import com.armutlu.apporganizer.data.local.HomeGridItemEntity
import com.armutlu.apporganizer.domain.home.GridPosition
import com.armutlu.apporganizer.domain.home.HomeGridItemType
import com.armutlu.apporganizer.domain.home.HomeGridItemTypeCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Faz S2 — [FolderFreeGrid] için ince ViewModel. [HomeGridItemDao] üzerinden klasör içi
 * uygulama pozisyonlarını okur/yazar. Her `screenIndex` başına ayrı bir [StateFlow] cache'lenir
 * (birden fazla klasör açılıp kapanabilir — aynı ViewModel örneği hiltViewModel() ile Composable
 * yaşam döngüsüne bağlı kalır).
 */
@HiltViewModel
class FolderFreeGridViewModel @Inject constructor(
    private val dao: HomeGridItemDao,
) : ViewModel() {

    private val screenFlows = mutableMapOf<Int, StateFlow<Map<String, GridPosition>>>()

    /** Belirli bir sanal ekrandaki (klasördeki) tüm uygulama kısayollarının pozisyon haritası. */
    fun observePositions(screenIndex: Int): StateFlow<Map<String, GridPosition>> {
        return screenFlows.getOrPut(screenIndex) {
            dao.observeByScreen(screenIndex)
                .map { entities ->
                    entities
                        .filter { HomeGridItemTypeCodec.decode(it.itemType) == HomeGridItemType.APP_SHORTCUT }
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
                    itemType = HomeGridItemTypeCodec.encode(HomeGridItemType.APP_SHORTCUT),
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
}
