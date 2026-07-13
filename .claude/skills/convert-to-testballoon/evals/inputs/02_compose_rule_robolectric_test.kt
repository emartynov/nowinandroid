package com.example.test

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.CounterScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CounterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun initialCount_isZero() {
        var count = 0
        composeTestRule.setContent {
            CounterScreen(
                count = count,
                onIncrement = { count++ },
            )
        }
        composeTestRule.onNodeWithText("Count: 0").assertExists()
    }

    @Test
    fun clicking_increment_updatesCount() {
        var count = 0
        composeTestRule.setContent {
            CounterScreen(
                count = count,
                onIncrement = { count++ },
            )
        }
        composeTestRule.onNodeWithText("Increment").performClick()
        assertEquals(1, count)
    }
}
