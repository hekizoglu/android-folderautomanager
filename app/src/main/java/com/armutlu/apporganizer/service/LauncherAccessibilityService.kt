package com.armutlu.apporganizer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import timber.log.Timber

/**
 * Accessibility Service: launcher'da drag & drop ile ikon organizasyonu.
 *
 * Çalışma adımları:
 *   1. startOrganize() → HOME'a git
 *   2. dumpLauncherTree() → accessibility tree'yi tamamen logla
 *   3. Her uygulama için: ikon bul → long press → drag → hedef
 *
 * Debug logları ViewModel.appendDebugLog() üzerinden Settings ekranına gider.
 */
class LauncherAccessibilityService : AccessibilityService() {

    data class AppOrgInfo(
        val packageName: String,
        val categoryId: String,
        val appName: String
    )

    companion object {
        @Volatile var instance: LauncherAccessibilityService? = null
        @Volatile var isRunning = false

        // Gesture zamanlamaları (ms)
        // Long press: ViewConfiguration'dan dinamik alınır (cihaz ayarına göre değişir)
        // MIUI için 1.5x çarpanı uygulanır (AOSP CTS'de önerilen)
        private const val LONG_PRESS_MULTIPLIER = 1.5f
        private const val DRAG_MS         = 800L   // 800ms drag süresi yeterli
        private const val HOME_WAIT_MS    = 1500L
        private const val BETWEEN_APP_MS  = 800L
        private const val RETRY_WAIT_MS   = 1200L
    }

    private val handler = Handler(Looper.getMainLooper())

    // Dinamik long press süresi: sistemin ViewConfiguration'ından alınır
    private val longPressMs: Long
        get() = (ViewConfiguration.getLongPressTimeout() * LONG_PRESS_MULTIPLIER).toLong()

    private var pendingApps: List<AppOrgInfo> = emptyList()
    private var currentIndex = 0
    private var statusCallback: ((String) -> Unit)? = null
    private val folderPositions = mutableMapOf<String, Pair<Float, Float>>()

    // İstatistikler
    private var statsFound   = 0
    private var statsMissed  = 0
    private var statsDragged = 0
    private var statsFailed  = 0

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isRunning = true
        Timber.d("[A11y] Service connected — Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Timber.w("[A11y] Service interrupted")
        isRunning = false; instance = null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        Timber.w("[A11y] Service unbound")
        isRunning = false; instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        isRunning = false; instance = null
        Timber.d("[A11y] Service destroyed")
    }

    // ── Public API ─────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    fun startOrganize(apps: List<AppOrgInfo>, onStatus: (String) -> Unit) {
        pendingApps    = apps
        currentIndex   = 0
        statusCallback = onStatus
        folderPositions.clear()
        statsFound = 0; statsMissed = 0; statsDragged = 0; statsFailed = 0

        log("════════════════════════════════════")
        log("🚀 ORGANIZE BAŞLADI")
        log("════════════════════════════════════")
        log("📱 Cihaz: ${Build.MANUFACTURER} ${Build.MODEL}")
        log("🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        log("⏱ Long press eşiği: ${ViewConfiguration.getLongPressTimeout()}ms → kullanılan: ${longPressMs}ms")
        log("⏱ Drag süresi: ${DRAG_MS}ms")

        // MIUI/HyperOS özel uyarıları
        if (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
            || Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
            || Build.MANUFACTURER.equals("POCO", ignoreCase = true)) {
            log("⚠️ MIUI/HyperOS cihazı tespit edildi")
            log("   ⚠️ ARAŞTIRMA BULGUSU: MIUI home screen ikonları")
            log("   ⚠️ erişilebilirlik ağacında görünmeyebilir (Xiaomi kısıtlaması)")
            log("   📋 ZORUNLU ADIMLAR:")
            log("   1. Uygulama Bilgisi > Diğer İzinler > Arka planda açılır pencere: İZİN VER")
            log("   2. Uygulama Bilgisi > Otomatik Başlatma: ETKİNLEŞTİR")
            log("   3. Pil Optimizasyonu: KISITLAMA YOK")
            log("   4. Ayarlar > APK kısıtlamaları > Bu uygulama için > Kısıtlı ayarlara izin ver")
            log("   ℹ️ Tree dump ile ağaç yapısı kontrol edilecek...")
        }

        // Servis kabiliyetlerini logla
        logServiceCapabilities()

        log("📋 Toplam uygulama: ${apps.size}")
        log("🏠 HOME'a gidiliyor...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        handler.postDelayed({
            log("────────────────────────────────────")
            log("🔍 LAUNCHER TREE DUMP başlıyor")
            log("────────────────────────────────────")
            dumpAllWindows()
            log("────────────────────────────────────")
            log("▶️  Uygulama işleme döngüsü başlıyor")
            log("────────────────────────────────────")
            processNextApp()
        }, HOME_WAIT_MS)
    }

    // ── Servis diagnostik ──────────────────────────────────────────────────

    private fun logServiceCapabilities() {
        try {
            val info = serviceInfo
            log("⚙️ Servis özellikleri:")
            log("   canPerformGestures    : ${info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES != 0}")
            log("   canRetrieveWindows   : ${info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT != 0}")
            log("   canRequestFilterKeys : ${info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS != 0}")
            log("   feedbackType         : ${info.feedbackType}")
            log("   eventTypes           : 0x${info.eventTypes.toString(16)}")
        } catch (e: Exception) {
            log("⚠️ serviceInfo alınamadı: ${e.message}")
        }
    }

    // ── Full accessibility tree dump ───────────────────────────────────────

    private fun dumpAllWindows() {
        val wins = windows
        if (wins == null) {
            log("⛔ windows API null — erişilebilirlik izni yeterli değil")
            val activeRoot = rootInActiveWindow
            if (activeRoot != null) {
                log("ℹ️ rootInActiveWindow mevcut: ${activeRoot.packageName}")
                dumpTree(activeRoot, "ROOT", 0, maxDepth = 4)
                activeRoot.recycle()
            } else {
                log("⛔ rootInActiveWindow de null — servis pencereye erişemiyor")
            }
            return
        }

        log("🪟 Toplam pencere: ${wins.size}")
        wins.forEachIndexed { i, window ->
            val root = window.root
            val typeStr = when (window.type) {
                AccessibilityWindowInfo.TYPE_APPLICATION     -> "APPLICATION"
                AccessibilityWindowInfo.TYPE_INPUT_METHOD    -> "INPUT_METHOD"
                AccessibilityWindowInfo.TYPE_SYSTEM          -> "SYSTEM"
                AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "A11Y_OVERLAY"
                else                                         -> "TYPE_${window.type}"
            }
            val bounds = Rect(); window.getBoundsInScreen(bounds)
            log("  📦 Pencere[$i]: pkg=${root?.packageName} type=$typeStr bounds=$bounds")

            if (root != null) {
                val pkg = root.packageName?.toString() ?: ""
                val isLauncher = pkg.contains("launcher", ignoreCase = true)
                    || pkg.contains("home", ignoreCase = true)
                    || pkg.contains("desktop", ignoreCase = true)

                if (isLauncher) {
                    log("  🎯 LAUNCHER penceresi bulundu: $pkg")
                    log("  📊 Alt düğüm sayısı: ${root.childCount}")
                    dumpTree(root, "LAUNCHER", 0, maxDepth = 5)
                }
                root.recycle()
            } else {
                log("  ⚠️ Pencere[$i] root null")
            }
        }
    }

    private fun dumpTree(node: AccessibilityNodeInfo, label: String, depth: Int, maxDepth: Int) {
        if (depth > maxDepth) return
        val indent = "  ".repeat(depth)
        val cls    = node.className?.toString()?.substringAfterLast('.') ?: "?"
        val pkg    = node.packageName?.toString() ?: ""
        val desc   = node.contentDescription?.toString()?.take(40) ?: ""
        val text   = node.text?.toString()?.take(40) ?: ""
        val bounds = Rect(); node.getBoundsInScreen(bounds)
        val flags  = buildString {
            if (node.isClickable)     append("C")
            if (node.isLongClickable) append("L")
            if (node.isScrollable)    append("S")
            if (node.isFocusable)     append("F")
        }

        val info = buildString {
            append("${indent}[${cls}]")
            if (pkg.isNotBlank() && pkg != "com.miui.home") append(" pkg=$pkg")
            if (desc.isNotBlank()) append(" cd=\"$desc\"")
            if (text.isNotBlank()) append(" txt=\"$text\"")
            if (flags.isNotBlank()) append(" [$flags]")
            append(" ${bounds.width()}x${bounds.height()}@(${bounds.left},${bounds.top})")
        }
        log(info)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            dumpTree(child, "$label[$i]", depth + 1, maxDepth)
            child.recycle()
        }
    }

    // ── Uygulama işleme döngüsü ────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processNextApp() {
        if (currentIndex >= pendingApps.size) {
            log("════════════════════════════════════")
            log("✅ ORGANIZE TAMAMLANDI")
            log("   İkon bulundu : $statsFound")
            log("   Drag başarılı: $statsDragged")
            log("   Drag başarısız: $statsFailed")
            log("   Atlandı       : $statsMissed")
            log("════════════════════════════════════")
            return
        }

        val app = pendingApps[currentIndex]
        val progress = "${currentIndex + 1}/${pendingApps.size}"

        log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        log("[$progress] ${app.appName}")
        log("   pkg     : ${app.packageName}")
        log("   kategori: ${app.categoryId}")

        // Launcher kontrolü
        val activeWindow = rootInActiveWindow?.packageName?.toString() ?: "null"
        log("   aktif pencere: $activeWindow")
        if (activeWindow != "null"
            && !activeWindow.contains("launcher", ignoreCase = true)
            && !activeWindow.contains("home", ignoreCase = true)
            && !activeWindow.contains("desktop", ignoreCase = true)) {
            log("   ⚠️ Launcher değil → HOME'a dönülüyor")
            performGlobalAction(GLOBAL_ACTION_HOME)
            handler.postDelayed({ processNextApp() }, RETRY_WAIT_MS)
            return
        }

        // İkonu bul
        log("   🔎 İkon aranıyor...")
        val iconNode = findAppIconWithLog(app.packageName, app.appName)

        if (iconNode == null) {
            log("   ⏭ İkon erişilebilirlik ağacında bulunamadı → ATLANDI")
            log("      ℹ️ MIUI/HyperOS: home screen ikonları ağaçta görünmeyebilir")
            log("      ℹ️ Yukarıdaki tree dump'a bakın — boş geliyorsa MIUI kısıtlamasıdır")
            statsMissed++
            currentIndex++
            handler.postDelayed({ processNextApp() }, BETWEEN_APP_MS / 4)
            return
        }

        // Bounds
        val iconBounds = Rect()
        iconNode.getBoundsInScreen(iconBounds)
        val fromX = iconBounds.exactCenterX()
        val fromY = iconBounds.exactCenterY()
        log("   📍 İkon koordinatı: (${"%.0f".format(fromX)}, ${"%.0f".format(fromY)})")
        log("   📐 İkon boyutu: ${iconBounds.width()}x${iconBounds.height()}")
        log("   🏷️  class: ${iconNode.className}")
        log("   🏷️  isLongClickable: ${iconNode.isLongClickable}")
        statsFound++

        // Hedef
        val target = getOrCreateFolderTarget(app.categoryId, fromX, fromY)
        log("   🎯 Hedef koordinatı: (${"%.0f".format(target.first)}, ${"%.0f".format(target.second)})")
        log("   📏 Mesafe: ${"%.0f".format(
            Math.hypot((target.first - fromX).toDouble(), (target.second - fromY).toDouble())
        )} px")
        log("   👆 Long press (${longPressMs}ms) + drag (${DRAG_MS}ms) başlatılıyor...")

        dragIconToTarget(fromX, fromY, target.first, target.second) { success ->
            if (success) {
                log("   ✅ Drag tamamlandı")
                statsDragged++
            } else {
                log("   ❌ Drag iptal — sistem gesture'ı reddetti")
                statsFailed++
            }
            currentIndex++
            handler.postDelayed({ processNextApp() }, BETWEEN_APP_MS)
        }
    }

    // ── İkon arama ─────────────────────────────────────────────────────────

    private fun findAppIconWithLog(packageName: String, appName: String): AccessibilityNodeInfo? {
        // Strateji 1: windows API
        val wins = windows
        if (wins != null) {
            var launcherRoot: AccessibilityNodeInfo? = null
            for (window in wins) {
                val root = window.root ?: continue
                val pkg  = root.packageName?.toString() ?: ""
                if (pkg.contains("launcher", ignoreCase = true)
                    || pkg.contains("home", ignoreCase = true)
                    || pkg.contains("desktop", ignoreCase = true)) {
                    launcherRoot = root
                    break
                }
                root.recycle()
            }

            if (launcherRoot == null) {
                log("   ⚠️ S1: Launcher penceresi yok — tüm pencereler taranıyor (${wins.size} pencere)")
                for (window in wins) {
                    val root = window.root ?: continue
                    findByPackage(root, packageName)?.let {
                        log("   ✓ S1-all: packageName ile bulundu")
                        return it
                    }
                    findByName(root, appName, packageName)?.let {
                        log("   ✓ S1-all: appName ile bulundu")
                        return it
                    }
                    root.recycle()
                }
            } else {
                log("   🔍 S1: Launcher root: ${launcherRoot.packageName}, childCount=${launcherRoot.childCount}")
                findByPackage(launcherRoot, packageName)?.let {
                    log("   ✓ S1: packageName ile bulundu")
                    launcherRoot.recycle()
                    return it
                }
                findByName(launcherRoot, appName, packageName)?.let {
                    log("   ✓ S1: appName ile bulundu")
                    launcherRoot.recycle()
                    return it
                }
                launcherRoot.recycle()
            }
        } else {
            log("   ⚠️ windows API null")
        }

        // Strateji 2: rootInActiveWindow
        val activeRoot = rootInActiveWindow
        if (activeRoot != null) {
            log("   🔍 S2: rootInActiveWindow: ${activeRoot.packageName}, childCount=${activeRoot.childCount}")
            findByPackage(activeRoot, packageName)?.let {
                log("   ✓ S2: packageName ile bulundu")
                activeRoot.recycle()
                return it
            }
            findByName(activeRoot, appName, packageName)?.let {
                log("   ✓ S2: appName ile bulundu")
                activeRoot.recycle()
                return it
            }
            activeRoot.recycle()
        } else {
            log("   ⚠️ S2: rootInActiveWindow null")
        }

        return null
    }

    /** Accessibility tree'de packageName ile node ara (recursive). */
    private fun findByPackage(root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        if (root.packageName?.toString() == pkg) return root
        val extras = root.extras
        if (extras != null) {
            val ep = extras.getString("extra_package_name") ?: extras.getString("packageName")
            if (ep == pkg) return root
        }
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findByPackage(child, pkg)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    /** App adıyla contentDescription/text ara, sonra tıklanabilir container'ı döndür. */
    private fun findByName(root: AccessibilityNodeInfo, appName: String, pkgHint: String): AccessibilityNodeInfo? {
        val candidates = root.findAccessibilityNodeInfosByText(appName)
        if (candidates.isEmpty()) {
            // Bu pencerede ne görünüyor? İlk 8 node'u logla
            val samples = mutableListOf<String>()
            collectSamples(root, samples, 8)
            log("   📋 Pencerede görülen ilk node'lar: ${samples.joinToString(" | ").ifBlank { "(boş)" }}")
            return null
        }

        log("   📋 findByText '$appName': ${candidates.size} aday bulundu")
        for (node in candidates) {
            val desc   = node.contentDescription?.toString() ?: ""
            val text   = node.text?.toString() ?: ""
            val cls    = node.className?.toString() ?: ""
            val bounds = Rect(); node.getBoundsInScreen(bounds)
            log("      aday: cls=${cls.substringAfterLast('.')} cd=\"$desc\" txt=\"$text\" ${bounds.width()}x${bounds.height()}")

            val matched = desc.equals(appName, ignoreCase = true)
                || text.equals(appName, ignoreCase = true)
                || desc.startsWith("$appName,", ignoreCase = true)
                || desc.startsWith("$appName ", ignoreCase = true)
                || text.startsWith("$appName,", ignoreCase = true)
                || text.startsWith("$appName ", ignoreCase = true)

            if (matched) {
                log("      ✓ eşleşti!")
                val container = findClickableContainer(node) ?: node
                candidates.filter { it != node && it != container }.forEach { it.recycle() }
                return container
            } else {
                log("      ✗ eşleşmedi (beklenildi: '$appName')")
                node.recycle()
            }
        }
        return null
    }

    /** Node'un tıklanabilir atasını bul (max 6 seviye). */
    private fun findClickableContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var depth = 0
        var current = node.parent
        while (current != null && depth < 6) {
            if (current.isClickable || current.isLongClickable) return current
            val parent = current.parent
            current.recycle()
            current = parent
            depth++
        }
        current?.recycle()
        return null
    }

    /** Debug: tree'den ilk N tane text/contentDescription topla. */
    private fun collectSamples(node: AccessibilityNodeInfo, out: MutableList<String>, max: Int) {
        if (out.size >= max) return
        val desc = node.contentDescription?.toString()
        val text = node.text?.toString()
        when {
            !desc.isNullOrBlank() -> out.add("\"$desc\"")
            !text.isNullOrBlank() -> out.add("txt:\"$text\"")
        }
        for (i in 0 until node.childCount) {
            if (out.size >= max) break
            val child = node.getChild(i) ?: continue
            collectSamples(child, out, max)
            child.recycle()
        }
    }

    // ── Klasör hedefi ──────────────────────────────────────────────────────

    private fun getOrCreateFolderTarget(categoryId: String, fromX: Float, fromY: Float): Pair<Float, Float> {
        folderPositions[categoryId]?.let { return it }

        // Mevcut klasör ikonunu bul
        findFolderNode(categoryId)?.let { folder ->
            val bounds = Rect()
            folder.getBoundsInScreen(bounds)
            val pos = Pair(bounds.exactCenterX(), bounds.exactCenterY())
            folderPositions[categoryId] = pos
            folder.recycle()
            log("   📁 Mevcut klasör bulundu: (${pos.first.toInt()},${pos.second.toInt()})")
            return pos
        }

        // Yeni hedef koordinatı hesapla (grid)
        val display = resources.displayMetrics
        val screenW = display.widthPixels.toFloat()
        val screenH = display.heightPixels.toFloat()
        val count   = folderPositions.size
        val col     = count % 4
        val row     = count / 4
        val cellW   = screenW / 4f
        val cellH   = (screenH * 0.65f) / 4f
        val pos = Pair(
            cellW * col + cellW / 2f,
            cellH * row + cellH / 2f + screenH * 0.1f
        )
        folderPositions[categoryId] = pos
        log("   📍 Yeni hedef hesaplandı: sıra=$count col=$col row=$row pos=(${pos.first.toInt()},${pos.second.toInt()})")
        return pos
    }

    private fun findFolderNode(categoryId: String): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findByClass(root, "com.android.launcher3.folder.FolderIcon")
            ?: findByClass(root, "com.miui.home.launcher.FolderIcon")
            ?: findByClass(root, "com.miui.home.recents.FolderIcon")
            ?: findByClass(root, "com.sec.android.app.launcher.uninstall.FolderIcon")
    }

    private fun findByClass(root: AccessibilityNodeInfo, className: String): AccessibilityNodeInfo? {
        if (root.className?.toString() == className) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findByClass(child, className)
            if (found != null) return found
            child.recycle()
        }
        return null
    }

    // ── Gesture ────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dragIconToTarget(
        fromX: Float, fromY: Float,
        toX: Float,   toY: Float,
        onComplete: (Boolean) -> Unit
    ) {
        val currentLongPressMs = longPressMs

        // Long press: parmak aşağı, hareket yok — willContinue=true ile parmak ekranda kalır
        val longPressPath = Path().apply { moveTo(fromX, fromY) }
        val longPress = GestureDescription.StrokeDescription(
            longPressPath,
            0L,                  // startTime
            currentLongPressMs,  // dinamik: ViewConfig.getLongPressTimeout() * 1.5
            true                 // willContinue=true: parmak ekranda kalır, drag için zorunlu
        )

        // Drag: long press biter bitmez başlar; moveTo aynı koordinattan başlamalı (API zorunluluğu)
        val dragPath = Path().apply {
            moveTo(fromX, fromY)   // continueStroke için önceki stroke'un son noktasıyla aynı olmalı
            lineTo(toX, toY)       // lineTo (quadTo yerine) daha güvenilir
        }
        val drag = longPress.continueStroke(
            dragPath,
            0L,      // relative delay: long press biter bitmez başlar
            DRAG_MS, // drag süresi
            false    // willContinue=false: drag sonunda parmak kalkar
        )

        log("   🖐 Gesture gönderiliyor...")
        log("      from: (${fromX.toInt()}, ${fromY.toInt()})")
        log("      to  : (${toX.toInt()},   ${toY.toInt()})")
        log("      longPress: ${currentLongPressMs}ms (ViewConfig=${ViewConfiguration.getLongPressTimeout()}ms x${LONG_PRESS_MULTIPLIER}) | drag: ${DRAG_MS}ms")

        val dispatched = dispatchGesture(
            GestureDescription.Builder()
                .addStroke(longPress)
                .addStroke(drag)
                .build(),
            object : GestureResultCallback() {
                override fun onCompleted(g: GestureDescription) {
                    log("   🖐 Gesture: onCompleted")
                    onComplete(true)
                }
                override fun onCancelled(g: GestureDescription) {
                    log("   🖐 Gesture: onCancelled (sistem reddetti)")
                    onComplete(false)
                }
            },
            handler
        )

        if (!dispatched) {
            log("   ❌ dispatchGesture() false döndü — servis gesture yapamıyor")
            log("      canPerformGestures yetkisi eksik olabilir")
            onComplete(false)
        }
    }

    private fun log(msg: String) {
        Timber.d("[A11y] $msg")
        statusCallback?.invoke(msg)
    }
}
