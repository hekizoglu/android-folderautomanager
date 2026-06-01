package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val folders by viewModel.folders.collectAsState()
    val openFolder by viewModel.openFolder.collectAsState()
    val allAppsOpen by viewModel.allAppsOpen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredAllApps by viewModel.filteredAllApps.collectAsState()

    // Swipe-up tespiti
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    // Geri tuşu yönetimi
    BackHandler(enabled = allAppsOpen || openFolder != null) {
        if (allAppsOpen) viewModel.closeAllApps()
        else viewModel.closeFolder()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Duvar kağıdı için arka plan şeffaf (tema windowShowWallpaper=true)
            .background(Color.Black.copy(alpha = 0.35f))
            .pointerInput(allAppsOpen) {
                if (!allAppsOpen) {
                    detectVerticalDragGestures(
                        onDragEnd = { swipeDelta = 0f },
                        onVerticalDrag = { _, dy ->
                            swipeDelta += dy
                            if (swipeDelta < -180f) {
                                viewModel.openAllApps()
                                swipeDelta = 0f
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Saat & Tarih ──────────────────────────────────────────────
            ClockSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
            )

            // ── Klasör grid ───────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(folders, key = { it.category.categoryId }) { folder ->
                    FolderTile(
                        folder = folder,
                        onClick = { viewModel.openFolder(folder) }
                    )
                }
            }

            // ── Swipe-up ipucu ────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "↑",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 18.sp
                )
                Text(
                    text = "Tüm Uygulamalar",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        // ── Ayarlar FAB ───────────────────────────────────────────────────
        IconButton(
            onClick = { viewModel.openManager(context) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Yönetim Ekranı",
                tint = Color.White
            )
        }

        // ── Tüm Uygulamalar Drawer ────────────────────────────────────────
        AnimatedVisibility(
            visible = allAppsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AllAppsDrawer(
                apps = filteredAllApps,
                searchQuery = searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                onAppClick = { pkg ->
                    viewModel.launchApp(context, pkg)
                },
                onClose = viewModel::closeAllApps
            )
        }
    }

    // ── Klasör Bottom Sheet ───────────────────────────────────────────────
    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = viewModel::closeFolder,
            onAppClick = { pkg -> viewModel.launchApp(context, pkg) }
        )
    }
}

// ── Saat Bileşeni ─────────────────────────────────────────────────────────

@Composable
private fun ClockSection(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("tr")) }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp
        )
        Text(
            text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// ── Klasör Kutusu ─────────────────────────────────────────────────────────

@Composable
private fun FolderTile(
    folder: AppFolder,
    onClick: () -> Unit
) {
    val catColor = remember(folder.category.colorHex) {
        runCatching { android.graphics.Color.parseColor(folder.category.colorHex) }
            .getOrDefault(android.graphics.Color.GRAY)
            .let { Color(it) }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji + badge
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(catColor.copy(alpha = 0.3f))
                    .border(1.dp, catColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = folder.category.iconEmoji, fontSize = 28.sp)
            }
            // App sayısı badge
            Surface(
                color = catColor,
                shape = CircleShape,
                modifier = Modifier
                    .size(18.dp)
                    .padding(0.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = folder.apps.size.coerceAtMost(99).toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = folder.category.categoryName,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(72.dp)
        )
    }
}

// ── Tüm Uygulamalar Ekranı ────────────────────────────────────────────────

@Composable
private fun AllAppsDrawer(
    apps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A).copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Başlık + kapat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tüm Uygulamalar",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                }
            }

            // Arama kutusu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Ara...", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp)
                        }
                        inner()
                    }
                )
            }

            // App grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppIconView(
                        app = app,
                        onClick = {
                            onAppClick(app.packageName)
                            onClose()
                        },
                        iconSize = 52.dp
                    )
                }
            }
        }
    }
}
