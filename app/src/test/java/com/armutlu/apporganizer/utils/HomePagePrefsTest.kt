package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.presentation.ui.launcher.AppFolder
import com.armutlu.apporganizer.presentation.ui.launcher.model.HomePageAnchor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Döngü P02 — HomePagePrefs migration + anchor get/set testleri.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md Döngü P02 (satır 449-501).
 */
class HomePagePrefsTest {

    private lateinit var context: Context
    private lateinit var homePageBacking: SharedPreferences
    private lateinit var appPrefsBacking: SharedPreferences

    private fun folder(categoryId: String, order: Int = 0): AppFolder =
        AppFolder(
            category = Category(categoryId = categoryId, categoryName = categoryId, displayOrder = order),
            apps = emptyList(),
        )

    @Before
    fun setup() {
        homePageBacking = HomePagePrefsFakeSharedPreferences()
        appPrefsBacking = HomePagePrefsFakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(HomePagePrefs.PREFS_NAME, any()) } returns homePageBacking
        every { context.getSharedPreferences(AppPrefs.PREFS_NAME, any()) } returns appPrefsBacking
    }

    // ── Saf fonksiyon: eski index -> anchor ─────────────────────────────────

    @Test fun `legacy page 0 with folder A maps to folder A anchor`() {
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(
            legacyIndex = 0, folders = listOf(folder("A"), folder("B")), pageSize = 1,
        )
        assertEquals(HomePageAnchor.Folder("A"), anchor)
    }

    @Test fun `legacy page 2 with pageSize change finds correct folder anchor`() {
        // 5 klasor, pageSize=2 -> sayfa 2 = index 4 (tek klasor: E)
        val folders = listOf(folder("A"), folder("B"), folder("C"), folder("D"), folder("E"))
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(legacyIndex = 2, folders = folders, pageSize = 2)
        assertEquals(HomePageAnchor.Folder("E"), anchor)
    }

    @Test fun `legacy index out of range falls back to dashboard`() {
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(
            legacyIndex = 9, folders = listOf(folder("A")), pageSize = 1,
        )
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    @Test fun `empty folders falls back to dashboard`() {
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(legacyIndex = 0, folders = emptyList(), pageSize = 8)
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    @Test fun `negative legacy index falls back to dashboard`() {
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(legacyIndex = -1, folders = listOf(folder("A")), pageSize = 8)
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    @Test fun `zero page size falls back to dashboard`() {
        val anchor = HomePagePrefs.deriveAnchorFromLegacyIndex(legacyIndex = 0, folders = listOf(folder("A")), pageSize = 0)
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    // ── Migration bayrağı (Context tabanlı, tek seferlik) ───────────────────

    @Test fun `migration runs once and writes folder anchor from legacy int`() {
        appPrefsBacking.edit().putInt(AppPrefs.KEY_LAST_HOME_PAGE, 0).apply()
        val folders = listOf(folder("A"), folder("B"))

        val anchor = HomePagePrefs.getLastHomePageAnchor(context, folders, pageSize = 1)

        assertEquals(HomePageAnchor.Folder("A"), anchor)
        assertTrue(homePageBacking.getBoolean(HomePagePrefs.KEY_HOME_PAGER_MIGRATED, false))
    }

    @Test fun `migration does not run twice even if legacy value changes`() {
        appPrefsBacking.edit().putInt(AppPrefs.KEY_LAST_HOME_PAGE, 0).apply()
        val folders = listOf(folder("A"), folder("B"))

        val first = HomePagePrefs.getLastHomePageAnchor(context, folders, pageSize = 1)
        assertEquals(HomePageAnchor.Folder("A"), first)

        // Legacy deger degisse bile migration tekrar calismamali.
        appPrefsBacking.edit().putInt(AppPrefs.KEY_LAST_HOME_PAGE, 1).apply()
        val second = HomePagePrefs.getLastHomePageAnchor(context, folders, pageSize = 1)
        assertEquals(HomePageAnchor.Folder("A"), second)
    }

    @Test fun `explicit set overrides stored anchor and marks migrated`() {
        HomePagePrefs.setLastHomePageAnchor(context, HomePageAnchor.Folder("custom"))
        val anchor = HomePagePrefs.getLastHomePageAnchor(context, emptyList(), pageSize = 8)
        assertEquals(HomePageAnchor.Folder("custom"), anchor)
    }

    @Test fun `corrupt stored anchor value does not crash and falls back to dashboard`() {
        homePageBacking.edit()
            .putBoolean(HomePagePrefs.KEY_HOME_PAGER_MIGRATED, true)
            .putString(HomePagePrefs.KEY_LAST_HOME_PAGE_ANCHOR, "??garbage??")
            .apply()

        val anchor = HomePagePrefs.getLastHomePageAnchor(context, emptyList(), pageSize = 8)
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    // ── Yeni kurulum ──────────────────────────────────────────────────────

    @Test fun `fresh install has no legacy page written writes dashboard fallback via migration`() {
        // AppPrefs.getLastHomePage default 0 doner (roadmap kurali: yeni kurulumda gercek anchor
        // dashboard'a dusmeli cunku klasor listesi henuz olusmamis olabilir).
        val anchor = HomePagePrefs.getLastHomePageAnchor(context, emptyList(), pageSize = 8)
        assertEquals(HomePageAnchor.Dashboard, anchor)
    }

    @Test fun `fresh install start page mode defaults to smart dashboard and persists`() {
        val mode = HomePagePrefs.getStartPageMode(context)
        assertEquals(HomePagePrefs.StartPageMode.SMART_DASHBOARD, mode)
        assertEquals("SMART_DASHBOARD", homePageBacking.getString(HomePagePrefs.KEY_HOME_START_PAGE_MODE, null))
    }

    @Test fun `start page mode round-trips`() {
        HomePagePrefs.setStartPageMode(context, HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE)
        assertEquals(HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE, HomePagePrefs.getStartPageMode(context))
    }

    @Test fun `smart dashboard enabled defaults to true`() {
        assertTrue(HomePagePrefs.isSmartDashboardEnabled(context))
    }

    @Test fun `smart dashboard enabled can be disabled`() {
        HomePagePrefs.setSmartDashboardEnabled(context, false)
        assertFalse(HomePagePrefs.isSmartDashboardEnabled(context))
    }

    // ── Diagnostics köprüsü — yalnız tip, categoryId sizmaz ─────────────────

    @Test fun `peek anchor type before migration reports UNMIGRATED`() {
        assertEquals("UNMIGRATED", HomePagePrefs.peekLastHomePageAnchorType(context))
    }

    @Test fun `peek anchor type after migration reports FOLDER without leaking categoryId`() {
        HomePagePrefs.setLastHomePageAnchor(context, HomePageAnchor.Folder("secret_category"))
        val type = HomePagePrefs.peekLastHomePageAnchorType(context)
        assertEquals("FOLDER", type)
        assertFalse(type.contains("secret_category"))
    }

    @Test fun `anchor type label mapping is exhaustive`() {
        assertEquals("DASHBOARD", HomePagePrefs.anchorTypeLabel(HomePageAnchor.Dashboard))
        assertEquals("FOLDER", HomePagePrefs.anchorTypeLabel(HomePageAnchor.Folder("x")))
        assertEquals("PAGE_INDEX", HomePagePrefs.anchorTypeLabel(HomePageAnchor.PageIndex(2)))
    }

    // ── Backup/restore köprüsü ───────────────────────────────────────────────

    @Test fun `backup fields round-trip through toBackupFields and fromBackupFields`() {
        HomePagePrefs.setStartPageMode(context, HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE)
        HomePagePrefs.setLastHomePageAnchor(context, HomePageAnchor.Folder("work"))
        HomePagePrefs.setSmartDashboardEnabled(context, false)

        val fields = HomePagePrefs.toBackupFields(context)

        val restoredBacking = HomePagePrefsFakeSharedPreferences()
        val restoredContext: Context = mockk(relaxed = true)
        every { restoredContext.getSharedPreferences(HomePagePrefs.PREFS_NAME, any()) } returns restoredBacking

        HomePagePrefs.fromBackupFields(restoredContext, fields)

        assertEquals(HomePagePrefs.StartPageMode.RESTORE_LAST_PAGE, HomePagePrefs.getStartPageMode(restoredContext))
        assertEquals(HomePageAnchor.Folder("work"), HomePagePrefs.getLastHomePageAnchor(restoredContext, emptyList(), 8))
        assertFalse(HomePagePrefs.isSmartDashboardEnabled(restoredContext))
    }
}

/** HomeLayoutPrefsTest/AppPrefsLastHomePageTest'teki fake'in bu dosyaya özel kopyası. */
private class HomePagePrefsFakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String> ?: defValues)
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
            key?.let { pending[it] = value }; return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            key?.let { pending[it] = values }; return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            key?.let { pending[it] = value }; return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let { removals.add(it) }; return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearAll = true; return this
        }
        override fun commit(): Boolean {
            apply(); return true
        }
        override fun apply() {
            if (clearAll) map.clear()
            removals.forEach { map.remove(it) }
            pending.forEach { (k, v) -> map[k] = v }
        }
    }
}
