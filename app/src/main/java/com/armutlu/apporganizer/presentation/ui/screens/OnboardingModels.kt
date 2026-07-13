package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.armutlu.apporganizer.R

// ── Renkler ve gradyanlar ────────────────────────────────────────────────────

internal val OnboardingBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63), Color(0xFF24243E))
)
internal val OnboardingAccentPurple      = Color(0xFF6C63FF)
internal val OnboardingAccentPurpleLight = Color(0xFF9C8FFF)
internal val OnboardingButtonGradient = Brush.horizontalGradient(
    colors = listOf(OnboardingAccentPurple, OnboardingAccentPurpleLight)
)
internal val OnboardingTealGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF00897B), Color(0xFF26C6DA))
)

// ── Yardımcı fonksiyon ───────────────────────────────────────────────────────

internal fun isDefaultLauncherApp(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return info?.activityInfo?.packageName == context.packageName
}

// ── Onboarding adım modeli ────────────────────────────────────────────────────

internal enum class OnboardingStep(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    @StringRes val buttonLabelRes: Int,
    @StringRes val whyRes: Int = 0,
    @StringRes val privacyNoteRes: Int = 0,
    val isSkippable: Boolean = false
) {
    WELCOME(
        titleRes = R.string.onb_welcome_title,
        descriptionRes = R.string.onb_welcome_desc,
        icon = Icons.Default.Apps,
        buttonLabelRes = R.string.onb_welcome_btn
    ),
    SET_LAUNCHER(
        titleRes = R.string.onb_setlauncher_title,
        descriptionRes = R.string.onb_setlauncher_desc,
        icon = Icons.Default.Home,
        buttonLabelRes = R.string.onb_setlauncher_btn,
        whyRes = R.string.onb_setlauncher_why
    ),
    THEME_SELECT(
        titleRes = R.string.onb_theme_title,
        descriptionRes = R.string.onb_theme_desc,
        icon = Icons.Default.Palette,
        buttonLabelRes = R.string.onb_theme_btn,
        whyRes = R.string.onb_theme_why
    ),
    QUICK_SETTINGS(
        titleRes = R.string.onb_quick_settings_title,
        descriptionRes = R.string.onb_quick_settings_desc,
        icon = Icons.Default.Tune,
        buttonLabelRes = R.string.onb_quick_settings_btn,
        whyRes = R.string.onb_quick_settings_why,
        isSkippable = true
    ),
    ORGANIZATION_PREVIEW(
        titleRes = R.string.onb_org_preview_title,
        descriptionRes = R.string.onb_org_preview_desc,
        icon = Icons.Default.Folder,
        buttonLabelRes = R.string.onb_org_preview_btn,
        whyRes = R.string.onb_org_preview_why
    ),
    DONE(
        titleRes = R.string.onb_done_title,
        descriptionRes = R.string.onb_done_desc,
        icon = Icons.Default.CheckCircle,
        buttonLabelRes = R.string.onb_done_btn
    )
}
