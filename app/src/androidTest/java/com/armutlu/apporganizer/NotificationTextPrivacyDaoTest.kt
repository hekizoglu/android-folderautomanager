package com.armutlu.apporganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTextPrivacyDaoTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun nonEmptyNotificationTextInputIsPersistedAsEmptyString() = runTest {
        val dao = database.appDao()
        dao.insertApp(
            AppInfo(
                packageName = "com.test.private",
                appName = "Private Test",
            )
        )

        dao.updateNotificationText(
            packageName = "com.test.private",
            text = "OTP 123456 hesabınıza giriş için kullanıldı",
        )

        assertEquals("", dao.getAppByPackageName("com.test.private")?.notificationText)
    }

    @Test
    fun batchNotificationTextInputCannotPersistContent() = runTest {
        val dao = database.appDao()
        dao.insertApps(
            listOf(
                AppInfo(packageName = "com.test.one", appName = "One"),
                AppInfo(packageName = "com.test.two", appName = "Two"),
            )
        )

        dao.updateNotificationTexts(
            mapOf(
                "com.test.one" to "Bakiye 5.000 TL",
                "com.test.two" to "Doğrulama kodu 654321",
            )
        )

        assertEquals("", dao.getAppByPackageName("com.test.one")?.notificationText)
        assertEquals("", dao.getAppByPackageName("com.test.two")?.notificationText)
    }

    @Test
    fun legacyNotificationTextRowsArePurged() = runTest {
        val dao = database.appDao()
        dao.insertApp(
            AppInfo(
                packageName = "com.test.legacy",
                appName = "Legacy",
                notificationText = "Eski kalıcı bildirim içeriği",
            )
        )

        dao.clearAllNotificationTexts()

        assertEquals("", dao.getAppByPackageName("com.test.legacy")?.notificationText)
    }
}
