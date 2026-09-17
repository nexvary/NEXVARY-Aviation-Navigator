package com.nexvary.aviationnavigator

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlightPlanningUiGateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun validatedPlanProducesMetricsAndSurvivesNavigationToMap() {
        composeRule.onNodeWithTag("nav_PLAN").performClick()
        composeRule.onNodeWithTag("page_PLAN").assertIsDisplayed()

        composeRule.onNodeWithTag("validate_plan_button")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("plan_metrics")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.onNodeWithTag("open_route_map_button")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("page_MAP").assertIsDisplayed()
        composeRule.onNodeWithTag("route_overlay_status").assertIsDisplayed()
    }
}
