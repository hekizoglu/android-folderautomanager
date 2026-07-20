package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * F1 denetimi (P0): DeepSeek API anahtari ve (eski) FCM token ayri "deepseek_prefs" /
 * "device_prefs" SharedPreferences dosyalarinda tutuluyordu (ana app_organizer_prefs Auto
 * Backup'a dahildi, exclusion kurali yanlis dosya adini hedefliyordu). FCM kaldirildiktan
 * sonra (D-S6) getFcmToken/setFcmToken publik fonksiyonlari silindi, ama eski cihazlarda
 * app_organizer_prefs icinde kalmis olabilecek fcm_token degerini device_prefs'e tasiyip
 * ana dosyadan silen migrateSensitivePrefsIfNeeded legacy temizligi hala calisir — bu test
 * o temizligi (dogrudan store map'leri okuyarak) ve idempotent davranisi dogrular.
 *
 * Her dosya adi icin ayri mock SharedPreferences/Editor ciftini, o dosyanin icindeki
 * key->value durumunu bir MutableMap ile simule ederek gercekci get/put davranisi saglar.
 */
class AppPrefsSensitiveMigrationTest {

    private fun fakePrefs(): Pair<SharedPreferences, MutableMap<String, Any?>> {
        val store = mutableMapOf<String, Any?>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putString(any(), any()) } answers {
            store[firstArg()] = secondArg<String?>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            store[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { editor.remove(any()) } answers {
            store.remove(firstArg())
            editor
        }
        every { editor.apply() } answers {}

        val prefs = mockk<SharedPreferences>(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.getString(any(), any()) } answers {
            (store[firstArg()] as? String) ?: secondArg()
        }
        every { prefs.getBoolean(any(), any()) } answers {
            (store[firstArg()] as? Boolean) ?: secondArg()
        }
        return prefs to store
    }

    private lateinit var context: Context
    private lateinit var mainPrefs: SharedPreferences
    private lateinit var mainStore: MutableMap<String, Any?>
    private lateinit var deepSeekPrefs: SharedPreferences
    private lateinit var deepSeekStore: MutableMap<String, Any?>
    private lateinit var devicePrefs: SharedPreferences
    private lateinit var deviceStore: MutableMap<String, Any?>

    @Before
    fun setup() {
        val (main, mStore) = fakePrefs()
        mainPrefs = main
        mainStore = mStore
        val (ds, dsStore) = fakePrefs()
        deepSeekPrefs = ds
        deepSeekStore = dsStore
        val (dev, devStore) = fakePrefs()
        devicePrefs = dev
        deviceStore = devStore

        context = mockk(relaxed = true)
        every { context.getSharedPreferences(AppPrefs.PREFS_NAME, Context.MODE_PRIVATE) } returns mainPrefs
        every { context.getSharedPreferences("deepseek_prefs", Context.MODE_PRIVATE) } returns deepSeekPrefs
        every { context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE) } returns devicePrefs
    }

    @Test
    fun `legacy deepseek key and fcm token are migrated to separate files and removed from main`() {
        mainStore["deepseek_api_key"] = "sk-legacy-secret"
        mainStore["fcm_token"] = "legacy-fcm-token"

        val migratedKey = AppPrefs.getDeepSeekApiKey(context)

        assertEquals("sk-legacy-secret", migratedKey)
        assertEquals("sk-legacy-secret", deepSeekStore["deepseek_api_key"])
        assertTrue(!mainStore.containsKey("deepseek_api_key"))

        assertEquals("legacy-fcm-token", deviceStore["fcm_token"])
        assertTrue(!mainStore.containsKey("fcm_token"))

        assertEquals(true, mainStore["sensitive_prefs_migrated_v1"])
    }

    @Test
    fun `migration runs only once — second call is a no-op`() {
        mainStore["deepseek_api_key"] = "sk-legacy-secret"
        AppPrefs.getDeepSeekApiKey(context)
        assertEquals(true, mainStore["sensitive_prefs_migrated_v1"])

        // Simulate a value re-appearing in main after the flag is already set (should never
        // happen in practice) — migration must NOT run again and overwrite the real value.
        mainStore["deepseek_api_key"] = "sk-should-be-ignored"
        val result = AppPrefs.getDeepSeekApiKey(context)

        assertEquals("sk-legacy-secret", result)
        assertEquals("sk-legacy-secret", deepSeekStore["deepseek_api_key"])
    }

    @Test
    fun `fresh install with no legacy values migrates cleanly to empty strings`() {
        val key = AppPrefs.getDeepSeekApiKey(context)

        assertEquals("", key)
        assertTrue(!deviceStore.containsKey("fcm_token"))
        assertEquals(true, mainStore["sensitive_prefs_migrated_v1"])
    }

    @Test
    fun `setDeepSeekApiKey writes to separate file not main prefs`() {
        AppPrefs.setDeepSeekApiKey(context, "sk-new-key")

        assertEquals("sk-new-key", deepSeekStore["deepseek_api_key"])
        assertTrue(!mainStore.containsKey("deepseek_api_key"))
    }

}
