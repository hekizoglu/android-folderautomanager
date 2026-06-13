package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object ShortcutHelper {

    fun getShortcuts(context: Context, packageName: String): List<ShortcutInfo> {
        val la = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        return runCatching {
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                )
            }
            la.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun getShortcutIcon(context: Context, shortcut: ShortcutInfo, sizePx: Int = 96): ImageBitmap? {
        val la = context.getSystemService(LauncherApps::class.java) ?: return null
        return runCatching {
            val drawable = la.getShortcutIconDrawable(
                shortcut, context.resources.displayMetrics.densityDpi
            ) ?: return@runCatching null
            drawable.toBitmap(sizePx).asImageBitmap()
        }.getOrNull()
    }

    fun launchShortcut(context: Context, shortcut: ShortcutInfo) {
        val la = context.getSystemService(LauncherApps::class.java) ?: return
        runCatching { la.startShortcut(shortcut, null, null) }
    }

    private fun Drawable.toBitmap(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        setBounds(0, 0, size, size)
        draw(canvas)
        return bmp
    }
}
