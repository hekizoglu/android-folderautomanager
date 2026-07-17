package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Yapilandirilmis metin referansi (Dongu M03) — UI'ya dogrudan Turkce string VERILMEZ,
 * resource id + argumanlar tasinir; cagiran taraf (ViewModel/Composable)
 * `context.getString(resId, *args.toTypedArray())` ile cozer.
 *
 * PulseInsightSpec (bkz. DigitalPulseModels.kt) ile ayni "metin icermeyen model" desenini izler.
 */
data class MissionTextSpec(
    val resId: Int,
    val args: List<Any> = emptyList(),
)
