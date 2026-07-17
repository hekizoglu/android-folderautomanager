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
import com.armutlu.apporganizer.domain.common.DataFreshnessResolver
import com.armutlu.apporganizer.domain.home.DigitalPulseRepository
import com.armutlu.apporganizer.domain.home.HomeIoDispatcher
import com.armutlu.apporganizer.domain.home.MissionRuntimeRepository
import com.armutlu.apporganizer.domain.home.NoOpDigitalPulseSource
import com.armutlu.apporganizer.domain.home.NoOpSmartTickerSource
import com.armutlu.apporganizer.domain.home.RealMissionRuntimeSource
import com.armutlu.apporganizer.domain.home.SmartTickerEngine
import com.armutlu.apporganizer.domain.time.PeriodBoundaryResolver
import com.armutlu.apporganizer.domain.usecase.missions.DefaultMissionUsageStatsSource
import com.armutlu.apporganizer.domain.usecase.missions.MissionSettlementTransactionRunner
import com.armutlu.apporganizer.domain.usecase.missions.MissionUsageStatsSource
import androidx.room.withTransaction
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

    // Dongu H03 — gorev/skor/serit bilesenlerinin ortak tazelik dili. Ayni Clock provider'i
    // (satir 47) kullanilir; testlerde DataFreshnessResolver dogrudan sabit Clock ile ornekleniyor.
    @Provides
    @Singleton
    fun provideDataFreshnessResolver(clock: Clock): DataFreshnessResolver =
        DataFreshnessResolver(clock)

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
    fun provideMissionInstanceDao(db: AppDatabase): com.armutlu.apporganizer.data.local.MissionInstanceDao =
        db.missionInstanceDao()

    // Dongu M02 — MissionMetricSnapshotProvider'in UsageStatsHelper cagrilarini soyutlayan
    // kaynak; uretimde gercek Android API'sine delege eder (bkz. MissionUsageStatsSource.kt).
    @Provides
    @Singleton
    fun provideMissionUsageStatsSource(): MissionUsageStatsSource = DefaultMissionUsageStatsSource()

    // Dongu M04 — SettleMissionInstancesUseCase'in odul+status yazimini Room withTransaction
    // uzerinden atomik calistiran uretim implementasyonu. Testlerde gercek Room kurmadan
    // basit bir pass-through ile degistirilir (bkz. SettleMissionInstancesUseCaseTest).
    @Provides
    @Singleton
    fun provideMissionSettlementTransactionRunner(db: AppDatabase): MissionSettlementTransactionRunner =
        object : MissionSettlementTransactionRunner {
            override suspend fun <T> runInTransaction(block: suspend () -> T): T =
                db.withTransaction { block() }
        }

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

    // Dongu H02 — HomeIntelligenceCoordinator kaynak interface'leri icin gecici no-op binding'ler.
    // D00/M/T dongulerinde gercek DigitalPulseRepository/MissionRuntimeRepository/SmartTickerEngine
    // implementasyonlari bu provider'lari degistirecek.
    @Provides
    @Singleton
    fun provideDigitalPulseRepository(impl: NoOpDigitalPulseSource): DigitalPulseRepository = impl

    // Dongu M07 — gercek implementasyona baglandi (bkz. RealMissionRuntimeSource).
    @Provides
    @Singleton
    fun provideMissionRuntimeRepository(impl: RealMissionRuntimeSource): MissionRuntimeRepository = impl

    @Provides
    @Singleton
    fun provideSmartTickerEngine(impl: NoOpSmartTickerSource): SmartTickerEngine = impl

    @Provides
    @HomeIoDispatcher
    fun provideHomeIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

}
