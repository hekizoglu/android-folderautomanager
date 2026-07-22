package com.armutlu.apporganizer.domain.usecase

import android.icu.text.Collator
import java.util.Locale

object TurkishCategorySorter {
    private val turkishCollator: Collator by lazy {
        Collator.getInstance(Locale("tr", "TR"))
    }
    private val englishCollator: Collator by lazy {
        Collator.getInstance(Locale.ENGLISH)
    }

    fun getCollator(locale: Locale): Collator = when {
        locale.language == "tr" -> turkishCollator
        locale.language == "en" -> englishCollator
        else -> englishCollator
    }

    fun sort(items: List<String>, locale: Locale = Locale.getDefault()): List<String> {
        val collator = getCollator(locale)
        return items.sortedWith { a, b -> collator.compare(a, b) }
    }

    fun <T> sortBy(items: List<T>, selector: (T) -> String, locale: Locale = Locale.getDefault()): List<T> {
        val collator = getCollator(locale)
        return items.sortedWith { a, b -> collator.compare(selector(a), selector(b)) }
    }
}
