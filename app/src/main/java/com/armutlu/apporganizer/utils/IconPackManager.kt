package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.ConcurrentHashMap

object IconPackManager {

    private val filterCache = ConcurrentHashMap<String, Map<String, String>>()

    data class IconPackInfo(val packageName: String, val label: String)

    private val ICON_PACK_INTENTS = listOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.THEMES",
        "com.gau.go.launcherex.theme",
        "app.lawnchair.ICON_PACK",
        "com.teslacoilsw.launcher.THEME"
    )

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        return ICON_PACK_INTENTS.flatMap { action ->
            runCatching {
                pm.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA)
                    .map { it.activityInfo.packageName }
            }.getOrDefault(emptyList())
        }.distinct().mapNotNull { pkg ->
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                IconPackInfo(pkg, pm.getApplicationLabel(info).toString())
            }.getOrNull()
        }
    }

    fun loadIcon(context: Context, iconPackPkg: String, appPkg: String, sizePx: Int = 96): Bitmap? {
        if (iconPackPkg.isEmpty()) return null
        val drawableName = getMapping(context, iconPackPkg)[appPkg] ?: return null
        return loadDrawableByName(context, iconPackPkg, drawableName, sizePx)
    }

    fun clearCache() {
        filterCache.clear()
    }

    private fun getMapping(context: Context, iconPackPkg: String): Map<String, String> {
        filterCache[iconPackPkg]?.let { return it }
        val mapping = parseAppFilter(context, iconPackPkg)
        filterCache[iconPackPkg] = mapping
        return mapping
    }

    private fun parseAppFilter(context: Context, iconPackPkg: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        runCatching {
            val res = context.packageManager.getResourcesForApplication(iconPackPkg)
            val resId = res.getIdentifier("appfilter", "xml", iconPackPkg)
            if (resId == 0) return result
            val parser = res.getXml(resId)
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                if (parser.name != "item") continue
                val component = parser.getAttributeValue(null, "component") ?: continue
                val drawable = parser.getAttributeValue(null, "drawable") ?: continue
                val pkg = component
                    .removePrefix("ComponentInfo{")
                    .removeSuffix("}")
                    .substringBefore("/")
                    .trim()
                if (pkg.isNotEmpty()) result[pkg] = drawable
            }
            parser.close()
        }
        return result
    }

    private fun loadDrawableByName(
        context: Context,
        iconPackPkg: String,
        drawableName: String,
        sizePx: Int
    ): Bitmap? = runCatching {
        val res = context.packageManager.getResourcesForApplication(iconPackPkg)
        val resId = res.getIdentifier(drawableName, "drawable", iconPackPkg)
        if (resId == 0) return null
        res.getDrawable(resId, null)?.toBitmap(sizePx, sizePx)
    }.getOrNull()
}
