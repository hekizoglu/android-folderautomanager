package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
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
    val searchQuery by viewModel.searchQuery.collectAsState()

    val haptic = LocalHapticFeedback.current
    val dockPackages by viewModel.dockPackages.collectAsState()

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { viewModel.loadDockPackages(context) }

    BackHandler(enabled = allAppsOpen || openFolder != null) {
        if (allAppsOpen) viewModel.closeAllApps()
        else viewModel.closeFolder()
    }

    // Root box — fully transparent so wallpaper shows through
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(allAppsOpen) {
                if (!allAppsOpen) {
                    detectVerticalDragGestures(
                        onDragEnd = { swipeDelta = 0f },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            swipeDelta += dy
                            if (swipeDelta < -swipeThresholdPx) {
                                viewModel.openAllApps()
                                swipeDelta = 0f
                            }
                        }
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { viewModel.openManager(context) }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clock widget — top center, Pixel style
            PixelClockWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 8.dp)
            )

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
                items(folders, key = { it.category.categoryId }) { folder ->
                    FolderTile(
                        folder = folder,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.openFolder(folder)
                        }
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

            // Bottom dock — frosted pill
            PixelDock(
                packages = dockPackages,
                onLaunchApp = { pkg ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.launchApp(context, pkg)
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
                onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                onClose = viewModel::closeAllApps
            )
        }
    }

    // Folder bottom sheet
    openFolder?.let { folder ->
        FolderSheet(
            folder = folder,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            onDismiss = viewModel::closeFolder,
            onAppClick = { pkg -> viewModel.launchApp(context, pkg) }
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
