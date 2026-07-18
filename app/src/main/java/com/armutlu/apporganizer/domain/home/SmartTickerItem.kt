package com.armutlu.apporganizer.domain.home

/**
 * Döngü T01 — Ana ekran haber şeridinin (ticker) tipli davranış modeli.
 * Roadmap §3.3 (ANA_EKRAN_AKILLI_NABIZ_GOREVLER_DIJITAL_YASAM_ROADMAP.md satır 1597-1650):
 * eskiden [com.armutlu.apporganizer.utils.TickerSpec] sadece metin + priority taşıyordu;
 * bu model tür, süre (expiresAt) ve davranışı (action) açık şekilde taşır.
 *
 * [TickerComposer] bu tipi üretir; [HomeTickerRow] bu tipi DOĞRUDAN tüketir (Döngü T04 —
 * eski [com.armutlu.apporganizer.presentation.ui.launcher.TickerItem] köprüsü kaldırıldı).
 */
data class SmartTickerItem(
    val id: String,
    val type: SmartTickerType,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val priority: Int,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val action: TickerAction = TickerAction.None,
    val suggestionKey: String? = null,
    val autoAdvanceAllowed: Boolean = true,
    val sensitive: Boolean = false,
) {
    /** Dismiss/rotasyon takibi için kararlı kimlik — eski TickerItem.key ile aynı davranış (D226). */
    val dedupeKey: String get() = suggestionKey ?: id

    /** [expiresAt] tanımlıysa ve geçmişse öğe artık gösterilmemeli. */
    fun isExpired(nowMillis: Long): Boolean = expiresAt != null && nowMillis >= expiresAt
}

/**
 * Ticker öğesinin türü — roadmap §Döngü T01'de tanımlanan sabit liste. Mevcut 6 üretici
 * (bildirim özeti, unutulan uygulama, içgörü, düşük güven/sınıflandırma incelemesi, özellik
 * ipucu, haftalık özet) bu türlere eşlenir.
 */
enum class SmartTickerType {
    /** Kritik sağlık/izin sorunu — ör. gelecekte izin eksikliği uyarıları. */
    CRITICAL_HEALTH,

    /** Bugün çözülebilen somut eylem — ör. bildirim özeti, düşük güvenli sınıflandırma incelemesi. */
    ACTION_REQUIRED,

    /** Devam eden görev ilerlemesi. */
    MISSION_PROGRESS,

    /** Tamamlanan görev/başarı bildirimi. */
    MISSION_ACHIEVEMENT,

    /** Dijital Nabız skorunda anlamlı değişim. */
    PULSE_CHANGE,

    /** Bağlamsal öneri — ör. unutulan uygulama, içgörü kartı. */
    CONTEXTUAL_SUGGESTION,

    /** Haftalık/Wrapped rapor hazır bildirimi. */
    WEEKLY_REPORT,

    /** Özellik keşfi/ipucu. */
    FEATURE_DISCOVERY,
}
