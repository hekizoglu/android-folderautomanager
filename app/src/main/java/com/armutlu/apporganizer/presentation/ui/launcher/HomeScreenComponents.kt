package com.armutlu.apporganizer.presentation.ui.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.utils.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PixelClockWidget(modifier: Modifier = Modifier) {
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
        Text(
            text = timeFormat.format(now),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-2).sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
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

/** Pixel Launcher stili Google arama cubugu — tikklayinca Google web aramasi acar. */
@Composable
internal fun GoogleSearchBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .height(50.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(25.dp)
            )
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(intent) }
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("G", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("o", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("o", color = Color(0xFFFBBC04), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("g", color = Color(0xFF4285F4), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("l", color = Color(0xFF34A853), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("e", color = Color(0xFFEA4335), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Ara veya URL gir",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Sesli ara",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier
                .size(22.dp)
                .clickable {
                    val voiceIntent = Intent("android.speech.action.WEB_SEARCH").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(voiceIntent) }
                }
        )
    }
}

/** Frosted pill dock — packages listesi DockPrefs'ten gelir, kullanici tarafindan secilebilir. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PixelDock(
    packages: List<String>,
    onLaunchApp: (String) -> Unit,
    onLongPress: () -> Unit = {},
    onAppLongPress: (String) -> Unit = {},
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
                    onClick = { onLaunchApp(pkg) },
                    onLongClick = { onAppLongPress(pkg) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DockIcon(
    packageName: String,
    label: String,
    iconSize: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { iconSize.roundToPx() }
    val iconPackPkg = remember { AppPrefs.getIconPack(context) }
    val cacheKey = if (iconPackPkg.isEmpty()) "${packageName}_$px" else "${packageName}_${px}_$iconPackPkg"
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCacheInternal[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, packageName, px)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: context.packageManager.getApplicationIcon(packageName).toBitmap(px, px).asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp)
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = label,
                modifier = Modifier.size(iconSize)
            )
        } ?: Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        )
    }
}

/** Pixel Launcher tarz sonKullanilan/onerileri satiri — 4 ikon yatay dizilir. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppSuggestionsRow(
    apps: List<AppInfo>,
    iconPackPkg: String = "",
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    if (apps.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Son Kullanilanlar",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                SuggestionAppItem(
                    app = app,
                    iconPackPkg = iconPackPkg,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestionAppItem(
    app: AppInfo,
    iconPackPkg: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val iconSize = 48.dp
    val px = with(LocalDensity.current) { iconSize.roundToPx() }
    val cacheKey = if (iconPackPkg.isEmpty()) "${app.packageName}_$px" else "${app.packageName}_${px}_$iconPackPkg"
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCacheInternal[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val packBitmap = if (iconPackPkg.isNotEmpty())
                        com.armutlu.apporganizer.utils.IconPackManager.loadIcon(context, iconPackPkg, app.packageName, px)
                    else null
                    packBitmap?.asImageBitmap()
                        ?: context.packageManager.getApplicationIcon(app.packageName).toBitmap(px, px).asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCacheInternal.put(cacheKey, loaded)
            value = loaded
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = app.appName,
                modifier = Modifier.size(iconSize)
            )
        } ?: Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.appName,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
internal fun SwipeHint(context: Context, visible: Boolean) {
    val swipeHintFeatureEnabled = remember { AppPrefs.isSwipeHintEnabled(context) }
    val showSwipeHint = remember { swipeHintFeatureEnabled && AppPrefs.shouldShowSwipeHint(context) }
    val infiniteTransition = rememberInfiniteTransition(label = "swipe_hint")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swipe_y"
    )
    LaunchedEffect(showSwipeHint) {
        if (showSwipeHint) {
            AppPrefs.incrementSwipeHintCount(context)
        }
    }
    if (showSwipeHint && visible) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(20.dp).offset(y = offsetY.dp)
            )
            Text(
                text = "Tum uygulamalar",
                color = Color.White.copy(alpha = 0.40f),
                fontSize = 11.sp
            )
        }
    }
}
