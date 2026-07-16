package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class AppPrefsTelemetryConsentTest {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE) } returns prefs
    }

    @Test
    fun `fresh install defaults consent and telemetry to disabled`() {
        every { prefs.getBoolean(AppPrefs.KEY_TELEMETRY_CONSENT_DECIDED, false) } returns false
        every { prefs.getBoolean(AppPrefs.KEY_TELEMETRY_ENABLED, false) } returns false

        assertFalse(AppPrefs.isTelemetryConsentDecided(context))
        assertFalse(AppPrefs.isTelemetryEnabled(context))
    }

    @Test
    fun `consent writes all persistence fields atomically`() {
        AppPrefs.setTelemetryConsent(context, enabled = true, version = 1, changedAt = 1234L)

        verify { editor.putBoolean(AppPrefs.KEY_TELEMETRY_CONSENT_DECIDED, true) }
        verify { editor.putBoolean(AppPrefs.KEY_TELEMETRY_ENABLED, true) }
        verify { editor.putInt(AppPrefs.KEY_TELEMETRY_CONSENT_VERSION, 1) }
        verify { editor.putLong(AppPrefs.KEY_TELEMETRY_LAST_CHANGED_AT, 1234L) }
        verify(exactly = 1) { editor.apply() }
    }
}
