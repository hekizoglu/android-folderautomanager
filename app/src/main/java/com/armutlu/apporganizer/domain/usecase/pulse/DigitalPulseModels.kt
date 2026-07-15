package com.armutlu.apporganizer.domain.usecase.pulse

import com.armutlu.apporganizer.domain.usecase.wrapped.WrappedEngine

/**
 * Dijital Nabız V2 modelleri — saf Kotlin, Android bağımlılığı yok.
 * Tüm metinler UI katmanında string resource ile çözülür; buradaki logLabel yalnızca
 * log/AI-prompt amaçlı ASCII açıklamadır, kullanıcıya gösterilmez.
 */

enum class DataConfidence { LOW, MEDIUM, HIGH }

/** Skoru etkileyen (veya nötr bırakılan) sebepler — UI, id üzerinden resource çözer. */
enum class PulseReasonId(val logLabel: String) {
    ORGANIZATION_HIGH("Uygulamalarin buyuk kismi kategorilenmis"),
    ORGANIZATION_UNCATEGORIZED("Cok sayida kategorisiz uygulama var"),
    ATTENTION_CALM("Bildirim yuku sakin"),
    ATTENTION_NOISY("Bazi uygulamalar bildirimle rahatsiz ediyor"),
    ATTENTION_NIGHT("Gece bildirim orani yuksek"),
    ATTENTION_NO_PERMISSION("Bildirim erisimi yok, notr degerlendirildi"),
    BALANCE_STEADY("Kullanim dagilimi kendi normaline yakin"),
    BALANCE_SHIFT("Kategori dagiliminda sert degisim"),
    BALANCE_NO_BASELINE("Karsilastirma icin veri birikiyor"),
    CLEANUP_TIDY("Kullanilmayan uygulama orani dusuk"),
    CLEANUP_UNUSED("Uzun suredir acilmayan uygulamalar var"),
    CONSISTENCY_STEADY("Kullanim duzeni istikrarli"),
    CONSISTENCY_VOLATILE("Kullanim haftalar arasi cok degisken"),
    CONSISTENCY_NO_DATA("Kilit acma verisi yok, notr degerlendirildi"),
    TASK_MISSIONS("Gorev etkisi sinirli katki olarak uygulandi"),
}

/**
 * @param value sebeple ilgili sayısal bağlam (ör. kategorisiz uygulama sayısı) — UI metninde arg.
 * @param delta alt skora yaklaşık etkisi (pozitif/negatif/0-nötr) — renk ve işaret için.
 */
data class PulseScoreReason(
    val id: PulseReasonId,
    val value: Int = 0,
    val delta: Int = 0,
)

/** Dijital Nabız toplam + 5 alt skor (hepsi 0..100). */
data class DigitalPulseScore(
    val total: Int,
    val organization: Int,
    val attention: Int,
    val balance: Int,
    val cleanup: Int,
    val consistency: Int,
    val confidence: DataConfidence,
    val reasons: List<PulseScoreReason>,
)

/** Bildirim sinyalleri — null verilirse izin yok demektir (CEZALANDIRMA YOK, nötr + düşük güven). */
data class PulseNotificationSignals(
    val totalNotifications: Int,
    val disturbingCount: Int,
    val distractingCount: Int,
    val nightCount: Int = 0,
)

data class PulseInput(
    val apps: List<WrappedEngine.AppSnapshot>,
    val notification: PulseNotificationSignals?,
    val previousCategoryUsage: Map<String, Long>?, // null = ilk hafta (baseline yok)
    val folderCount: Int = 0,
    val unlockCount: Int? = null,
    val previousUnlockCount: Int? = null,
    val taskScoreContribution: Int = 0,
    val hasUsageAccess: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
)

/** İçgörü türleri — UI her tür için string resource eşler. */
enum class PulseInsightType {
    NOTIF_ISSUE,      // rahatsız eden bildirim sorunu (en yüksek öncelik)
    NOTIF_CALM,       // belirgin olumlu gelişme
    UNUSED_APPS,      // kullanılmayan uygulama önerisi
    CATEGORY_SHIFT,   // haftalık kategori değişimi
    UNLOCK_TREND,     // kilit açma trendi
    ORGANIZED_WELL,   // düzen başarısı
    GENERAL,          // genel bilgi (rapor hazır)
}

/**
 * Yapılandırılmış içgörü — metin İÇERMEZ; UI/ViewModel resource'tan çözer.
 * @param priority küçük sayı = yüksek öncelik.
 * @param positive true=olumlu, false=olumsuz, null=nötr bilgi.
 * @param routeKey dokunma hedefi ("WRAPPED_REPORT" | "NOTIFICATION_REPORT" | "USAGE_REPORT" | "REPORTS_CENTER").
 */
data class PulseInsightSpec(
    val id: String,
    val type: PulseInsightType,
    val priority: Int,
    val positive: Boolean?,
    val routeKey: String?,
    val intArg: Int? = null,
    val textArg: String? = null,
)
