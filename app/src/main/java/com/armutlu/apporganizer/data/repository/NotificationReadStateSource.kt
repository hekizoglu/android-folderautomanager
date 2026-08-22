package com.armutlu.apporganizer.data.repository

import android.content.Context
import com.armutlu.apporganizer.utils.NotificationReadPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

interface NotificationReadStateSource {
    val lastReadAt: StateFlow<Map<String, Long>>
}

class SharedPrefsNotificationReadStateSource @Inject constructor(
    @ApplicationContext context: Context,
) : NotificationReadStateSource {
    override val lastReadAt: StateFlow<Map<String, Long>> =
        NotificationReadPrefs.observe(context)
}
