/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.google.samples.apps.nowinandroid.interests.impl

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.espresso.Espresso
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.core.navigation.rememberNavigationState
import com.google.samples.apps.nowinandroid.core.navigation.toEntries
import com.google.samples.apps.nowinandroid.feature.interests.api.R
import com.google.samples.apps.nowinandroid.feature.interests.api.navigation.InterestsNavKey
import com.google.samples.apps.nowinandroid.feature.interests.impl.LIST_PANE_TEST_TAG
import com.google.samples.apps.nowinandroid.feature.interests.impl.navigation.interestsEntry
import com.google.samples.apps.nowinandroid.feature.topic.impl.navigation.topicEntry
import com.google.samples.apps.nowinandroid.uitesthiltmanifest.HiltComponentActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.disable
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val EXPANDED_WIDTH = "w1200dp-h840dp"
private const val COMPACT_WIDTH = "w412dp-h915dp"

@HiltAndroidTest
@Suppress("VisibleForTests")
internal class InterestsListDetailFixture : JUnit4RulesContext() {
    val hiltRule = rule(HiltAndroidRule(this))
    val composeTestRule = rule(createAndroidComposeRule<HiltComponentActivity>())

    @Inject
    lateinit var topicsRepository: TopicsRepository
}

val InterestsListDetailScreenTest by testSuite {
    robolectricTestSuite<InterestsListDetailScreenTestContent>(
        "InterestsListDetailScreen tests",
        testConfig = TestConfig.robolectric {
            sdk = 35
            application = HiltTestApplication::class
        }
// TODO: re-enable when https://github.com/infix-de/testBalloon/issues/86 is fixed
            .disable(),
    )
}

class InterestsListDetailScreenTestContent : RobolectricTestSuiteContent({
    testFixture {
        InterestsListDetailFixture()
    } asContextForEach {

        test(
            "expandedWidth initialState showsTwoPanesWithPlaceholder",
            testConfig = TestConfig.robolectric { qualifiers = EXPANDED_WIDTH },
        ) {
            hiltRule.inject()
            val placeholderText =
                composeTestRule.activity.getString(R.string.feature_interests_api_select_an_interest)
            composeTestRule.apply {
                setContent { NiaTheme { TestNavDisplay() } }
                onNodeWithTag(LIST_PANE_TEST_TAG).assertIsDisplayed()
                onNodeWithText(placeholderText).assertIsDisplayed()
            }
        }

        test(
            "compactWidth initialState showsListPane",
            testConfig = TestConfig.robolectric { qualifiers = COMPACT_WIDTH },
        ) {
            hiltRule.inject()
            val placeholderText =
                composeTestRule.activity.getString(R.string.feature_interests_api_select_an_interest)
            composeTestRule.apply {
                setContent { NiaTheme { TestNavDisplay() } }
                onNodeWithTag(LIST_PANE_TEST_TAG).assertIsDisplayed()
                onNodeWithText(placeholderText).assertIsNotDisplayed()
            }
        }

        test(
            "expandedWidth topicSelected updatesDetailPane",
            testConfig = TestConfig.robolectric { qualifiers = EXPANDED_WIDTH },
        ) {
            hiltRule.inject()
            val placeholderText =
                composeTestRule.activity.getString(R.string.feature_interests_api_select_an_interest)
            val getTopics: () -> List<Topic> =
                { runBlocking { topicsRepository.getTopics().first().sortedBy { it.name } } }
            composeTestRule.apply {
                setContent { NiaTheme { TestNavDisplay() } }
                val firstTopic = getTopics().first()
                onNodeWithText(firstTopic.name).performClick()
                waitForIdle()
                onNodeWithTag(LIST_PANE_TEST_TAG).assertIsDisplayed()
                onNodeWithText(placeholderText).assertIsNotDisplayed()
                onNodeWithTag(firstTopic.testTag).assertIsDisplayed()
            }
        }

        test(
            "compactWidth topicSelected showsTopicDetailPane",
            testConfig = TestConfig.robolectric { qualifiers = COMPACT_WIDTH },
        ) {
            hiltRule.inject()
            val getTopics: () -> List<Topic> =
                { runBlocking { topicsRepository.getTopics().first().sortedBy { it.name } } }
            composeTestRule.apply {
                setContent { NiaTheme { TestNavDisplay() } }
                val firstTopic = getTopics().first()
                onNodeWithText(firstTopic.name).performClick()
                onNodeWithTag(LIST_PANE_TEST_TAG).assertIsNotDisplayed()
                onNodeWithTag(firstTopic.testTag).assertIsDisplayed()
            }
        }

        test(
            "compactWidth backPressFromTopicDetail showsListPane",
            testConfig = TestConfig.robolectric { qualifiers = COMPACT_WIDTH },
        ) {
            hiltRule.inject()
            val getTopics: () -> List<Topic> =
                { runBlocking { topicsRepository.getTopics().first().sortedBy { it.name } } }
            composeTestRule.apply {
                setContent { NiaTheme { TestNavDisplay() } }
                val firstTopic = getTopics().first()
                onNodeWithText(firstTopic.name).performClick()
                waitForIdle()
                Espresso.pressBack()
                onNodeWithTag(LIST_PANE_TEST_TAG).assertIsDisplayed()
                onNodeWithTag(firstTopic.testTag).assertIsNotDisplayed()
            }
        }
    }
})

private val Topic.testTag
    get() = "topic:${this.id}"

@Composable
private fun TestNavDisplay() {
    val startKey = InterestsNavKey(null)

    val navigationState = rememberNavigationState(
        startKey = startKey,
        topLevelKeys = setOf(startKey),
    )

    val navigator = Navigator(navigationState)

    val entryProvider = entryProvider {
        interestsEntry(navigator)
        topicEntry(navigator)
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        sceneStrategy = rememberListDetailSceneStrategy(),
    )
}
