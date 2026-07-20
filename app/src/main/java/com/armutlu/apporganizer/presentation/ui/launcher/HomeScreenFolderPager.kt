package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.telemetry.FolderAppCountBucket
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import java.util.concurrent.TimeUnit

/**
 * Döngü P04: `FolderPager`ın (P05'te söküldü — bkz. HomePagerHost.kt) tek-sayfa grid render
 * mantığı buraya taşındı. Saf/test edilebilir — pager state veya sayfa geçiş efekti bağımlılığı
 * yok; `globalStartIndex` ile dışarıdan verilen gerçek index üzerinden drag/reorder ve
 * empty-slot davranışı hesaplanır.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm Döngü P04/P05.
 * Döngü P05'ten itibaren `HomePagerHost`'un bir sayfası olarak (folderPageContent slotu üzerinden)
 * çağrılır; sayfa geçiş graphicsLayer efekti artık tek yerden `HomePagerHost` tarafından uygulanır
 * (eskiden `FolderPager` içindeydi, iç içe HorizontalPager kaldırıldığı için buraya taşınamaz).
 */
@Composable
internal fun FolderGridPage(
    pageFolders: List<AppFolder>,
    globalStartIndex: Int,
    pageSize: Int,
    columnsCount: Int,
    dragFromIndex: Int?,
    dragToIndex: Int?,
    textAlpha: Float,
    folderSizeDp: Int,
    labelColor: Color,
    customFolderNames: Map<String, String>,
    customFolderEmojis: Map<String, String>,
    customFolderColors: Map<String, String>,
    folderCountVisible: Boolean,
    folderSwipeHint: Boolean,
    notifTextEnabled: Boolean,
    unusedInfoEnabled: Boolean = false,
    folderBadgeEnabled: Boolean = false,
    folderShape: String,
    pixelLookEnabled: Boolean = false,
    folderGlassBorderEnabled: Boolean = true,
    haptic: HapticFeedback,
    onFolderClick: (AppFolder) -> Unit,
    onFolderLongClick: (AppFolder) -> Unit,
    onSwipeUp: (String) -> Unit,
    onNotificationTap: (String) -> Unit,
    // EX02 — "Bu sayfadaki klasörler" alt bilgi panelindeki "N okunmamış bildirim" satırı
    // artık gerçek hedefe sahip: Bildirim Raporu ekranı (Routes.NOTIFICATION_REPORT).
    // pageNotifications == 0 iken de (ipucu metni gösterilirken) dokunulabilir kalır; tek
    // hedef olduğundan davranış tutarlıdır.
    onNotificationSummaryTap: () -> Unit = {},
    onDragStart: (index: Int) -> Unit,
    onDrag: (dragAmount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onHomeLongPress: () -> Unit,
    editMode: Boolean = false,
    // Döngü P19 madde 5 — "Klasör sayfası X / Y" açıklaması. İndicator dot'ları zaten bu metni
    // taşıyordu (home_page_indicator_folder_page), ama TalkBack kullanıcısı indicator'a değil
    // doğrudan sayfa İÇERİĞİNE odaklandığında (swipe ile page'e girince) aynı bilgi eksikti —
    // bu parametre `HomeScreen.kt`'de `homePagerCurrentPageDescription`/indicator ile AYNI
    // string kaynağından (home_page_indicator_folder_page) üretilip buraya geçirilir; null ise
    // (tek klasör sayfası / eski çağrı yerleri) davranış DEĞİŞMEZ, semantics eklenmez.
    pageAccessibilityLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pageApps = pageFolders.sumOf { it.apps.size }
    val pageNotifications = pageFolders.sumOf { folder -> folder.apps.sumOf { it.notificationCount } }
    var pageInsightsEnabled by remember(context) {
        mutableStateOf(AppPrefs.isFolderPageInsightsEnabled(context))
    }
    var pageInsightsMutedUntil by remember(context) {
        mutableStateOf(AppPrefs.getFolderPageInsightsMutedUntil(context))
    }
    val pageInsightText = remember(pageFolders, pageInsightsEnabled, pageInsightsMutedUntil) {
        if (!pageInsightsEnabled || pageInsightsMutedUntil > System.currentTimeMillis()) {
            null
        } else {
            buildFolderPageInsightText(pageFolders)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (pageAccessibilityLabel != null) {
                    Modifier.semantics {
                        isTraversalGroup = true
                        paneTitle = pageAccessibilityLabel
                        contentDescription = pageAccessibilityLabel
                    }
                } else Modifier
            )
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxSize(),
                // Klasör grubu, özellikle tek satırlı/az klasörlü sayfalarda üstte kalmaz;
                // tüm grup kullanılabilir alanda dikey olarak merkezlenir.
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
                userScrollEnabled = false
            ) {
        items(pageFolders.size) { pageIndex ->
            val index = globalStartIndex + pageIndex
            val folder = pageFolders[pageIndex]
            val isDragging = dragFromIndex == index
            val isDropTarget = dragToIndex == index && dragFromIndex != null && !isDragging
            FolderTile(
                folder = folder,
                onClick = {
                    if (dragFromIndex == null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        AppAnalytics.folderOpened(
                            folderType = if (folder.category.isSystemCategory) {
                                TelemetryEvent.FolderType.SYSTEM
                            } else {
                                TelemetryEvent.FolderType.USER_CREATED
                            },
                            appCount = FolderAppCountBucket.from(folder.apps.size)
                        )
                        onFolderClick(folder)
                    }
                },
                onLongClick = {
                    if (dragFromIndex == null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFolderLongClick(folder)
                    }
                },
                onSwipeUp = onSwipeUp,
                onNotificationTap = onNotificationTap,
                textAlpha = textAlpha,
                folderSizeDp = folderSizeDp,
                labelColor = labelColor,
                customName = customFolderNames[folder.category.categoryId],
                customEmoji = customFolderEmojis[folder.category.categoryId],
                customColor = customFolderColors[folder.category.categoryId],
                folderCountVisible = folderCountVisible,
                folderSwipeHintEnabled = folderSwipeHint,
                notifTextEnabled = notifTextEnabled,
                unusedInfoEnabled = unusedInfoEnabled,
                folderBadgeEnabled = folderBadgeEnabled,
                folderShape = folderShape,
                pixelLookEnabled = pixelLookEnabled,
                folderGlassBorderEnabled = folderGlassBorderEnabled,
                modifier = Modifier
                    .then(if (folderGestureMode(editMode) == FolderGestureMode.REORDER) Modifier.pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart(index)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel
                        )
                    } else Modifier)
                    .then(
                        when {
                            isDragging ->
                                Modifier
                                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                    .scale(1.08f)
                            isDropTarget ->
                                Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    RoundedCornerShape(12.dp)
                                )
                            dragFromIndex != null ->
                                Modifier.alpha(0.72f)
                            else -> Modifier
                        }
                    )
            )
        }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (pageInsightText != null) 152.dp else 112.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .background(Color(0xFF171717), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Bu sayfadaki klasörler",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${pageFolders.size} klasör · $pageApps uygulama",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (pageInsightText != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = pageInsightText,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)
                            AppPrefs.muteFolderPageInsights(context, until)
                            pageInsightsMutedUntil = until
                        }
                    ) { Text("Sessize al") }
                    TextButton(
                        onClick = {
                            AppPrefs.setFolderPageInsightsEnabled(context, false)
                            pageInsightsEnabled = false
                        }
                    ) { Text("Kapat") }
                }
                Spacer(Modifier.width(1.dp))
            }
            Text(
                text = if (pageNotifications > 0) "$pageNotifications okunmamış bildirim" else "Yeni bildirim ve öneriler burada görünür",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onNotificationSummaryTap)
                    .semantics {
                        role = Role.Button
                        contentDescription = if (pageNotifications > 0) {
                            "$pageNotifications okunmamış bildirim. Bildirim raporunu açmak için dokun."
                        } else {
                            "Bildirim raporunu açmak için dokun."
                        }
                    }
            )
        }
    }
}

internal enum class FolderGestureMode { CONTEXT_MENU, REORDER }

internal fun folderGestureMode(editMode: Boolean): FolderGestureMode =
    if (editMode) FolderGestureMode.REORDER else FolderGestureMode.CONTEXT_MENU

private fun buildFolderPageInsightText(pageFolders: List<AppFolder>): String? {
    val smallFolderCount = pageFolders.count { it.apps.size in 1..2 }
    val pendingAppCount = pageFolders.sumOf { folder ->
        folder.apps.count { app ->
            !app.isSystemApp &&
                !app.isHidden &&
                !app.isCategoryLocked &&
                app.classificationReviewState == "PENDING"
        }
    }
    val parts = buildList {
        if (smallFolderCount >= 2) add("Klasor birlestir: $smallFolderCount kucuk klasor aday")
        if (pendingAppCount > 0) add("Uygulama tanimla: $pendingAppCount bekliyor")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
