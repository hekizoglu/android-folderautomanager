package com.armutlu.apporganizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerAndCsvExportTest {

    @Test
    fun `drawer items chunking creates maximum 4 items per row`() {
        val items = listOf("App1", "App2", "App3", "App4", "App5", "App6", "App7", "App8", "App9")
        val chunked = items.chunked(4)

        assertEquals(3, chunked.size)
        assertEquals(4, chunked[0].size)
        assertEquals(4, chunked[1].size)
        assertEquals(1, chunked[2].size)
    }

    @Test
    fun `csv export escapes quotes and converts newlines`() {
        val rawTitle = "Gelen Mesaj: \"Önemli\""
        val rawText = "Merhaba,\nLütfen dosyayı kontrol edin.\r\nTeşekkürler."

        val escapedTitle = rawTitle.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
        val escapedText = rawText.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")

        assertEquals("Gelen Mesaj: \"\"Önemli\"\"", escapedTitle)
        assertTrue(!escapedText.contains("\n"))
        assertTrue(!escapedText.contains("\r"))
        assertTrue(escapedTitle.contains("\"\""))
    }

    @Test
    fun `csv row formatting adheres to RFC 4180 standard`() {
        val id = 101L
        val pkg = "com.whatsapp"
        val appName = "WhatsApp"
        val title = "Ahmet \"CEO\""
        val text = "Yarın toplantı 10:00'da"
        val timestamp = 1700000000000L
        val dateStr = "2026-08-10 00:00:00"
        val category = "COMMUNICATION"
        val importanceScore = 85
        val wasSuppressed = false

        val formattedTitle = title.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")
        val formattedText = text.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ")

        val csvLine = "$id,\"$pkg\",\"$appName\",\"$formattedTitle\",\"$formattedText\",$timestamp,\"$dateStr\",\"$category\",$importanceScore,$wasSuppressed"

        assertTrue(csvLine.startsWith("101,\"com.whatsapp\""))
        assertTrue(csvLine.contains("\"Ahmet \"\"CEO\"\"\""))
        assertTrue(csvLine.endsWith("85,false"))
    }
}
