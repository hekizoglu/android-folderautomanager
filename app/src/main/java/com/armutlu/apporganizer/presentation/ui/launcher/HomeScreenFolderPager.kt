package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.utils.AppAnalytics

@Composable
internal fun FolderPager(
    pagerState: PagerState,
    displayFolders: List<AppFolder>,
    pageSize: Int,
    dragFromIndex: Int?,
    dragToIndex: Int?,
    dragOffsetX: Float,
    dragOffsetY: Float,
    textAlpha: Float,
    folderSizeDp: Int,
    labelColor: Color,
    customFolderNames: Map<String, String>,
    customFolderEmojis: Map<String, String>,
    customFolderColors: Map<String, String>,
    folderCountVisible: Boolean,
    folderSwipeHint: Boolean,
    notifTextEnabled: Boolean,
    folderShape: String,
    haptic: HapticFeedback,
    onFolderClick: (AppFolder) -> Unit,
    onFolderLongClick: (AppFolder) -> Unit,
    onSwipeUp: (String) -> Unit,
    onNotificationTap: (String) -> Unit,
    onDragStart: (index: Int) -> Unit,
    onDrag: (dragAmount: Offset, page: Int) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onHomeLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
    ) { page ->
        val pageStart = page * pageSize
        val pageFolders = displayFolders.drop(pageStart).take(pageSize)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
            userScrollEnabled = false
        ) {
            items(pageFolders.size) { pageIndex ->
                val index = pageStart + pageIndex
                val folder = pageFolders[pageIndex]
                val isDragging = dragFromIndex == index
                val isDropTarget = dragToIndex == index && dragFromIndex != null && !isDragging
                FolderTile(
                    folder = folder,
                    onClick = {
                        if (dragFromIndex == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            AppAnalytics.folderOpened(folder.category.categoryId, folder.category.categoryName)
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
                    folderShape = folderShape,
                    modifier = Modifier
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDragStart(index)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount, page)
                                },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel
                            )
                        }
                        .then(
                            when {
                                isDragging ->
                                    Modifier
                                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                        .scale(1.08f)
                                isDropTarget ->
                                    Modifier.background(
                                        Color(0xFF00897B).copy(alpha = 0.28f),
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
}
