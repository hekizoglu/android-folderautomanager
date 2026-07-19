package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.usecase.missions.MissionActionRouter
import com.armutlu.apporganizer.presentation.viewmodel.MissionsViewModel
import kotlinx.coroutines.delay

/**
 * Gorevler ekrani (D257) — gunluk/haftalik gorevler + toplam yildiz sayaci.
 * Acilista otomatik dogrulama (refresh) yapilir; yeni yildiz kazanildiginda
 * basit AnimatedVisibility tebrik karti gosterilir (konfeti kutuphanesi yok).
 *
 * Dongu M05: her gorev satirinin sonunda, action None degilse kucuk bir eylem butonu
 * gosterilir. Route cozumu MissionActionRouter'da tek yerde yapilir; bu composable sadece
 * sonucu tuketir (Screen -> onNavigateToRoute, SystemIntent -> context.startActivity).
 *
 * Dongu M06: gorev satiri kendisi ilerleme odakli yeniden tasarlandi — bkz. MissionCard.kt
 * (MissionRow composable'i, 300 satir kuralini korumak icin ayri dosyaya cikarildi).
 */
@Composable
fun MissionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.refresh() }

    // Tebrik karti birkac saniye sonra kendini kapatir.
    LaunchedEffect(uiState.celebrateStars) {
        if (uiState.celebrateStars != null) {
            delay(4000)
            viewModel.dismissCelebration()
        }
    }

    SettingsSubScreenScaffold(
        title = stringResource(R.string.missions_screen_title),
        onNavigateBack = onNavigateBack,
    ) {
        item {
            StarsHeader(
                totalStars = uiState.totalStars,
                taskScore = uiState.taskScore,
                taskScoreDelta = uiState.taskScoreDelta,
                taskScoreLastEvent = uiState.taskScoreLastEvent,
                currentStreak = uiState.currentStreak,
                bestStreak = uiState.bestStreak,
                goldenStreak = uiState.goldenStreak,
                streakFrozenYesterday = uiState.streakFrozenYesterday,
            )
        }

        item {
            AnimatedVisibility(
                visible = uiState.celebrateStars != null,
                enter = slideInVertically { -it } + fadeIn(),
                exit = fadeOut(),
            ) {
                CelebrationCard(stars = uiState.celebrateStars ?: 0)
            }
        }

        item { SettingsSectionTitle(stringResource(R.string.missions_daily_section)) }
        item {
            SettingsCard {
                uiState.daily.forEachIndexed { index, mission ->
                    if (index > 0) MissionDivider()
                    MissionRow(
                        mission = mission,
                        onActionClick = { handleMissionAction(mission.action, context, onNavigateToRoute) },
                        justCompleted = mission.justCompleted,
                        onLongPressAction = {
                            mission.longPressTargetPackageName?.let { pkg ->
                                handleMissionAction(
                                    com.armutlu.apporganizer.domain.usecase.missions.MissionAction.OpenAppInfo(pkg),
                                    context,
                                    onNavigateToRoute,
                                )
                            }
                        },
                    )
                }
                if (uiState.daily.isEmpty() && !uiState.loading) {
                    Text(
                        text = stringResource(R.string.missions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        item { SettingsSectionTitle(stringResource(R.string.missions_weekly_section)) }
        item {
            SettingsCard {
                uiState.weekly.forEachIndexed { index, mission ->
                    if (index > 0) MissionDivider()
                    MissionRow(
                        mission = mission,
                        onActionClick = { handleMissionAction(mission.action, context, onNavigateToRoute) },
                        justCompleted = mission.justCompleted,
                        onLongPressAction = {
                            mission.longPressTargetPackageName?.let { pkg ->
                                handleMissionAction(
                                    com.armutlu.apporganizer.domain.usecase.missions.MissionAction.OpenAppInfo(pkg),
                                    context,
                                    onNavigateToRoute,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StarsHeader(
    totalStars: Int,
    taskScore: Int,
    taskScoreDelta: Int,
    taskScoreLastEvent: String,
    currentStreak: Int = 0,
    bestStreak: Int = 0,
    goldenStreak: Int = 0,
    streakFrozenYesterday: Boolean = false,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "⭐", fontSize = 40.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$totalStars",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.missions_total_stars),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(8.dp))
            StarLevelRow(totalStars)
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.missions_task_score_value, taskScore),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (taskScoreDelta != 0 && taskScoreLastEvent.isNotBlank()) {
                Text(
                    text = stringResource(
                        R.string.missions_task_score_delta,
                        taskScoreLastEvent,
                        if (taskScoreDelta > 0) "+$taskScoreDelta" else taskScoreDelta.toString(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            // Dongu G4 — Streak (Seri) Sistemi: sadece anlamli bir seri varsa gosterilir (>=1),
            // "0 gün" gibi olumsuz bir mesaj asla uretilmez (M08 ceza yok ilkesi).
            if (currentStreak >= 1) {
                Spacer(Modifier.height(8.dp))
                val goldenSuffix = if (goldenStreak >= 1) {
                    stringResource(R.string.missions_streak_golden_suffix, goldenStreak)
                } else {
                    ""
                }
                Text(
                    text = stringResource(R.string.missions_streak_row, currentStreak, bestStreak) + goldenSuffix,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (streakFrozenYesterday) {
                Text(
                    text = stringResource(R.string.missions_streak_frozen_yesterday),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/**
 * Dongu G6 — Yildiz Ekonomisi: seviye rozeti + bir sonraki seviyeye ince ilerleme satiri.
 * SADECE Gorevler ekraninda gosterilir — HomeMissionCard'a bilerek DOKUNULMADI (plan G6,
 * kart sade kalmali). [com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem]
 * saf hesaplamayi yapar, burada sadece stringResource'a esleme yapilir.
 */
@Composable
private fun StarLevelRow(totalStars: Int) {
    val level = com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.levelFor(totalStars)
    val levelLabel = stringResource(level.labelRes())
    Text(
        text = stringResource(R.string.missions_level_badge, levelLabel),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val starsToNext = com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.starsToNextLevel(totalStars)
    val nextLevelOrdinal = level.ordinal + 1
    val nextLevel = com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.entries.getOrNull(nextLevelOrdinal)
    Text(
        text = if (starsToNext != null && nextLevel != null) {
            stringResource(R.string.missions_level_progress, levelLabel, starsToNext, stringResource(nextLevel.labelRes()))
        } else {
            stringResource(R.string.missions_level_max, levelLabel)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
    )
}

/** [com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level] -> yerelleştirilmiş string resource. */
private fun com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.labelRes(): Int = when (this) {
    com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.BEGINNER -> R.string.missions_level_beginner
    com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.STEADY -> R.string.missions_level_steady
    com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.FOCUSED -> R.string.missions_level_focused
    com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.BALANCE_MASTER -> R.string.missions_level_balance_master
    com.armutlu.apporganizer.domain.usecase.missions.StarLevelSystem.Level.MASTER -> R.string.missions_level_master
}

@Composable
private fun CelebrationCard(stars: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(
            text = stringResource(R.string.missions_congrats, stars),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/**
 * Dongu M05: MissionActionRouter'dan gelen hedefi tuketir — route ise navigate, Intent ise
 * startActivity. Dongu G2: SystemIntent cozumlenemezse (resolveActivity null — cihaz/ROM
 * bu ayari desteklemiyorsa, orn. bazi OEM'lerde Bedtime ayri uygulamadir) kullanim raporuna
 * duser — bu karar UI katmaninda Intent cozumlemesiyle verilir, domain katmani Android
 * PackageManager'a bagimli olmaz.
 */
private fun handleMissionAction(
    action: com.armutlu.apporganizer.domain.usecase.missions.MissionAction,
    context: android.content.Context,
    onNavigateToRoute: (String) -> Unit,
) {
    when (val target = MissionActionRouter.resolve(action)) {
        is MissionActionRouter.RouteTarget.Screen -> onNavigateToRoute(target.route)
        is MissionActionRouter.RouteTarget.SystemIntent -> {
            val intent = android.content.Intent(target.intentAction)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            // Dongu G3b — App Info (ve benzeri paket-ozel) intent'ler data URI'si gerektirir.
            target.dataPackage?.let { pkg ->
                intent.data = android.net.Uri.fromParts("package", pkg, null)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                onNavigateToRoute(MissionActionRouter.ROUTE_USAGE_REPORT)
            }
        }
        MissionActionRouter.RouteTarget.None -> Unit
    }
}

@Composable
private fun MissionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
