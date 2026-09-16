package com.nexvary.aviationnavigator

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationIntegrityGateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val sections = listOf("HOME", "MAP", "PLAN", "RADAR", "TRAFFIC", "ABOUT")
    private val secondarySections = sections.filterNot { it == "HOME" }

    @Test
    fun everyTopLevelTabAndInternalHomeLinkReachesTheCorrectPage() {
        composeRule.onNodeWithTag("page_HOME").assertIsDisplayed()

        secondarySections.forEach { target ->
            composeRule.onNodeWithTag("nav_$target")
                .assertHasClickAction()
                .performClick()
            composeRule.onNodeWithTag("page_$target").assertIsDisplayed()

            composeRule.onNodeWithTag("back_button")
                .assertHasClickAction()
                .performClick()
            composeRule.onNodeWithTag("page_HOME").assertIsDisplayed()
        }

        secondarySections.forEach { target ->
            composeRule.onNodeWithTag("quick_$target")
                .performScrollTo()
                .assertHasClickAction()
                .performClick()
            composeRule.onNodeWithTag("page_$target").assertIsDisplayed()

            composeRule.onNodeWithTag("back_button")
                .assertHasClickAction()
                .performClick()
            composeRule.onNodeWithTag("page_HOME").assertIsDisplayed()
        }
    }

    @Test
    fun globalNavigationRemainsConnectedFromEveryPage() {
        val route = listOf("MAP", "PLAN", "RADAR", "TRAFFIC", "ABOUT", "HOME")

        route.forEach { target ->
            sections.forEach { availableTarget ->
                composeRule.onNodeWithTag("nav_$availableTarget")
                    .assertIsDisplayed()
                    .assertHasClickAction()
            }

            composeRule.onNodeWithTag("nav_$target").performClick()
            composeRule.onNodeWithTag("page_$target").assertIsDisplayed()
        }
    }

    @Test
    fun androidBackAndVisibleBackButtonBothReturnToHome() {
        secondarySections.forEach { target ->
            composeRule.onNodeWithTag("nav_$target").performClick()
            composeRule.onNodeWithTag("page_$target").assertIsDisplayed()

            composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithTag("page_HOME").assertIsDisplayed()

            composeRule.onNodeWithTag("nav_$target").performClick()
            composeRule.onNodeWithTag("back_button").performClick()
            composeRule.onNodeWithTag("page_HOME").assertIsDisplayed()
        }
    }
}
