package com.armutlu.apporganizer.di

import android.content.Context
import androidx.room.Room
import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.AppDatabase
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.domain.usecase.AppClassifier
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_organizer_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideAppDao(db: AppDatabase): AppDao = db.appDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideAppDatabaseService(@ApplicationContext context: Context): AppDatabaseService {
        return AppDatabaseService(context).also { it.loadFromCacheSync() }
    }

    @Provides
    @Singleton
    fun provideAppClassifier(appDatabaseService: AppDatabaseService): AppClassifier =
        AppClassifier(appDatabaseService)
}
