package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.armutlu.apporganizer.utils.BadgeColorEngine
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache get() = iconCacheInternal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderTile(
    folder: AppFolder,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSwipeUp: ((String) -> Unit)? = null,
    onNotificationTap: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    textAlpha: Float = 1f,
    folderSizeDp: Int = 72,
    labelColor: Color = Color.White,
    customName: String? = null,
    customEmoji: String? = null,
    customColor: String? = null,
    folderCountVisible: Boolean = true,
    folderSwipeHintEnabled: Boolean = true,
    notifTextEnabled: Boolean = false,
    unusedInfoEnabled: Boolean = false,
    folderShape: String = "circle",
    folderGlassEnabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var swipeDy by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(density) { 40.dp.toPx() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "folderTileScale"
    )

    val previewApps = folder.apps.take(4)
    val totalBadge  = folder.apps.sumOf { it.notificationCount }
    val topApp = remember(folder.apps) { folder.apps.maxByOrNull { it.usageCount } }
    val folderLabel = remember(folder, customName, topApp, totalBadge) {
        buildString {
            append(customName?.takeIf { it.isNotEmpty() } ?: folder.category.categoryName)
            append(", ${folder.apps.size} uygulama")
            if (totalBadge > 0) append(", $totalBadge bildirim")
            if (topApp != null) append(", yukarı kaydırınca ${topApp.appName} açılır")
        }
    }

    val tileWidth = folderSizeDp.dp
    val circleSize = (folderSizeDp * 5 / 6).dp  // 60/72 oranı korunuyor
    val miniIconSize = (folderSizeDp / 3).dp     // 22/72 yaklaşık

    Column(
        modifier = modifier
            .width(tileWidth)
            .scale(scale)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = folderLabel
            }
            .pointerInput(folder) {
                detectVerticalDragGestures(
                    onDragStart = { swipeDy = 0f },
                    onDragEnd = { swipeDy = 0f },
                    onDragCancel = { swipeDy = 0f },
                    onVerticalDrag = { change, delta ->
                        if (delta >= 0f) return@detectVerticalDragGestures
                        swipeDy += delta
                        if (swipeDy < -swipeThresholdPx) {
                            if (topApp != null && onSwipeUp != null) {
                                change.consume()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSwipeUp(topApp.packageName)
                            }
                            swipeDy = 0f
                        }
                    }
                )
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick?.invoke()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 60dp circle — kategori renginde frosted glass + badge
        val catColor = remember(folder.category.colorHex, customColor) {
            val hex = customColor?.takeIf { it.isNotEmpty() } ?: folder.category.colorHex
            runCatching { Color(android.graphics.Color.parseColor(hex)) }
                .getOrDefault(Color.White)
        }
        // Etiket metni klasör ikon dairesinin DIŞINDA, doğrudan duvar kağıdı/tema arka planının
        // üzerinde durur — bu yüzden customColor'a (dairenin rengi) göre değil, HomeScreen'den
        // gelen labelColor'a (gerçek arka plana göre hesaplanmış) göre renklendirilmeli.
        // ESKİ HATA (D210'da düzeltildi): açık customColor'lu klasörlerde metin neredeyse siyah
        // (0xFF212121) yapılıyordu ama koyu duvar kağıdında bu görünmez oluyordu — "klasör ismi
        // kayboluyor" bug'ı buradan kaynaklanıyordu.
        val effectiveLabelColor = labelColor
        val tileShape = when (folderShape) {
            "square"   -> RoundedCornerShape(0.dp)
            "rounded"  -> RoundedCornerShape(16.dp)
            "triangle" -> GenericShape { size, _ ->
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            else -> CircleShape // "circle"
        }
        Box {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(tileShape)
                .background(catColor.copy(alpha = 0.30f))
                .then(if (folderGlassEnabled) Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), tileShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (folder.apps.isEmpty()) {
                Text(
                    text = customEmoji?.takeIf { it.isNotEmpty() } ?: folder.category.iconEmoji,
                    fontSize = 24.sp
                )
            } else {
                // 2x2 mini icon grid
                MiniIconGrid(
                    apps = previewApps,
                    iconSize = miniIconSize
                )
            }
        }
        // Klasör badge — renk: en yüksek bildirimli uygulamanın kategorisine göre
        if (totalBadge > 0) {
            val badgeText = if (totalBadge > 99) "99+" else totalBadge.toString()
            val badgeW = if (totalBadge > 9) 20.dp else 16.dp
            val topNotifApp = folder.apps.maxByOrNull { it.notificationCount }
            val badgeIntelligence = com.armutlu.apporganizer.utils.AppPrefs.isBadgeIntelligenceEnabled(context)
            val folderBadgeColor = if (badgeIntelligence && topNotifApp != null)
                BadgeColorEngine.badgeColor(topNotifApp.categoryId, topNotifApp.packageName)
            else
                BadgeColorEngine.Red
            Box(
                modifier = Modifier
                    .size(badgeW, 16.dp)
                    .align(Alignment.TopEnd)
                    .shadow(3.dp, androidx.compose.foundation.shape.RoundedCornerShape(8.dp), ambientColor = folderBadgeColor, spotColor = folderBadgeColor)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(folderBadgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    badgeText,
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
        } // outer Box

        Spacer(Modifier.height(4.dp))

        Text(
            text = customName?.takeIf { it.isNotEmpty() } ?: folder.category.categoryName,
            color = effectiveLabelColor.copy(alpha = textAlpha),
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier.width(tileWidth)
        )
        // folderCountVisible — HomeScreen'den reaktif parametre olarak gelir
        // Renk: effectiveLabelColor — açık duvar kağıdı/açık klasör renginde de okunur (D199 görsel düzeltme)
        if (folderCountVisible) {
            Text(
                text = "${folder.apps.size}",
                color = effectiveLabelColor.copy(alpha = 0.50f * textAlpha),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
        // Hizli acma ipucu — klasor uzerinde yukari kaydirinca en cok kullanilan uygulama acilir
        // folderSwipeHintEnabled — HomeScreen'den reaktif parametre olarak gelir
        if (folderSwipeHintEnabled && topApp != null && onSwipeUp != null) {
            Row(
                modifier = Modifier.width(tileWidth),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "↑",
                    color = effectiveLabelColor.copy(alpha = 0.40f * textAlpha),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = topApp.appName,
                    color = effectiveLabelColor.copy(alpha = 0.45f * textAlpha),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // Son bildirim metni — "AppAdi: mesaj" formatinda, tiklaninca uygulama acar
        // notifTextEnabled — HomeScreen'den reaktif parametre olarak gelir
        // Önce bildirim metni olan uygulamayı bul; yoksa badge > 0 olan uygulamayı al
        val latestNotifApp = remember(folder.apps) {
            folder.apps.filter { it.notificationText.isNotBlank() }
                .maxByOrNull { it.notificationCount }
                ?: folder.apps.filter { it.notificationCount > 0 }
                    .maxByOrNull { it.notificationCount }
        }
        val showNotifText = notifTextEnabled && latestNotifApp != null
        if (showNotifText && latestNotifApp != null) {
            val notifDisplayText = when {
                latestNotifApp.notificationText.isNotBlank() ->
                    "${latestNotifApp.appName}: ${latestNotifApp.notificationText}"
                totalBadge == 1 -> "${latestNotifApp.appName}: 1 yeni bildirim"
                totalBadge > 1  -> "$totalBadge yeni bildirim"
                else -> "${latestNotifApp.appName}: yeni bildirim"
            }
            Text(
                text = notifDisplayText,
                color = effectiveLabelColor.copy(alpha = 0.65f * textAlpha),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(tileWidth)
                    .then(
                        if (onNotificationTap != null) {
                            Modifier.combinedClickable(
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNotificationTap(latestNotifApp.packageName)
                                }
                            )
                        } else Modifier
                    )
            )
        }
        // Kullanım bilgisi alt yazısı — "X gündür açılmadı" / "Hiç açılmadı"
        // unusedInfoEnabled — HomeScreen'den reaktif parametre olarak gelir
        // Bildirim metni gösteriliyorsa bu satır gizlenir (iki bilgi aynı anda gösterilmez)
        if (unusedInfoEnabled && !showNotifText) {
            val unusedInfoText = remember(folder.apps) {
                val now = System.currentTimeMillis()
                // Önce hiç açılmamış uygulama; yoksa en uzun süredir (7+ gün) açılmayan uygulama
                val neverOpened = folder.apps.firstOrNull {
                    it.lastUsedTimestamp == 0L && it.firstInstalledTime > 0L
                }
                if (neverOpened != null) {
                    "${neverOpened.appName}: hiç açılmadı"
                } else {
                    folder.apps.filter { it.lastUsedTimestamp > 0L }
                        .minByOrNull { it.lastUsedTimestamp }
                        ?.let { app ->
                            val days = (now - app.lastUsedTimestamp) / (1000L * 60 * 60 * 24)
                            if (days >= 7L) "${app.appName}: $days gündür açılmadı" else null
                        }
                }
            }
            if (unusedInfoText != null) {
                Text(
                    text = unusedInfoText,
                    color = effectiveLabelColor.copy(alpha = 0.55f * textAlpha),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(tileWidth)
                )
            }
        }
    }
}

@Composable
private fun MiniIconGrid(
    apps: List<com.armutlu.apporganizer.domain.models.AppInfo>,
    iconSize: Dp
) {
    // Pad to 4 slots (null = empty)
    val slots: List<com.armutlu.apporganizer.domain.models.AppInfo?> = buildList {
        addAll(apps)
        repeat(4 - apps.size) { add(null) }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: slots 0, 1
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniAppIcon(app = slots[0], size = iconSize)
            MiniAppIcon(app = slots[1], size = iconSize)
        }
        // Row 2: slots 2, 3
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            MiniAppIcon(app = slots[2], size = iconSize)
            MiniAppIcon(app = slots[3], size = iconSize)
        }
    }
}

@Composable
private fun MiniAppIcon(
    app: com.armutlu.apporganizer.domain.models.AppInfo?,
    size: Dp
) {
    if (app == null) {
        // Empty slot — transparent placeholder
        Box(modifier = Modifier.size(size))
        return
    }

    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val pxSize = with(density) { size.roundToPx() }
    val cacheKey = "${app.packageName}_$pxSize"

    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(
        initialValue = iconCache[cacheKey],
        key1 = cacheKey
    ) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager
                        .getApplicationIcon(app.packageName)
                        .toBitmap(pxSize, pxSize)
                        .asImageBitmap()
                }.getOrNull()
            }
            if (loaded != null) iconCache.put(cacheKey, loaded)
            value = loaded
        }
    }

    val bitmapSnapshot = bitmap
    if (bitmapSnapshot != null) {
        Image(
            bitmap = bitmapSnapshot,
            contentDescription = app.appName,
            modifier = Modifier.size(size)
        )
    } else {
        // Fallback: small circle with first letter
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f))
                .semantics { contentDescription = app.appName },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.appName.take(1).uppercase(),
                color = Color.White,
                fontSize = 8.sp
            )
        }
    }
}
