package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.semantics.SemanticsNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.AllAppsDrawer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home V2 GENEL görsel regresyon paketi (tur 19).
 *
 * HomeV2VisualUiTest klasör kartı/grid/dock/saat temellerini kapsar; bu dosya kalan
 * yüzeyleri ve uç durumları tarar: uygulama çekmecesi (dar ekran + büyük font),
 * klasör kartı uç durumları (boş önizleme, 9+ rozet), nabız şeridi dar ekran ve
 * sayfa-arası sıralama matematiği.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class HomeV2GeneralVisualTest {

    @get:Rule
    val rule = createComposeRule()

    // ── Taşma dedektörü (aynı yöntem: semantics ağacı yürüyüşü) ───────────────

    private fun SemanticsNode.flattenInto(into: MutableList<SemanticsNode>) {
        into.add(this)
        children.forEach { it.flattenInto(into) }
    }

    /** Kaydırılabilir kapsayıcıların sınırları (LazyColumn öğeleri viewport dışında
     *  raporlanır — bu gerçek taşma değildir). */
    private fun collectScrollableBounds(node: SemanticsNode, into: MutableList<androidx.compose.ui.geometry.Rect>) {
        val isScrollable = node.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) ||
            node.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollToIndex)
        if (isScrollable) {
            val p = node.positionInRoot
            into.add(androidx.compose.ui.geometry.Rect(p.x, p.y, p.x + node.size.width.toFloat(), p.y + node.size.height.toFloat()))
        }
        node.children.forEach { collectScrollableBounds(it, into) }
    }

    private fun assertNoOverflow(context: String) {
        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val screenRight = rootNode.size.width.toFloat()
        val screenBottom = rootNode.size.height.toFloat()
        val all = mutableListOf<SemanticsNode>()
        rootNode.flattenInto(all)
        val scrollables = mutableListOf<androidx.compose.ui.geometry.Rect>()
        collectScrollableBounds(rootNode, scrollables)
        val offenders = all.filter { n ->
            val p = n.positionInRoot
            val w = n.size.width.toFloat()
            val h = n.size.height.toFloat()
            val visible = w > 0f && h > 0f
            val overflows = visible && (p.x < -1f || p.y < -1f || p.x + w > screenRight + 1f || p.y + h > screenBottom + 1f)
            // Kaydırılabilir kapsayıcı içindeki öğeler viewport dışına uzanabilir (beklenen davranış)
            val insideScrollable = scrollables.any { r -> p.x >= r.left - 1f && p.y >= r.top - 1f && p.x <= r.right + 1f }
            overflows && !insideScrollable
        }
        assertTrue(
            "EKRAN TAŞMASI ($context): " + offenders.joinToString { it.positionInRoot.toString() },
            offenders.isEmpty(),
        )
    }

    private fun app(pkg: String, name: String = pkg.substringAfterLast('.')) =
        AppInfo(packageName = pkg, appName = name)

    private fun manyApps(n: Int): List<AppInfo> = (1..n).map { app("com.example.app$it", "Uygulama $it") }

    // ── Uygulama çekmecesi ────────────────────────────────────────────────────

    @Test
    fun `drawer with many apps fits on narrow screen`() {
        rule.setContent {
            MaterialTheme {
                AllAppsDrawer(
                    apps = manyApps(40),
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Uygulama 1").assertIsDisplayed()
        assertNoOverflow("çekmece 40 uygulama (360x800)")
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `drawer fits on small phone screen`() {
        rule.setContent {
            MaterialTheme {
                AllAppsDrawer(
                    apps = manyApps(25),
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("çekmece (320x568)")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp", fontScale = 1.5f)
    fun `drawer survives large font scale`() {
        rule.setContent {
            MaterialTheme {
                AllAppsDrawer(
                    apps = manyApps(15),
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("çekmece (fontScale 1.5)")
    }

    @Test
    fun `drawer search filters and stays in bounds`() {
        rule.setContent {
            MaterialTheme {
                AllAppsDrawer(
                    apps = manyApps(30) + app("com.special.findme", "BulBeni"),
                    searchQuery = "bulbeni",
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("BulBeni").assertIsDisplayed()
        assertNoOverflow("çekmece arama sonucu")
    }

    // ── Klasör kartı uç durumları ────────────────────────────────────────────

    private fun tile(
        id: String,
        title: String,
        appCount: Int,
        preview: List<String> = emptyList(),
        notif: Int = 0,
        urgent: Boolean = false,
    ) = FolderTileState(
        categoryId = id,
        title = title,
        emoji = "📁",
        colorHex = "#00897B",
        appCount = appCount,
        previewPackages = preview,
        notificationTotal = notif,
        hasUrgentNotification = urgent,
        quickLaunchPackage = preview.firstOrNull(),
    )

    @Test
    fun `folder tile with empty preview renders intact`() {
        rule.setContent {
            MaterialTheme {
                FolderTileV2(
                    tile = tile("empty", "Boş Önizlemeli Klasör", 0),
                    previewApps = emptyList(),
                    onOpen = {},
                    onAppClick = {},
                    modifier = Modifier.fillMaxWidth().height(FOLDER_CELL_HEIGHT),
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Boş Önizlemeli Klasör").assertIsDisplayed()
        assertNoOverflow("boş önizlemeli kart")
    }

    @Test
    fun `folder tile with huge notification badge renders 9 plus`() {
        rule.setContent {
            MaterialTheme {
                FolderTileV2(
                    tile = tile("busy", "Yoğun Klasör", 7, preview = listOf("com.a"), notif = 120, urgent = true),
                    previewApps = listOf(app("com.a")),
                    onOpen = {},
                    onAppClick = {},
                    modifier = Modifier.fillMaxWidth().height(FOLDER_CELL_HEIGHT),
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("9+").assertIsDisplayed()
        assertNoOverflow("9+ rozetli kart")
    }

    // ── Nabız şeridi dar ekran ───────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `clock header with long mission title fits on narrow screen`() {
        rule.setContent {
            MaterialTheme {
                ClockHeaderV2(
                    pulse = PulseStripState(
                        pulseScoreText = "58",
                        pulseBandLabel = "BALANCED",
                        missionTitle = "Bugün ekran süreni iki saatin altında tutarak odaklanmanı koru",
                        missionProgressFraction = 0.66f,
                        missionStreak = 5,
                    ),
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("saat + uzun görev başlığı (320px)")
    }

    // ── Sayfa-arası sıralama matematiği (saf mantık) ─────────────────────────

    @Test
    fun `cross-page reorder math maps local indices to global order`() {
        // 8 klasör, sayfa başına 4 (2 sütun x 2 satır) → sayfa 0: 0..3, sayfa 1: 4..7
        val folders = (1..8).map { tile("cat$it", "K$it", it) }
        val pages = folderChunks(folders, pageSize = 4, columns = 2)
        assertEquals(2, pages.size)
        assertEquals(4, pages[0].size)

        // Sayfa 1'de yerel 0 → 2 taşıma, globalde 4 → 6 demektir
        val pageOffset = pages.take(1).sumOf { it.size }
        val global = folders.map { it.categoryId }.toMutableList()
        val from = pageOffset + 0
        val to = pageOffset + 2
        val item = global.removeAt(from)
        global.add(to, item)
        assertEquals(listOf("cat1", "cat2", "cat3", "cat4", "cat6", "cat7", "cat5", "cat8"), global)

        // moveItem aynı sonucu vermelidir
        val viaHelper = moveItem(folders.map { it.categoryId }, from, to)
        assertEquals(global, viaHelper)
    }

    @Test
    fun `reorder within single page keeps other pages untouched`() {
        val folders = (1..6).map { tile("cat$it", "K$it", it) }
        val reordered = moveItem(folders.map { it.categoryId }, 0, 2)
        assertEquals(listOf("cat2", "cat3", "cat1", "cat4", "cat5", "cat6"), reordered)
    }
}
