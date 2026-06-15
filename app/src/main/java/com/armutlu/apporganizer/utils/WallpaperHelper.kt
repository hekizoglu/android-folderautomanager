package com.armutlu.apporganizer.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WallpaperHelper {

    suspend fun applyColorWallpaper(context: Context, hexColor: String) = withContext(Dispatchers.IO) {
        val wm = WallpaperManager.getInstance(context)
        val width = wm.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
        val height = wm.desiredMinimumHeight.takeIf { it > 0 } ?: 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = hexColor.toColorInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        wm.setBitmap(bitmap)
        bitmap.recycle()
    }

    suspend fun applyGradientWallpaper(context: Context, startHex: String, endHex: String) = withContext(Dispatchers.IO) {
        val wm = WallpaperManager.getInstance(context)
        val width = wm.desiredMinimumWidth.takeIf { it > 0 } ?: 1080
        val height = wm.desiredMinimumHeight.takeIf { it > 0 } ?: 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                startHex.toColorInt(), endHex.toColorInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        wm.setBitmap(bitmap)
        bitmap.recycle()
    }
}
