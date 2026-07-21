package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessTab
import com.armutlu.apporganizer.domain.home.smartaccess.NotificationAccessItem
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState
import com.armutlu.apporganizer.domain.models.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HeroDashboardInteractionTest {
    @get:Rule val compose = createComposeRule()

    private val spec = HomeHeroLayoutPolicy.resolve(360, 640, 1f)

    @Test fun hero_cards_share_one_adaptive_width() {
        compose.setContent {
            Box(Modifier.size(width = 360.dp, height = 640.dp)) {
                HeroDashboardPage(
                    pulse = null,
                    smartAccess = SmartAccessUiState(),
                    onOpenWeeklyReport = {},
                    onClockLongPress = {},
                    onOpenPulse = {},
                    onOpenSearch = {},
                    onOpenSearchSettings = {},
                    onOpenUsageAccessSettings = {},
                    onOpenNotificationAccessSettings = {},
                    onLaunchApp = {},
                    onAppLongClick = {},
                )
            }
        }
        val widths = listOf(
            "hero_clock_card",
            "hero_digital_life_card",
            "hero_search_card",
            "hero_smart_access_card",
        ).map { tag -> compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.width }
        assertTrue(widths.first() > 0f)
        widths.drop(1).forEach { width -> assertEquals(widths.first(), width, 0.5f) }
    }

    @Test fun search_card_routes_primary_and_source_actions() {
        var searchClicks = 0
        var sourceClicks = 0
        compose.setContent {
            HeroSearchCard(
                spec = spec,
                onOpenSearch = { searchClicks++ },
                onOpenSources = { sourceClicks++ },
            )
        }
        compose.onNodeWithTag("hero_search_card").performClick()
        compose.onNodeWithTag("hero_search_sources").performClick()
        compose.runOnIdle {
            assertEquals(1, searchClicks)
            assertEquals(1, sourceClicks)
        }
    }

    @Test fun clock_supports_click_and_long_click() {
        var clicks = 0
        var longClicks = 0
        compose.setContent {
            HeroClockCard(spec, { clicks++ }, { longClicks++ })
        }
        compose.onNodeWithTag("hero_clock_card").performClick()
        compose.onNodeWithTag("hero_clock_card").performTouchInput { longClick() }
        compose.runOnIdle {
            assertEquals(1, clicks)
            assertEquals(1, longClicks)
        }
    }

    @Test fun smart_access_tab_selection_is_exposed_to_semantics() {
        compose.setContent {
            SmartAccessCard(
                state = SmartAccessUiState(usagePermissionGranted = true),
                spec = spec,
                selectedTab = SmartAccessTab.RECENT,
                onTabSelected = {},
                onOpenUsageSettings = {},
                onOpenNotificationSettings = {},
                onLaunchApp = {},
                onAppLongClick = {},
            )
        }
        compose.onNodeWithTag("hero_smart_tab_recent").assertIsSelected()
        compose.onNodeWithTag("hero_smart_access_settings").assertHeightIsAtLeast(48.dp)
    }

    @Test fun notification_permission_empty_state_opens_notification_settings() {
        var usageClicks = 0
        var notificationClicks = 0
        compose.setContent {
            SmartAccessCard(
                state = SmartAccessUiState(
                    usagePermissionGranted = true,
                    notificationPermissionGranted = false,
                ),
                spec = spec,
                selectedTab = SmartAccessTab.NOTIFICATIONS,
                onTabSelected = {},
                onOpenUsageSettings = { usageClicks++ },
                onOpenNotificationSettings = { notificationClicks++ },
                onLaunchApp = {},
                onAppLongClick = {},
            )
        }
        compose.onNodeWithTag("hero_smart_access_empty_action").performClick()
        compose.runOnIdle {
            assertEquals(0, usageClicks)
            assertEquals(1, notificationClicks)
        }
    }

    @Test fun loading_state_does_not_expose_a_permission_action() {
        compose.setContent {
            SmartAccessCard(
                state = SmartAccessUiState(loading = true),
                spec = spec,
                selectedTab = SmartAccessTab.NOW,
                onTabSelected = {},
                onOpenUsageSettings = {},
                onOpenNotificationSettings = {},
                onLaunchApp = {},
                onAppLongClick = {},
            )
        }
        compose.onNodeWithTag("hero_smart_access_empty_action").assertHasNoClickAction()
    }

    @Test fun notification_tab_draws_only_the_authoritative_smart_access_badge() {
        val app = AppInfo(
            packageName = "com.example.notifications",
            appName = "Notifications",
            notificationCount = 3,
        )
        compose.setContent {
            SmartAccessCard(
                state = SmartAccessUiState(
                    notificationApps = listOf(NotificationAccessItem(app, count = 7, lastPostedAt = 1L)),
                    notificationPermissionGranted = true,
                    loading = false,
                ),
                spec = spec,
                selectedTab = SmartAccessTab.NOTIFICATIONS,
                onTabSelected = {},
                onOpenUsageSettings = {},
                onOpenNotificationSettings = {},
                onLaunchApp = {},
                onAppLongClick = {},
            )
        }

        compose.onNodeWithTag("smart_access_notification_badge_${app.packageName}").assertExists()
        compose.onNodeWithTag("app_notification_badge_${app.packageName}").assertDoesNotExist()
    }

    @Test fun empty_dock_click_opens_editor_instead_of_being_a_no_op() {
        var editClicks = 0
        compose.setContent {
            HeroDock(
                packages = emptyList(),
                appsByPackage = emptyMap(),
                onLaunchApp = {},
                onAppLongClick = {},
                onEditDock = { editClicks++ },
            )
        }
        compose.onNodeWithTag("hero_dock").performClick()
        compose.runOnIdle { assertEquals(1, editClicks) }
    }
}
