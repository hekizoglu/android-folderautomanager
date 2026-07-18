package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.telemetry.FolderAppCountBucket
import com.armutlu.apporganizer.telemetry.TelemetryEvent

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
    haptic: HapticFeedback,
    onFolderClick: (AppFolder) -> Unit,
    onFolderLongClick: (AppFolder) -> Unit,
    onSwipeUp: (String) -> Unit,
    onNotificationTap: (String) -> Unit,
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnsCount),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = modifier.then(
            if (pageAccessibilityLabel != null) {
                Modifier.semantics {
                    isTraversalGroup = true
                    paneTitle = pageAccessibilityLabel
                    contentDescription = pageAccessibilityLabel
                }
            } else {
                Modifier
            }
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
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
        val emptySlots = pageSize - pageFolders.size
        if (emptySlots > 0) {
            items(emptySlots) { _ ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHomeLongPress()
                                }
                            )
                        }
                )
            }
        }
    }
}

internal enum class FolderGestureMode { CONTEXT_MENU, REORDER }

internal fun folderGestureMode(editMode: Boolean): FolderGestureMode =
    if (editMode) FolderGestureMode.REORDER else FolderGestureMode.CONTEXT_MENU
