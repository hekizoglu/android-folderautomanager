package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.domain.models.SmartNotificationSettings
import com.armutlu.apporganizer.utils.SmartNotificationPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

interface SmartNotificationSettingsSource {
    val settings: StateFlow<SmartNotificationSettings>
}

class SharedPrefsSmartNotificationSettingsSource @Inject constructor(
    @ApplicationContext context: Context,
) : SmartNotificationSettingsSource {
    init {
        SmartNotificationPrefs.initialize(context)
    }

    override val settings: StateFlow<SmartNotificationSettings> = SmartNotificationPrefs.settings
}
