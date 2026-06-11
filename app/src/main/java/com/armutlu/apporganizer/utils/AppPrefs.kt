package com.armutlu.apporganizer.utils

import android.content.Context

object AppPrefs {
    const val PREFS_NAME = "app_organizer_prefs"
    const val KEY_ONBOARDING_DONE = "onboarding_done"
    const val KEY_LAUNCHER_SETUP_SHOWN = "launcher_setup_shown"

    // Kullanılmayan uygulamaları gri göster — gün cinsinden (0 = kapalı)
    const val KEY_UNUSED_GREY_DAYS = "unused_grey_days"
    const val UNUSED_GREY_DEFAULT = 0  // varsayılan kapalı

    fun getUnusedGreyDays(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_UNUSED_GREY_DAYS, UNUSED_GREY_DEFAULT)

    fun setUnusedGreyDays(context: Context, days: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_UNUSED_GREY_DAYS, days).apply()

    // Swipe-up ipucu — ilk 5 açılışta göster, sonra kaybol
    const val KEY_SWIPE_HINT_COUNT = "swipe_hint_count"
    const val SWIPE_HINT_MAX = 5

    fun shouldShowSwipeHint(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SWIPE_HINT_COUNT, 0) < SWIPE_HINT_MAX
    }

    fun incrementSwipeHintCount(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_SWIPE_HINT_COUNT, 0)
        prefs.edit().putInt(KEY_SWIPE_HINT_COUNT, current + 1).apply()
    }
}
