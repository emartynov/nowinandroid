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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTab
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTabRow
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

val TabsScreenshotTests by testSuite {
    robolectricTestSuite<TabsScreenshotTestsContent>(
        "Tabs screenshot tests",
        testConfig = TestConfig.robolectric {
            application = HiltTestApplication::class
            qualifiers = "480dpi"
        },
    )
}

class TabsScreenshotTestsContent : RobolectricTestSuiteContent({
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        test("tabs multiple themes") {
            composeTestRule.captureMultiTheme("Tabs") {
                NiaTabsExample()
            }
        }

        test("tabs huge font") {
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                ) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.FontScale(2f),
                    ) {
                        NiaTheme {
                            NiaTabsExample("Looooong item")
                        }
                    }
                }
            }
            composeTestRule.onRoot()
                .captureRoboImage(
                    "src/test/screenshots/Tabs/Tabs_fontScale2.png",
                    roborazziOptions = DefaultRoborazziOptions,
                )
        }
    }
})

@Composable
private fun NiaTabsExample(label: String = "Topics") {
    Surface {
        val titles = listOf(label, "People")
        NiaTabRow(selectedTabIndex = 0) {
            titles.forEachIndexed { index, title ->
                NiaTab(
                    selected = index == 0,
                    onClick = { },
                    text = { Text(text = title) },
                )
            }
        }
    }
}
