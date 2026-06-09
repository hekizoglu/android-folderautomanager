package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.domain.models.AppInfo
import kotlinx.coroutines.launch

private val DrawerBackground = Color(0xFFF8F8F8)
private val SearchBarColor   = Color(0xFFE8E8E8)
private val SearchIconColor  = Color(0xFF5F6368)
private val TextPrimary      = Color(0xFF202124)
private val CloseIconColor   = Color(0xFF5F6368)
private val DragHandleColor  = Color.Black.copy(alpha = 0.15f)
private val TealColor        = Color(0xFF00897B)
private val ChipBg           = Color(0xFFE0E0E0)
private val ChipBgActive     = TealColor
private val IndexLetterColor = Color(0xFF5F6368)

private const val SWIPE_DOWN_THRESHOLD = 80f

enum class SortMode(val label: String) {
    ALPHA("A-Z"),
    USAGE("Kullanım"),
    SIZE("Boyut")
}

@Composable
fun AllAppsDrawer(
    apps: List<AppInfo>,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onClose: () -> Unit,
    onAppClick: (String) -> Unit,
    iconSize: Dp = 56.dp
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var sortMode   by remember { mutableStateOf(SortMode.ALPHA) }
    val haptic     = LocalHapticFeedback.current
    val gridState  = rememberLazyGridState()
    val scope      = rememberCoroutineScope()

    // Sıralama uygula
    val sortedApps = remember(apps, sortMode, searchQuery) {
        val filtered = if (searchQuery.isBlank()) apps
                       else apps.filter { it.appName.contains(searchQuery, ignoreCase = true) }
        when (sortMode) {
            SortMode.ALPHA -> filtered.sortedBy { it.appName.lowercase() }
            SortMode.USAGE -> filtered.sortedByDescending { it.usageCount }
            SortMode.SIZE  -> filtered.sortedBy { it.appName.lowercase() } // APK boyutu runtime'da alınabilir, şimdilik alfa
        }
    }

    // Fihrist için harf listesi (sadece ALPHA modunda)
    val indexLetters = remember(sortedApps, sortMode) {
        if (sortMode == SortMode.ALPHA)
            sortedApps.map { it.appName.first().uppercaseChar().toString() }.distinct()
        else emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrawerBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > SWIPE_DOWN_THRESHOLD) onClose()
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onVerticalDrag = { _, delta ->
                        if (delta > 0) dragOffset += delta else dragOffset = 0f
                    }
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Ana içerik (grid + header)
            Column(modifier = Modifier.weight(1f)) {

                // Drag handle
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DragHandleColor)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Arama + kapat
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f).height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SearchBarColor)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = SearchIconColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text("Uygulama ara...", color = SearchIconColor, fontSize = 15.sp)
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    singleLine = true,
                                    cursorBrush = SolidColor(TextPrimary),
                                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp)
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Temizle", tint = SearchIconColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, "Kapat", tint = CloseIconColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sıralama chip'leri
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortMode.entries.forEach { mode ->
                        val active = sortMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) ChipBgActive else ChipBg)
                                .clickable { sortMode = mode }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) Color.White else TextPrimary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // App grid
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(items = sortedApps, key = { it.packageName }) { app ->
                        AppIconView(
                            app = app,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAppClick(app.packageName)
                            },
                            iconSize = iconSize,
                            showLabel = true
                        )
                    }
                }
            }

            // Sağ fihrist (sadece A-Z modunda ve arama yokken)
            if (sortMode == SortMode.ALPHA && searchQuery.isEmpty() && indexLetters.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(20.dp)
                        .padding(vertical = 48.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    indexLetters.forEach { letter ->
                        Text(
                            text = letter,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndexLetterColor,
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val idx = sortedApps.indexOfFirst {
                                        it.appName.first().uppercaseChar().toString() == letter
                                    }
                                    if (idx >= 0) {
                                        // Her satırda 4 app var
                                        val row = idx / 4
                                        scope.launch { gridState.animateScrollToItem(row * 4) }
                                    }
                                }
                                .padding(2.dp)
                        )
                    }
                }
            }
        }
    }
}
