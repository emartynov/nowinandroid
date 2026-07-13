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

package com.google.samples.apps.nowinandroid.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaOverlayLoadingWheel
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.testing.util.DefaultRoborazziOptions
import com.google.samples.apps.nowinandroid.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite

val LoadingWheelScreenshotTests by testSuite {
    robolectricTestSuite<LoadingWheelScreenshotTestsContent>(
        "LoadingWheel screenshot tests",
        testConfig = TestConfig.robolectric {
            application = HiltTestApplication::class
            qualifiers = "480dpi"
        },
    )
}

class LoadingWheelScreenshotTestsContent : RobolectricTestSuiteContent({
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        test("loading wheel multiple themes") {
            composeTestRule.captureMultiTheme("LoadingWheel") {
                Surface {
                    NiaLoadingWheel(contentDesc = "test")
                }
            }
        }

        test("overlay loading wheel multiple themes") {
            composeTestRule.captureMultiTheme("LoadingWheel", "OverlayLoadingWheel") {
                Surface {
                    NiaOverlayLoadingWheel(contentDesc = "test")
                }
            }
        }

        test("loading wheel animation") {
            composeTestRule.mainClock.autoAdvance = false
            composeTestRule.setContent {
                NiaTheme {
                    NiaLoadingWheel(contentDesc = "")
                }
            }
            // Try multiple frames of the animation; some arbitrary, some synchronized with duration.
            listOf(20L, 115L, 724L, 1000L).forEach { deltaTime ->
                composeTestRule.mainClock.advanceTimeBy(deltaTime)
                composeTestRule.onRoot()
                    .captureRoboImage(
                        "src/test/screenshots/LoadingWheel/LoadingWheel_animation_$deltaTime.png",
                        roborazziOptions = DefaultRoborazziOptions,
                    )
            }
        }
    }
})
