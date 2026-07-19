package com.armutlu.apporganizer.domain.usecase.missions

/**
 * Dongu G6 — Yildiz Ekonomisi kozmetik acilim: seviyeyle acilan YENI klasor emoji setleri.
 * Saf Kotlin — Android bagimliligi yok, unit test edilebilir.
 *
 * KIRMIZI CIZGI (plan G6): mevcut hicbir emoji/set kilitlenmez — [EMOJI_PICKER]
 * (FolderRenameDialog.kt) HERKESE her zaman acik kalir. Burada tanimlanan setler SADECE
 * YENI eklenen, ekstra kozmetik secenekler; kilit durumu yalniz bunlar icin gecerlidir.
 */
object FolderEmojiSets {

    /** Seviyeyle acilan bir emoji seti. */
    data class UnlockableSet(
        val id: String,
        val nameRes: Int,
        val emojis: List<String>,
        val requiredLevel: StarLevelSystem.Level,
    )

    /**
     * Doğa seti — StarLevelSystem.Level.FOCUSED (25 ⭐) seviyesinde açılır (plan G6 örneği).
     * Uzay seti — StarLevelSystem.Level.BALANCE_MASTER (50 ⭐) seviyesinde açılır.
     */
    val SETS: List<UnlockableSet> = listOf(
        UnlockableSet(
            id = "nature",
            nameRes = com.armutlu.apporganizer.R.string.folder_emoji_set_nature,
            emojis = listOf("🌲", "🌸", "🍃", "🌊", "🌻", "🦋", "🍄", "🌵"),
            requiredLevel = StarLevelSystem.Level.FOCUSED,
        ),
        UnlockableSet(
            id = "space",
            nameRes = com.armutlu.apporganizer.R.string.folder_emoji_set_space,
            emojis = listOf("🚀", "🪐", "🌌", "🛰️", "👽", "🌠", "🌍", "☄️"),
            requiredLevel = StarLevelSystem.Level.BALANCE_MASTER,
        ),
    )

    /** [totalStars] ile bu set açıldı mı — kullanıcının mevcut seviyesi >= gereken seviye. */
    fun isUnlocked(set: UnlockableSet, totalStars: Int): Boolean =
        totalStars >= set.requiredLevel.minStars

    /** Kilitliyse gösterilecek bilgi metnine gerekli veri: hedef seviye adı + eşik ⭐. */
    fun lockInfo(set: UnlockableSet): Pair<StarLevelSystem.Level, Int> =
        set.requiredLevel to set.requiredLevel.minStars
}
