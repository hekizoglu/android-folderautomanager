package com.armutlu.apporganizer.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * AppPrefs.deriveClassificationMode() pure function testleri (P0.6, ROADMAP_AI_AUDIT).
 * Context gerektirmeyen migration cekirdegi: eski paralel toggle kombinasyonundan
 * (manufacturer_classify + LLM daha once kullanilmis mi) tek ClassificationMode turetir.
 * Amac: kullanici hicbir sey degistirmeden mevcut davranisin AYNEN korunmasi.
 */
class AppPrefsClassificationModeTest {

    @Test
    fun `LLM daha once kullanilmissa uretici ayari ne olursa olsun LOCAL_WITH_LLM_FALLBACK doner`() {
        val result = AppPrefs.deriveClassificationMode(manufacturerEnabled = true, llmEverUsed = true)
        assertEquals(AppPrefs.ClassificationMode.LOCAL_WITH_LLM_FALLBACK, result)
    }

    @Test
    fun `LLM kullanilmis ve uretici kapaliyken de LOCAL_WITH_LLM_FALLBACK doner`() {
        val result = AppPrefs.deriveClassificationMode(manufacturerEnabled = false, llmEverUsed = true)
        assertEquals(AppPrefs.ClassificationMode.LOCAL_WITH_LLM_FALLBACK, result)
    }

    @Test
    fun `LLM kullanilmamis ve uretici aciksa LOCAL_WITH_MANUFACTURER doner`() {
        val result = AppPrefs.deriveClassificationMode(manufacturerEnabled = true, llmEverUsed = false)
        assertEquals(AppPrefs.ClassificationMode.LOCAL_WITH_MANUFACTURER, result)
    }

    @Test
    fun `ikisi de kapaliysa LOCAL_ONLY doner`() {
        val result = AppPrefs.deriveClassificationMode(manufacturerEnabled = false, llmEverUsed = false)
        assertEquals(AppPrefs.ClassificationMode.LOCAL_ONLY, result)
    }

    @Test
    fun `varsayilan kullanici (hicbir sey degistirmemis) LOCAL_WITH_MANUFACTURER alir`() {
        // KEY_MANUFACTURER_CLASSIFY varsayilani true (D115 oncesi tum kullanicilarin durumu),
        // LLM hic kullanilmamis olabilir -> eski davranis: uretici aciktı, LLM kapaliydi.
        val result = AppPrefs.deriveClassificationMode(manufacturerEnabled = true, llmEverUsed = false)
        assertEquals(AppPrefs.ClassificationMode.LOCAL_WITH_MANUFACTURER, result)
    }
}
