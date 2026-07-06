package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.presentation.ui.theme.AppFont
import com.armutlu.apporganizer.presentation.ui.theme.AppTheme

/** Adım ikonu — AnimatedContent ile geçiş animasyonlu */
@Composable
internal fun OnboardingStepIcon(steps: List<OnboardingStep>, stepIndex: Int) {
    AnimatedContent(
        targetState = stepIndex,
        transitionSpec = {
            fadeIn() + slideInHorizontally { it / 3 } togetherWith fadeOut() + slideOutHorizontally { -it / 3 }
        },
        label = "icon"
    ) { idx ->
        val s = steps[idx]
        val isLauncher = s == OnboardingStep.SET_LAUNCHER
        val iconBg = if (isLauncher) OnboardingTealGradient else null
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(1.5.dp,
                        if (isLauncher) Color(0xFF00897B).copy(0.6f) else OnboardingAccentPurple.copy(0.4f),
                        CircleShape)
                    .then(
                        if (iconBg != null) Modifier.background(iconBg)
                        else Modifier.background(OnboardingAccentPurple.copy(0.25f))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(s.icon, null, modifier = Modifier.size(52.dp), tint = Color.White)
            }
        }
    }
}

/** İlerleme noktaları */
@Composable
internal fun OnboardingStepDots(steps: List<OnboardingStep>, stepIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.indices.forEach { i ->
            Box(
                modifier = Modifier
                    .size(if (i == stepIndex) 24.dp else 7.dp, 7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (i == stepIndex) OnboardingAccentPurple
                        else Color.White.copy(0.20f)
                    )
            )
        }
    }
}

/** Başlık + açıklama animasyonlu */
@Composable
internal fun OnboardingStepHeader(steps: List<OnboardingStep>, stepIndex: Int) {
    AnimatedContent(targetState = stepIndex, label = "text") { idx ->
        val s = steps[idx]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(s.titleRes), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
            Text(stringResource(s.descriptionRes), fontSize = 16.sp, color = Color.White.copy(0.75f), textAlign = TextAlign.Center, lineHeight = 26.sp)
        }
    }
}

/** "Neden gerekli" açıklama kutusu */
@Composable
internal fun OnboardingWhyBox(currentStep: OnboardingStep) {
    if (currentStep.whyRes == 0) return
    val isLauncher = currentStep == OnboardingStep.SET_LAUNCHER
    val accentColor = if (isLauncher) Color(0xFF00897B) else OnboardingAccentPurple
    val whyText = stringResource(currentStep.whyRes)
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.08f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.width(3.dp).height(48.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = accentColor, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Text(whyText, fontSize = 14.sp, color = Color.White.copy(0.75f), lineHeight = 20.sp)
            }
        }
    }
    if (currentStep.privacyNoteRes != 0) {
        Spacer(Modifier.height(8.dp))
        val privacyText = stringResource(currentStep.privacyNoteRes)
        Box(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF00897B).copy(alpha = 0.15f))
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFF26C6DA), modifier = Modifier.size(16.dp))
                Text(privacyText, fontSize = 12.sp, color = Color(0xFF26C6DA), lineHeight = 18.sp)
            }
        }
    }
}

/** Durum göstergesi — yalnızca SET_LAUNCHER adımında launcher ayarlandıysa gösterilir */
@Composable
internal fun OnboardingStatusBadge(
    currentStep: OnboardingStep,
    launcherSet: Boolean,
    notifGranted: Boolean = false,
    notifAccessGranted: Boolean = false,
    usageStatsGranted: Boolean = false,
    unusedGreyDays: Int = 0
) {
    val isLauncher = currentStep == OnboardingStep.SET_LAUNCHER
    val statusText = when (currentStep) {
        OnboardingStep.SET_LAUNCHER -> if (launcherSet) stringResource(R.string.onb_status_launcher_set) else null
        else -> null
    } ?: return
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLauncher) Color(0xFF00897B).copy(0.25f) else OnboardingAccentPurple.copy(0.20f))
            .padding(12.dp)
    ) {
        Text(stringResource(R.string.onb_status_ok, statusText), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

/** THEME_SELECT adımı UI */
@Composable
internal fun OnboardingThemeSelector(
    selectedTheme: AppTheme,
    selectedFont: AppFont,
    onThemeChange: (AppTheme) -> Unit,
    onFontChange: (AppFont) -> Unit
) {
    Text(stringResource(R.string.appearance_color_theme), fontSize = 13.sp, color = Color.White.copy(0.6f), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    // DYNAMIC (Material You) yalnızca Android 12+ cihazlarda listelenir
    val onboardingThemes = AppTheme.entries.filter {
        it != AppTheme.DYNAMIC ||
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(onboardingThemes, key = { it.name }) { theme ->
            val isSelected = selectedTheme == theme
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onThemeChange(theme) }.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(theme.previewBrush)
                        .border(if (isSelected) 3.dp else 1.dp,
                            if (isSelected) Color.White else Color.White.copy(0.3f), CircleShape)
                )
                Spacer(Modifier.height(4.dp))
                Text(theme.label, fontSize = 11.sp, color = if (isSelected) Color.White else Color.White.copy(0.5f))
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.appearance_font), fontSize = 13.sp, color = Color.White.copy(0.6f), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppFont.entries.forEach { font ->
            val isSelected = selectedFont == font
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) OnboardingAccentPurple else Color.White.copy(0.12f))
                    .clickable { onFontChange(font) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(font.label, fontSize = 13.sp, color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** UNUSED_GREY gün seçici chip'leri */
@Composable
internal fun OnboardingGreyDayChips(unusedGreyDays: Int, onSelect: (Int) -> Unit) {
    val offLabel = stringResource(R.string.onb_off)
    val options = listOf(0 to offLabel, 7 to "7 ${stringResource(R.string.onb_days)}", 14 to "14 ${stringResource(R.string.onb_days)}", 30 to "30 ${stringResource(R.string.onb_days)}")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        options.forEach { (days, label) ->
            val selected = unusedGreyDays == days
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Color(0xFF00897B) else Color.White.copy(0.15f))
                    .clickable { onSelect(days) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = Color.White, fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

/** Toggle switch — AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE */
@Composable
internal fun OnboardingToggleRow(value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(0.08f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(if (value) R.string.onb_on else R.string.onb_off), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        androidx.compose.material3.Switch(
            checked = value, onCheckedChange = onValueChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = OnboardingAccentPurple
            )
        )
    }
}
