package com.armutlu.apporganizer.domain.models

/**
 * Cihaz içi akıllı bildirim sınıflandırmasının kullanıcıya dönük kategorileri.
 *
 * UI rengi veya Compose bağımlılığı özellikle burada tutulmaz; domain katmanı Android/Compose
 * ayrıntılarından bağımsız kalır. [defaultImportance] yalnızca sınıflandırıcı için başlangıç
 * puanıdır, nihai skor içerik ve sistem önceliğiyle yeniden hesaplanır.
 */
enum class NotificationCategory(
    val defaultImportance: Int,
    val suppressible: Boolean = false,
) {
    MESSAGING(defaultImportance = 60),
    DELIVERY(defaultImportance = 58),
    FINANCE(defaultImportance = 78),
    PROMOTION(defaultImportance = 15, suppressible = true),
    REMINDER(defaultImportance = 64),
    SOCIAL(defaultImportance = 40),
    SYSTEM(defaultImportance = 50),
    OTHER(defaultImportance = 35),
}
