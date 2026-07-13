package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.viewmodel.MissionsViewModel
import kotlinx.coroutines.delay

/**
 * Gorevler ekrani (D257) — gunluk/haftalik gorevler + toplam yildiz sayaci.
 * Acilista otomatik dogrulama (refresh) yapilir; yeni yildiz kazanildiginda
 * basit AnimatedVisibility tebrik karti gosterilir (konfeti kutuphanesi yok).
 */
@Composable
fun MissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
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
        item { StarsHeader(totalStars = uiState.totalStars) }

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
                    MissionRow(mission = mission, onCompleteManually = { viewModel.completeManually(mission.id) })
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
                    MissionRow(mission = mission, onCompleteManually = { viewModel.completeManually(mission.id) })
                }
            }
        }
    }
}

@Composable
private fun StarsHeader(totalStars: Int) {
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
        }
    }
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

@Composable
private fun MissionRow(
    mission: MissionsViewModel.MissionUi,
    onCompleteManually: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = if (mission.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (mission.completed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mission.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (mission.completed) FontWeight.Normal else FontWeight.Medium,
            )
            Text(
                text = "⭐ x${mission.starReward}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!mission.completed && !mission.autoCheckable) {
            TextButton(onClick = onCompleteManually) {
                Text(stringResource(R.string.missions_mark_done))
            }
        }
    }
}

@Composable
private fun MissionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
