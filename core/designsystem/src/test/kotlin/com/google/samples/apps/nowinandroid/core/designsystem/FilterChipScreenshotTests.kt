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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaFilterChip
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.testing.util.DefaultRoborazziOptions
import com.google.samples.apps.nowinandroid.core.testing.util.captureMultiTheme
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.JUnit4RulesContext
import de.infix.testBalloon.framework.testSuite
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "480dpi")
@LooperMode(LooperMode.Mode.PAUSED)
val FilterChipScreenshotTests by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val composeTestRule = rule(createAndroidComposeRule<ComponentActivity>())
        }
    } asContextForEach {

        test("filter chip multiple themes") {
            composeTestRule.captureMultiTheme("FilterChip") {
                Surface {
                    NiaFilterChip(selected = false, onSelectedChange = {}) {
                        Text("Unselected chip")
                    }
                }
            }
        }

        test("filter chip multiple themes selected") {
            composeTestRule.captureMultiTheme("FilterChip", "FilterChipSelected") {
                Surface {
                    NiaFilterChip(selected = true, onSelectedChange = {}) {
                        Text("Selected Chip")
                    }
                }
            }
        }

        test("filter chip huge font") {
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                ) {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.FontScale(2f) then
                            DeviceConfigurationOverride.ForcedSize(DpSize(80.dp, 40.dp)),
                    ) {
                        NiaTheme {
                            NiaBackground {
                                NiaFilterChip(selected = true, onSelectedChange = {}) {
                                    Text("Chip")
                                }
                            }
                        }
                    }
                }
            }
            composeTestRule.onRoot()
                .captureRoboImage(
                    "src/test/screenshots/FilterChip/FilterChip_fontScale2.png",
                    roborazziOptions = DefaultRoborazziOptions,
                )
        }
    }
}
