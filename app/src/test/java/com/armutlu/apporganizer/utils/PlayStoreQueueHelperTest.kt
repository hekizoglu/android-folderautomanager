package com.armutlu.apporganizer.utils

import org.junit.Assert.*
import org.junit.Test

class PlayStoreQueueHelperTest {

    @Test
    fun `bos paket listesinde null doner`() {
        assertNull(PlayStoreQueueHelper.nextSelectedIndex(emptyList(), emptySet(), 0))
    }

    @Test
    fun `hicbir paket secili degilse null doner`() {
        val packages = listOf("com.a", "com.b", "com.c")
        assertNull(PlayStoreQueueHelper.nextSelectedIndex(packages, emptySet(), 0))
    }

    @Test
    fun `ilk cagrida ilk secili paketin index'i donuyor`() {
        val packages = listOf("com.a", "com.b", "com.c")
        val selected = setOf("com.a", "com.c")
        assertEquals(0, PlayStoreQueueHelper.nextSelectedIndex(packages, selected, 0))
    }

    @Test
    fun `secili olmayan paket atlanip bir sonraki secili bulunuyor`() {
        val packages = listOf("com.a", "com.b", "com.c")
        val selected = setOf("com.c")
        // com.a index 0'da secili degil, com.b da degil -> com.c index 2 donmeli
        assertEquals(2, PlayStoreQueueHelper.nextSelectedIndex(packages, selected, 0))
    }

    @Test
    fun `currentIndex sonrasindan devam ediyor`() {
        val packages = listOf("com.a", "com.b", "com.c")
        val selected = setOf("com.a", "com.b", "com.c")
        // com.a acildi, currentIndex 1 -> sonraki com.b
        assertEquals(1, PlayStoreQueueHelper.nextSelectedIndex(packages, selected, 1))
    }

    @Test
    fun `tum secili paketler acildiktan sonra null doner`() {
        val packages = listOf("com.a", "com.b")
        val selected = setOf("com.a", "com.b")
        assertEquals(2, packages.size)
        // currentIndex listenin sonunu gectiginde null donmeli
        assertNull(PlayStoreQueueHelper.nextSelectedIndex(packages, selected, 2))
    }

    @Test
    fun `playStoreUrl dogru formatta uretiliyor`() {
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.whatsapp",
            PlayStoreQueueHelper.playStoreUrl("com.whatsapp")
        )
    }

    @Test
    fun `totalSelectedCount secili paket sayisini doner`() {
        assertEquals(3, PlayStoreQueueHelper.totalSelectedCount(setOf("com.a", "com.b", "com.c")))
        assertEquals(0, PlayStoreQueueHelper.totalSelectedCount(emptySet()))
    }
}
