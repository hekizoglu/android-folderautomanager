package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * D242e — Onboarding sonsuz döngü regresyonu (emülatör + gerçek cihaz testinde bulundu):
 * [AppPrefs.markOnboardingDone] önceden marker dosyası yazımı (writeInstallMarker) başarısız
 * olsa bile `onboarding_done` flag'ini true yapıyordu — bir sonraki [AppPrefs.isOnboardingDone]
 * çağrısı `flagDone=true && hasInstallMarker=false` olduğu için hep `false` dönüp onboarding'i
 * sonsuza kadar tekrar gösteriyordu. Fix: flag SADECE marker dosyası gerçekten yazıldıysa true
 * olur.
 */
class AppPrefsOnboardingMarkerTest {

    private lateinit var context: Context
    private lateinit var backingPrefs: SharedPreferences
    private lateinit var filesDir: File

    @Before
    fun setup() {
        backingPrefs = OnboardingMarkerFakeSharedPreferences()
        filesDir = File.createTempFile("appprefs_test", "").apply {
            delete()
            mkdirs()
        }
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns backingPrefs
        every { context.filesDir } returns filesDir
    }

    @Test
    fun `marker yazma basarili olunca flag true olur ve isOnboardingDone true doner`() {
        AppPrefs.markOnboardingDone(context)

        assertTrue(File(filesDir, "install_marker").exists())
        assertTrue(AppPrefs.isOnboardingDone(context))
    }

    @Test
    fun `marker dosyasi yoksa flag true olsa bile isOnboardingDone false doner`() {
        // Eski (bug'lı) davranışı simüle et: flag doğrudan true yazılmış ama marker hiç yok.
        backingPrefs.edit().putBoolean(AppPrefs.KEY_ONBOARDING_DONE, true).apply()

        assertFalse(AppPrefs.isOnboardingDone(context))
    }

    @Test
    fun `resetOnboarding sonrasi hem flag hem marker temizlenir`() {
        AppPrefs.markOnboardingDone(context)
        assertTrue(AppPrefs.isOnboardingDone(context))

        AppPrefs.resetOnboarding(context)

        assertFalse(AppPrefs.isOnboardingDone(context))
        assertFalse(File(filesDir, "install_marker").exists())
    }
}

private class OnboardingMarkerFakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST")
        (map[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(map)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            key?.let { pending[it] = values }
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { pending[it] = value }
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { removals.add(it) }
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearAll) map.clear()
            removals.forEach { map.remove(it) }
            pending.forEach { (k, v) -> map[k] = v }
        }
    }
}
