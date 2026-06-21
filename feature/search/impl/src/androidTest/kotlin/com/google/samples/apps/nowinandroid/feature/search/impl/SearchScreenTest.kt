/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.feature.search.impl

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import com.google.samples.apps.nowinandroid.core.data.model.RecentSearchQuery
import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig.DARK
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand.ANDROID
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.testing.data.followableTopicTestData
import com.google.samples.apps.nowinandroid.core.testing.data.newsResourcesTestData
import com.google.samples.apps.nowinandroid.core.ui.R.string
import com.google.samples.apps.nowinandroid.feature.search.api.R
import de.infix.testBalloon.framework.JUnit4RulesContext
import de.infix.testBalloon.framework.testSuite

/**
 * UI test for checking the correct behaviour of the Search screen.
 */
val SearchScreenTest by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        val clearSearchContentDesc =
            composeTestRule.activity.getString(R.string.feature_search_api_clear_search_text_content_desc)
        val clearRecentSearchesContentDesc =
            composeTestRule.activity.getString(R.string.feature_search_api_clear_recent_searches_content_desc)
        val followButtonContentDesc =
            composeTestRule.activity.getString(string.core_ui_interests_card_follow_button_content_desc)
        val unfollowButtonContentDesc =
            composeTestRule.activity.getString(string.core_ui_interests_card_unfollow_button_content_desc)
        val topicsString =
            composeTestRule.activity.getString(R.string.feature_search_api_topics)
        val updatesString =
            composeTestRule.activity.getString(R.string.feature_search_api_updates)
        val tryAnotherSearchString =
            composeTestRule.activity.getString(R.string.feature_search_api_try_another_search) +
                " " + composeTestRule.activity.getString(R.string.feature_search_api_interests) +
                " " + composeTestRule.activity.getString(R.string.feature_search_api_to_browse_topics)
        val searchNotReadyString =
            composeTestRule.activity.getString(R.string.feature_search_api_not_ready)

        val userData = UserData(
            bookmarkedNewsResources = setOf("1", "3"),
            viewedNewsResources = setOf("1", "2", "4"),
            followedTopics = emptySet(),
            themeBrand = ANDROID,
            darkThemeConfig = DARK,
            shouldHideOnboarding = true,
            useDynamicColor = false,
        )

        test("searchTextField isFocused") {
            composeTestRule.setContent {
                SearchScreen()
            }

            composeTestRule
                .onNodeWithTag("searchTextField")
                .assertIsFocused()
        }

        test("emptySearchResult emptyScreenIsDisplayed") {
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.Success(),
                )
            }

            composeTestRule
                .onNodeWithText(tryAnotherSearchString)
                .assertIsDisplayed()
        }

        test("emptySearchResult nonEmptyRecentSearches emptySearchScreenAndRecentSearchesAreDisplayed") {
            val recentSearches = listOf("kotlin")
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.Success(),
                    recentSearchesUiState = RecentSearchQueriesUiState.Success(
                        recentQueries = recentSearches.map(::RecentSearchQuery),
                    ),
                )
            }

            composeTestRule
                .onNodeWithText(tryAnotherSearchString)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithContentDescription(clearRecentSearchesContentDesc)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText("kotlin")
                .assertIsDisplayed()
        }

        test("searchResultWithTopics allTopicsAreVisible followButtonsVisibleForTheNumOfFollowedTopics") {
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.Success(topics = followableTopicTestData),
                )
            }

            composeTestRule
                .onNodeWithText(topicsString)
                .assertIsDisplayed()

            val scrollableNode = composeTestRule
                .onAllNodes(hasScrollToNodeAction())
                .onFirst()

            followableTopicTestData.forEachIndexed { index, followableTopic ->
                scrollableNode.performScrollToIndex(index)

                composeTestRule
                    .onNodeWithText(followableTopic.topic.name)
                    .assertIsDisplayed()
            }

            composeTestRule
                .onAllNodesWithContentDescription(followButtonContentDesc)
                .assertCountEquals(2)
            composeTestRule
                .onAllNodesWithContentDescription(unfollowButtonContentDesc)
                .assertCountEquals(1)
        }

        test("searchResultWithNewsResources firstNewsResourcesIsVisible") {
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.Success(
                        newsResources = newsResourcesTestData.map {
                            UserNewsResource(
                                newsResource = it,
                                userData = userData,
                            )
                        },
                    ),
                )
            }

            composeTestRule
                .onNodeWithText(updatesString)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(newsResourcesTestData[0].title)
                .assertIsDisplayed()
        }

        test("emptyQuery notEmptyRecentSearches verifyClearSearchesButton displayed") {
            val recentSearches = listOf("kotlin", "testing")
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.EmptyQuery,
                    recentSearchesUiState = RecentSearchQueriesUiState.Success(
                        recentQueries = recentSearches.map(::RecentSearchQuery),
                    ),
                )
            }

            composeTestRule
                .onNodeWithContentDescription(clearRecentSearchesContentDesc)
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText("kotlin")
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText("testing")
                .assertIsDisplayed()
        }

        test("searchNotReady verifySearchNotReadyMessageIsVisible") {
            composeTestRule.setContent {
                SearchScreen(
                    searchResultUiState = SearchResultUiState.SearchNotReady,
                )
            }

            composeTestRule
                .onNodeWithText(searchNotReadyString)
                .assertIsDisplayed()
        }
    }
}
