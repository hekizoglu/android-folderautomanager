package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.provider.Settings
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerAction
import com.armutlu.apporganizer.telemetry.TelemetryEvent
import com.armutlu.apporganizer.telemetry.TelemetryManager
import kotlinx.coroutines.delay

/**
 * Döngü T04 — HomeTickerRow davranış ve erişilebilirlik yenilemesi
 * (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1791-1844).
 *
 * T01 köprüsü ([presentation.ui.launcher.TickerItem] + LauncherViewModel.toTickerItem)
 * KALDIRILDI — bileşen artık [SmartTickerItem]'ı doğrudan tüketir.
 *
 * Davranış değişiklikleri (roadmap):
 * - `basicMarquee` kaldırıldı — başlık ve alt başlık her biri en fazla 1 satır, doğal kırpma.
 * - Varsayılan otomatik geçiş 10 saniye (öncesi 6 sn).
 * - `autoAdvanceAllowed = false` öğeler (kritik/aksiyon gerektiren) otomatik geçmez.
 * - Dokunma sonrası otomatik geçiş en az 15 saniye durur; manuel swipe timer'ı sıfırlar.
 * - Ekran görünür değilken (`visible = false`) timer çalışmaz.
 * - TalkBack (dokunarak keşfet) aktifken otomatik geçiş tamamen kapanır.
 * - Sistem "animasyonları azalt" tercihinde slide yerine yalnız fade kullanılır.
 */
@Composable
internal fun HomeTickerRow(
    items: List<SmartTickerItem>,
    onItemClick: (SmartTickerItem) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    onMute: ((durationMillis: Long) -> Unit)? = null,
    onDismissItem: ((SmartTickerItem) -> Unit)? = null,
    onHideType: ((SmartTickerType) -> Unit)? = null,
    onOpenTickerSettings: (() -> Unit)? = null,
    onDisableTicker: (() -> Unit)? = null,
    autoAdvanceEnabled: Boolean = true,
    autoAdvanceIntervalMs: Long = AUTO_ADVANCE_INTERVAL_MS,
) {
    if (items.isEmpty()) return
    var index by remember(items.size) { mutableStateOf(0) }
    var direction by remember { mutableStateOf(1) } // 1 = ileri, -1 = geri
    // Basili tut -> genisletilmis menu (D233 + T04: icerik bazli kontrol eklendi).
    var menuOpen by remember { mutableStateOf(false) }
    // Ayni ogeye art arda tiklama sonrasi navigation cakismasi/donma tespit edildi (D247 Roadmap #5).
    var lastClickAt by remember { mutableStateOf(0L) }
    // Dokunma sonrasi otomatik gecis en az 15 sn durur (roadmap T04) — bu zaman damgasindan
    // once LaunchedEffect'in yeniden baslamasi engellenir.
    var pausedUntil by remember { mutableStateOf(0L) }

    val current = items[index.coerceIn(0, items.lastIndex)]
    // D247/Roadmap #25: rememberUpdatedState ile "canli" referanslar — gesture yeniden
    // baslatilmadan en guncel index/kapatma degerlerini okur.
    val latestCurrent by rememberUpdatedState(current)
    val latestOnItemClick by rememberUpdatedState(onItemClick)

    val context = LocalContext.current
    val accessibilityManager = LocalAccessibilityManager.current
    // TalkBack/dokunarak-kesfet aktifken otomatik gecis tamamen kapanir (roadmap T04).
    val touchExplorationEnabled = accessibilityManager?.let {
        runCatching {
            val am = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java)
            am?.isTouchExplorationEnabled == true
        }.getOrDefault(false)
    } ?: false
    // Sistem "animasyonlari azalt" tercihi — slide yerine yalniz fade/anlik gecis (roadmap T04).
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }

    // Otomatik ilerleme — visible=false, TalkBack aktif, kritik öğe (autoAdvanceAllowed=false),
    // Ayarlar'dan kapatılmış (autoAdvanceEnabled=false, roadmap T05) veya kullanıcı yakın
    // zamanda etkileşimde bulunduysa (pausedUntil) çalışmaz.
    LaunchedEffect(index, items.size, visible, touchExplorationEnabled, current.autoAdvanceAllowed, autoAdvanceEnabled, autoAdvanceIntervalMs) {
        if (!visible || !autoAdvanceEnabled || touchExplorationEnabled || !current.autoAdvanceAllowed || items.size <= 1) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val waitMillis = autoAdvanceIntervalMs.coerceAtLeast(pausedUntil - now)
        delay(waitMillis)
        direction = 1
        index = (index + 1) % items.size
        // Döngü U02 — otomatik geçiş sonrası yeni gösterilen öğenin türü (başlık/metin YOK).
        TelemetryManager.log(TelemetryEvent.TickerAutoAdvanced(items[index.coerceIn(0, items.lastIndex)].type.toWireType()))
    }

    // Döngü U02 — her yeni öğe göründüğünde bir kez "impression" (tür + sıradaki pozisyon).
    LaunchedEffect(current.dedupeKey) {
        TelemetryManager.log(TelemetryEvent.TickerImpression(current.type.toWireType(), positionBucketOf(index)))
    }

    // "CANLI" noktası — yumuşak nabız animasyonu (reduceMotion'da da düşük maliyetli, atlanmıyor).
    val pulse by rememberInfiniteTransition(label = "ticker_pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ticker_pulse_alpha"
    )

    val typeLabel = current.type.toTypeLabel()
    val subtitleText = current.subtitle
    val a11yDescription = if (current.action != TickerAction.None) {
        stringResource(
            R.string.ticker_content_description,
            typeLabel,
            if (subtitleText != null) "${current.title} — $subtitleText" else current.title,
        )
    } else {
        stringResource(
            R.string.ticker_content_description_no_action,
            typeLabel,
            if (subtitleText != null) "${current.title} — $subtitleText" else current.title,
        )
    }
    val prevActionLabel = stringResource(R.string.ticker_action_previous)
    val nextActionLabel = stringResource(R.string.ticker_action_next)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(current.type.emphasisBackgroundColor())
                .border(0.5.dp, current.type.emphasisBorderColor(), RoundedCornerShape(18.dp))
                .semantics(mergeDescendants = true) {
                    contentDescription = a11yDescription
                    customActions = buildList {
                        if (items.size > 1) {
                            add(CustomAccessibilityAction(prevActionLabel) {
                                direction = -1
                                index = (index - 1 + items.size) % items.size
                                pausedUntil = System.currentTimeMillis() + USER_INTERACTION_PAUSE_MS
                                true
                            })
                            add(CustomAccessibilityAction(nextActionLabel) {
                                direction = 1
                                index = (index + 1) % items.size
                                pausedUntil = System.currentTimeMillis() + USER_INTERACTION_PAUSE_MS
                                true
                            })
                        }
                    }
                }
                .pointerInputTicker(
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastClickAt > 700L) {
                            lastClickAt = now
                            pausedUntil = now + USER_INTERACTION_PAUSE_MS
                            // Döngü U02 — açılan öğenin türü + o anki pozisyonu (başlık/hedef YOK).
                            TelemetryManager.log(TelemetryEvent.TickerOpened(latestCurrent.type.toWireType(), positionBucketOf(index)))
                            latestOnItemClick(latestCurrent)
                        }
                    },
                    onLongPress = { if (onMute != null || onDismissItem != null) menuOpen = true },
                    onSwipe = { forward ->
                        direction = if (forward) 1 else -1
                        index = if (forward) (index + 1) % items.size
                                else (index - 1 + items.size) % items.size
                        pausedUntil = System.currentTimeMillis() + USER_INTERACTION_PAUSE_MS
                        if (forward) {
                            // Döngü U02 — kullanıcının elle "sonraki"ye geçtiği öğenin türü.
                            TelemetryManager.log(TelemetryEvent.TickerManualNext(items[index.coerceIn(0, items.lastIndex)].type.toWireType()))
                        }
                    }
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(pulse)
                    .background(current.type.dotColor(), CircleShape)
            )
            val transitionSpec: androidx.compose.animation.AnimatedContentTransitionScope<Int>.() -> androidx.compose.animation.ContentTransform = {
                if (reduceMotion) {
                    fadeIn(tween(150)).togetherWith(fadeOut(tween(120)))
                } else {
                    (slideInHorizontally { it * direction } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { -it * direction } + fadeOut(tween(150)))
                }
            }
            AnimatedContent(
                targetState = index,
                transitionSpec = transitionSpec,
                label = "ticker_content",
                modifier = Modifier.weight(1f)
            ) { i ->
                val item = items[i.coerceIn(0, items.lastIndex)]
                Column {
                    Text(
                        text = "${item.icon} ${item.title}",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.sp,
                        fontWeight = if (item.type.isEmphasized()) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    item.subtitle?.let { sub ->
                        Text(
                            text = sub,
                            color = Color.White.copy(alpha = 0.70f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (items.size > 1) {
                Text(
                    text = stringResource(R.string.ticker_page_indicator, index + 1, items.size),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp
                )
            }
            if (onDismissItem != null) {
                IconButton(
                    onClick = {
                        // Döngü U02 — kapatılan öğenin türü (başlık/hedef YOK).
                        TelemetryManager.log(TelemetryEvent.TickerDismissed(current.type.toWireType()))
                        onDismissItem(current)
                        if (items.size > 1) {
                            index = index.coerceIn(0, (items.size - 2).coerceAtLeast(0))
                        }
                    },
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = context.getString(R.string.ticker_action_dismiss) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Uzun basma menüsü — sessize alma + içerik bazlı kontrol (roadmap T04).
            if (onMute != null || onDismissItem != null || onHideType != null || onOpenTickerSettings != null || onDisableTicker != null) {
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onDismissItem != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ticker_menu_hide_item)) },
                            onClick = {
                                menuOpen = false
                                // Döngü U02 — kapatılan öğenin türü (başlık/hedef YOK).
                                TelemetryManager.log(TelemetryEvent.TickerDismissed(current.type.toWireType()))
                                onDismissItem(current)
                            }
                        )
                    }
                    if (onHideType != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ticker_menu_hide_type)) },
                            onClick = {
                                menuOpen = false
                                // Döngü U02 — tüm türü kapatan kullanıcı kararı (bkz. roadmap ticker_type_disabled).
                                TelemetryManager.log(TelemetryEvent.TickerTypeDisabled(current.type.toWireType()))
                                onHideType(current.type)
                            }
                        )
                    }
                    if (onMute != null) {
                        listOf(
                            stringResource(R.string.ticker_mute_8h) to 8L * 60 * 60 * 1000,
                            stringResource(R.string.ticker_mute_1d) to 24L * 60 * 60 * 1000,
                            stringResource(R.string.ticker_mute_7d) to 7L * 24 * 60 * 60 * 1000,
                        ).forEach { (label, duration) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menuOpen = false
                                    // Döngü U02 — sessize alma (tüm şerit) — o anki öğenin türü örneklenir.
                                    TelemetryManager.log(TelemetryEvent.TickerSnoozed(current.type.toWireType()))
                                    onMute(duration)
                                }
                            )
                        }
                    }
                    if (onOpenTickerSettings != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ticker_menu_settings)) },
                            onClick = {
                                menuOpen = false
                                onOpenTickerSettings()
                            }
                        )
                    }
                    if (onDisableTicker != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ticker_menu_disable)) },
                            onClick = {
                                menuOpen = false
                                onDisableTicker()
                            }
                        )
                    }
                }
            }
        }
    }
}

private const val AUTO_ADVANCE_INTERVAL_MS = 10_000L
private const val USER_INTERACTION_PAUSE_MS = 15_000L

/**
 * Dijital Yaşam skoru -> renk eşlemesi — [DigitalLifeCard] tarafından da kullanılır.
 * T04 rewrite'ında ticker artık ham skor metnini üretmiyor (bu üretici TickerComposer'dan
 * D00'da kaldırıldı) ama paylaşılan yardımcı fonksiyon burada korunur (aynı dosya/paket).
 */
internal fun digitalLifeScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF2E7D32)
    score >= 60 -> Color(0xFF43A047)
    score >= 40 -> Color(0xFFF9A825)
    else -> Color(0xFFE53935)
}

/** [SmartTickerType] -> kapalı telemetri enum'u (Döngü U02) — asla başlık/alt başlık taşımaz. */
private fun SmartTickerType.toWireType(): TelemetryEvent.TickerItemType = when (this) {
    SmartTickerType.CRITICAL_HEALTH -> TelemetryEvent.TickerItemType.CRITICAL_HEALTH
    SmartTickerType.ACTION_REQUIRED -> TelemetryEvent.TickerItemType.ACTION_REQUIRED
    SmartTickerType.MISSION_PROGRESS -> TelemetryEvent.TickerItemType.MISSION_PROGRESS
    SmartTickerType.MISSION_ACHIEVEMENT -> TelemetryEvent.TickerItemType.MISSION_ACHIEVEMENT
    SmartTickerType.PULSE_CHANGE -> TelemetryEvent.TickerItemType.PULSE_CHANGE
    SmartTickerType.CONTEXTUAL_SUGGESTION -> TelemetryEvent.TickerItemType.CONTEXTUAL_SUGGESTION
    SmartTickerType.WEEKLY_REPORT -> TelemetryEvent.TickerItemType.WEEKLY_REPORT
    SmartTickerType.FEATURE_DISCOVERY -> TelemetryEvent.TickerItemType.FEATURE_DISCOVERY
}

/** Şerit içi sıra (0-based index) -> kapalı pozisyon bucket'ı (Döngü U02). */
private fun positionBucketOf(index: Int): TelemetryEvent.PositionBucket = when {
    index <= 0 -> TelemetryEvent.PositionBucket.FIRST
    index <= 4 -> TelemetryEvent.PositionBucket.TWO_TO_FIVE
    else -> TelemetryEvent.PositionBucket.SIX_PLUS
}

@Composable
private fun SmartTickerType.toTypeLabel(): String = when (this) {
    SmartTickerType.CRITICAL_HEALTH -> stringResource(R.string.ticker_type_critical_health)
    SmartTickerType.ACTION_REQUIRED -> stringResource(R.string.ticker_type_action_required)
    SmartTickerType.MISSION_PROGRESS -> stringResource(R.string.ticker_type_mission_progress)
    SmartTickerType.MISSION_ACHIEVEMENT -> stringResource(R.string.ticker_type_mission_achievement)
    SmartTickerType.PULSE_CHANGE -> stringResource(R.string.ticker_type_pulse_change)
    SmartTickerType.CONTEXTUAL_SUGGESTION -> stringResource(R.string.ticker_type_contextual_suggestion)
    SmartTickerType.WEEKLY_REPORT -> stringResource(R.string.ticker_type_weekly_report)
    SmartTickerType.FEATURE_DISCOVERY -> stringResource(R.string.ticker_type_feature_discovery)
}

/** Renk-bağımsız tip göstergesi zaten ikon (emoji) ile sağlanıyor — bu yalnız ek görsel vurgu. */
private fun SmartTickerType.isEmphasized(): Boolean =
    this == SmartTickerType.CRITICAL_HEALTH || this == SmartTickerType.ACTION_REQUIRED

private fun SmartTickerType.emphasisBackgroundColor(): Color = when (this) {
    SmartTickerType.CRITICAL_HEALTH -> Color(0xFFE53935).copy(alpha = 0.22f)
    SmartTickerType.ACTION_REQUIRED -> Color(0xFFF9A825).copy(alpha = 0.18f)
    else -> Color.White.copy(alpha = 0.10f)
}

private fun SmartTickerType.emphasisBorderColor(): Color = when (this) {
    SmartTickerType.CRITICAL_HEALTH -> Color(0xFFE53935).copy(alpha = 0.55f)
    SmartTickerType.ACTION_REQUIRED -> Color(0xFFF9A825).copy(alpha = 0.45f)
    else -> Color.White.copy(alpha = 0.18f)
}

private fun SmartTickerType.dotColor(): Color = when (this) {
    SmartTickerType.CRITICAL_HEALTH -> Color(0xFFE53935)
    SmartTickerType.ACTION_REQUIRED -> Color(0xFFF9A825)
    else -> Color(0xFF26C6DA)
}

/**
 * Tap + basili tutma + yatay swipe'ı tek gesture döngüsünde birleştirir (D247 Roadmap #6).
 *
 * Kök neden: eskiden tap ve swipe ayrı `pointerInput` bloklarındaydı; HomeScreen'deki ana
 * `HorizontalPager` (klasör sayfalama) aynı yatay drag'i nested-scroll/ata gesture olarak
 * yakalayıp tüketebiliyordu, ticker'a hiç ulaşmıyordu. Çözüm: tek `awaitEachGesture` döngüsünde
 * `down` olayını hemen `consume()` ederek jesti en baştan bu bileşene kilitliyoruz; böylece üst
 * `HorizontalPager` aynı dokunuşu çalamaz.
 */
private fun Modifier.pointerInputTicker(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipe: (forward: Boolean) -> Unit,
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // Jesti hemen tüketerek üst HorizontalPager'ın (klasör sayfalama) bu dokunuşu
        // çalmasını engelle — nested scroll çakışmasının kök nedeni buydu.
        down.consume()
        val downTimeMillis = System.currentTimeMillis()
        var isDragging = false
        var accumulated = 0f
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                change.consume()
                if (!isDragging) {
                    val elapsed = System.currentTimeMillis() - downTimeMillis
                    if (elapsed >= viewConfiguration.longPressTimeoutMillis) onLongPress() else onTap()
                }
                break
            }
            accumulated += change.positionChange().x
            if (!isDragging && kotlin.math.abs(accumulated) > viewConfiguration.touchSlop) {
                isDragging = true
            }
            change.consume()
        }
        if (isDragging) {
            // Sola kaydırma = sonraki haber (haber şeridi alışkanlığı), sağa = önceki
            if (accumulated < -48f) onSwipe(true)
            else if (accumulated > 48f) onSwipe(false)
        }
    }
}
