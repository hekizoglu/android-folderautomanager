package com.armutlu.apporganizer.telemetry

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyDisclosureContractTest {
    @Test
    fun `in-app disclosure names controlled services and independent fcm`() {
        val strings = File("src/main/res/values/strings.xml").readText()
        val disclosure = Regex(
            "<string name=\"usage_data_sharing_description\">(.*?)</string>",
            RegexOption.DOT_MATCHES_ALL,
        ).find(strings)?.groupValues?.get(1).orEmpty()

        listOf("Analytics", "Crashlytics", "Performance", "FCM", "sonraki uygulama açılışında").forEach {
            assertTrue("Missing disclosure term: $it", disclosure.contains(it))
        }
    }

    @Test
    fun `play declaration covers every firebase data category`() {
        val declaration = File("../docs/PLAY_DATA_SAFETY_DECLARATION.md").readText()

        listOf(
            "Uygulama etkileşimleri",
            "Kilitlenme günlükleri",
            "Tanılama",
            "Cihaz veya diğer kimlikler",
            "Uygulama işlevselliği",
            "Analiz",
            "hata teşhisi",
        ).forEach { assertTrue("Missing Play declaration term: $it", declaration.contains(it)) }
    }
}
