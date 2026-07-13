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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaButton
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaOutlinedButton
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite

val ButtonScreenshotTests by testSuite {
    robolectricTestSuite<ButtonScreenshotTestsContent>(
        "Button screenshot tests",
        testConfig = TestConfig.robolectric {
            application = HiltTestApplication::class
            qualifiers = "480dpi"
        },
    )
}

class ButtonScreenshotTestsContent : RobolectricTestSuiteContent({
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        test("nia button multiple themes") {
            composeTestRule.captureMultiTheme("Button") { description ->
                Surface {
                    NiaButton(onClick = {}, text = { Text("$description Button") })
                }
            }
        }

        test("nia outline button multiple themes") {
            composeTestRule.captureMultiTheme("Button", "OutlineButton") { description ->
                Surface {
                    NiaOutlinedButton(onClick = {}, text = { Text("$description OutlineButton") })
                }
            }
        }

        test("nia button leading icon multiple themes") {
            composeTestRule.captureMultiTheme(
                name = "Button",
                overrideFileName = "ButtonLeadingIcon",
                shouldCompareAndroidTheme = false,
            ) { description ->
                Surface {
                    NiaButton(
                        onClick = {},
                        text = { Text("$description Icon Button") },
                        leadingIcon = { Icon(imageVector = NiaIcons.Add, contentDescription = null) },
                    )
                }
            }
        }
    }
})
