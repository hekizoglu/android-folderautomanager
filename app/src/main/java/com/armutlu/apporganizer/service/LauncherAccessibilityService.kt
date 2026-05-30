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
        Timber.d("LauncherAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        isRunning = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        isRunning = false
        instance = null
    }

    // ── Public API ─────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    fun startOrganize(apps: List<AppOrgInfo>, onStatus: (String) -> Unit) {
        pendingApps    = apps
        currentIndex   = 0
        folderPositions.clear()
        statusCallback = onStatus

        log("🚀 organize başladı: ${apps.size} uygulama")
        log("Ekran: ${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
        log("Aktif pencere: ${rootInActiveWindow?.packageName}")
        goHome()
        handler.postDelayed({
            log("HOME sonrası pencere: ${rootInActiveWindow?.packageName}")
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
        if (!activeWindow.contains("launcher") && !activeWindow.contains("home")) {
            log("  ⚠️ Launcher değil, HOME'a dönülüyor...")
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

    private fun findAppIcon(packageName: String, appName: String): AccessibilityNodeInfo? {
        val wins = windows ?: return null
        for (window in wins) {
            val root = window.root ?: continue
            val pkg  = root.packageName?.toString() ?: ""
            if (!pkg.contains("launcher") && !pkg.contains("home")) {
                root.recycle(); continue
            }

            // Strateji 1: packageName eşleşmesi (Pixel / Samsung / Nova)
            findNodeByPackageName(root, packageName)?.let { return it }

            // Strateji 2: app adıyla contentDescription araması (MIUI / HyperOS)
            findNodeByAppName(root, appName)?.let { return it }

            root.recycle()
        }
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
     * MIUI / HyperOS için: app adını contentDescription olarak taşıyan
     * node'u bul, sonra tıklanabilir/sürüklenebilir üst container'ı döndür.
     *
     * MIUI Home'da BubbleTextView → contentDescription = "AppName" (tam eşleşme).
     */
    private fun findNodeByAppName(root: AccessibilityNodeInfo, appName: String): AccessibilityNodeInfo? {
        // findAccessibilityNodeInfosByText substring arar — sonra tam eşleşme filtrele
        val candidates = root.findAccessibilityNodeInfosByText(appName)
        for (node in candidates) {
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            if (desc == appName || text == appName) {
                // Tıklanabilir üst node'u bul (ikon container)
                val icon = findClickableAncestor(node) ?: node
                // Diğer adayları recycle et
                candidates.filter { it != node && it != icon }.forEach { it.recycle() }
                return icon
            }
            node.recycle()
        }
        return null
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
