package com.armutlu.apporganizer.presentation.ui.launcher

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.service.AppNotificationListenerService
import com.armutlu.apporganizer.utils.AppAnalytics
import com.armutlu.apporganizer.utils.AppPrefs
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun FolderScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val folder by viewModel.openFolder.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var folderCarouselEnabled by remember { mutableStateOf(AppPrefs.isFolderCarouselEnabled(context)) }
    var folderCarouselPosition by remember { mutableStateOf(AppPrefs.getFolderCarouselPosition(context)) }
    var folderSearchEnabled by remember { mutableStateOf(AppPrefs.isFolderSearchEnabled(context)) }
    var folderTransitionEffect by remember { mutableStateOf(AppPrefs.getFolderTransitionEffect(context)) }
    var folderNavigatorMutedUntil by remember { mutableStateOf(AppPrefs.getFolderNavigatorMutedUntil(context)) }
    // Görev 1: HomeScreen ile aynı kök zemin — klasörden çıkarken duvar kağıdı flaşı olmasın.
    var bgType by remember { mutableStateOf(AppPrefs.getBgType(context)) }
    var bgColorInt by remember { mutableStateOf(AppPrefs.getBgColor(context)) }
    var bgGradientStyle by remember { mutableStateOf(AppPrefs.getHomeBackgroundStyle(context)) }

    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppPrefs.KEY_FOLDER_CAROUSEL_ENABLED) {
                folderCarouselEnabled = AppPrefs.isFolderCarouselEnabled(context)
            }
            if (key == AppPrefs.KEY_FOLDER_CAROUSEL_POSITION) {
                folderCarouselPosition = AppPrefs.getFolderCarouselPosition(context)
            }
            if (key == AppPrefs.KEY_FOLDER_SEARCH_ENABLED) {
                folderSearchEnabled = AppPrefs.isFolderSearchEnabled(context)
            }
            if (key == AppPrefs.KEY_FOLDER_TRANSITION_EFFECT) {
                folderTransitionEffect = AppPrefs.getFolderTransitionEffect(context)
            }
            if (key == AppPrefs.KEY_FOLDER_NAVIGATOR_MUTED_UNTIL) {
                folderNavigatorMutedUntil = AppPrefs.getFolderNavigatorMutedUntil(context)
            }
            if (key == AppPrefs.KEY_BG_TYPE) {
                bgType = AppPrefs.getBgType(context)
            }
            if (key == AppPrefs.KEY_BG_COLOR) {
                bgColorInt = AppPrefs.getBgColor(context)
            }
            if (key == AppPrefs.KEY_HOME_BACKGROUND_STYLE) {
                bgGradientStyle = AppPrefs.getHomeBackgroundStyle(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    BackHandler { onBack() }

    AnimatedVisibility(
        visible = folder != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val f = folder ?: return@AnimatedVisibility

        val primary = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface
        val textSecondary = onSurface.copy(alpha = 0.55f)
        val dividerColor = onSurface.copy(alpha = 0.08f)
        val surface = MaterialTheme.colorScheme.surface

        var sortMode by remember {
            val saved = AppPrefs.getFolderSortMode(context)
            mutableStateOf(AllAppsSortMode.entries.firstOrNull { it.name == saved } ?: AllAppsSortMode.ALPHA)
        }
        var searchQuery by remember { mutableStateOf("") }
        val catId = f.category.categoryId
        var customName by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomNames(context)[catId] ?: "")
        }
        var customEmoji by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomEmojis(context)[catId] ?: "")
        }
        var customColor by remember(catId) {
            mutableStateOf(AppPrefs.getFolderCustomColors(context)[catId] ?: "")
        }
        // Reaktivite (Fix 2a, LEARNINGS E6 pattern): customName/Emoji/Color eskiden sadece
        // remember(catId) ile bir kez okunuyordu — FolderRenameDialog dışında (örn. HomeScreen'de
        // düzenleme sonrası) değişirse FolderScreen açıkken güncellenmiyordu.
        DisposableEffect(context, catId) {
            val prefs = context.getSharedPreferences(AppPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                when (key) {
                    AppPrefs.KEY_FOLDER_CUSTOM_NAMES ->
                        customName = AppPrefs.getFolderCustomNames(context)[catId] ?: ""
                    AppPrefs.KEY_FOLDER_CUSTOM_EMOJIS ->
                        customEmoji = AppPrefs.getFolderCustomEmojis(context)[catId] ?: ""
                    AppPrefs.KEY_FOLDER_CUSTOM_COLORS ->
                        customColor = AppPrefs.getFolderCustomColors(context)[catId] ?: ""
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        var showEditDialog by remember { mutableStateOf(false) }
        var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }
        // Ekran köküne alındı (P0.1 fix) — eskiden contextMenuApp?.let{} bloğunun İÇİNDE
        // tanımlıydı: "Kategori Değiştir"e basınca contextMenuApp = null olduğu an bu blok
        // yok oluyor, categoryPickerApp state'i de sıfırlanıyor, picker hiç açılmıyordu.
        // HomeScreen.kt (satır 293) ile aynı pattern: state ekran seviyesinde, iki sheet
        // birbirinden bağımsız kardeş composable olarak render edilir.
        var categoryPickerApp by remember { mutableStateOf<AppInfo?>(null) }
        val scope = rememberCoroutineScope()
        val folderSwipeThresholdPx = with(density) { 96.dp.toPx() }
        val folderTransitionOffsetPx = with(density) { 86.dp.toPx() }
        val folderVelocityThresholdPxPerSecond = with(density) { 700.dp.toPx() }
        val transitionMode = remember(folderTransitionEffect) {
            resolveFolderTransitionMode(folderTransitionEffect)
        }
        val reduceMotionEnabled = remember { !ValueAnimator.areAnimatorsEnabled() }
        val contentOffset = remember { Animatable(0f) }
        var transitionDirection by remember { mutableIntStateOf(1) }
        var isSettlingTransition by remember { mutableStateOf(false) }

        fun settleSpec() = tween<Float>(
            durationMillis = when {
                reduceMotionEnabled -> 120
                transitionMode == FolderTransitionMode.IOS_ZOOM_FADE -> 260
                else -> 240
            },
            easing = FastOutSlowInEasing,
        )

        suspend fun animateToCenter() {
            contentOffset.animateTo(targetValue = 0f, animationSpec = settleSpec())
        }

        suspend fun performCommittedFolderTransition(direction: Int) {
            transitionDirection = direction
            contentOffset.animateTo(
                targetValue = direction * folderTransitionOffsetPx,
                animationSpec = settleSpec(),
            )
            val moved = viewModel.openAdjacentFolder(
                next = mapFolderTransitionDirectionToNextFlag(direction)
            )
            if (!moved) {
                contentOffset.snapTo(0f)
                return
            }
            searchQuery = ""
            contentOffset.snapTo(-direction * folderTransitionOffsetPx)
            animateToCenter()
        }

        fun settleFolderTransition(
            dragOffsetPx: Float,
            velocityPxPerSecond: Float,
            forcedDirection: Int? = null,
        ) {
            if (isSettlingTransition) return
            scope.launch {
                isSettlingTransition = true
                try {
                    val commitDirection = forcedDirection ?: computeFolderSettleTarget(
                        dragOffsetPx = dragOffsetPx,
                        velocityPxPerSecond = velocityPxPerSecond,
                        settleDistancePx = folderSwipeThresholdPx,
                        velocityThresholdPxPerSecond = folderVelocityThresholdPxPerSecond,
                    ).commitDirection
                    if (commitDirection == 0) {
                        animateToCenter()
                    } else {
                        performCommittedFolderTransition(commitDirection)
                    }
                } finally {
                    isSettlingTransition = false
                }
            }
        }

        val folderIndex = remember(folders, catId) {
            folders.indexOfFirst { it.category.categoryId == catId }
        }
        val previousFolder = remember(folders, folderIndex, folderCarouselEnabled) {
            if (folderCarouselEnabled && folderIndex >= 0 && folders.size > 1) {
                folders[(folderIndex - 1 + folders.size) % folders.size]
            } else {
                null
            }
        }
        val nextFolder = remember(folders, folderIndex, folderCarouselEnabled) {
            if (folderCarouselEnabled && folderIndex >= 0 && folders.size > 1) {
                folders[(folderIndex + 1) % folders.size]
            } else {
                null
            }
        }
        val carouselPosition = when (folderCarouselPosition) {
            AppPrefs.FOLDER_CAROUSEL_POS_TOP,
            AppPrefs.FOLDER_CAROUSEL_POS_MIDDLE,
            AppPrefs.FOLDER_CAROUSEL_POS_BOTTOM -> folderCarouselPosition
            else -> AppPrefs.FOLDER_CAROUSEL_POS_BOTTOM
        }
        val folderNavigatorMuted = folderNavigatorMutedUntil > System.currentTimeMillis()
        val showFolderNavigator = previousFolder != null && nextFolder != null && !folderNavigatorMuted

        val catColor = remember(f.category.colorHex, customColor) {
            val hex = customColor.ifBlank { null } ?: f.category.colorHex
            runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(primary)
        }

        val trLocale = java.util.Locale("tr")
        val sortedApps = remember(f.apps, sortMode, searchQuery) {
            val base = if (searchQuery.isBlank()) f.apps
            else {
                val q = searchQuery.lowercase(trLocale)
                f.apps.filter { it.appName.lowercase(trLocale).contains(q) }
            }
            base.sortedByMode(sortMode)
        }

        val badgeCounts by AppNotificationListenerService.badgeCounts.collectAsState()
        val latestTexts by AppNotificationListenerService.latestTexts.collectAsState()
        val appsWithNotifs = remember(f.apps, badgeCounts) {
            f.apps.filter { (badgeCounts[it.packageName] ?: 0) > 0 }
        }
        val displayApps = remember(sortedApps, badgeCounts, latestTexts) {
            sortedApps.withLiveNotificationState(
                badgeCounts = badgeCounts,
                latestTexts = latestTexts,
            )
        }

        val transitionFrame = remember(
            transitionMode,
            contentOffset.value,
            folderTransitionOffsetPx,
            reduceMotionEnabled,
            folderCarouselEnabled,
        ) {
            buildFolderTransitionFrame(
                mode = transitionMode,
                rawOffsetPx = if (folderCarouselEnabled) contentOffset.value else 0f,
                settleDistancePx = folderTransitionOffsetPx,
                reduceMotionEnabled = reduceMotionEnabled,
            )
        }
        val draggableState = rememberDraggableState { dragDelta ->
            if (isSettlingTransition) return@rememberDraggableState
            scope.launch {
                contentOffset.stop()
                contentOffset.snapTo(
                    (contentOffset.value + dragDelta).coerceIn(
                        -folderTransitionOffsetPx,
                        folderTransitionOffsetPx,
                    )
                )
            }
        }
        fun navigateAdjacentFolder(next: Boolean) {
            if (!folderCarouselEnabled || isSettlingTransition) return
            settleFolderTransition(
                dragOffsetPx = contentOffset.value,
                velocityPxPerSecond = 0f,
                forcedDirection = if (next) -1 else 1,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (folderCarouselEnabled && folders.size > 1) {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = draggableState,
                            enabled = !isSettlingTransition,
                            onDragStopped = { velocity ->
                                val commitDirection = shouldCommitFolderTransition(
                                    dragOffsetPx = contentOffset.value,
                                    velocityPxPerSecond = velocity,
                                    settleDistancePx = folderSwipeThresholdPx,
                                    velocityThresholdPxPerSecond = folderVelocityThresholdPxPerSecond,
                                )
                                if (commitDirection != 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                settleFolderTransition(
                                    dragOffsetPx = contentOffset.value,
                                    velocityPxPerSecond = velocity,
                                )
                            },
                        )
                    } else {
                        Modifier
                    }
                )
                .homeRootBackground(bgType, bgColorInt, bgGradientStyle)
                .then(
                    // "Duvar Kağıdı" seçiliyken homeRootBackground transparan bırakır; klasör
                    // içeriğinin okunabilirliği için hafif karartma katmanı eklenir (surface
                    // rengiyle değil, tema-nötr siyah yarı saydam ile — duvar kağıdı hâlâ sızar).
                    if (bgType == "wallpaper") Modifier.background(Color.Black.copy(alpha = 0.35f))
                    else Modifier
                )
        ) {
            if (showFolderNavigator && folderCarouselEnabled && transitionFrame.direction != 0) {
                FolderTransitionPreview(
                    previousFolder = previousFolder!!,
                    nextFolder = nextFolder!!,
                    frame = transitionFrame,
                    context = context,
                    onSurface = onSurface,
                    accent = catColor,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .graphicsLayer {
                        translationX = transitionFrame.translationX
                        alpha = transitionFrame.currentAlpha
                        scaleX = transitionFrame.currentScale
                        scaleY = transitionFrame.currentScale
                        rotationY = transitionFrame.currentRotationY
                    }
            ) {
                // Üst bar — geri + klasör başlık + düzenle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = onSurface,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(catColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = customEmoji.ifBlank { f.category.iconEmoji },
                            fontSize = 22.sp,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customName.ifBlank { f.category.categoryName },
                            color = onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${f.apps.size} uygulama",
                            color = textSecondary,
                            fontSize = 13.sp,
                        )
                        if (showFolderNavigator && carouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_TOP) {
                            Spacer(modifier = Modifier.height(7.dp))
                            FolderIndexNavigator(
                                previousFolder = previousFolder!!,
                                nextFolder = nextFolder!!,
                                context = context,
                                onSurface = onSurface,
                                accent = catColor,
                                onPrevious = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navigateAdjacentFolder(next = false)
                                },
                                onNext = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navigateAdjacentFolder(next = true)
                                },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(onSurface.copy(alpha = 0.08f))
                            .clickable { showEditDialog = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.folder_edit),
                            tint = onSurface.copy(0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Arama çubuğu — varsayılan KAPALI, Ayarlar > Klasör İçi Arama ile açılır
                if (folderSearchEnabled) Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(onSurface.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "${customName.ifBlank { f.category.categoryName }} içinde ara...",
                                color = textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            cursorBrush = SolidColor(primary),
                            textStyle = TextStyle(color = onSurface, fontSize = 13.sp),
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            "Aramayı temizle",
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp).clickable { searchQuery = "" },
                        )
                    }
                }

                // Bildirim bandı
                if (appsWithNotifs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(appsWithNotifs, key = { it.packageName }) { app ->
                            val count = badgeCounts[app.packageName] ?: 0
                            val text = latestTexts[app.packageName] ?: ""
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(onSurface.copy(alpha = 0.10f))
                                    .semantics {
                                        contentDescription = "${app.appName}, $count bildirim"
                                        onClick(label = "Uygulamayı aç") {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            AppAnalytics.appLaunched("folder_screen")
                                            viewModel.launchApp(context, app.packageName)
                                            true
                                        }
                                    }
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        AppAnalytics.appLaunched("folder_screen")
                                        viewModel.launchApp(context, app.packageName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "$count",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Column {
                                        Text(app.appName, color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (text.isNotEmpty()) {
                                            Text(text, color = textSecondary, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))
                }

                // Sıralama chip'leri — her kriter (A-Z, Kullanım, Boyut, Yükleme) TEK buton;
                // aktif kriterin butonuna tekrar basınca yön (asc/desc) değişir (D210 — çift buton kaldırıldı)
                val sortBaseModes = remember {
                    listOf(AllAppsSortMode.SMART, AllAppsSortMode.ALPHA, AllAppsSortMode.USAGE, AllAppsSortMode.SIZE_DESC, AllAppsSortMode.INSTALL_DATE)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                ) {
                    itemsIndexed(sortBaseModes, key = { _, mode -> mode.name }) { _, baseMode ->
                        val active = sortMode.baseMode() == baseMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) primary else onSurface.copy(alpha = 0.12f))
                                .clickable {
                                    sortMode = if (active) sortMode.opposite() else baseMode
                                    AppPrefs.setFolderSortMode(context, sortMode.name)
                                }
                                .padding(horizontal = 11.dp, vertical = 5.dp),
                        ) {
                            Text(
                                if (active) sortMode.label else baseMode.label,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else textSecondary,
                            )
                        }
                    }
                }

                Spacer(Modifier.fillMaxWidth().height(1.dp).background(dividerColor))

                // Uygulama grid'i
                if (sortedApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (searchQuery.isNotEmpty()) "\"$searchQuery\" bulunamadı" else "Bu klasör boş",
                            color = textSecondary,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 76.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        items(displayApps, key = { it.packageName }) { app ->
                            AppIconView(
                                app = app,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    AppAnalytics.appLaunched("folder_screen")
                                    viewModel.launchApp(context, app.packageName)
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    contextMenuApp = app
                                },
                                iconSize = 56.dp,
                                showLabel = true,
                            )
                        }
                    }
                }

                if (showFolderNavigator && carouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_BOTTOM) {
                    FolderIndexNavigator(
                        previousFolder = previousFolder!!,
                        nextFolder = nextFolder!!,
                        context = context,
                        onSurface = onSurface,
                        accent = catColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        onPrevious = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navigateAdjacentFolder(next = false)
                        },
                        onNext = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navigateAdjacentFolder(next = true)
                        },
                    )
                }
            }

            // Klasör düzenleme dialog'u
            if (showFolderNavigator && carouselPosition == AppPrefs.FOLDER_CAROUSEL_POS_MIDDLE) {
                // Aktif sürükleme/geçiş animasyonu sırasında (offset sıfırdan uzaklaşınca)
                // bu navigatör solmalı — FolderPageTurnPeek 3D efektiyle çakışmasın (kullanıcı
                // geri bildirimi: ortada beliren istenmeyen "sonraki klasör" butonu).
                val navAlphaTarget = 1f - transitionFrame.progress
                val navAlpha by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = navAlphaTarget,
                    label = "folderIndexNavigatorAlpha",
                )
                FolderIndexNavigator(
                    previousFolder = previousFolder!!,
                    nextFolder = nextFolder!!,
                    context = context,
                    onSurface = onSurface,
                    accent = catColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 18.dp)
                        .graphicsLayer { alpha = navAlpha },
                    onPrevious = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigateAdjacentFolder(next = false)
                    },
                    onNext = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navigateAdjacentFolder(next = true)
                    },
                )
            }

            if (showEditDialog) {
                // Dongu G6 — Yildiz Ekonomisi kozmetik acilim kilit kontrolu icin toplam ⭐.
                // homeMissionSummary zaten HomeIntelligenceCoordinator uzerinden reaktif akiyor
                // (M07), burada ek bir kaynak/DI eklenmedi.
                val missionSummaryForEdit by viewModel.homeMissionSummary.collectAsState()
                FolderRenameDialog(
                    currentName = customName.ifBlank { f.category.categoryName },
                    currentEmoji = customEmoji.ifBlank { f.category.iconEmoji },
                    currentColor = customColor,
                    totalStars = missionSummaryForEdit?.totalStars ?: 0,
                    onDismiss = { showEditDialog = false },
                    onSave = { newName, newEmoji, newColor ->
                        val nameToSave = if (newName == f.category.categoryName) "" else newName
                        val emojiToSave = if (newEmoji == f.category.iconEmoji) "" else newEmoji
                        customName = nameToSave
                        customEmoji = emojiToSave
                        customColor = newColor
                        AppPrefs.setFolderCustomName(context, catId, nameToSave)
                        AppPrefs.setFolderCustomEmoji(context, catId, emojiToSave)
                        AppPrefs.setFolderCustomColor(context, catId, newColor)
                        // Dongu G3a — DAILY_CUSTOMIZE_FOLDER gorevi sinyali: emoji veya renk
                        // fiilen degistiyse (bos string'e donus/varsayilana sifirlama SAYILMAZ)
                        // gunde bir kez kaydedilir (TaskScoreManager.record kendi ici gunluk
                        // tekillik korumasi tasir).
                        if (emojiToSave.isNotBlank() || newColor.isNotBlank()) {
                            scope.launch {
                                com.armutlu.apporganizer.utils.TaskScoreManager.record(
                                    context,
                                    com.armutlu.apporganizer.utils.TaskScoreManager.EventType.FolderCustomized,
                                )
                            }
                        }
                        showEditDialog = false
                    },
                )
            }

            // Uygulama context menüsü (uzun bas)
            contextMenuApp?.let { app ->
                val dockPackages by viewModel.dockPackages.collectAsState()

                AppContextMenu(
                    app = app,
                    isFavorite = false,
                    isDocked = app.packageName in dockPackages,
                    onDismiss = { contextMenuApp = null },
                    onLaunch = {
                        viewModel.launchApp(context, app.packageName)
                        contextMenuApp = null
                    },
                    onAddToDock = {
                        viewModel.addToDock(context, app.packageName)
                        contextMenuApp = null
                    },
                    onRemoveFromDock = {
                        viewModel.removeFromDock(context, app.packageName)
                        contextMenuApp = null
                    },
                    onChangeCategory = {
                        // Önce picker state'ine yaz, SONRA menüyü kapat (D268 sıralama —
                        // AppContextMenu.kt'de onClick = { onChangeCategory(); onDismiss() }
                        // olduğu için burası zaten app'i categoryPickerApp'e atayıp
                        // contextMenuApp'i null'lıyor; ancak categoryPickerApp artık ekran
                        // kökünde olduğu için contextMenuApp null olsa da picker render kalır).
                        categoryPickerApp = app
                        contextMenuApp = null
                    },
                    onHideApp = { hidden ->
                        viewModel.setAppHidden(app.packageName, hidden)
                        contextMenuApp = null
                    },
                    onSaveNote = { note ->
                        viewModel.saveAppNote(app.packageName, note)
                        contextMenuApp = null
                    },
                    onToggleFavorite = { _ ->
                        viewModel.toggleFavorite(context, app.packageName)
                        contextMenuApp = null
                    },
                )
            }

            // Kategori seçici — contextMenuApp?.let{} bloğunun DIŞINDA, kardeş composable
            // olarak render edilir (P0.1 fix). Böylece contextMenuApp null olduktan sonra da
            // categoryPickerApp dolu olduğu sürece sheet ekranda kalır.
            categoryPickerApp?.let { pickerApp ->
                val categories by viewModel.categories.collectAsState()
                CategoryPickerSheet(
                    app = pickerApp,
                    categories = categories,
                    onDismiss = { categoryPickerApp = null },
                    onCategorySelected = { catIdSelected ->
                        viewModel.updateAppCategory(pickerApp.packageName, catIdSelected)
                        categoryPickerApp = null
                    },
                )
            }
        }
    }
}

// "Kaydırma / Parallax" efekti: gelen komşu klasör düz bir kenar şeridi olarak, sürükleme
// ilerledikçe ekrana doğru kayarak (translationX) ve hafifçe belirginleşerek (alpha) görünür —
// 3D döndürme yok, sade bir yatay kaydırma hissi (page_turn'e alternatif, D262).
@Composable
private fun FolderSlideParallaxPeek(
    previousFolder: AppFolder,
    nextFolder: AppFolder,
    transitionDirection: Int,
    offsetValue: Float,
    offsetMax: Float,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
) {
    val progress = (abs(offsetValue) / offsetMax.coerceAtLeast(1f)).coerceIn(0f, 1f)
    if (progress <= 0.01f) return

    val showStart = transitionDirection > 0
    val previewFolder = if (showStart) previousFolder else nextFolder
    Box(modifier = Modifier.fillMaxSize()) {
        FolderPageEdgeStrip(
            folder = previewFolder,
            startEdge = showStart,
            context = context,
            onSurface = onSurface,
            accent = accent,
            modifier = Modifier
                .align(if (showStart) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {
                    // Düz kayma: komşu önizleme sabit konumdan sürükleme yönünde hafifçe içeri girer
                    translationX = if (showStart) (1f - progress) * -24f else (1f - progress) * 24f
                    alpha = (0.35f + progress * 0.65f).coerceIn(0f, 1f)
                },
        )
    }
}

// "Defter yaprağı çevirme" efekti: gelen komşu klasör 3D eksende (rotationY + cameraDistance)
// döndürülerek, hafifçe ölçeklenerek ve kayan kenarında koyulaşan bir gölge gradyanıyla
// beliriyor — Compose'da tam fiziksel bir page-curl simülasyonu yerine performanslı,
// GPU hızlandırmalı (graphicsLayer, Canvas değil) inandırıcı bir 3D flip illüzyonu.
@Composable
private fun FolderPageTurnPeek(
    previousFolder: AppFolder,
    nextFolder: AppFolder,
    transitionDirection: Int,
    offsetValue: Float,
    offsetMax: Float,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
) {
    val progress = (abs(offsetValue) / offsetMax.coerceAtLeast(1f)).coerceIn(0f, 1f)
    if (progress <= 0.01f) return

    val showStart = transitionDirection > 0
    val previewFolder = if (showStart) previousFolder else nextFolder
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize()) {
        FolderPageEdgeStrip(
            folder = previewFolder,
            startEdge = showStart,
            context = context,
            onSurface = onSurface,
            accent = accent,
            modifier = Modifier
                .align(if (showStart) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {
                    cameraDistance = 10f * density.density
                    // Yeni sayfa, menteşesi ekranın dışında kalan taraftan hafifçe döner —
                    // ilerleme arttıkça düzleşerek tam görünüme gelir (0° = tam düz).
                    rotationY = if (showStart) (1f - progress) * -22f else (1f - progress) * 22f
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                        pivotFractionX = if (showStart) 0f else 1f,
                        pivotFractionY = 0.5f,
                    )
                    val scale = 0.92f + progress * 0.08f
                    scaleX = scale
                    scaleY = scale
                    alpha = (0.35f + progress * 0.65f).coerceIn(0f, 1f)
                },
        )
    }
}

@Composable
private fun FolderTransitionPreview(
    previousFolder: AppFolder,
    nextFolder: AppFolder,
    frame: FolderTransitionFrame,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
) {
    val showStart = frame.direction > 0
    val previewFolder = if (showStart) previousFolder else nextFolder
    Box(modifier = Modifier.fillMaxSize()) {
        FolderPageEdgeStrip(
            folder = previewFolder,
            startEdge = showStart,
            context = context,
            onSurface = onSurface,
            accent = accent,
            modifier = Modifier
                .align(if (showStart) Alignment.CenterStart else Alignment.CenterEnd)
                .graphicsLayer {
                    translationX = frame.previewTranslationX
                    alpha = frame.previewAlpha
                    scaleX = frame.previewScale
                    scaleY = frame.previewScale
                },
        )
    }
}

@Composable
private fun FolderPageEdgeStrip(
    folder: AppFolder,
    startEdge: Boolean,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val catId = folder.category.categoryId
    val customName = AppPrefs.getFolderCustomNames(context)[catId].orEmpty()
    val customEmoji = AppPrefs.getFolderCustomEmojis(context)[catId].orEmpty()
    val title = customName.ifBlank { folder.category.categoryName }
    val shadowColor = androidx.compose.ui.graphics.Color.Black
    // Kağıt kenarı hissi: sayfanın kaydığı tarafta hafif koyulaşan gradyan gölge.
    val edgeShadowBrush = remember(startEdge) {
        if (startEdge) {
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(shadowColor.copy(alpha = 0.28f), androidx.compose.ui.graphics.Color.Transparent),
            )
        } else {
            androidx.compose.ui.graphics.Brush.horizontalGradient(
                listOf(androidx.compose.ui.graphics.Color.Transparent, shadowColor.copy(alpha = 0.28f)),
            )
        }
    }
    Box(
        modifier = modifier
            .width(96.dp)
            .height(184.dp)
            .clip(
                if (startEdge) {
                    RoundedCornerShape(topStart = 0.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 0.dp)
                } else {
                    RoundedCornerShape(topStart = 22.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 22.dp)
                },
            )
            .background(accent.copy(alpha = 0.20f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(customEmoji.ifBlank { folder.category.iconEmoji }, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = onSurface.copy(alpha = 0.75f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        // "Ciltli kağıt kenarı" — sayfanın döndüğü tarafa koyulaşan ince gölge şeridi.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(18.dp)
                .align(if (startEdge) Alignment.CenterStart else Alignment.CenterEnd)
                .background(edgeShadowBrush),
        )
    }
}

internal fun List<AppInfo>.withLiveNotificationState(
    badgeCounts: Map<String, Int>,
    latestTexts: Map<String, String>,
): List<AppInfo> = map { app ->
    val liveCount = badgeCounts[app.packageName]
    val liveText = latestTexts[app.packageName]
    if (liveCount == null && liveText == null) {
        app
    } else {
        app.copy(
            notificationCount = liveCount ?: app.notificationCount,
            notificationText = liveText ?: app.notificationText,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderIndexNavigator(
    previousFolder: AppFolder,
    nextFolder: AppFolder,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderTopNavChip(
                folder = previousFolder,
                direction = "<",
                modifier = Modifier.weight(1f),
                context = context,
                onSurface = onSurface,
                accent = accent,
                onClick = onPrevious,
            )
            FolderTopNavChip(
                folder = nextFolder,
                direction = ">",
                modifier = Modifier.weight(1f),
                context = context,
                onSurface = onSurface,
                accent = accent,
                onClick = onNext,
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            listOf(
                "Sessize Al (1 gun)" to 24L * 60 * 60 * 1000,
                "Sessize Al (1 hafta)" to 7L * 24 * 60 * 60 * 1000,
            ).forEach { (label, duration) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        menuOpen = false
                        AppPrefs.setFolderNavigatorMutedUntil(context, System.currentTimeMillis() + duration)
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Kapat") },
                onClick = {
                    menuOpen = false
                    AppPrefs.setFolderCarouselEnabled(context, false)
                }
            )
        }
    }
}

@Composable
private fun FolderTopNavChip(
    folder: AppFolder,
    direction: String,
    modifier: Modifier = Modifier,
    context: android.content.Context,
    onSurface: Color,
    accent: Color,
    onClick: () -> Unit,
) {
    val catId = folder.category.categoryId
    val customName = AppPrefs.getFolderCustomNames(context)[catId].orEmpty()
    val customEmoji = AppPrefs.getFolderCustomEmojis(context)[catId].orEmpty()
    val customColor = AppPrefs.getFolderCustomColors(context)[catId].orEmpty()
    val color = remember(folder.category.colorHex, customColor) {
        val hex = customColor.ifBlank { folder.category.colorHex }
        runCatching { Color(android.graphics.Color.parseColor(hex)) }
            .getOrDefault(onSurface.copy(alpha = 0.35f))
    }
    val title = customName.ifBlank { folder.category.categoryName }

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(accent.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (direction == "<") {
            Text(
                text = direction,
                color = onSurface.copy(alpha = 0.62f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(customEmoji.ifBlank { folder.category.iconEmoji }, fontSize = 12.sp)
        }
        Text(
            text = title,
            color = onSurface.copy(alpha = 0.86f),
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (direction == ">") {
            Text(
                text = direction,
                color = onSurface.copy(alpha = 0.62f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
