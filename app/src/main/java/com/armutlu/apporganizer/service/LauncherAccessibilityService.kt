package com.armutlu.apporganizer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
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
 * Çalışma prensibi:
 *   1. Launcher açık değilse HOME tuşuna bas
 *   2. Ekranı tara — uygulama ikonlarını bul
 *   3. Her ikonu hedef klasöre (veya başka bir ikona üstüne) sürükle
 *   4. Klasör oluşursa devam et, oluşmazsa ShortcutManager fallback
 *
 * Desteklenen launcher'lar: Pixel Launcher, Samsung One UI Home
 */
class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        // AppOrganizer'dan komut almak için intent action'lar
        const val ACTION_ORGANIZE    = "com.armutlu.apporganizer.ACTION_ORGANIZE"
        const val ACTION_STATUS      = "com.armutlu.apporganizer.ACTION_STATUS"
        const val EXTRA_CATEGORY_MAP = "category_map" // packageName -> categoryId JSON

        // Servisin dışarıdan kontrol edilebilmesi için singleton referans
        @Volatile var instance: LauncherAccessibilityService? = null
        @Volatile var isRunning = false
    }

    private val handler = Handler(Looper.getMainLooper())

    // Organize işlemi sırasında tutulan durum
    private var pendingApps: List<Pair<String, String>> = emptyList() // (packageName, categoryId)
    private var currentIndex = 0
    private var statusCallback: ((String) -> Unit)? = null

    // Klasör merkezleri: categoryId -> (x, y) — ilk ikon bırakıldıktan sonra güncellenir
    private val folderPositions = mutableMapOf<String, Pair<Float, Float>>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Timber.d("LauncherAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* node değişimlerini izliyoruz */ }

    override fun onInterrupt() {
        isRunning = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        instance = null
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * AppOrganizer'dan çağrılır. apps: List<(packageName, categoryId)>
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun startOrganize(
        apps: List<Pair<String, String>>,
        onStatus: (String) -> Unit
    ) {
        pendingApps   = apps
        currentIndex  = 0
        folderPositions.clear()
        statusCallback = onStatus

        onStatus("Ana ekrana gidiliyor...")
        goHome()
        // HOME animasyonu bitmesini bekle
        handler.postDelayed({ processNextApp() }, 1500)
    }

    // ── İşlem döngüsü ──────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processNextApp() {
        if (currentIndex >= pendingApps.size) {
            statusCallback?.invoke("✅ Tamamlandı! ${pendingApps.size} uygulama organize edildi.")
            return
        }

        val (pkg, categoryId) = pendingApps[currentIndex]
        statusCallback?.invoke("(${currentIndex + 1}/${pendingApps.size}) $pkg işleniyor...")

        val iconNode = findAppIcon(pkg)
        if (iconNode == null) {
            Timber.w("Icon not found for $pkg, skipping")
            currentIndex++
            handler.postDelayed({ processNextApp() }, 200)
            return
        }

        val iconBounds = Rect()
        iconNode.getBoundsInScreen(iconBounds)
        val fromX = iconBounds.exactCenterX()
        val fromY = iconBounds.exactCenterY()

        // Hedef klasörün konumunu bul veya oluştur
        val target = getOrCreateFolderTarget(categoryId, fromX, fromY)

        dragIconToTarget(fromX, fromY, target.first, target.second) {
            currentIndex++
            // Klasör oluşma/yerleşme animasyonunu bekle
            handler.postDelayed({ processNextApp() }, 600)
        }
    }

    // ── Yardımcı: ikon bul ─────────────────────────────────────────────────

    private fun findAppIcon(packageName: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null

        // Paket adına göre ara
        fun search(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.packageName?.toString() == packageName) return node
            // contentDescription veya text ile de ara
            val desc = node.contentDescription?.toString() ?: ""
            val text = node.text?.toString() ?: ""
            // Launcher ikon node'larının genellikle packageName'i yoktur,
            // contentDescription uygulama adını içerir — PackageManager'dan ismi al
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = search(child)
                if (found != null) return found
                child.recycle()
            }
            return null
        }

        // Launcher package'ında ara
        val windows = windows ?: return null
        for (window in windows) {
            val windowRoot = window.root ?: continue
            val launcherPkg = windowRoot.packageName?.toString() ?: ""
            if (!launcherPkg.contains("launcher") && !launcherPkg.contains("home")) {
                windowRoot.recycle()
                continue
            }
            // contentDescription ile package bilgisi eşleşen node'u bul
            val found = findNodeByPackage(windowRoot, packageName)
            if (found != null) return found
            windowRoot.recycle()
        }
        return null
    }

    private fun findNodeByPackage(root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        // Launcher'da ikon node'ları genellikle ViewGroup içinde,
        // contentDescription uygulama adını tutar. Tag olarak package adı da tutulabilir.
        if (root.packageName?.toString() == pkg) return root

        // Android 11+ bazı launcher'lar ikon package adını node'a ekler
        val extras = root.extras
        if (extras != null) {
            val nodePkg = extras.getString("extra_package_name")
                ?: extras.getString("packageName")
            if (nodePkg == pkg) return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findNodeByPackage(child, pkg)
            if (found != null) return found
            if (found == null) child.recycle()
        }
        return null
    }

    // ── Yardımcı: klasör hedefi ────────────────────────────────────────────

    /**
     * Kategorinin klasörü daha önce oluşturulduysa konumunu döndür.
     * Yoksa boş bir alan hesapla — ilk ikon orada bırakılır,
     * ikincisi üstüne sürüklenince Android otomatik klasör oluşturur.
     */
    private fun getOrCreateFolderTarget(
        categoryId: String,
        fromX: Float,
        fromY: Float
    ): Pair<Float, Float> {
        folderPositions[categoryId]?.let { return it }

        // Mevcut kategori klasörlerini tara
        val existingFolder = findFolderNode(categoryId)
        if (existingFolder != null) {
            val bounds = Rect()
            existingFolder.getBoundsInScreen(bounds)
            val pos = Pair(bounds.exactCenterX(), bounds.exactCenterY())
            folderPositions[categoryId] = pos
            existingFolder.recycle()
            return pos
        }

        // Klasör yok — ilk ikon için ekranın sağ tarafına hedef belirle
        // (launcher'ın boş alanı). İkinci ikon üstüne gelince klasör oluşur.
        val display = resources.displayMetrics
        val screenW = display.widthPixels.toFloat()
        val screenH = display.heightPixels.toFloat()

        // Mevcut klasör sayısına göre satır/sütun hesapla (3'lü grid)
        val existingCount = folderPositions.size
        val col = existingCount % 4
        val row = existingCount / 4
        val cellW = screenW / 4f
        val cellH = (screenH * 0.7f) / 4f  // ekranın üst %70'i
        val targetX = cellW * col + cellW / 2f
        val targetY = cellH * row + cellH / 2f + screenH * 0.08f // status bar offset

        val pos = Pair(targetX, targetY)
        folderPositions[categoryId] = pos
        return pos
    }

    private fun findFolderNode(categoryId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        // Her launcher'ın folder class adı farklı
        return findNodeByClass(root, "com.android.launcher3.folder.FolderIcon")            // Pixel
            ?: findNodeByClass(root, "com.sec.android.app.launcher.uninstall.FolderIcon") // Samsung
            ?: findNodeByClass(root, "com.miui.home.launcher.FolderIcon")                  // MIUI
            ?: findNodeByClass(root, "com.miui.home.recents.FolderIcon")                   // HyperOS
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

    // ── Yardımcı: gesture'lar ─────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dragIconToTarget(
        fromX: Float, fromY: Float,
        toX: Float, toY: Float,
        onComplete: () -> Unit
    ) {
        // 1. Önce uzun bas (600ms) — drag modunu aktif et
        // 2. Sonra o konumdan hedef konuma sürükle (800ms)
        // İki ayrı stroke — willContinue flag'i ile birleştirilir
        val longPressPath = Path().apply { moveTo(fromX, fromY) }
        val dragPath = Path().apply {
            moveTo(fromX, fromY)
            // Smooth eğri ile sürükle
            quadTo(
                (fromX + toX) / 2f, (fromY + toY) / 2f - 100f,
                toX, toY
            )
        }

        val longPress = GestureDescription.StrokeDescription(
            longPressPath,
            0L,       // startTime
            600L,     // duration — uzun bas
            true      // willContinue — drag gelecek
        )
        val drag = longPress.continueStroke(dragPath, 0L, 900L, false)

        val gesture = GestureDescription.Builder()
            .addStroke(longPress)
            .addStroke(drag)
            .build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                Timber.d("Drag completed: ($fromX,$fromY) -> ($toX,$toY)")
                onComplete()
            }
            override fun onCancelled(gestureDescription: GestureDescription) {
                Timber.w("Drag cancelled — skipping")
                onComplete()
            }
        }, handler)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}
