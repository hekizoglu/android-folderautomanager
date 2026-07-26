package com.armutlu.apporganizer.di

import com.armutlu.apporganizer.data.repository.InMemorySmartNotificationRepository
import com.armutlu.apporganizer.data.repository.SmartNotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmartNotificationModule {

    @Binds
    @Singleton
    abstract fun bindSmartNotificationRepository(
        implementation: InMemorySmartNotificationRepository,
    ): SmartNotificationRepository
}
