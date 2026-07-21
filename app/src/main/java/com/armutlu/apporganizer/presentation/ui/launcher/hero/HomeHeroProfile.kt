package com.armutlu.apporganizer.presentation.ui.launcher.hero

/** Hero Dashboard'ın cihaz ve erişilebilirlik profilleri. */
internal enum class HomeHeroProfile {
    COMPACT_PHONE,
    PHONE,
    LARGE_PHONE,
    TABLET,
    LANDSCAPE,
    ACCESSIBLE,
}

/** Compose dışı, JVM testinden doğrulanabilen yerleşim kararı. */
internal data class HomeHeroLayoutSpec(
    val profile: HomeHeroProfile,
    val contentMaxWidthDp: Int,
    val horizontalPaddingDp: Int,
    val clockWidthDp: Int,
    val clockHeightDp: Int,
    val digitalLifeHeightDp: Int,
    val searchHeightDp: Int,
    val smartAccessHeightDp: Int,
    val dockHeightDp: Int,
    val clockTextSizeSp: Int,
    val appSlots: Int = 5,
    val scrollEnabled: Boolean,
)
