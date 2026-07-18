package com.armutlu.apporganizer.presentation.ui.launcher.model

/**
 * Ana ekranda "son görüntülenen sayfa" bilgisinin SEMANTİK temsili — ham `Int` page index
 * yerine hangi sayfa TÜRÜ/kimliği görüntülendiğini saklar. Klasör sayısı değişince veya
 * Dashboard eklenince/kaldırılınca kullanıcı yanlış sayfaya düşmesin diye [HomePageSpec.stableKey]
 * ile uyumlu bir kimlik taşır.
 *
 * Roadmap: ANA_EKRAN_DASHBOARD_GLOBAL_ARAMA_KLASOR_SAYFALARI_ROADMAP.md bölüm 3.1 (HomePageSpec),
 * Döngü P02 (satır 449-501).
 *
 * Serileştirme (SharedPreferences string olarak saklanır — [serialize]/[deserialize]):
 * - Dashboard      -> "dashboard"
 * - Folder(id)     -> "folder:<categoryId>"
 * - PageIndex(idx) -> "index:<idx>"
 */
sealed interface HomePageAnchor {

    data object Dashboard : HomePageAnchor

    data class Folder(val categoryId: String) : HomePageAnchor

    /** Klasör kimliği bilinmiyorsa (eski migration veya bozuk veri) ham index'e düşen fallback. */
    data class PageIndex(val index: Int) : HomePageAnchor

    companion object {
        private const val PREFIX_FOLDER = "folder:"
        private const val PREFIX_INDEX = "index:"
        private const val VALUE_DASHBOARD = "dashboard"

        fun serialize(anchor: HomePageAnchor): String = when (anchor) {
            is Dashboard -> VALUE_DASHBOARD
            is Folder -> "$PREFIX_FOLDER${anchor.categoryId}"
            is PageIndex -> "$PREFIX_INDEX${anchor.index}"
        }

        /**
         * Bozuk/tanınmayan/boş değer güvenle `null` döner — çağıran taraf migration veya
         * varsayılan (Dashboard) fallback uygulamalıdır. Asla exception fırlatmaz.
         */
        fun deserialize(raw: String?): HomePageAnchor? {
            if (raw.isNullOrBlank()) return null
            return when {
                raw == VALUE_DASHBOARD -> Dashboard
                raw.startsWith(PREFIX_FOLDER) -> {
                    val id = raw.removePrefix(PREFIX_FOLDER)
                    if (id.isBlank()) null else Folder(id)
                }
                raw.startsWith(PREFIX_INDEX) -> {
                    raw.removePrefix(PREFIX_INDEX).toIntOrNull()?.let { PageIndex(it) }
                }
                else -> null
            }
        }
    }
}
