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

package com.google.samples.apps.nowinandroid.core.datastore

import com.google.samples.apps.nowinandroid.core.datastore.test.InMemoryDataStore
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.first
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val NiaPreferencesDataSourceTest by testSuite {
    testFixture {
        NiaPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))
    } asContextForEach {

        test("shouldHideOnboardingIsFalseByDefault") {
            assertFalse(userData.first().shouldHideOnboarding)
        }

        test("userShouldHideOnboardingIsTrueWhenSet") {
            setShouldHideOnboarding(true)
            assertTrue(userData.first().shouldHideOnboarding)
        }

        test("userShouldHideOnboarding_unfollowsLastTopic_shouldHideOnboardingIsFalse") {
            // Given: user completes onboarding by selecting a single topic.
            setTopicIdFollowed("1", true)
            setShouldHideOnboarding(true)

            // When: they unfollow that topic.
            setTopicIdFollowed("1", false)

            // Then: onboarding should be shown again
            assertFalse(userData.first().shouldHideOnboarding)
        }

        test("userShouldHideOnboarding_unfollowsAllTopics_shouldHideOnboardingIsFalse") {
            // Given: user completes onboarding by selecting several topics.
            setFollowedTopicIds(setOf("1", "2"))
            setShouldHideOnboarding(true)

            // When: they unfollow those topics.
            setFollowedTopicIds(emptySet())

            // Then: onboarding should be shown again
            assertFalse(userData.first().shouldHideOnboarding)
        }

        test("shouldUseDynamicColorFalseByDefault") {
            assertFalse(userData.first().useDynamicColor)
        }

        test("userShouldUseDynamicColorIsTrueWhenSet") {
            setDynamicColorPreference(true)
            assertTrue(userData.first().useDynamicColor)
        }
    }
}
