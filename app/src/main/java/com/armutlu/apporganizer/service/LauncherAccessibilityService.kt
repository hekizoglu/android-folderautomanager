package com.armutlu.apporganizer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Katman 2: Accessibility Service ile launcher'da fiziksel drag & drop.
 *
 * İkon arama stratejileri (öncelik sırasıyla):
 *   1. Package name eşleşmesi (Pixel, Samsung, Nova)
 *   2. extras bundle'da package adı (Android 11+)
 *   3. App adıyla contentDescription/text araması (MIUI, HyperOS)
 */
class LauncherAccessibilityService : AccessibilityService() {

    /** Organize edilecek uygulamanın tüm bilgilerini taşır. */
    data class AppOrgInfo(
        val packageName: String,
        val categoryId: String,
        val appName: String
    )

    companion object {
        const val ACTION_ORGANIZE    = "com.armutlu.apporganizer.ACTION_ORGANIZE"
        const val ACTION_STATUS      = "com.armutlu.apporganizer.ACTION_STATUS"
        const val EXTRA_CATEGORY_MAP = "category_map"

        @Volatile var instance: LauncherAccessibilityService? = null
        @Volatile var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())

    private var pendingApps: List<AppOrgInfo> = emptyList()
    private var currentIndex = 0
    private var statusCallback: ((String) -> Unit)? = null
    private val folderPositions = mutableMapOf<String, Pair<Float, Float>>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Timber.d("LauncherAccessibilityService connected — Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Olayları dinlemiyoruz; windows API'yi doğrudan kullanıyoruz
    }

    override fun onInterrupt() {
        Timber.w("LauncherAccessibilityService interrupted")
        isRunning = false
        instance = null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Timber.w("LauncherAccessibilityService unbound")
        isRunning = false
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        instance = null
        Timber.d("LauncherAccessibilityService destroyed")
    }

    // ── Public API ─────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    fun startOrganize(apps: List<AppOrgInfo>, onStatus: (String) -> Unit) {
        pendingApps    = apps
        currentIndex   = 0
        folderPositions.clear()
        statusCallback = onStatus
        windowsLogged  = false

        log("🚀 organize başladı: ${apps.size} uygulama")
        log("Ekran: ${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
        log("Aktif pencere: ${rootInActiveWindow?.packageName}")
        log("windows API: ${windows?.size ?: "null"} pencere")
        goHome()
        handler.postDelayed({
            val activeAfterHome = rootInActiveWindow?.packageName?.toString() ?: "null"
            log("HOME sonrası pencere: $activeAfterHome")
            val allWins = windows?.mapNotNull { it.root?.packageName?.toString() } ?: emptyList()
            log("Tüm pencereler: ${allWins.joinToString(", ").ifBlank { "(boş)" }}")
            if (allWins.isEmpty() && activeAfterHome == "null") {
                log("⛔ Pencere erişimi yok! Erişilebilirlik iznini kapatıp tekrar açın.")
            }
            processNextApp()
        }, 1500)
    }

    private fun log(msg: String) {
        Timber.d("[A11y] $msg")
        statusCallback?.invoke(msg)
    }

    // ── İşlem döngüsü ──────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processNextApp() {
        if (currentIndex >= pendingApps.size) {
            log("✅ Tamamlandı! ${pendingApps.size} uygulama işlendi.")
            return
        }

        val app = pendingApps[currentIndex]
        log("(${currentIndex + 1}/${pendingApps.size}) ${app.packageName} → ${app.categoryId}")

        val activeWindow = rootInActiveWindow?.packageName?.toString() ?: "null"
        if (activeWindow != "null" &&
            !activeWindow.contains("launcher", ignoreCase = true) &&
            !activeWindow.contains("home", ignoreCase = true) &&
            !activeWindow.contains("desktop", ignoreCase = true)) {
            log("  ⚠️ Launcher değil ($activeWindow), HOME'a dönülüyor...")
            goHome()
            handler.postDelayed({ processNextApp() }, 1000)
            return
        }

        val iconNode = findAppIcon(app.packageName, app.appName)
        if (iconNode == null) {
            log("  ⏭ İkon görünümde değil: ${app.packageName} — atlanıyor")
            currentIndex++
            handler.postDelayed({ processNextApp() }, 200)
            return
        }

        val iconBounds = Rect()
        iconNode.getBoundsInScreen(iconBounds)
        val fromX = iconBounds.exactCenterX()
        val fromY = iconBounds.exactCenterY()
        log("  ✓ İkon bulundu (${fromX.toInt()},${fromY.toInt()}) strateji: ${iconNode.packageName}")

        val target = getOrCreateFolderTarget(app.categoryId, fromX, fromY)
        log("  → Hedef: (${target.first.toInt()},${target.second.toInt()})")

        dragIconToTarget(fromX, fromY, target.first, target.second) { success ->
            log(if (success) "  ✅ drag tamamlandı" else "  ⚠️ drag iptal")
            currentIndex++
            handler.postDelayed({ processNextApp() }, 700)
        }
    }

    // ── İkon arama — 3 stratejili ──────────────────────────────────────────

    private var windowsLogged = false

    private fun findAppIcon(packageName: String, appName: String): AccessibilityNodeInfo? {
        // --- Önce windows listesinden launcher penceresini ara ---
        val wins = windows
        if (wins != null) {
            if (!windowsLogged) {
                windowsLogged = true
                val pkgs = wins.mapNotNull { it.root?.packageName?.toString() }
                log("Pencereler (windows): ${pkgs.joinToString(", ").ifBlank { "(boş)" }}")
            }

            var launcherFound = false
            for (window in wins) {
                val root = window.root ?: continue
                val pkg  = root.packageName?.toString() ?: ""
                if (!pkg.contains("launcher", ignoreCase = true) &&
                    !pkg.contains("home", ignoreCase = true) &&
                    !pkg.contains("desktop", ignoreCase = true)) {
                    root.recycle(); continue
                }
                launcherFound = true
                findNodeByPackageName(root, packageName)?.let { return it }
                findNodeByAppName(root, appName)?.let { return it }
                root.recycle()
            }

            // Launcher penceresi yoksa tüm pencereleri tara (bazı cihazlarda package adı farklıdır)
            if (!launcherFound) {
                log("Launcher penceresi bulunamadı — tüm pencereler taranıyor")
                for (window in wins) {
                    val root = window.root ?: continue
                    findNodeByPackageName(root, packageName)?.let { return it }
                    findNodeByAppName(root, appName)?.let { return it }
                    root.recycle()
                }
            }
        }

        // --- Fallback: rootInActiveWindow (windows API boş dönerse) ---
        val activeRoot = rootInActiveWindow ?: return null
        log("rootInActiveWindow fallback: ${activeRoot.packageName}")
        findNodeByPackageName(activeRoot, packageName)?.let { return it }
        findNodeByAppName(activeRoot, appName)?.let { return it }
        activeRoot.recycle()
        return null
    }

    /** packageName sahibi veya extras'ta package bilgisi olan node'u bul. */
    private fun findNodeByPackageName(root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        if (root.packageName?.toString() == pkg) return root

        val extras = root.extras
        if (extras != null) {
            val ep = extras.getString("extra_package_name") ?: extras.getString("packageName")
            if (ep == pkg) return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByPackageName(child, pkg)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    /**
     * MIUI / HyperOS için: app adını contentDescription veya text olarak taşıyan
     * node'u bul, sonra tıklanabilir/sürüklenebilir üst container'ı döndür.
     *
     * Eşleşme önceliği (geniş → dar):
     *   1. Tam eşleşme: desc == appName
     *   2. Büyük/küçük harf farkı yok: desc.equals(appName, ignoreCase=true)
     *   3. İlk kelime: desc.startsWith("$appName,") veya desc.startsWith("$appName ")
     *      (ör. "WhatsApp, 3 bildirim")
     */
    private fun findNodeByAppName(root: AccessibilityNodeInfo, appName: String): AccessibilityNodeInfo? {
        val candidates = root.findAccessibilityNodeInfosByText(appName)
        if (candidates.isEmpty()) {
            // Debug: bu pencerede hangi text değerleri var?
            val samples = mutableListOf<String>()
            collectSampleTexts(root, samples, 6)
            if (samples.isNotEmpty()) log("  '$appName' bulunamadı — örnek: ${samples.joinToString(" | ")}")
            return null
        }

        for (node in candidates) {
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            val matched = desc.equals(appName, ignoreCase = true)
                || text.equals(appName, ignoreCase = true)
                || desc.startsWith("$appName,", ignoreCase = true)
                || desc.startsWith("$appName ", ignoreCase = true)
                || text.startsWith("$appName,", ignoreCase = true)
                || text.startsWith("$appName ", ignoreCase = true)
            if (matched) {
                val icon = findClickableAncestor(node) ?: node
                candidates.filter { it != node && it != icon }.forEach { it.recycle() }
                return icon
            }
            // Eşleşmedi ama benzer — debug için logla
            log("  '$appName' aday reddedildi: cd='$desc' txt='$text'")
            node.recycle()
        }
        return null
    }

    /** Debug: root altından en fazla max adet text/contentDescription örneği topla. */
    private fun collectSampleTexts(node: AccessibilityNodeInfo, out: MutableList<String>, max: Int) {
        if (out.size >= max) return
        val desc = node.contentDescription?.toString()
        val text = node.text?.toString()
        when {
            !desc.isNullOrBlank() -> out.add("cd:$desc")
            !text.isNullOrBlank() -> out.add("txt:$text")
        }
        for (i in 0 until node.childCount) {
            if (out.size >= max) break
            val child = node.getChild(i) ?: continue
            collectSampleTexts(child, out, max)
            child.recycle()
        }
    }

    /** Verilen node'un tıklanabilir/sürüklenebilir atalarını yukarı doğru ara (maks 5 seviye). */
    private fun findClickableAncestor(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var depth = 0
        var current = node.parent
        while (current != null && depth < 5) {
            if (current.isClickable || current.isLongClickable) return current
            val parent = current.parent
            current.recycle()
            current = parent
            depth++
        }
        current?.recycle()
        return null
    }

    // ── Klasör hedefi ─────────────────────────────────────────────────────

    private fun getOrCreateFolderTarget(categoryId: String, fromX: Float, fromY: Float): Pair<Float, Float> {
        folderPositions[categoryId]?.let { return it }

        findFolderNode(categoryId)?.let { folder ->
            val bounds = Rect()
            folder.getBoundsInScreen(bounds)
            val pos = Pair(bounds.exactCenterX(), bounds.exactCenterY())
            folderPositions[categoryId] = pos
            folder.recycle()
            return pos
        }

        val display = resources.displayMetrics
        val screenW = display.widthPixels.toFloat()
        val screenH = display.heightPixels.toFloat()
        val count   = folderPositions.size
        val col     = count % 4
        val row     = count / 4
        val cellW   = screenW / 4f
        val cellH   = (screenH * 0.7f) / 4f
        val pos = Pair(
            cellW * col + cellW / 2f,
            cellH * row + cellH / 2f + screenH * 0.08f
        )
        folderPositions[categoryId] = pos
        return pos
    }

    private fun findFolderNode(categoryId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findNodeByClass(root, "com.android.launcher3.folder.FolderIcon")
            ?: findNodeByClass(root, "com.sec.android.app.launcher.uninstall.FolderIcon")
            ?: findNodeByClass(root, "com.miui.home.launcher.FolderIcon")
            ?: findNodeByClass(root, "com.miui.home.recents.FolderIcon")
    }

    private fun findNodeByClass(root: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByClass(child, className)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    // ── Gesture'lar ────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dragIconToTarget(
        fromX: Float, fromY: Float,
        toX: Float, toY: Float,
        onComplete: (Boolean) -> Unit
    ) {
        val longPressPath = Path().apply { moveTo(fromX, fromY) }
        val dragPath = Path().apply {
            moveTo(fromX, fromY)
            quadTo((fromX + toX) / 2f, (fromY + toY) / 2f - 100f, toX, toY)
        }

        val longPress = GestureDescription.StrokeDescription(longPressPath, 0L, 600L, true)
        val drag      = longPress.continueStroke(dragPath, 0L, 900L, false)

        dispatchGesture(
            GestureDescription.Builder().addStroke(longPress).addStroke(drag).build(),
            object : GestureResultCallback() {
                override fun onCompleted(g: GestureDescription) { onComplete(true) }
                override fun onCancelled(g: GestureDescription) { log("  drag cancelled"); onComplete(false) }
            },
            handler
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)
}
