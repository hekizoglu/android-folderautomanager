package com.armutlu.apporganizer.presentation.ui.launcher.hero

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessTab
import com.armutlu.apporganizer.domain.home.smartaccess.SmartAccessUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HeroDashboardInteractionTest {
    @get:Rule val compose = createComposeRule()

    private val spec = HomeHeroLayoutPolicy.resolve(360, 640, 1f)

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
}
