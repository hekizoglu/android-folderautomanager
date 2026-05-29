package com.armutlu.apporganizer.data.repository

import com.armutlu.apporganizer.data.local.AppDao
import com.armutlu.apporganizer.data.local.CategoryDao
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.AppClassifier
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class AppRepositoryTest {
    
    @MockK
    private lateinit var appDao: AppDao
    
    @MockK
    private lateinit var categoryDao: CategoryDao
    
    @MockK
    private lateinit var classifier: AppClassifier
    
    private lateinit var repository: AppRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = AppRepository(appDao, categoryDao, classifier)
    }
    
    @Test
    fun testInsertAppsWithClassification() = runBlocking {
        val apps = listOf(
            AppInfo("com.facebook.katana", "Facebook"),
            AppInfo("com.minecraft", "Minecraft")
        )
        
        coEvery { classifier.classifyApp(any()) } returnsMany listOf("social", "games")
        coEvery { appDao.insertApps(any()) } returns Unit
        
        repository.insertApps(apps)
        
        coVerify { appDao.insertApps(any()) }
    }
    
    @Test
    fun testUpdateAppCategory() = runBlocking {
        coEvery { appDao.updateAppCategory(any(), any()) } returns Unit
        
        repository.updateAppCategory("com.test", "games")
        
        coVerify { appDao.updateAppCategory("com.test", "games") }
    }
    
    @Test
    fun testGetAppsByCategory() = runBlocking {
        val mockApps = listOf(
            AppInfo("com.app1", "App 1", "social"),
            AppInfo("com.app2", "App 2", "social")
        )
        
        coEvery { appDao.getAppsByCategory("social") } returns flowOf(mockApps)
        
        val result = repository.getAppsByCategory("social")
        assertNotNull(result)
    }
    
    @Test
    fun testCountApps() = runBlocking {
        coEvery { appDao.countApps() } returns 10
        
        val count = repository.countApps()
        
        assertEquals(10, count)
        coVerify { appDao.countApps() }
    }
    
    @Test
    fun testDeleteApp() = runBlocking {
        coEvery { appDao.deleteAppByPackageName(any()) } returns Unit
        
        repository.deleteApp("com.test.app")
        
        coVerify { appDao.deleteAppByPackageName("com.test.app") }
    }
    
    @Test
    fun testSyncInstalledApps() = runBlocking {
        val installedApps = listOf(
            AppInfo("com.app1", "App 1"),
            AppInfo("com.app2", "App 2")
        )
        
        coEvery { appDao.getAllPackageNames() } returns listOf("com.app1")
        coEvery { classifier.classifyApp(any()) } returns "games"
        coEvery { appDao.insertApps(any()) } returns Unit
        coEvery { appDao.deleteAppByPackageName(any()) } returns Unit
        
        repository.syncInstalledApps(installedApps)
        
        coVerify { appDao.getAllPackageNames() }
    }
    
    @Test
    fun testAppExists() = runBlocking {
        coEvery { appDao.appExists("com.test") } returns true
        
        val exists = repository.appExists("com.test")
        
        assertTrue(exists)
    }
}
