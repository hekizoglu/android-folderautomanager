package com.armutlu.apporganizer.domain.home.smartaccess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAccessNotificationPolicyTest {
    @Test fun `pencere siniri dahildir`() {
        assertTrue(SmartAccessNotificationPolicy.isWithinWindow(76L, 100L, 24L))
    }

    @Test fun `pencere disindaki ve gecersiz kayitlar dislanir`() {
        assertFalse(SmartAccessNotificationPolicy.isWithinWindow(75L, 100L, 24L))
        assertFalse(SmartAccessNotificationPolicy.isWithinWindow(0L, 100L, 24L))
        assertFalse(SmartAccessNotificationPolicy.isWithinWindow(100L, 100L, 0L))
    }
}
