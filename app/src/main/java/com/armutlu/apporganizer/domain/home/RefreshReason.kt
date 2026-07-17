package com.armutlu.apporganizer.domain.home

/**
 * HomeIntelligenceCoordinator.refresh() çağrısının tetikleyicisi.
 * Döngü H02 — sadece orkestrasyon amaçlı; ileride log/telemetri ve
 * kaynak bazlı refresh optimizasyonu için kullanılabilir.
 */
enum class RefreshReason {
    APP_START,
    HOME_RESUME,
    MISSION_EVENT,
    NOTIFICATION_EVENT,
    APP_CATALOG_CHANGED,
    MANUAL_REFRESH,
}
