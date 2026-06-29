package com.armutlu.apporganizer.di

import android.content.Context
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.local.SearchDao
import com.armutlu.apporganizer.data.local.SearchIndexer
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.data.repository.SearchRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.appDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideSearchDao(db: AppDatabase): SearchDao = db.searchDao()

    @Provides
    @Singleton
    fun provideSearchRepository(
        @ApplicationContext context: Context,
        searchDao: SearchDao,
        appDao: AppDao,
        categoryDao: CategoryDao,
        indexer: SearchIndexer
    ): SearchRepository = SearchRepository(context, searchDao, appDao, categoryDao, indexer)

    @Provides
    @Singleton
    fun provideAppDatabaseService(@ApplicationContext context: Context): AppDatabaseService {
        return AppDatabaseService(context).also { it.loadFromCacheSync() }
    }

}
