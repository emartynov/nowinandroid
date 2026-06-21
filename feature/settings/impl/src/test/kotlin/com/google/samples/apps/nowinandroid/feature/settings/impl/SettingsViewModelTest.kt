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

package com.google.samples.apps.nowinandroid.feature.settings.impl

import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig.DARK
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand.ANDROID
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.util.mainDispatcherTestConfig
import com.google.samples.apps.nowinandroid.feature.settings.impl.SettingsUiState.Loading
import com.google.samples.apps.nowinandroid.feature.settings.impl.SettingsUiState.Success
import de.infix.testBalloon.framework.testSuite
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.assertEquals

val SettingsViewModelTest by testSuite(testConfig = mainDispatcherTestConfig) {
    testFixture {
        object {
            val userDataRepository = TestUserDataRepository()
            val viewModel = SettingsViewModel(userDataRepository)
        }
    } asContextForEach {
        test("state is initially loading") {
            assertEquals(Loading, viewModel.settingsUiState.value)
        }

        test("state is success after user data loaded") {
            backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.settingsUiState.collect() }

            userDataRepository.setThemeBrand(ANDROID)
            userDataRepository.setDarkThemeConfig(DARK)

            assertEquals(
                Success(
                    UserEditableSettings(
                        brand = ANDROID,
                        darkThemeConfig = DARK,
                        useDynamicColor = false,
                    ),
                ),
                viewModel.settingsUiState.value,
            )
        }
    }
}
