package com.armutlu.apporganizer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.TickerHistoryDao
import com.armutlu.apporganizer.data.local.TickerHistoryEntity
import com.armutlu.apporganizer.domain.home.TickerAction
import com.armutlu.apporganizer.domain.home.TickerActionCodec
import com.armutlu.apporganizer.domain.home.TickerActionRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "Tüm haberler" arşiv ekranı (mail kutusu — okundu/okunmadı) için Hilt ViewModel.
 * [TickerHistoryDao.observeAll] doğrudan tüketilir; tıklama hedefi çözümü [TickerActionCodec]
 * ile geri çözülür ve mevcut [com.armutlu.apporganizer.domain.home.TickerActionRouter] YENİDEN
 * KULLANILIR (kopyalanmaz) — ekran sadece decode edilmiş [TickerAction]'ı Screen'e iletir.
 */
@HiltViewModel
class TickerHistoryViewModel @Inject constructor(
    private val dao: TickerHistoryDao,
) : ViewModel() {

    data class TickerHistoryUi(
        val id: String,
        val type: String,
        val title: String,
        val subtitle: String?,
        val icon: String,
        val createdAt: Long,
        val isRead: Boolean,
        val action: TickerAction,
        val sensitive: Boolean,
    )

    val items: StateFlow<List<TickerHistoryUi>> = dao.observeAll()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val unreadCount: StateFlow<Int> = dao.countUnread()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), 0)

    fun markRead(id: String) {
        viewModelScope.launch { dao.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { dao.markAllRead() }
    }

    /**
     * [TickerAction] -> gerçek navigasyon route string'i. [TickerActionRouter.resolve] (mevcut
     * TEK çözüm noktası) ve [com.armutlu.apporganizer.presentation.navigation.Routes.fromTickerRoute]
     * (LauncherViewModel.resolveTickerTarget ile PAYLAŞILAN eşleme) YENİDEN KULLANILIR —
     * kopyalanmaz. Route bulunamazsa null döner (Screen dokunmayı yok sayar).
     */
    fun resolveRoute(action: TickerAction): String? {
        val target = TickerActionRouter.resolve(action)
        val screen = target as? TickerActionRouter.RouteTarget.Screen ?: return null
        return com.armutlu.apporganizer.presentation.navigation.Routes.fromTickerRoute(screen.route)
            ?: screen.route
    }

    private fun TickerHistoryEntity.toUi(): TickerHistoryUi = TickerHistoryUi(
        id = id,
        type = type,
        title = title,
        subtitle = subtitle,
        icon = icon,
        createdAt = createdAt,
        isRead = isRead,
        action = TickerActionCodec.decode(actionType),
        sensitive = sensitive,
    )
}
