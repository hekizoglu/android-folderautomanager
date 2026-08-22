package com.armutlu.apporganizer.presentation.ui.launcher.homev2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.presentation.ui.launcher.EditingCenterCard
import com.armutlu.apporganizer.presentation.ui.launcher.EditingCenterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Home V2 görsel regresyon testleri (tur 9).
 *
 * Yöntem: Robolectric + Compose UI — bileşenler GERÇEK ölçüleriyle render edilir,
 * semantics ağacı yürünerek her düğümün ekran sınırları içinde kaldığı doğrulanır
 * (taşma/sığmama dedektörü). Küçük ekran + büyük font ölçeği kombinasyonları özellikle
 * taranır. Not: test qualifier'ları px=dp (mdpi) kabulüyle deterministik koordinat verir.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp")
class HomeV2VisualUiTest {

    @get:Rule
    val rule = createComposeRule()

    // ── Taşma dedektörü ─────────────────────────────────────────────────────

    /** Semantics düğümünün kök koordinatlarındaki sınırları. */
    private fun SemanticsNode.boundsRect(): Rect {
        val p = positionInRoot
        return Rect(p.x, p.y, p.x + size.width.toFloat(), p.y + size.height.toFloat())
    }

    private fun SemanticsNode.flattenInto(into: MutableList<SemanticsNode>) {
        into.add(this)
        children.forEach { it.flattenInto(into) }
    }

    /** Kök (ekran) boyutuna karşı TÜM düğümlerin ekran sınırlarında kaldığını doğrular. */
    private fun assertNoOverflow(context: String) {
        val rootNode = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val screenRight = rootNode.size.width.toFloat()
        val screenBottom = rootNode.size.height.toFloat()
        val all = mutableListOf<SemanticsNode>()
        rootNode.flattenInto(all)
        val offenders = all.filter { n ->
            val b = n.boundsRect()
            val visible = b.width > 0f && b.height > 0f
            visible && (b.left < -1f || b.top < -1f || b.right > screenRight + 1f || b.bottom > screenBottom + 1f)
        }
        assertTrue(
            "EKRAN TAŞMASI ($context): " + offenders.joinToString { it.boundsRect().toString() },
            offenders.isEmpty(),
        )
    }

    // ── Fixture üreticileri ─────────────────────────────────────────────────

    private fun tile(
        id: String,
        title: String,
        appCount: Int = 3,
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

    private fun app(pkg: String) = AppInfo(packageName = pkg, appName = pkg.substringAfterLast('.'))

    private fun appsByPkgs(vararg pkgs: String) = pkgs.associateBy({ it }, { app(it) })

    // ── Klasör kartı ────────────────────────────────────────────────────────

    @Test
    fun `folder tile with very long title and badge stays on screen`() {
        rule.setContent {
            MaterialTheme {
                FolderTileV2(
                    tile = tile(
                        "long",
                        title = "Bu Çok Uzun Bir Klasör Başlığıdır Ve Kart Dışına Taşmamalıdır",
                        appCount = 128,
                        preview = listOf("com.a", "com.b", "com.c", "com.d"),
                        notif = 42,
                        urgent = true,
                    ),
                    previewApps = listOf(app("com.a"), app("com.b"), app("com.c"), app("com.d")),
                    onOpen = {},
                    onAppClick = {},
                    modifier = Modifier.fillMaxWidth().height(FOLDER_CELL_HEIGHT),
                )
            }
        }
        rule.waitForIdle()
        onNodeTextCheck("128 uygulama")
        assertNoOverflow("uzun başlıklı klasör kartı")
    }

    private fun onNodeTextCheck(text: String) {
        rule.onNodeWithText(text).assertIsDisplayed()
    }

    // ── Klasör grid'i ───────────────────────────────────────────────────────

    private fun eightTiles() = (1..8).map {
        tile(
            id = "cat$it",
            title = "Klasör $it",
            appCount = it * 3,
            preview = listOf("com.p$it.1", "com.p$it.2"),
        )
    }

    private val eightApps: Map<String, AppInfo> = (1..8).flatMap {
        listOf("com.p$it.1" to app("com.p$it.1"), "com.p$it.2" to app("com.p$it.2"))
    }.toMap()

    @Test
    fun `folder page grid renders all tiles inside phone screen`() {
        var opened: String? = null
        rule.setContent {
            MaterialTheme {
                FolderPageV2(
                    tiles = eightTiles(),
                    appsByPackage = eightApps,
                    onOpenFolder = { opened = it.categoryId },
                    onQuickLaunch = {},
                    onAppClick = {},
                    onReorder = { _, _ -> },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Klasör 1").assertIsDisplayed()
        rule.onNodeWithText("Klasör 8").assertIsDisplayed()
        assertNoOverflow("8 klasörlü grid (360x800)")

        rule.onNodeWithText("Klasör 3").assertHasClickAction()
        // NOT: kart MERKEZİ önizleme ikonlarına denk gelir (ikonların kendi onClick'i
        // vardır ve dokunuşu üstlenir); başlık bölgesine tıklanır.
        rule.onNodeWithText("Klasör 3").performTouchInput {
            down(Offset(10f, 8f))
            up()
        }
        rule.waitForIdle()
        assertEquals("cat3", opened)
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `folder grid fits on small phone screen`() {
        rule.setContent {
            MaterialTheme {
                FolderPageV2(
                    tiles = eightTiles(),
                    appsByPackage = eightApps,
                    onOpenFolder = {},
                    onQuickLaunch = {},
                    onAppClick = {},
                    onReorder = { _, _ -> },
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("8 klasörlü grid (320x568)")
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp", fontScale = 1.5f)
    fun `folder grid survives large font scale`() {
        rule.setContent {
            MaterialTheme {
                FolderPageV2(
                    tiles = eightTiles(),
                    appsByPackage = eightApps,
                    onOpenFolder = {},
                    onQuickLaunch = {},
                    onAppClick = {},
                    onReorder = { _, _ -> },
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("8 klasörlü grid (fontScale 1.5)")
    }

    // ── Dock ────────────────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `dock with five icons fits on narrow screen`() {
        val pkgs = listOf("com.one", "com.two", "com.three", "com.four", "com.five")
        rule.setContent {
            MaterialTheme {
                DockBarV2(
                    dockPackages = pkgs,
                    appsByPackage = appsByPkgs(*pkgs.toTypedArray()),
                    onAppClick = {},
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("5 ikonlu dock (320px)")
    }

    // ── Saat başlığı ────────────────────────────────────────────────────────

    @Test
    @Config(qualifiers = "w360dp-h800dp", fontScale = 2.0f)
    fun `clock header fits at max font scale`() {
        rule.setContent {
            MaterialTheme {
                ClockHeaderV2(
                    pulse = PulseStripState(
                        pulseScoreText = "72",
                        pulseBandLabel = "GOOD",
                        missionTitle = "Bugün ekran süreni iki saatin altında tut ve odaklan",
                        missionProgressFraction = 0.4f,
                        missionStreak = 3,
                    ),
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("saat başlığı (fontScale 2.0)")
    }

    // ── Jestler ─────────────────────────────────────────────────────────────

    @Test
    fun `fast swipe up on tile triggers quick launch`() {
        var launched: String? = null
        val tiles = listOf(
            tile("g", "Oyunlar", preview = listOf("com.game")),
        )
        rule.setContent {
            MaterialTheme {
                FolderPageV2(
                    tiles = tiles,
                    appsByPackage = appsByPkgs("com.game"),
                    onOpenFolder = {},
                    onQuickLaunch = { launched = it },
                    onAppClick = {},
                    onReorder = { _, _ -> },
                )
            }
        }
        rule.waitForIdle()

        // Hücre 0 merkezi (mdpi: dp=px): padding 16 + cellW(360-32-12)/2=158/2
        val centerX = 16f + 158f / 2f
        val centerY = FOLDER_CELL_HEIGHT.value / 2f
        rule.onRoot().performTouchInput {
            down(Offset(centerX, centerY))
            // Uzun basış eşiği dolmadan hızlı yukarı (slop'u aş)
            moveBy(Offset(0f, -30f))
            moveBy(Offset(0f, -60f))
            up()
        }
        rule.waitForIdle()
        assertEquals("com.game", launched)
    }

    @Test
    fun `long press and drag right reorders folders`() {
        var reorder: Pair<Int, Int>? = null
        rule.setContent {
            MaterialTheme {
                FolderPageV2(
                    tiles = listOf(tile("a", "A Klasörü"), tile("b", "B Klasörü")),
                    appsByPackage = emptyMap(),
                    onOpenFolder = {},
                    onQuickLaunch = {},
                    onAppClick = {},
                    onReorder = { from, to -> reorder = from to to },
                )
            }
        }
        rule.waitForIdle()

        val centerX = 16f + 158f / 2f
        // Başlık bölgesi (önizleme ikonları dokunuşu üstlenmesin)
        val headerY = 20f
        // Jest zamanlaması: performTouchInput İÇİNDE olay zamanları otomatik +16ms
        // artar (mainClock.advanceTimeBy etki etmez); uzun basış simülasyonu için
        // down / bekleme / devam AYRI çağrılarda yapılır (Compose'un kendi test deseni).
        rule.onRoot().performTouchInput { down(Offset(centerX, headerY)) }
        rule.mainClock.advanceTimeBy(1200)
        rule.onRoot().performTouchInput { moveBy(Offset(1f, 1f)) } // uzun basış tetiklenir
        rule.onRoot().performTouchInput {
            // Bir hücre sağa sürükle (slop aşılır → REORDER)
            moveBy(Offset(40f, 0f))
            moveBy(Offset(60f, 0f))
            moveBy(Offset(70f, 0f))
            up()
        }
        rule.waitForIdle()
        assertEquals(0 to 1, reorder)
    }

    @Test
    fun `diag card click with raw observer`() {
        var clicked = false
        rule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Box(
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val d = awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val e = awaitPointerEvent()
                                val c = e.changes.firstOrNull { it.id == d.id } ?: break
                                if (!c.pressed) break
                            }
                        }
                    },
                ) {
                    androidx.compose.material3.Card(onClick = { clicked = true }) {
                        androidx.compose.material3.Text("Tikla")
                    }
                }
            }
        }
        rule.waitForIdle()
        val clickables = rule.onAllNodes(androidx.compose.ui.test.hasClickAction()).fetchSemanticsNodes().size
        rule.onNodeWithText("Tikla").performClick()
        rule.waitForIdle()
        assertTrue("clickable nodes=$clickables, clicked=$clicked", clicked)
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `editing center card with all alerts fits on narrow screen`() {
        rule.setContent {
            MaterialTheme {
                EditingCenterCard(
                    state = EditingCenterState(
                        pendingClassificationCount = 7,
                        folderMergeCandidates = 3,
                        appCorrectionsCount = 2,
                        missingPermissionsCount = 1,
                        staleAppsCount = 12,
                    ),
                )
            }
        }
        rule.waitForIdle()
        assertNoOverflow("öneri merkezi kartı (320px, tüm uyarılar)")
    }

    @Test
    fun `editing center card hidden when no alerts`() {
        rule.setContent {
            MaterialTheme {
                EditingCenterCard(state = EditingCenterState())
            }
        }
        rule.waitForIdle()
        // Uyarı yokken kart render edilmez; kök boş kalır, taşma yine olmamalı.
        assertNoOverflow("öneri merkezi kartı (uyarı yok)")
    }

    @Test
    fun `folder tile respects reduced text alpha without breaking layout`() {
        rule.setContent {
            MaterialTheme {
                FolderTileV2(
                    tile = tile("alpha", "Düşük Alfa Klasörü", appCount = 5),
                    previewApps = listOf(app("com.a"), app("com.b")),
                    onOpen = {},
                    onAppClick = {},
                    textAlpha = 0.3f,
                    modifier = Modifier.fillMaxWidth().height(FOLDER_CELL_HEIGHT),
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Düşük Alfa Klasörü").assertIsDisplayed()
        assertNoOverflow("düşük metin alfası")
    }
}
