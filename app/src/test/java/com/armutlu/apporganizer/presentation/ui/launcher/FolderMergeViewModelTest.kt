package com.armutlu.apporganizer.presentation.ui.launcher

import com.armutlu.apporganizer.domain.models.AppInfo
import com.armutlu.apporganizer.domain.models.Category
import com.armutlu.apporganizer.domain.usecase.folder.FolderMergeCandidateScorer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull

class FolderMergeViewModelTest {

    private lateinit var viewModel: FolderMergeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        viewModel = FolderMergeViewModel()
    }

    @Test
    fun testLoadSuggestions_loadsAndEmptyByDefault() = runTest(testDispatcher) {
        val apps = listOf(
            AppInfo(packageName = "com.app.one", appName = "One", categoryId = "games"),
            AppInfo(packageName = "com.app.two", appName = "Two", categoryId = "games"),
        )
        val categories = listOf(
            Category(categoryId = "games", categoryName = "Oyunlar"),
            Category(categoryId = "entertainment", categoryName = "Eğlence"),
        )

        viewModel.loadSuggestions(apps, categories)

        val state = viewModel.uiState.value
        assertEquals(null, state.selectedSuggestionId)
        assertEquals(0, state.selectedAppsToMove.size)
    }

    @Test
    fun testToggleAppSelection_addsAndRemoves() = runTest(testDispatcher) {
        viewModel.toggleAppSelection("com.app.one")
        var state = viewModel.uiState.value
        assertEquals(1, state.selectedAppsToMove.size)

        viewModel.toggleAppSelection("com.app.one")
        state = viewModel.uiState.value
        assertEquals(0, state.selectedAppsToMove.size)
    }

    @Test
    fun testSelectTargetFolder_setsTargetId() = runTest(testDispatcher) {
        viewModel.selectTargetFolder("entertainment")
        val state = viewModel.uiState.value
        assertEquals("entertainment", state.targetFolderId)
    }

    @Test
    fun testClearError_removesError() = runTest(testDispatcher) {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testRejectMerge_clearsAllState() = runTest(testDispatcher) {
        viewModel.toggleAppSelection("com.app.one")
        viewModel.selectTargetFolder("entertainment")

        viewModel.rejectMerge()

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedAppsToMove.size)
        assertNull(state.targetFolderId)
        assertNull(state.mergePlan)
    }
}
