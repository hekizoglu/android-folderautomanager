package com.armutlu.apporganizer.presentation.ui.screens.onboarding

import androidx.annotation.StringRes
import com.armutlu.apporganizer.R
import com.armutlu.apporganizer.domain.models.Category

/**
 * Onboarding Kategori açıklamalarını merkezi ve yerelleştirilmiş olarak çözen yardımcı sınıf.
 */
object CategoryDescriptionResolver {

    @StringRes
    fun getDescriptionResId(categoryId: String): Int {
        return when (categoryId) {
            Category.CAT_COMMUNICATION -> R.string.cat_desc_communication
            Category.CAT_SOCIAL -> R.string.cat_desc_social
            Category.CAT_FINANCE -> R.string.cat_desc_finance
            Category.CAT_ENTERTAINMENT, Category.CAT_VIDEO, Category.CAT_MUSIC -> R.string.cat_desc_entertainment
            Category.CAT_PRODUCTIVITY, Category.CAT_BUSINESS -> R.string.cat_desc_productivity
            Category.CAT_SHOPPING -> R.string.cat_desc_shopping
            Category.CAT_GAMES -> R.string.cat_desc_games
            Category.CAT_PHOTOGRAPHY -> R.string.cat_desc_photography
            Category.CAT_UTILITIES, Category.CAT_AUTO, Category.CAT_WEATHER -> R.string.cat_desc_utilities
            Category.CAT_EDUCATION, Category.CAT_BOOKS -> R.string.cat_desc_education
            Category.CAT_HEALTH, Category.CAT_SPORTS -> R.string.cat_desc_health
            Category.CAT_NEWS -> R.string.cat_desc_news
            Category.CAT_TRAVEL, Category.CAT_MAPS, Category.CAT_FOOD -> R.string.cat_desc_travel
            else -> R.string.cat_desc_default
        }
    }
}
