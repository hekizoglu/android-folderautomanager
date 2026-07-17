package com.armutlu.apporganizer.domain.home

import javax.inject.Qualifier

/**
 * Döngü H02 — HomeIntelligenceCoordinator'ın IO dispatcher bağımlılığı için qualifier.
 * Projede henüz genel bir DispatcherProvider/qualifier standardı yok; bu qualifier
 * sadece domain.home paketine özel, ileride genel bir standarda taşınabilir.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HomeIoDispatcher
