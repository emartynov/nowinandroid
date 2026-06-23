/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.feature.interests.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.google.samples.apps.nowinandroid.core.testing.data.followableTopicTestData
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.testSuite
import com.google.samples.apps.nowinandroid.core.ui.R as CoreUiR
import com.google.samples.apps.nowinandroid.feature.interests.api.R as InterestsR

/**
 * UI test for checking the correct behaviour of the Interests screen;
 * Verifies that, when a specific UiState is set, the corresponding
 * composables and details are shown
 */
val InterestsScreenTest by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        val interestsLoading =
            composeTestRule.activity.getString(InterestsR.string.feature_interests_api_loading)
        val interestsEmptyHeader =
            composeTestRule.activity.getString(InterestsR.string.feature_interests_api_empty_header)
        val interestsTopicCardFollowButton =
            composeTestRule.activity.getString(CoreUiR.string.core_ui_interests_card_follow_button_content_desc)
        val interestsTopicCardUnfollowButton =
            composeTestRule.activity.getString(CoreUiR.string.core_ui_interests_card_unfollow_button_content_desc)

        test("niaLoadingWheel inTopics whenScreenIsLoading showLoading") {
            composeTestRule.setContent {
                InterestsScreenContent(uiState = InterestsUiState.Loading)
            }

            composeTestRule
                .onNodeWithContentDescription(interestsLoading)
                .assertExists()
        }

        test("interestsWithTopics whenTopicsFollowed showFollowedAndUnfollowedTopicsWithInfo") {
            composeTestRule.setContent {
                InterestsScreenContent(
                    uiState = InterestsUiState.Interests(
                        topics = followableTopicTestData,
                        selectedTopicId = null,
                    ),
                )
            }

            composeTestRule
                .onNodeWithText(followableTopicTestData[0].topic.name)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(followableTopicTestData[1].topic.name)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(followableTopicTestData[2].topic.name)
                .assertIsDisplayed()

            composeTestRule
                .onAllNodesWithContentDescription(interestsTopicCardFollowButton)
                .assertCountEquals(numberOfUnfollowedTopics)
        }

        test("topicsEmpty whenDataIsEmptyOccurs thenShowEmptyScreen") {
            composeTestRule.setContent {
                InterestsScreenContent(uiState = InterestsUiState.Empty)
            }

            composeTestRule
                .onNodeWithText(interestsEmptyHeader)
                .assertIsDisplayed()
        }
    }
}

@Composable
private fun InterestsScreenContent(uiState: InterestsUiState) {
    InterestsScreen(
        uiState = uiState,
        followTopic = { _, _ -> },
        onTopicClick = {},
    )
}

private val numberOfUnfollowedTopics = followableTopicTestData.filter { !it.isFollowed }.size
