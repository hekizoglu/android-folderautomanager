package com.armutlu.apporganizer.presentation.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.Message
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
    RESTORE_BACKUP(
        titleRes = R.string.onb_restore_title,
        descriptionRes = R.string.onb_restore_desc,
        icon = Icons.Default.Restore,
        buttonLabelRes = R.string.onb_restore_btn,
        isSkippable = true
    ),
    QUERY_PACKAGES(
        titleRes = R.string.onb_query_title,
        descriptionRes = R.string.onb_query_desc,
        icon = Icons.AutoMirrored.Filled.ManageSearch,
        buttonLabelRes = R.string.onb_query_btn,
        whyRes = R.string.onb_query_why,
        isSkippable = true
    ),
    USAGE_ACCESS(
        titleRes = R.string.onb_usage_title,
        descriptionRes = R.string.onb_usage_desc,
        icon = Icons.Default.QueryStats,
        buttonLabelRes = R.string.onb_usage_btn,
        whyRes = R.string.onb_usage_why,
        privacyNoteRes = R.string.onb_usage_privacy,
        isSkippable = true
    ),
    NOTIFICATIONS(
        titleRes = R.string.onb_notif_title,
        descriptionRes = R.string.onb_notif_desc,
        icon = Icons.Default.Notifications,
        buttonLabelRes = R.string.onb_notif_btn,
        whyRes = R.string.onb_notif_why,
        isSkippable = true
    ),
    UNUSED_GREY(
        titleRes = R.string.onb_unused_title,
        descriptionRes = R.string.onb_unused_desc,
        icon = Icons.Default.Visibility,
        buttonLabelRes = R.string.onb_unused_btn,
        whyRes = R.string.onb_unused_why,
        isSkippable = true
    ),
    AUTO_BACKUP(
        titleRes = R.string.onb_autobackup_title,
        descriptionRes = R.string.onb_autobackup_desc,
        icon = Icons.Default.Backup,
        buttonLabelRes = R.string.onb_autobackup_btn,
        whyRes = R.string.onb_autobackup_why,
        isSkippable = true
    ),
    NOTIF_TEXT(
        titleRes = R.string.onb_notiftext_title,
        descriptionRes = R.string.onb_notiftext_desc,
        icon = Icons.AutoMirrored.Filled.Message,
        buttonLabelRes = R.string.onb_notiftext_btn,
        whyRes = R.string.onb_notiftext_why,
        isSkippable = true
    ),
    NOTIF_ACCESS(
        titleRes = R.string.onb_notifaccess_title,
        descriptionRes = R.string.onb_notifaccess_desc,
        icon = Icons.Default.Notifications,
        buttonLabelRes = R.string.onb_notifaccess_btn,
        whyRes = R.string.onb_notifaccess_why,
        privacyNoteRes = R.string.onb_notifaccess_privacy,
        isSkippable = true
    ),
    SWIPE_HINT(
        titleRes = R.string.onb_swipe_title,
        descriptionRes = R.string.onb_swipe_desc,
        icon = Icons.Default.SwipeUp,
        buttonLabelRes = R.string.onb_swipe_btn,
        whyRes = R.string.onb_swipe_why,
        isSkippable = true
    ),
    NEW_BADGE(
        titleRes = R.string.onb_newbadge_title,
        descriptionRes = R.string.onb_newbadge_desc,
        icon = Icons.Default.NewReleases,
        buttonLabelRes = R.string.onb_newbadge_btn,
        whyRes = R.string.onb_newbadge_why,
        isSkippable = true
    ),
    FOLDER_COUNT(
        titleRes = R.string.onb_foldercount_title,
        descriptionRes = R.string.onb_foldercount_desc,
        icon = Icons.Default.Folder,
        buttonLabelRes = R.string.onb_foldercount_btn,
        whyRes = R.string.onb_foldercount_why,
        isSkippable = true
    ),
    NAV_HIDE(
        titleRes = R.string.onb_navhide_title,
        descriptionRes = R.string.onb_navhide_desc,
        icon = Icons.Default.FullscreenExit,
        buttonLabelRes = R.string.onb_navhide_btn,
        whyRes = R.string.onb_navhide_why,
        isSkippable = true
    ),
    THEME_SELECT(
        titleRes = R.string.onb_theme_title,
        descriptionRes = R.string.onb_theme_desc,
        icon = Icons.Default.Palette,
        buttonLabelRes = R.string.onb_theme_btn,
        whyRes = R.string.onb_theme_why
    ),
    SET_LAUNCHER(
        titleRes = R.string.onb_setlauncher_title,
        descriptionRes = R.string.onb_setlauncher_desc,
        icon = Icons.Default.Home,
        buttonLabelRes = R.string.onb_setlauncher_btn,
        whyRes = R.string.onb_setlauncher_why
    ),
    CLASSIFY_MODE(
        titleRes = R.string.onb_classify_title,
        descriptionRes = R.string.onb_classify_desc,
        icon = Icons.Default.Category,
        buttonLabelRes = R.string.onb_classify_btn,
        isSkippable = true
    ),
    QUICK_SETTINGS(
        titleRes = R.string.onb_quick_settings_title,
        descriptionRes = R.string.onb_quick_settings_desc,
        icon = Icons.Default.Tune,
        buttonLabelRes = R.string.onb_quick_settings_btn,
        whyRes = R.string.onb_quick_settings_why,
        isSkippable = true
    ),
    DONE(
        titleRes = R.string.onb_done_title,
        descriptionRes = R.string.onb_done_desc,
        icon = Icons.Default.CheckCircle,
        buttonLabelRes = R.string.onb_done_btn
    )
}
