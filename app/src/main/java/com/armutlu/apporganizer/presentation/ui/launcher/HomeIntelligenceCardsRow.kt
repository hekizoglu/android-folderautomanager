package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.home.HomeMissionSummary
import com.armutlu.apporganizer.domain.home.HomePulseSummary
import com.armutlu.apporganizer.domain.home.PulseAction

/**
 * Döngü U00 (roadmap satır 1911-1956) — Görevler (M07) ve Dijital Yaşam (D02/D03) kartlarını
 * tek bir yerleşim bileşeninde birleştirir. Eskiden bu iki kart HomeScreen.kt içinde ayrı ayrı
 * `Row` bloğunda çağrılıyordu (bkz. eski satır 615-667); artık tek composable tüm görünürlük +
 * genişlik/yükseklik kararlarını taşır, HomeScreen sadece state + click handler sağlar.
 *
 * Yerleşim kuralları (roadmap esas alınmıştır):
 * - İki kart da varsa yan yana ve eşit yükseklikte (geniş/normal ekran) — dar ekran veya büyük
 *   fontta (fontScale >= 1.3f) dikeye geçilir, kırpma yerine okunabilirlik önceliklidir.
 * - Yalnızca biri varsa tam genişlik.
 * - İkisi de yoksa satır tamamen gizlenir (yükseklik/padding sızmaz — klasör gridine alan kalır).
 * - Tablet genişliğinde gereksiz büyüme olmaması için satır maksimum içerik genişliğiyle sınırlanır.
 */
private val CardsRowHorizontalPadding = 16.dp
private val CardsRowVerticalPadding = 2.dp
private val CardsGap = 8.dp
private val CardsRowMaxWidth = 640.dp
private const val NARROW_SCREEN_WIDTH_DP = 360
private const val LARGE_FONT_SCALE_THRESHOLD = 1.3f

@Composable
internal fun HomeIntelligenceCardsRow(
    missionsEnabled: Boolean,
    mission: HomeMissionSummary?,
    digitalLifeCardVisible: Boolean,
    pulse: HomePulseSummary?,
    onMissionClick: () -> Unit,
    onPulseClick: () -> Unit,
    onPulseReasonAction: (PulseAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // D03: DigitalLifeCard kendi içinde summary==null'da hiçbir şey çizmez — burada da aynı
    // kuralı uygulayarak "kart açık ama veri yok" durumunda boş alan bırakmayız.
    val showMission = missionsEnabled
    val showPulse = digitalLifeCardVisible && pulse != null
    if (!showMission && !showPulse) return

    val configuration = LocalConfiguration.current
    val stackVertically = configuration.screenWidthDp < NARROW_SCREEN_WIDTH_DP ||
        configuration.fontScale >= LARGE_FONT_SCALE_THRESHOLD

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardsRowHorizontalPadding, vertical = CardsRowVerticalPadding),
        horizontalArrangement = Arrangement.Center,
    ) {
        val content = @Composable {
            if (stackVertically || !(showMission && showPulse)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(CardsGap),
                ) {
                    if (showMission) {
                        HomeMissionCard(
                            summary = mission,
                            onClick = onMissionClick,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (showPulse) {
                        DigitalLifeCard(
                            summary = pulse,
                            onClick = onPulseClick,
                            modifier = Modifier.fillMaxWidth(),
                            onReasonAction = onPulseReasonAction,
                        )
                    }
                }
            } else {
                // İki kart da var, geniş ekran: yan yana, eşit ağırlık (aynı yükseklik sınıfı).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CardsGap),
                ) {
                    HomeMissionCard(
                        summary = mission,
                        onClick = onMissionClick,
                        modifier = Modifier.weight(1f),
                    )
                    DigitalLifeCard(
                        summary = pulse,
                        onClick = onPulseClick,
                        modifier = Modifier.weight(1f),
                        onReasonAction = onPulseReasonAction,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.widthIn(max = CardsRowMaxWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
