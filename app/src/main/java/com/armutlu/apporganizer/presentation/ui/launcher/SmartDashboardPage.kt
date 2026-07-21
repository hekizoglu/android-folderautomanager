package com.armutlu.apporganizer.presentation.ui.launcher

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Hero Dashboard migration — Commit 1 (bkz. YENI_HERO_DASHBOARD roadmap).
 *
 * Bu composable eskiden P06 "Sayfa 0" içerik listesindeki tüm zeka bileşenlerini
 * (saat, görev/dijital yaşam kartları, arama/widget alanı, içgörü kartları, nabız şeridi,
 * favoriler) tek yerde topluyordu. Hero tasarımına geçiş için BİLİNÇLİ olarak sadeleştirildi:
 * yalnızca saat (PulseClockWidget) kalır, geri kalan bölümler kaldırıldı.
 *
 * TODO: HeroDashboardPage — Hero tasarımı bu placeholder'ın yerini alacak (sonraki commit'ler).
 *
 * Roadmap: YENI_HERO_DASHBOARD migration Commit 1.
 */
@Composable
internal fun SmartDashboardPage(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier,
    // Faz S4 — opsiyonel: widget sürüklerken kenara yaklaşınca sayfa otomatik kaydırılsın diye
    // HomeScreen'deki gerçek pagerState buradan iletilir. Hero migration'da henüz kullanılmıyor.
    pagerState: PagerState? = null,
) {
    val scrollState = rememberScrollState()

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Görev S2 — Usta (100⭐) ödülü: altın saat aksanı yalnızca MASTER seviyesinde VE
        // kullanıcı Ayarlar'dan açtıysa gösterilir (MasterRewardPolicy tek karar noktası).
        // Reaktif AppPrefs paterni (LEARNINGS): remember{} tek başına Settings'ten dönüşte
        // güncellenmez — OnSharedPreferenceChangeListener ile canlı tutulur.
        val context = androidx.compose.ui.platform.LocalContext.current
        var masterClockPrefEnabled by remember {
            androidx.compose.runtime.mutableStateOf(
                com.armutlu.apporganizer.utils.AppPrefs.isMasterClockStyleEnabled(context)
            )
        }
        androidx.compose.runtime.DisposableEffect(context) {
            val sharedPrefs = context.getSharedPreferences(
                com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
            )
            val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == com.armutlu.apporganizer.utils.AppPrefs.KEY_MASTER_CLOCK_STYLE_ENABLED) {
                    masterClockPrefEnabled = com.armutlu.apporganizer.utils.AppPrefs.isMasterClockStyleEnabled(context)
                }
            }
            sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
            onDispose { sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        val masterGoldAccent = remember(state.intelligence.mission?.totalStars, masterClockPrefEnabled) {
            com.armutlu.apporganizer.domain.usecase.missions.MasterRewardPolicy.isGoldClockAccentActive(
                totalStars = state.intelligence.mission?.totalStars ?: 0,
                prefEnabled = masterClockPrefEnabled,
            )
        }
        PulseClockWidget(
            compact = state.clock.compact,
            onOpenWeeklyReport = actions.onOpenWeeklyReport,
            onOpenScoreDetails = actions.onOpenScoreDetails,
            onLongPress = actions.onClockLongPress,
            masterGoldAccent = masterGoldAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (state.clock.compact) 16.dp else 32.dp, bottom = 8.dp)
        )

        // Görev S1 — todayCardEnabled açıkken tek bağlamsal "BUGÜN" kartı gösterilir.
        // spec null ise (TodayCardSelector hiçbir önceliği seçemedi) HİÇBİR ŞEY çizilmez.
        if (state.intelligence.todayCardEnabled) {
            state.intelligence.todayCardSpec?.let { spec ->
                TodayCard(
                    spec = spec,
                    onMissionClick = actions.onMissionClick,
                    onPulseClick = actions.onPulseClick,
                    onFolderReviewClick = actions.onOpenFolderStats,
                    onReportReadyClick = actions.onOpenWeeklyReport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        } else {
            HomeIntelligenceCardsRow(
                missionsEnabled = state.intelligence.missionsEnabled,
                mission = state.intelligence.mission,
                digitalLifeCardVisible = state.intelligence.digitalLifeCardVisible,
                pulse = state.intelligence.pulse,
                onMissionClick = actions.onMissionClick,
                onPulseClick = actions.onPulseClick,
                onPulseReasonAction = actions.onPulseReasonAction,
            )
        }

        // TODO: HeroDashboardPage — arama/widget/içgörü-ticker/favoriler bölümleri Hero
        // tasarımı netleşene kadar burada YOK. Commit 1 kapsamı yalnızca state/composable
        // sadeleştirmesidir.
    }
}
