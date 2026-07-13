package com.armutlu.apporganizer.utils

/**
 * Restore sonrası eksik uygulamalar için çoklu Play Store açma kuyruğu.
 * Android'de tek bir intent ile birden fazla Play Store sayfası açılamaz (platform kısıtı),
 * bu yüzden seçili paketler sırayla, kullanıcı her dönüşte "Sonraki Uygulamayı Aç" ile
 * bir sonrakine geçecek şekilde index tabanlı bir kuyruk kullanılır.
 */
object PlayStoreQueueHelper {

    /**
     * [packages] listesindeki, [selected] kümesinde bulunan ve [currentIndex]'ten (dahil)
     * sonraki ilk paketin index'ini döner. Hiçbir seçili paket kalmadıysa null döner.
     */
    fun nextSelectedIndex(
        packages: List<String>,
        selected: Set<String>,
        currentIndex: Int
    ): Int? {
        if (packages.isEmpty()) return null
        val startFrom = currentIndex.coerceAtLeast(0)
        for (i in startFrom until packages.size) {
            if (packages[i] in selected) return i
        }
        return null
    }

    /** Play Store sayfası açılmış (denenmiş) seçili paket sayısı — ilerleme göstergesi için. */
    fun openedCount(selected: Set<String>, currentIndex: Int): Int =
        currentIndex.coerceIn(0, selected.size)

    /** Toplam açılacak (seçili) paket sayısı. */
    fun totalSelectedCount(selected: Set<String>): Int = selected.size

    /** Play Store detay sayfası URL'i. */
    fun playStoreUrl(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"
}
