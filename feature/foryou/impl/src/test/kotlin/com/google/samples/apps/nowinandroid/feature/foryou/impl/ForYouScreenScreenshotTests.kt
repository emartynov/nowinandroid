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

package com.google.samples.apps.nowinandroid.feature.foryou.impl

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesElements
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.matcher.ElementMatchers.withText
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.testing.util.DefaultTestDevices
import com.google.samples.apps.nowinandroid.core.testing.util.captureForDevice
import com.google.samples.apps.nowinandroid.core.testing.util.captureMultiDevice
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState
import com.google.samples.apps.nowinandroid.core.ui.NewsFeedUiState.Success
import com.google.samples.apps.nowinandroid.core.ui.UserNewsResourcePreviewParameterProvider
import com.google.samples.apps.nowinandroid.feature.foryou.impl.OnboardingUiState.NotShown
import com.google.samples.apps.nowinandroid.feature.foryou.impl.OnboardingUiState.Shown
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.JUnit4RulesContext
import de.infix.testBalloon.framework.testSuite
import org.hamcrest.Matchers
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.util.TimeZone

/**
 * Screenshot tests for the [ForYouScreen].
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
val ForYouScreenScreenshotTests by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
            val userNewsResources = UserNewsResourcePreviewParameterProvider().values.first()

            init {
                // Make time zone deterministic in tests
                TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            }
        }
    } asContextForEach {

        @Composable
        fun ForYouScreenTopicSelection() {
            NiaTheme {
                NiaBackground {
                    ForYouScreen(
                        isSyncing = false,
                        onboardingUiState = Shown(
                            topics = userNewsResources.flatMap { news -> news.followableTopics }
                                .distinctBy { it.topic.id },
                        ),
                        feedState = Success(
                            feed = userNewsResources,
                        ),
                        onTopicCheckedChanged = { _, _ -> },
                        saveFollowedTopics = {},
                        onNewsResourcesCheckedChanged = { _, _ -> },
                        onNewsResourceViewed = {},
                        onTopicClick = {},
                        deepLinkedUserNewsResource = null,
                        onDeepLinkOpened = {},
                    )
                }
            }
        }

        @Composable
        fun ForYouScreenPopulatedAndLoading() {
            NiaTheme {
                NiaBackground {
                    NiaTheme {
                        ForYouScreen(
                            isSyncing = true,
                            onboardingUiState = OnboardingUiState.Loading,
                            feedState = Success(
                                feed = userNewsResources,
                            ),
                            onTopicCheckedChanged = { _, _ -> },
                            saveFollowedTopics = {},
                            onNewsResourcesCheckedChanged = { _, _ -> },
                            onNewsResourceViewed = {},
                            onTopicClick = {},
                            deepLinkedUserNewsResource = null,
                            onDeepLinkOpened = {},
                        )
                    }
                }
            }
        }

        test("forYouScreenPopulatedFeed") {
            composeTestRule.captureMultiDevice("ForYouScreenPopulatedFeed") {
                NiaTheme {
                    ForYouScreen(
                        isSyncing = false,
                        onboardingUiState = NotShown,
                        feedState = Success(
                            feed = userNewsResources,
                        ),
                        onTopicCheckedChanged = { _, _ -> },
                        saveFollowedTopics = {},
                        onNewsResourcesCheckedChanged = { _, _ -> },
                        onNewsResourceViewed = {},
                        onTopicClick = {},
                        deepLinkedUserNewsResource = null,
                        onDeepLinkOpened = {},
                    )
                }
            }
        }

        test("forYouScreenLoading") {
            composeTestRule.captureMultiDevice("ForYouScreenLoading") {
                NiaTheme {
                    ForYouScreen(
                        isSyncing = false,
                        onboardingUiState = OnboardingUiState.Loading,
                        feedState = NewsFeedUiState.Loading,
                        onTopicCheckedChanged = { _, _ -> },
                        saveFollowedTopics = {},
                        onNewsResourcesCheckedChanged = { _, _ -> },
                        onNewsResourceViewed = {},
                        onTopicClick = {},
                        deepLinkedUserNewsResource = null,
                        onDeepLinkOpened = {},
                    )
                }
            }
        }

        test("forYouScreenTopicSelection") {
            composeTestRule.captureMultiDevice(
                "ForYouScreenTopicSelection",
                accessibilitySuppressions = Matchers.allOf(
                    AccessibilityCheckResultUtils.matchesCheck(TextContrastCheck::class.java),
                    Matchers.anyOf(
                        // Disabled Button
                        matchesElements(withText("Done")),

                        // TODO investigate, seems a false positive
                        matchesElements(withText("What are you interested in?")),
                        matchesElements(withText("UI")),
                    ),
                ),
            ) {
                ForYouScreenTopicSelection()
            }
        }

        test("forYouScreenTopicSelection_dark") {
            composeTestRule.captureForDevice(
                deviceName = "phone_dark",
                deviceSpec = DefaultTestDevices.PHONE.spec,
                screenshotName = "ForYouScreenTopicSelection",
                darkMode = true,
            ) {
                ForYouScreenTopicSelection()
            }
        }

        test("forYouScreenPopulatedAndLoading") {
            composeTestRule.captureMultiDevice("ForYouScreenPopulatedAndLoading") {
                ForYouScreenPopulatedAndLoading()
            }
        }

        test("forYouScreenPopulatedAndLoading_dark") {
            composeTestRule.captureForDevice(
                deviceName = "phone_dark",
                deviceSpec = DefaultTestDevices.PHONE.spec,
                screenshotName = "ForYouScreenPopulatedAndLoading",
                darkMode = true,
            ) {
                ForYouScreenPopulatedAndLoading()
            }
        }
    }
}
