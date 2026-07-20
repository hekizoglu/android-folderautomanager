package com.armutlu.apporganizer.domain.home

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [TickerActionCodec] round-trip doğrulaması — ticker arşivi ([TickerHistoryMapper]) her
 * [TickerAction] alt tipini kayıpsız kodlayıp geri çözebilmeli. Bilinmeyen/bozuk string
 * güvenle [TickerAction.None] döner.
 */
class TickerActionCodecTest {

    @Test
    fun `OpenFolder round-trips with categoryId`() {
        val action = TickerAction.OpenFolder("CAT_SOCIAL")
        val wire = TickerActionCodec.encode(action)
        assertEquals(action, TickerActionCodec.decode(wire))
    }

    @Test
    fun `OpenApp round-trips with packageName`() {
        val action = TickerAction.OpenApp("com.example.app")
        val wire = TickerActionCodec.encode(action)
        assertEquals(action, TickerActionCodec.decode(wire))
    }

    @Test
    fun `OpenAppList round-trips`() {
        assertEquals(TickerAction.OpenAppList, TickerActionCodec.decode(TickerActionCodec.encode(TickerAction.OpenAppList)))
    }

    @Test
    fun `OpenClassificationReview round-trips`() {
        val action = TickerAction.OpenClassificationReview
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenNotificationReport round-trips`() {
        val action = TickerAction.OpenNotificationReport
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenDashboard round-trips`() {
        val action = TickerAction.OpenDashboard
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenWeeklyReport round-trips`() {
        val action = TickerAction.OpenWeeklyReport
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenSettings round-trips for every section`() {
        SettingsSection.values().forEach { section ->
            val action = TickerAction.OpenSettings(section)
            assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
        }
    }

    @Test
    fun `OpenSearchStats round-trips`() {
        val action = TickerAction.OpenSearchStats
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenReportsCenter round-trips`() {
        val action = TickerAction.OpenReportsCenter
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenUsageReport round-trips`() {
        val action = TickerAction.OpenUsageReport
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `OpenMissions round-trips`() {
        val action = TickerAction.OpenMissions
        assertEquals(action, TickerActionCodec.decode(TickerActionCodec.encode(action)))
    }

    @Test
    fun `None round-trips`() {
        assertEquals(TickerAction.None, TickerActionCodec.decode(TickerActionCodec.encode(TickerAction.None)))
    }

    @Test
    fun `unknown wire string decodes to None safely`() {
        assertEquals(TickerAction.None, TickerActionCodec.decode("garbage_key:whatever"))
        assertEquals(TickerAction.None, TickerActionCodec.decode(""))
        assertEquals(TickerAction.None, TickerActionCodec.decode(null))
    }

    @Test
    fun `OpenFolder with missing param decodes to None`() {
        assertEquals(TickerAction.None, TickerActionCodec.decode("open_folder"))
    }

    @Test
    fun `OpenSettings with unknown section falls back to ROOT`() {
        val decoded = TickerActionCodec.decode("open_settings:NOT_A_REAL_SECTION")
        assertEquals(TickerAction.OpenSettings(SettingsSection.ROOT), decoded)
    }
}
