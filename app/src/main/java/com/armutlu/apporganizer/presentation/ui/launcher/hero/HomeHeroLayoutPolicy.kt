package com.armutlu.apporganizer.presentation.ui.launcher.hero

/**
 * Ekran ölçülerini Hero şartnamesindeki sabit profillere dönüştürür. Saf Kotlin'dir; UI
 * katmanı bu sonucu dp/sp değerlerine çevirir. Böylece breakpoint kararları composable'lara
 * dağılmaz ve 320x568 dahil küçük ekranlar kontrollü biçimde küçülür.
 */
internal object HomeHeroLayoutPolicy {
    private const val TABLET_MIN_WIDTH_DP = 600
    private const val LARGE_PHONE_MIN_WIDTH_DP = 400
    private const val COMPACT_PHONE_MAX_HEIGHT_DP = 599
    private const val LARGE_FONT_SCALE = 1.30f

    fun resolve(
        screenWidthDp: Int,
        screenHeightDp: Int,
        fontScale: Float,
    ): HomeHeroLayoutSpec {
        require(screenWidthDp > 0) { "screenWidthDp must be positive" }
        require(screenHeightDp > 0) { "screenHeightDp must be positive" }
        require(fontScale > 0f) { "fontScale must be positive" }

        val landscape = screenWidthDp > screenHeightDp
        val profile = when {
            fontScale >= LARGE_FONT_SCALE -> HomeHeroProfile.ACCESSIBLE
            landscape -> HomeHeroProfile.LANDSCAPE
            screenWidthDp >= TABLET_MIN_WIDTH_DP -> HomeHeroProfile.TABLET
            screenHeightDp <= COMPACT_PHONE_MAX_HEIGHT_DP || screenWidthDp < 340 ->
                HomeHeroProfile.COMPACT_PHONE
            screenWidthDp >= LARGE_PHONE_MIN_WIDTH_DP -> HomeHeroProfile.LARGE_PHONE
            else -> HomeHeroProfile.PHONE
        }

        return when (profile) {
            HomeHeroProfile.COMPACT_PHONE -> HomeHeroLayoutSpec(
                profile = profile,
                contentMaxWidthDp = minOf(304, screenWidthDp - 32),
                horizontalPaddingDp = 16,
                clockHeightDp = 96,
                digitalLifeHeightDp = 84,
                searchHeightDp = 64,
                smartAccessHeightDp = 162,
                dockHeightDp = 58,
                clockTextSizeSp = 64,
                scrollEnabled = true,
            )
            HomeHeroProfile.PHONE -> referencePhone(profile)
            HomeHeroProfile.LARGE_PHONE -> referencePhone(profile).copy(
                contentMaxWidthDp = 328,
            )
            HomeHeroProfile.TABLET -> referencePhone(profile).copy(
                contentMaxWidthDp = 420,
            )
            HomeHeroProfile.LANDSCAPE -> HomeHeroLayoutSpec(
                profile = profile,
                contentMaxWidthDp = minOf(720, screenWidthDp - 48),
                horizontalPaddingDp = 24,
                clockHeightDp = 104,
                digitalLifeHeightDp = 88,
                searchHeightDp = 68,
                smartAccessHeightDp = 162,
                dockHeightDp = 60,
                clockTextSizeSp = 68,
                scrollEnabled = true,
            )
            HomeHeroProfile.ACCESSIBLE -> referencePhone(profile).copy(
                contentMaxWidthDp = minOf(420, screenWidthDp - 32),
                horizontalPaddingDp = 16,
                clockHeightDp = 126,
                digitalLifeHeightDp = 116,
                searchHeightDp = 82,
                smartAccessHeightDp = 196,
                dockHeightDp = 76,
                clockTextSizeSp = 64,
                scrollEnabled = true,
            )
        }
    }

    private fun referencePhone(profile: HomeHeroProfile) = HomeHeroLayoutSpec(
        profile = profile,
        contentMaxWidthDp = 304,
        horizontalPaddingDp = 28,
        clockHeightDp = 114,
        digitalLifeHeightDp = 96,
        searchHeightDp = 74,
        smartAccessHeightDp = 162,
        dockHeightDp = 64,
        clockTextSizeSp = 76,
        scrollEnabled = false,
    )
}
