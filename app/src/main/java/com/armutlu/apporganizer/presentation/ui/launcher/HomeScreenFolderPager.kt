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
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.telemetry.FolderAppCountBucket
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import kotlin.math.absoluteValue

@Composable
internal fun FolderPager(
    pagerState: PagerState,
    displayFolders: List<AppFolder>,
    pageSize: Int,
    columnsCount: Int = 0,
    dragFromIndex: Int?,
    dragToIndex: Int?,
    @Suppress("UNUSED_PARAMETER") dragOffsetX: Float,
    @Suppress("UNUSED_PARAMETER") dragOffsetY: Float,
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
    onDrag: (dragAmount: Offset, page: Int) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onHomeLongPress: () -> Unit,
    editMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val columns = when {
        columnsCount > 0 -> columnsCount
        configuration.screenWidthDp >= 840 -> 6
        configuration.screenWidthDp >= 600 -> 5
        else -> 4
    }
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1)
    )
    HorizontalPager(
        state = pagerState,
        pageSpacing = 8.dp,
        flingBehavior = flingBehavior,
        modifier = modifier
    ) { page ->
        val signedOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        val pageOffset = signedOffset.absoluteValue.coerceIn(0f, 1f)
        val pageStart = page * pageSize
        val pageFolders = displayFolders.drop(pageStart).take(pageSize)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - (pageOffset * 0.18f)
                    scaleX = 1f - (pageOffset * 0.055f)
                    scaleY = 1f - (pageOffset * 0.055f)
                    rotationY = signedOffset.coerceIn(-1f, 1f) * -4f
                    cameraDistance = 18f * density
                },
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
                                    onDrag(dragAmount, page)
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
}

internal enum class FolderGestureMode { CONTEXT_MENU, REORDER }

internal fun folderGestureMode(editMode: Boolean): FolderGestureMode =
    if (editMode) FolderGestureMode.REORDER else FolderGestureMode.CONTEXT_MENU
