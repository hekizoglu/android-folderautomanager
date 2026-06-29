package com.armutlu.apporganizer.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DominantColorExtractor {

    /**
     * Verilen paket adlarından ikonları yükler ve dominant rengi döner.
     * Renk koyu/canlı öncelikli — pastel/beyaz arka plan ikonları için fallback vibrant.
     * Null dönerse varsayılan kategori rengi kullanılmalı.
     */
    suspend fun extractFromPackages(
        context: Context,
        packageNames: List<String>,
        iconSizePx: Int = 64
    ): String? = withContext(Dispatchers.IO) {
        val bitmaps = packageNames.take(4).mapNotNull { pkg ->
            runCatching { loadAppIcon(context, pkg, iconSizePx) }.getOrNull()
        }
        if (bitmaps.isEmpty()) return@withContext null

        // İlk 4 ikondan Palette oluştur, vibrant/dominant renk al
        val combinedBitmap = combineBitmaps(bitmaps, iconSizePx)
        val palette = Palette.Builder(combinedBitmap)
            .maximumColorCount(8)
            .generate()

        val color = palette.vibrantSwatch?.rgb
            ?: palette.darkVibrantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: return@withContext null

        "#%06X".format(color and 0xFFFFFF)
    }

    private fun combineBitmaps(bitmaps: List<Bitmap>, size: Int): Bitmap {
        val result = Bitmap.createBitmap(size * bitmaps.size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        bitmaps.forEachIndexed { i, bmp ->
            canvas.drawBitmap(bmp, (i * size).toFloat(), 0f, null)
        }
        return result
    }
}
