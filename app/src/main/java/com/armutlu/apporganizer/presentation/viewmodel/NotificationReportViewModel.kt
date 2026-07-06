package com.armutlu.apporganizer.presentation.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.NotificationEventDao
import com.armutlu.apporganizer.utils.NotificationAnalyzer
import com.armutlu.apporganizer.utils.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Son 7 günün bildirim davranışını analiz eder — NotificationAnalyzer'ı DB verisiyle besler.
 */
@HiltViewModel
class NotificationReportViewModel @Inject constructor(
    private val notificationEventDao: NotificationEventDao,
    private val appDao: AppDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationAnalyzer.Report?>(null)
    val uiState: StateFlow<NotificationAnalyzer.Report?> = _uiState.asStateFlow()

    private val _listenerPermissionGranted = MutableStateFlow(checkListenerPermission())
    val listenerPermissionGranted: StateFlow<Boolean> = _listenerPermissionGranted.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _listenerPermissionGranted.value = checkListenerPermission()
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) {
                runCatching {
                    val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    val events = notificationEventDao.eventsSince(since)
                    val appNames = appDao.getAllApps().associate { it.packageName to it.appName }
                    val usageMs = if (UsageStatsHelper.hasPermission(context)) {
                        UsageStatsHelper.getUsageCounts(context, days = 7)
                    } else {
                        emptyMap()
                    }
                    NotificationAnalyzer.analyze(events, appNames, usageMs)
                }.onFailure { e ->
                    Timber.e(e, "Bildirim raporu üretilemedi")
                }.getOrDefault(
                    NotificationAnalyzer.Report(0, emptyList(), emptyList(), emptyList(), emptyList())
                )
            }
            _uiState.value = report
        }
    }

    private fun checkListenerPermission(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            enabled?.contains(context.packageName) == true
        } catch (e: Exception) {
            false
        }
    }
}
