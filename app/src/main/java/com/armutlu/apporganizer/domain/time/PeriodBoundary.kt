package com.armutlu.apporganizer.domain.time

/**
 * Tek bir yerel gun veya ISO hafta doneminin sinirlarini tasir (Dongu H01).
 *
 * @property startInclusive donem baslangici, epoch millis (dahil)
 * @property endExclusive donem bitisi, epoch millis (haric) — bir sonraki donemin startInclusive'i
 * @property epochDay donemin temsil ettigi yerel gun ([java.time.LocalDate.toEpochDay])
 *   (haftalik donemlerde hafta baslangicinin, yani Pazartesi'nin, epochDay'i)
 * @property weekStartEpochDay gunluk donemlerde null; haftalik donemlerde hafta baslangici
 *   (Pazartesi) icin [java.time.LocalDate.toEpochDay] degeri — [epochDay] ile ayni
 */
data class PeriodBoundary(
    val startInclusive: Long,
    val endExclusive: Long,
    val epochDay: Long,
    val weekStartEpochDay: Long?,
)
