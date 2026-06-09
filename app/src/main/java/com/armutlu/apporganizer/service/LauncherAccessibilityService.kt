package com.armutlu.apporganizer.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LauncherAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}
}
