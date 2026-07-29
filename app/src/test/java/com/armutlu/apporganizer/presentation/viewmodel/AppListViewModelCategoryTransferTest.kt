package com.armutlu.apporganizer.presentation.viewmodel

import android.app.Application
import com.armutlu.apporganizer.data.remote.AppDatabaseService
import com.armutlu.apporganizer.data.remote.FetchResult
import com.armutlu.apporganizer.data.repository.AppRepository
import com.armutlu.apporganizer.data.repository.SearchRepository
import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.usecase.classify.AppClassifier
import com.armutlu.apporganizer.domain.usecase.classify.CategoryLLMFallback
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestion
import com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionEngine
import com.armutlu.apporganizer.utils.AppPrefs
import com.armutlu.apporganizer.utils.TaskScoreManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * FINDING-003 — toplu kategori taşıma DAO hatasını artık yutmuyor. Bu testler
 * AppListViewModel.updateAppsCategory/acceptSimilarCategorySuggestions/acceptFolderSuggestion
 * çağrılarının repository başarısız olduğunda AppPrefs override, arama indeksi,
 * dismissal/accepted-pattern ve puan yan etkilerini YAZMADIĞINI doğrular.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppListViewModelCategoryTransferTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockApplication: Application
    private lateinit var mockRepository: AppRepository
    private lateinit var mockSearchRepository: SearchRepository
    private lateinit var mockClassifier: AppClassifier
    private lateinit var mockLlmFallback: CategoryLLMFallback
    private lateinit var mockAppDatabaseService: AppDatabaseService

    private val appsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private lateinit var viewModel: AppListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockSearchRepository = mockk(relaxed = true)
        mockClassifier = mockk(relaxed = true)
        mockLlmFallback = mockk(relaxed = true)
        mockAppDatabaseService = mockk(relaxed = true)

        every { mockRepository.getAllAppsFlow() } returns appsFlow
        every { mockRepository.getAllCategoriesFlow() } returns flowOf(emptyList())
        coEvery { mockRepository.ensureDefaultCategories() } just Runs
        coEvery { mockAppDatabaseService.fetchAndCache() } returns FetchResult.NoCache

        mockkObject(AppPrefs)
        every { AppPrefs.isManualOverridesRoomMigrated(any()) } returns true
        every { AppPrefs.isShowSystemApps(any()) } returns false
        every { AppPrefs.isFolderSuggestionsEnabled(any()) } returns true
        every { AppPrefs.getSnoozedFolderSuggestions(any()) } returns emptyMap()
        every { AppPrefs.getDismissedFolderSuggestions(any()) } returns emptySet()
        every { AppPrefs.isFolderSuggestionsInfoDismissed(any()) } returns false
        every { AppPrefs.setManualCategoryOverride(any(), any(), any()) } just Runs
        every { AppPrefs.addAcceptedOverridePattern(any(), any(), any()) } just Runs
        every { AppPrefs.dismissFolderSuggestion(any(), any()) } just Runs

        mockkObject(TaskScoreManager)
        coEvery { TaskScoreManager.recordBulk(any(), any(), any()) } returns mockk(relaxed = true)

        mockkObject(FolderSuggestionEngine)
        every {
            FolderSuggestionEngine.generate(any(), any(), any(), any(), any())
        } returns emptyList()

        viewModel = AppListViewModel(
            application = mockApplication,
            repository = mockRepository,
            searchRepository = mockSearchRepository,
            classifier = mockClassifier,
            llmFallback = mockLlmFallback,
            appDatabaseService = mockAppDatabaseService,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(AppPrefs)
        unmockkObject(TaskScoreManager)
        unmockkObject(FolderSuggestionEngine)
        Dispatchers.resetMain()
    }

    // installTime/lastUpdated varsayılanı System.currentTimeMillis() — sabit değer vermezsek
    // aynı paket için iki app() çağrısı farklı AppInfo üretir ve mockk eq() eşleşmesi bozulur.
    private fun app(pkg: String, name: String, categoryId: String = "social") =
        AppInfo(packageName = pkg, appName = name, categoryId = categoryId, installTime = 0L, lastUpdated = 0L)

    // ── updateAppsCategory ───────────────────────────────────────────────────

    @Test
    fun `updateAppsCategory basarisizsa AppPrefs override yazilmaz`() = runTest {
        advanceUntilIdle()
        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")

        viewModel.updateAppsCategory(listOf("com.a", "com.b"), "games")
        advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.getAppByPackageName(any()) }
        io.mockk.verify(exactly = 0) { AppPrefs.setManualCategoryOverride(any(), any(), any()) }
        coVerify(exactly = 0) { mockSearchRepository.indexApp(any()) }
    }

    @Test
    fun `updateAppsCategory basarisizsa arama indeksi guncellenmez`() = runTest {
        advanceUntilIdle()
        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")

        viewModel.updateAppsCategory(listOf("com.a"), "games")
        advanceUntilIdle()

        coVerify(exactly = 0) { mockSearchRepository.indexApp(any()) }
    }

    @Test
    fun `updateAppsCategory basarisizsa secim temizlenmez`() = runTest {
        advanceUntilIdle()
        viewModel.toggleAppSelection("com.a")
        assertEquals(setOf("com.a"), viewModel.selectedApps.value)

        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")
        viewModel.updateAppsCategory(listOf("com.a"), "games")
        advanceUntilIdle()

        assertEquals(setOf("com.a"), viewModel.selectedApps.value)
    }

    @Test
    fun `updateAppsCategory basaridaysa tum yan etkiler repository yazisindan sonra calisir`() = runTest {
        advanceUntilIdle()
        coEvery { mockRepository.updateAppsCategory(listOf("com.a", "com.b"), "games") } just Runs
        coEvery { mockRepository.getAppByPackageName("com.a") } returns app("com.a", "A", "games")
        coEvery { mockRepository.getAppByPackageName("com.b") } returns app("com.b", "B", "games")

        viewModel.toggleAppSelection("com.a")
        viewModel.updateAppsCategory(listOf("com.a", "com.b"), "games")
        advanceUntilIdle()

        io.mockk.verify { AppPrefs.setManualCategoryOverride(any(), "com.a", "games") }
        io.mockk.verify { AppPrefs.setManualCategoryOverride(any(), "com.b", "games") }
        coVerify { mockSearchRepository.indexApp(app("com.a", "A", "games")) }
        coVerify { mockSearchRepository.indexApp(app("com.b", "B", "games")) }
        assertEquals(emptySet<String>(), viewModel.selectedApps.value)
    }

    // ── acceptSimilarCategorySuggestions ─────────────────────────────────────

    @Test
    fun `acceptSimilarCategorySuggestions basarisizsa accepted pattern ve TaskScore yazilmaz`() = runTest {
        advanceUntilIdle()
        seedSimilarSuggestions()
        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")

        viewModel.acceptSimilarCategorySuggestions(setOf("com.similar"))
        advanceUntilIdle()

        io.mockk.verify(exactly = 0) { AppPrefs.addAcceptedOverridePattern(any(), any(), any()) }
        coVerify(exactly = 0) { TaskScoreManager.recordBulk(any(), any(), any()) }
    }

    @Test
    fun `acceptSimilarCategorySuggestions basarisizsa oneri listesi korunur`() = runTest {
        advanceUntilIdle()
        seedSimilarSuggestions()
        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")

        viewModel.acceptSimilarCategorySuggestions(setOf("com.similar"))
        advanceUntilIdle()

        assertEquals(1, viewModel.suggestedSimilarApps.value.size)
        assertEquals("games", viewModel.suggestedSimilarCategoryId.value)
    }

    private fun seedSimilarSuggestions() {
        val field = AppListViewModel::class.java.getDeclaredField("_suggestedSimilarApps")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (field.get(viewModel) as MutableStateFlow<List<AppInfo>>).value =
            listOf(app("com.similar", "Similar", "social"))

        val categoryField = AppListViewModel::class.java.getDeclaredField("_suggestedSimilarCategoryId")
        categoryField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        (categoryField.get(viewModel) as MutableStateFlow<String?>).value = "games"
    }

    // ── acceptFolderSuggestion ────────────────────────────────────────────────

    @Test
    fun `acceptFolderSuggestion basarisizsa suggestion dismiss edilmez ve TaskScore yazilmaz`() = runTest {
        val suggestion = FolderSuggestion(
            id = "sugg-1",
            type = com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionType.CLEAN_UNUSED_APPS,
            title = "test",
            description = "test",
            packageNames = listOf("com.a"),
            targetCategoryId = "games",
            confidence = 80,
        )
        every {
            FolderSuggestionEngine.generate(any(), any(), any(), any(), any())
        } returns listOf(suggestion)
        coEvery { mockRepository.updateAppsCategory(any(), any()) } throws RuntimeException("dao error")

        // folderSuggestions StateFlow WhileSubscribed(5000) ile lazy — en az bir collector
        // olmadan upstream combine() hiç tetiklenmez ve .value her zaman emptyList() kalır.
        val collectJob = launch { viewModel.folderSuggestions.collect {} }
        advanceUntilIdle()
        viewModel.acceptFolderSuggestion(suggestion.id)
        advanceUntilIdle()

        io.mockk.verify(exactly = 0) { AppPrefs.dismissFolderSuggestion(any(), any()) }
        coVerify(exactly = 0) { TaskScoreManager.recordBulk(any(), any(), any()) }
        collectJob.cancel()
    }

    @Test
    fun `acceptFolderSuggestion basaridaysa dismiss ve puan repository yazisindan sonra calisir`() = runTest {
        val suggestion = FolderSuggestion(
            id = "sugg-2",
            type = com.armutlu.apporganizer.domain.usecase.folder.FolderSuggestionType.CLEAN_UNUSED_APPS,
            title = "test",
            description = "test",
            packageNames = listOf("com.a"),
            targetCategoryId = "games",
            confidence = 80,
        )
        every {
            FolderSuggestionEngine.generate(any(), any(), any(), any(), any())
        } returns listOf(suggestion)
        coEvery { mockRepository.updateAppsCategory(listOf("com.a"), "games") } just Runs
        coEvery { mockRepository.getAppByPackageName("com.a") } returns app("com.a", "A", "games")

        val collectJob = launch { viewModel.folderSuggestions.collect {} }
        advanceUntilIdle()
        viewModel.acceptFolderSuggestion(suggestion.id)
        advanceUntilIdle()

        io.mockk.verify { AppPrefs.dismissFolderSuggestion(any(), suggestion.id) }
        coVerify { TaskScoreManager.recordBulk(any(), any(), 1) }
        collectJob.cancel()
    }
}
