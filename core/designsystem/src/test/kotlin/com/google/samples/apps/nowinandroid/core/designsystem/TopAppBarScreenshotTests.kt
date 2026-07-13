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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopAppBar
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
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

val TopAppBarScreenshotTests by testSuite {
    robolectricTestSuite<TopAppBarScreenshotTestsContent>(
        "TopAppBar screenshot tests",
        testConfig = TestConfig.robolectric {
            application = HiltTestApplication::class
            qualifiers = "480dpi"
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
class TopAppBarScreenshotTestsContent : RobolectricTestSuiteContent({
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        test("top app bar multiple themes") {
            composeTestRule.captureMultiTheme("TopAppBar") {
                NiaTopAppBarExample()
            }
        }

        test("top app bar huge font") {
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                ) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.FontScale(2f),
                    ) {
                        NiaTheme {
                            NiaTopAppBarExample()
                        }
                    }
                }
            }
            composeTestRule.onRoot()
                .captureRoboImage(
                    "src/test/screenshots/TopAppBar/TopAppBar_fontScale2.png",
                    roborazziOptions = DefaultRoborazziOptions,
                )
        }
    }
})

@Composable
private fun NiaTopAppBarExample() {
    NiaTopAppBar(
        titleRes = android.R.string.untitled,
        navigationIcon = NiaIcons.Search,
        navigationIconContentDescription = "Navigation icon",
        actionIcon = NiaIcons.MoreVert,
        actionIconContentDescription = "Action icon",
    )
}
