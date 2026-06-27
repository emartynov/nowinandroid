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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.samples.apps.nowinandroid.core.data.repository.TopicsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.test.repository.FakeUserDataRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.TimeZoneMonitor
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.testing.util.DefaultRoborazziOptions
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation.LocalSnackbarHostState
import com.google.samples.apps.nowinandroid.uitesthiltmanifest.HiltComponentActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import de.infix.testBalloon.framework.core.JUnit4RulesContext
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.integration.robolectric.RobolectricTestSuiteContent
import de.infix.testBalloon.integration.robolectric.robolectric
import de.infix.testBalloon.integration.robolectric.robolectricTestSuite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.TimeZone
import javax.inject.Inject

/**
 * Tests that the Snackbar is correctly displayed on different screen sizes.
 */
val SnackbarScreenshotTests by testSuite {
    robolectricTestSuite<SnackbarScreenshotTestsContent>(
        "Snackbar screenshot tests",
        // Configure Robolectric to use a very large screen size that can fit all of the test sizes.
        // This allows enough room to render the content under test without clipping or scaling.
        testConfig = TestConfig.robolectric {
            application = HiltTestApplication::class
            qualifiers = "w1000dp-h1000dp-480dpi"
        },
    )
}

class SnackbarScreenshotTestsContent : RobolectricTestSuiteContent({
    testFixture { SnackbarScreenshotFixture() } asContextForEach {
        test("phone no snackbar") {
            captureSnackbarScreenshot(400.dp, 500.dp, "snackbar_compact_medium_noSnackbar") { }
        }
        test("snackbar shown phone") {
            captureSnackbarScreenshot(400.dp, 500.dp, "snackbar_compact_medium") { snackbarHostState ->
                snackbarHostState.showSnackbar(
                    "This is a test snackbar message",
                    actionLabel = "Action Label",
                    duration = Indefinite,
                )
            }
        }
        test("snackbar shown foldable") {
            captureSnackbarScreenshot(600.dp, 600.dp, "snackbar_medium_medium") { snackbarHostState ->
                snackbarHostState.showSnackbar(
                    "This is a test snackbar message",
                    actionLabel = "Action Label",
                    duration = Indefinite,
                )
            }
        }
        test("snackbar shown tablet") {
            captureSnackbarScreenshot(900.dp, 900.dp, "snackbar_expanded_expanded") { snackbarHostState ->
                snackbarHostState.showSnackbar(
                    "This is a test snackbar message",
                    actionLabel = "Action Label",
                    duration = Indefinite,
                )
            }
        }
    }
})

@HiltAndroidTest
class SnackbarScreenshotFixture : JUnit4RulesContext() {
    val hiltRule = rule(HiltAndroidRule(this))
    val composeTestRule = rule(createAndroidComposeRule<HiltComponentActivity>())

    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var timeZoneMonitor: TimeZoneMonitor
    @Inject lateinit var userDataRepository: FakeUserDataRepository
    @Inject lateinit var topicsRepository: TopicsRepository
    @Inject lateinit var userNewsResourceRepository: UserNewsResourceRepository

    fun captureSnackbarScreenshot(
        width: Dp,
        height: Dp,
        screenshotName: String,
        action: suspend (snackbarHostState: SnackbarHostState) -> Unit,
    ) {
        hiltRule.inject()
        runBlocking {
            userDataRepository.setShouldHideOnboarding(true)
            userDataRepository.setFollowedTopicIds(
                setOf(topicsRepository.getTopics().first().first().id),
            )
        }
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        lateinit var scope: CoroutineScope
        val snackbarHostState = SnackbarHostState()
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalSnackbarHostState provides snackbarHostState,
            ) {
                scope = rememberCoroutineScope()

                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(width, height)),
                ) {
                    BoxWithConstraints {
                        NiaTheme {
                            val appState = rememberNiaAppState(
                                networkMonitor = networkMonitor,
                                userNewsResourceRepository = userNewsResourceRepository,
                                timeZoneMonitor = timeZoneMonitor,
                            )
                            NiaApp(
                                appState = appState,
                                showSettingsDialog = false,
                                onSettingsDismissed = {},
                                onTopAppBarActionClick = {},
                                windowAdaptiveInfo = WindowAdaptiveInfo(
                                    windowSizeClass = WindowSizeClass.compute(
                                        maxWidth.value,
                                        maxHeight.value,
                                    ),
                                    windowPosture = Posture(),
                                ),
                            )
                        }
                    }
                }
            }
        }

        scope.launch { action(snackbarHostState) }

        composeTestRule.onRoot()
            .captureRoboImage(
                "src/testDemo/screenshots/$screenshotName.png",
                roborazziOptions = DefaultRoborazziOptions,
            )
    }
}
