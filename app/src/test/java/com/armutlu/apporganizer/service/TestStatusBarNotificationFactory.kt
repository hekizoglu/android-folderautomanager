package com.armutlu.apporganizer.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import io.mockk.every
import io.mockk.mockk

object TestStatusBarNotificationFactory {
    fun create(
        packageName: String,
        key: String,
        title: String,
        text: String,
        bigText: String = "",
        postTime: Long = 1L,
    ): StatusBarNotification {
        val extras = Bundle().apply {
            putCharSequence(Notification.EXTRA_TITLE, title)
            putCharSequence(Notification.EXTRA_TEXT, text)
            putCharSequence(Notification.EXTRA_BIG_TEXT, bigText)
        }
        val notification = Notification().apply {
            this.extras = extras
            `when` = postTime
        }
        return mockk(relaxed = true) {
            every { this@mockk.packageName } returns packageName
            every { this@mockk.key } returns key
            every { this@mockk.notification } returns notification
            every { this@mockk.postTime } returns postTime
            every { this@mockk.isOngoing } returns false
        }
    }
}
