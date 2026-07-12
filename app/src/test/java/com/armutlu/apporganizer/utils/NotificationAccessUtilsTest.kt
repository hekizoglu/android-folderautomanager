package com.armutlu.apporganizer.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessUtilsTest {

    @Test
    fun `containsPackage matches exact flattened component package`() {
        val flat = "com.example.listener/com.example.listener.AppNotificationListenerService:" +
            "com.other.app/com.other.app.Listener"

        assertTrue(NotificationAccessUtils.containsPackage(flat, "com.example.listener"))
    }

    @Test
    fun `containsPackage does not match substring package names`() {
        val flat = "com.example.listener.pro/com.example.listener.pro.AppNotificationListenerService"

        assertFalse(NotificationAccessUtils.containsPackage(flat, "com.example.listener"))
    }
}
