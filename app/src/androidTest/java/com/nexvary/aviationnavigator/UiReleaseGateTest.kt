package com.nexvary.aviationnavigator

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiReleaseGateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val navTags = listOf(
        "nav_HOME", "nav_MAP", "nav_PLAN", "nav_RADAR", "nav_TRAFFIC", "nav_ABOUT"
    )

    @Test
    fun primaryNavigationIsVisibleClickableAndNonOverlappingOnPhone() {
        val bounds = navTags.map { tag ->
            composeRule.onNodeWithTag(tag)
                .assertIsDisplayed()
                .assertHasClickAction()
                .fetchSemanticsNode()
                .boundsInRoot
        }

        bounds.forEach { rect ->
            assertTrue("Navigation target must have a positive width", rect.width > 0f)
            assertTrue("Navigation target must have a positive height", rect.height > 0f)
        }

        for (i in bounds.indices) {
            for (j in i + 1 until bounds.size) {
                assertFalse(
                    "Navigation controls overlap: ${navTags[i]} and ${navTags[j]}",
                    overlaps(bounds[i], bounds[j])
                )
            }
        }
    }

    @Test
    fun allHomeActionsAndAboutLinksExposeRealClickTargets() {
        listOf("MAP", "PLAN", "RADAR", "TRAFFIC", "ABOUT").forEach { target ->
            composeRule.onNodeWithTag("quick_$target")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasClickAction()
        }

        composeRule.onNodeWithTag("quick_ABOUT").performScrollTo().performClick()
        composeRule.onNodeWithTag("page_ABOUT").assertIsDisplayed()

        listOf(
            "external_website",
            "external_facebook",
            "external_email",
            "external_youtube",
            "external_x"
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun flightPlanValidationButtonActuallyChangesThePageState() {
        composeRule.onNodeWithTag("nav_PLAN")
            .assertHasClickAction()
            .performClick()
        composeRule.onNodeWithTag("page_PLAN").assertIsDisplayed()

        composeRule.onNodeWithTag("validate_plan_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        composeRule.onNodeWithText("Plan valid").assertIsDisplayed()
    }

    private fun overlaps(a: Rect, b: Rect): Boolean =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top
}
