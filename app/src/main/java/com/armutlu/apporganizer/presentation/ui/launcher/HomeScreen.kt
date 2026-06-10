package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
    val filteredApps by viewModel.filteredAllApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val haptic = LocalHapticFeedback.current
    val dockPackages by viewModel.dockPackages.collectAsState()
    var dockEditOpen by remember { mutableStateOf(false) }
    var contextMenuApp by remember { mutableStateOf<com.armutlu.apporganizer.domain.models.AppInfo?>(null) }
    var categoryPickerApp by remember { mutableStateOf<com.armutlu.apporganizer.domain.models.AppInfo?>(null) }

    // Drag & drop state
    var dragFromIndex by remember { mutableStateOf<Int?>(null) }
    var dragToIndex   by remember { mutableStateOf<Int?>(null) }
    var draggingFolders by remember { mutableStateOf<List<AppFolder>?>(null) }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    // rememberUpdatedState ile closure'lar her zaman güncel değeri okur
    val currentAllAppsOpen by rememberUpdatedState(allAppsOpen)
    LaunchedEffect(allAppsOpen) { if (allAppsOpen) swipeDelta = 0f }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!currentAllAppsOpen && available.y < -400f) {
                    viewModel.openAllApps()
                }
                return Velocity.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                if (!currentAllAppsOpen && available.y < 0f) {
                    swipeDelta += available.y
                    if (swipeDelta < -swipeThresholdPx) {
                        viewModel.openAllApps()
                        swipeDelta = 0f
                    }
                }
                return Offset.Zero
            }
        }
    }

    val isLoading = folders.isEmpty() && allApps.isEmpty()

    LaunchedEffect(Unit) {
        viewModel.loadDockPackages(context)
        viewModel.syncAppSizes(context)
    }

    // İzin verilmeden launcher seçildiyse veya veriler henüz yüklenmediyse güvenli fallback
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A2E)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "AppOrganizer",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "Uygulamalar yükleniyor...",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                ) {
                    Text("Launcher Ayarları")
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                ) {
                    Text("Uygulama Ayarları")
                }
            }
        }
        return
    }

    BackHandler(enabled = allAppsOpen || openFolder != null) {
        if (allAppsOpen) viewModel.closeAllApps()
        else viewModel.closeFolder()
    }

    // Root box — fully transparent so wallpaper shows through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // İzin uyarı banner'ı (kapatılabilir)
            PermissionsBanner()

            // Clock widget — top center, Pixel style (uzun bas → yönetim ekranı)
            PixelClockWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { viewModel.openManager(context) })
                    }
            )

            // İstatistik bandı — toplam klasör ve uygulama sayısı
            val totalApps   = folders.sumOf { it.apps.size }
            val totalFolders = folders.size
            if (totalFolders > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "$totalFolders klasör  ·  $totalApps uygulama",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Folder grid — 4 columns, centered
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                val displayFolders = draggingFolders ?: folders
                items(displayFolders.size) { index ->
                    val folder = displayFolders[index]
                    val isDragging = dragFromIndex == index
                    FolderTile(
                        folder = folder,
                        onClick = {
                            if (dragFromIndex == null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openFolder(folder)
                            }
                        },
                        onSwipeUp = { pkg -> viewModel.launchApp(context, pkg) },
                        modifier = Modifier
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragFromIndex = index
                                        draggingFolders = folders.toMutableList()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val from = dragFromIndex ?: return@detectDragGesturesAfterLongPress
                                        // Tahmini hedef indeks: her tile yaklaşık 90dp
                                        val tileWidthPx = with(density) { 90.dp.toPx() }
                                        val dx = change.position.x
                                        val dy = change.position.y
                                        val colCount = 4
                                        val colOffset = (dx / tileWidthPx).toInt().coerceIn(-1, 1)
                                        val rowOffset = (dy / tileWidthPx).toInt().coerceIn(-1, 1)
                                        val to = (from + rowOffset * colCount + colOffset)
                                            .coerceIn(0, (draggingFolders?.lastIndex ?: 0))
                                        if (to != dragToIndex) {
                                            dragToIndex = to
                                            draggingFolders = draggingFolders?.toMutableList()?.also { list ->
                                                if (from != to && from in list.indices && to in list.indices) {
                                                    val item = list.removeAt(from)
                                                    list.add(to, item)
                                                    dragFromIndex = to
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingFolders?.let { viewModel.reorderFolders(context, it) }
                                        dragFromIndex = null
                                        dragToIndex = null
                                        draggingFolders = null
                                    },
                                    onDragCancel = {
                                        dragFromIndex = null
                                        dragToIndex = null
                                        draggingFolders = null
                                    }
                                )
                            }
                            .then(if (isDragging) Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp)) else Modifier)
                    )
                }
            }

            // Drag pill handle — above dock, pure Pixel style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            // Bottom dock — frosted pill (uzun bas → düzenle)
            PixelDock(
                packages = dockPackages,
                onLaunchApp = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dockEditOpen = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // All Apps Drawer overlay
        AnimatedVisibility(
            visible = allAppsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AllAppsDrawer(
                apps = filteredApps,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onAppClick = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
                },
                onAppLongClick = { app ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    contextMenuApp = app
                },
                onClose = viewModel::closeAllApps
            )
        }
    }

    // Dock düzenleme sheet
    if (dockEditOpen) {
        DockEditSheet(
            allApps = allApps,
            dockPackages = dockPackages,
            onAdd = { viewModel.addToDock(context, it) },
            onRemove = { viewModel.removeFromDock(context, it) },
            onDismiss = { dockEditOpen = false }
        )
    }

    // App context menu (long press)
    contextMenuApp?.let { app ->
        AppContextMenu(
            app = app,
            isDocked = app.packageName in dockPackages,
            onDismiss = { contextMenuApp = null },
            onLaunch = { viewModel.launchApp(context, app.packageName) },
            onAddToDock = { viewModel.addToDock(context, app.packageName) },
            onRemoveFromDock = { viewModel.removeFromDock(context, app.packageName) },
            onChangeCategory = {
                categoryPickerApp = app
                contextMenuApp = null
            },
            onHideApp = { hidden ->
                viewModel.setAppHidden(app.packageName, hidden)
                contextMenuApp = null
            }
        )
    }

    // Kategori picker sheet
    categoryPickerApp?.let { app ->
        CategoryPickerSheet(
            app = app,
            onDismiss = { categoryPickerApp = null },
            onCategorySelected = { catId ->
                viewModel.updateAppCategory(app.packageName, catId)
            }
        )
    }

    // Folder bottom sheet
    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = viewModel::closeFolder,
            onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
            onAppLongClick = { app ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                contextMenuApp = app
            }
        )
    }
}

@Composable
private fun PixelClockWidget(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, d MMMM", Locale("tr")) }
    var now by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time — 72sp Thin weight, pure white, soft shadow
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
            textAlign = TextAlign.Center,
            // Soft drop shadow via modifier is not available directly; shadow() is for elevation.
            // We layer a blurred copy via alpha trick instead — keep it simple and readable.
        )
        Spacer(modifier = Modifier.height(2.dp))
        // Date — 16sp white 85% alpha
        Text(
            text = dateFormat.format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            letterSpacing = 0.sp
        )
    }
}

/** Frosted pill dock — packages listesi DockPrefs'ten gelir, kullanıcı tarafından seçilebilir. */
@Composable
private fun PixelDock(
    packages: List<String>,
    onLaunchApp: (String) -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val visiblePkgs = remember(packages) {
        packages.filter { pm.getLaunchIntentForPackage(it) != null }
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visiblePkgs.forEach { pkg ->
                val label = remember(pkg) {
                    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                }
                DockIcon(
                    packageName = pkg,
                    label = label,
                    iconSize = 48.dp,
                    onClick = { onLaunchApp(pkg) }
                )
            }
        }
    }
}

@Composable
private fun DockIcon(
    packageName: String,
    label: String,
    iconSize: Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { iconSize.roundToPx() }
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(px, px).asImageBitmap()
        }.getOrNull()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            )
        }
    }
}
