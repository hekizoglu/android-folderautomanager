package com.armutlu.apporganizer.di

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.ContactsIndexer
import com.armutlu.apporganizer.data.local.FilesIndexer
import com.armutlu.apporganizer.data.local.SearchDao
import com.armutlu.apporganizer.data.local.SearchIndexer
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    // Dongu H01 — tek zaman/hafta sinir kaynagi. Sistem saatine/saat dilimine gore sabitlenir;
    // testlerde PeriodBoundaryResolver dogrudan sabit Clock ile ornekleniyor, bu provider'lar
    // uretimde kullanilir.
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideZoneId(): ZoneId = ZoneId.systemDefault()

    @Provides
    @Singleton
    fun providePeriodBoundaryResolver(clock: Clock, zoneId: ZoneId): PeriodBoundaryResolver =
        PeriodBoundaryResolver(clock, zoneId)

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.appDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideSearchDao(db: AppDatabase): SearchDao = db.searchDao()

    @Provides
    fun provideNotificationEventDao(db: AppDatabase): com.armutlu.apporganizer.data.local.NotificationEventDao =
        db.notificationEventDao()

    @Provides
    fun provideWeeklyGoalDao(db: AppDatabase): com.armutlu.apporganizer.data.local.WeeklyGoalDao =
        db.weeklyGoalDao()

    @Provides
    fun provideMissionHistoryDao(db: AppDatabase): com.armutlu.apporganizer.data.local.MissionHistoryDao =
        db.missionHistoryDao()

    @Provides
    fun provideTaskScoreEventDao(db: AppDatabase): com.armutlu.apporganizer.data.local.TaskScoreEventDao =
        db.taskScoreEventDao()

    @Provides
    @Singleton
    fun provideFilesIndexer(
        @ApplicationContext context: Context,
        searchDao: SearchDao
    ): FilesIndexer = FilesIndexer(context, searchDao)

    @Provides
    @Singleton
    fun provideSearchRepository(
        @ApplicationContext context: Context,
        searchDao: SearchDao,
        appDao: AppDao,
        categoryDao: CategoryDao,
        indexer: SearchIndexer,
        contactsIndexer: ContactsIndexer,
        filesIndexer: FilesIndexer,
        db: AppDatabase
    ): SearchRepository = SearchRepository(context, searchDao, appDao, categoryDao, indexer, contactsIndexer, filesIndexer, db)

    @Provides
    @Singleton
    fun provideAppDatabaseService(@ApplicationContext context: Context): AppDatabaseService {
        return AppDatabaseService(context).also { it.loadFromCacheSync() }
    }

}
