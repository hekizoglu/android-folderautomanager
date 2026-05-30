package com.armutlu.apporganizer.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import timber.log.Timber

/**
 * Katman 1: ShortcutManager ile evrensel launcher organizasyonu.
 * Tüm launcher'larda çalışır — her kategori için ana ekrana
 * pinlenebilir kısayol oluşturur.
 */
class LauncherOrganizer(private val context: Context) {

    data class OrganizeResult(
        val success: Boolean,
        val pinnedCategories: Int = 0,
        val skippedCategories: Int = 0,
        val message: String = ""
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun organizeByCategories(
        apps: List<AppInfo>,
        categories: List<Category>
    ): OrganizeResult {
        val manager = context.getSystemService(ShortcutManager::class.java)
            ?: return OrganizeResult(false, message = "ShortcutManager mevcut değil")

        if (!manager.isRequestPinShortcutSupported) {
            return OrganizeResult(
                false,
                message = "Bu launcher pin kısayolunu desteklemiyor.\n" +
                        "Nova Launcher, Pixel Launcher veya Samsung One UI kullanın."
            )
        }

        val appsPerCategory = apps.groupBy { it.categoryId }
        var pinned = 0
        var skipped = 0

        // Mevcut shortcut'ları temizle (güncelleme için)
        try {
            val existingIds = manager.pinnedShortcuts.map { it.id }
            val categoryIds = categories.map { "category_${it.categoryId}" }
            val toRemove = existingIds.filter { it in categoryIds }
            if (toRemove.isNotEmpty()) manager.disableShortcuts(toRemove)
        } catch (e: Exception) {
            Timber.w(e, "Could not clear old shortcuts")
        }

        categories.forEach { category ->
            val categoryApps = appsPerCategory[category.categoryId] ?: return@forEach
            if (categoryApps.isEmpty()) { skipped++; return@forEach }

            try {
                val shortcutId = "category_${category.categoryId}"
                val label = "${category.iconEmoji} ${category.categoryName} (${categoryApps.size})"

                // Kategori rengiyle ikon oluştur
                val icon = createCategoryIcon(category, categoryApps)

                // Shortcut tıklandığında AppOrganizer'ı o kategoride aç
                val intent = Intent(context, Class.forName(
                    "com.armutlu.apporganizer.presentation.ui.MainActivity"
                )).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("open_category", category.categoryId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val shortcut = ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(label)
                    .setLongLabel("${category.categoryName} — ${categoryApps.size} uygulama")
                    .setIntent(intent)
                    .setIcon(Icon.createWithBitmap(icon))
                    .build()

                manager.requestPinShortcut(shortcut, null)
                pinned++
                Timber.d("Pinned shortcut for ${category.categoryName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to pin ${category.categoryName}")
                skipped++
            }
        }

        return OrganizeResult(
            success = pinned > 0,
            pinnedCategories = pinned,
            skippedCategories = skipped,
            message = buildString {
                if (pinned > 0) appendLine("✅ $pinned kategori için kısayol oluşturuldu.")
                if (skipped > 0) appendLine("⚠️ $skipped kategori atlandı (boş veya hata).")
                appendLine("\nAna ekranınıza bakın — her kategoriye ait kısayollar eklendi.")
                appendLine("Kısayola tıklayınca AppOrganizer ilgili kategoriyi açar.")
            }.trim()
        )
    }

    /**
     * Launcher'ı tespit et ve Accessibility Service'in kullanılabilir
     * olup olmadığını kontrol et.
     */
    fun detectLauncher(): LauncherType {
        return try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val info = pm.resolveActivity(intent, 0)
            val pkg = info?.activityInfo?.packageName ?: ""
            Timber.d("Detected launcher: $pkg")

            when {
                pkg.contains("com.google.android.apps.nexuslauncher") ||
                pkg.contains("com.android.launcher3") -> LauncherType.PIXEL

                pkg.contains("com.sec.android.app.launcher") ||
                pkg.contains("com.samsung") -> LauncherType.SAMSUNG

                pkg.contains("com.miui.home") ||
                pkg.contains("com.xiaomi") ||
                pkg.contains("com.hyperos") -> LauncherType.XIAOMI

                pkg.contains("com.teslacoilsw.launcher") -> LauncherType.NOVA

                pkg.contains("com.huawei.android.launcher") -> LauncherType.HUAWEI

                else -> LauncherType.UNKNOWN(pkg)
            }
        } catch (e: Exception) {
            LauncherType.UNKNOWN("unknown")
        }
    }

    private fun createCategoryIcon(category: Category, apps: List<AppInfo>): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Arka plan — kategori rengi
        val bgColor = try {
            Color.parseColor(category.colorHex)
        } catch (e: Exception) { Color.parseColor("#6200EE") }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 40f, 40f, paint)

        // Emoji / metin
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 80f
        val text = category.iconEmoji.ifBlank { category.categoryName.firstOrNull()?.toString() ?: "?" }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, size / 2f, size / 2f - bounds.exactCenterY(), paint)

        // Sağ alt köşede uygulama sayısı rozeti
        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText("${apps.size}", size - 28f, size - 16f, paint)

        return bitmap
    }
}

sealed class LauncherType {
    object PIXEL   : LauncherType()
    object SAMSUNG : LauncherType()
    object XIAOMI  : LauncherType()
    object NOVA    : LauncherType()
    object HUAWEI  : LauncherType()
    data class UNKNOWN(val pkg: String) : LauncherType()

    val displayName: String get() = when (this) {
        is PIXEL   -> "Pixel Launcher"
        is SAMSUNG -> "Samsung One UI"
        is XIAOMI  -> "MIUI Launcher"
        is NOVA    -> "Nova Launcher"
        is HUAWEI  -> "Huawei Launcher"
        is UNKNOWN -> "Bilinmeyen Launcher ($pkg)"
    }

    val supportsAccessibility: Boolean get() = when (this) {
        is PIXEL, is SAMSUNG, is XIAOMI -> true
        else -> false
    }
}
