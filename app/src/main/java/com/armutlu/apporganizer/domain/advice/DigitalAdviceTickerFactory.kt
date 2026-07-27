package com.armutlu.apporganizer.domain.advice

import com.armutlu.apporganizer.domain.home.SmartTickerItem
import com.armutlu.apporganizer.domain.home.SmartTickerType
import com.armutlu.apporganizer.domain.home.TickerAction

/**
 * P9 — [DigitalAdvice] → [SmartTickerItem] (roadmap §9/§11.3): tavsiye Ana ekran ticker'ında
 * [SmartTickerType.CONTEXTUAL_SUGGESTION] tipinde aday olarak sunulabilir. `suggestionKey`
 * [DigitalAdvice.suggestionKey] ile BİREBİR AYNI tutulur — Görevler ekranındaki "Bugünün
 * Tavsiyesi" kartı ile Ticker aynı öğeyi aynı anahtar üzerinden suppress eder (roadmap §12,
 * §11.3 son paragraf: aynı tavsiye iki kanalda aynı anda görünmesin).
 *
 * Başlık/mesaj string kaynak ID'leri BURADA çözülmez ([RealSmartTickerSource] gibi çağıran
 * taraf `context.getString(...)` ile çözer) — bu fonksiyon yalnız [SmartTickerItem.title]/
 * [SmartTickerItem.subtitle] alanlarına HAZIR metin bekler (mevcut ticker üretici desenleriyle
 * aynı — [com.armutlu.apporganizer.domain.home.MissionPulseTickerFactory] de metinleri kendi
 * içinde üretiyor ama advice metinleri zaten string resource ID taşıdığından çözümleme çağıran
 * tarafa bırakılır).
 */
object DigitalAdviceTickerFactory {

    private const val MS_PER_DAY = 24L * 3600 * 1000

    fun candidate(advice: DigitalAdvice?, title: String, subtitle: String?, nowMillis: Long): List<SmartTickerItem> {
        if (advice == null) return emptyList()
        return listOf(
            SmartTickerItem(
                id = "advice_${advice.id}",
                type = SmartTickerType.CONTEXTUAL_SUGGESTION,
                title = title,
                subtitle = subtitle,
                icon = "💡",
                priority = 40,
                createdAt = nowMillis,
                expiresAt = advice.expiresAt ?: (nowMillis + MS_PER_DAY),
                action = advice.action.toTickerAction(),
                suggestionKey = advice.suggestionKey,
                sensitive = advice.sensitive,
            ),
        )
    }

    private fun DigitalAdviceAction.toTickerAction(): TickerAction = when (this) {
        DigitalAdviceAction.OpenCategoryGoals -> TickerAction.OpenDashboard
        DigitalAdviceAction.OpenMissions -> TickerAction.OpenMissions
        DigitalAdviceAction.OpenNotificationReport -> TickerAction.OpenNotificationReport
        DigitalAdviceAction.OpenUsageReport -> TickerAction.OpenUsageReport
        DigitalAdviceAction.OpenClassificationReview -> TickerAction.OpenClassificationReview
        DigitalAdviceAction.OpenFocusSettings -> TickerAction.OpenSettings()
        DigitalAdviceAction.OpenUsageAccessSettings -> TickerAction.OpenSettings()
        DigitalAdviceAction.None -> TickerAction.None
    }
}
