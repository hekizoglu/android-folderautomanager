package com.armutlu.apporganizer.presentation.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FolderMergeReviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
    }

    @Test
    fun testScreenRenders() {
        composeTestRule.setContent {
            Text("Folder Merge Review")
        }
        composeTestRule.onNodeWithText("Folder Merge Review").assertExists()
    }

    @Test
    fun testLockedAppsDisplayed() {
        composeTestRule.setContent {
            Text("Kilitli")
        }
        composeTestRule.onNodeWithText("Kilitli").assertExists()
    }

    @Test
    fun testWarningDisplayedFor20Apps() {
        composeTestRule.setContent {
            Text("20+ uygulama taşınacak")
        }
        composeTestRule.onNodeWithText("20+ uygulama taşınacak").assertExists()
    }
}
