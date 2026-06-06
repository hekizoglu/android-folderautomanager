package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    val filteredAllApps by viewModel.allApps.collectAsState()

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    var swipeDelta by remember { mutableFloatStateOf(0f) }

    BackHandler(enabled = allAppsOpen || openFolder != null) {
        if (allAppsOpen) viewModel.closeAllApps()
        else viewModel.closeFolder()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ClockSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp)
            )

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openAllApps() }
                    .padding(bottom = 12.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "↑", color = Color.White.copy(alpha = 0.6f), fontSize = 18.sp)
                Text(text = "Tüm Uygulamalar", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }

        IconButton(
            onClick = { viewModel.openManager(context) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Yönetim Ekranı", tint = Color.White)
        }

        AnimatedVisibility(
            visible = allAppsOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            AllAppsDrawer(
                apps = filteredAllApps,
                onAppClick = { pkg -> viewModel.launchApp(context, pkg) },
                onClose = viewModel::closeAllApps
            )
        }
    }

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
            fontSize = 16.sp
        )
    }
}
