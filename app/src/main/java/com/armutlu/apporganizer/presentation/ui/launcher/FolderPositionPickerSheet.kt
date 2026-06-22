package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R

internal const val FOLDERS_PER_PAGE = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderPositionPickerSheet(
    allFolders: List<AppFolder>,
    currentIndex: Int,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primary   = MaterialTheme.colorScheme.primary
    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val haptic    = LocalHapticFeedback.current

    val pageCount = ((allFolders.size - 1) / FOLDERS_PER_PAGE) + 1
    var currentPage by remember { mutableStateOf(if (currentIndex >= 0) currentIndex / FOLDERS_PER_PAGE else 0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(onSurface.copy(0.2f)))
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Yeni konum seç", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            Text("Mevcut: ${currentIndex + 1}. sıra · Toplam: ${allFolders.size} klasör",
                fontSize = 12.sp, color = onSurface.copy(0.55f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            Spacer(Modifier.height(12.dp))

            val pageStart = currentPage * FOLDERS_PER_PAGE
            val pageEnd   = minOf(pageStart + FOLDERS_PER_PAGE, allFolders.size)
            val pageItems = allFolders.subList(pageStart, pageEnd)

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                itemsIndexed(pageItems) { localIdx, f ->
                    val globalIdx  = pageStart + localIdx
                    val isCurrent  = globalIdx == currentIndex
                    val isSelected = globalIdx == selectedIndex
                    val bgColor = when {
                        isSelected -> primary
                        isCurrent  -> primary.copy(alpha = 0.18f)
                        else       -> onSurface.copy(alpha = 0.07f)
                    }
                    val borderColor = when {
                        isSelected -> primary
                        isCurrent  -> primary.copy(alpha = 0.6f)
                        else       -> Color.Transparent
                    }
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = !isCurrent) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSelectIndex(globalIdx)
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(f.category.iconEmoji, fontSize = 18.sp)
                        Text("${globalIdx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (isSelected) surface else onSurface)
                        Text(f.category.categoryName, fontSize = 9.sp,
                            color = (if (isSelected) surface else onSurface).copy(alpha = 0.75f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (pageCount > 1) {
                Spacer(Modifier.height(12.dp))
                Text("Sayfa", fontSize = 11.sp, color = onSurface.copy(0.5f),
                    modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(pageCount) { pageIdx ->
                        val isActivePage = pageIdx == currentPage
                        val rangeStart = pageIdx * FOLDERS_PER_PAGE + 1
                        val rangeEnd   = minOf((pageIdx + 1) * FOLDERS_PER_PAGE, allFolders.size)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (isActivePage) primary else onSurface.copy(0.1f))
                                .clickable { currentPage = pageIdx }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$rangeStart–$rangeEnd", fontSize = 11.sp,
                                fontWeight = if (isActivePage) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActivePage) surface else onSurface)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel), color = onSurface.copy(0.7f))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onConfirm,
                    enabled = selectedIndex >= 0 && selectedIndex != currentIndex
                ) {
                    Text(stringResource(R.string.folder_move_confirm),
                        color = if (selectedIndex >= 0 && selectedIndex != currentIndex) primary else onSurface.copy(0.3f))
                }
            }
        }
    }
}
