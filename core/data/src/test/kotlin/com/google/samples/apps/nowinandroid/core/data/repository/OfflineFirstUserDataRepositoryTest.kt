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

package com.google.samples.apps.nowinandroid.core.data.repository

import com.google.samples.apps.nowinandroid.core.analytics.NoOpAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.datastore.NiaPreferencesDataSource
import com.google.samples.apps.nowinandroid.core.datastore.UserPreferences
import com.google.samples.apps.nowinandroid.core.datastore.test.InMemoryDataStore
import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import de.infix.testBalloon.framework.testSuite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val OfflineFirstUserDataRepositoryTest by testSuite {
    testFixture {
        object {
            val niaPreferencesDataSource = NiaPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))
            val subject = OfflineFirstUserDataRepository(
                niaPreferencesDataSource = niaPreferencesDataSource,
                NoOpAnalyticsHelper(),
            )
        }
    } asContextForEach {
        test("default user data is correct") {
            assertEquals(
                UserData(
                    bookmarkedNewsResources = emptySet(),
                    viewedNewsResources = emptySet(),
                    followedTopics = emptySet(),
                    themeBrand = ThemeBrand.DEFAULT,
                    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
                    useDynamicColor = false,
                    shouldHideOnboarding = false,
                ),
                subject.userData.first(),
            )
        }

        test("toggle followed topics delegates to nia preferences") {
            subject.setTopicIdFollowed(followedTopicId = "0", followed = true)
            assertEquals(setOf("0"), subject.userData.map { it.followedTopics }.first())

            subject.setTopicIdFollowed(followedTopicId = "1", followed = true)
            assertEquals(setOf("0", "1"), subject.userData.map { it.followedTopics }.first())
            assertEquals(
                niaPreferencesDataSource.userData.map { it.followedTopics }.first(),
                subject.userData.map { it.followedTopics }.first(),
            )
        }

        test("set followed topics delegates to nia preferences") {
            subject.setFollowedTopicIds(followedTopicIds = setOf("1", "2"))
            assertEquals(setOf("1", "2"), subject.userData.map { it.followedTopics }.first())
            assertEquals(
                niaPreferencesDataSource.userData.map { it.followedTopics }.first(),
                subject.userData.map { it.followedTopics }.first(),
            )
        }

        test("bookmark news resource delegates to nia preferences") {
            subject.setNewsResourceBookmarked(newsResourceId = "0", bookmarked = true)
            assertEquals(setOf("0"), subject.userData.map { it.bookmarkedNewsResources }.first())

            subject.setNewsResourceBookmarked(newsResourceId = "1", bookmarked = true)
            assertEquals(setOf("0", "1"), subject.userData.map { it.bookmarkedNewsResources }.first())
            assertEquals(
                niaPreferencesDataSource.userData.map { it.bookmarkedNewsResources }.first(),
                subject.userData.map { it.bookmarkedNewsResources }.first(),
            )
        }

        test("update viewed news resources delegates to nia preferences") {
            subject.setNewsResourceViewed(newsResourceId = "0", viewed = true)
            assertEquals(setOf("0"), subject.userData.map { it.viewedNewsResources }.first())

            subject.setNewsResourceViewed(newsResourceId = "1", viewed = true)
            assertEquals(setOf("0", "1"), subject.userData.map { it.viewedNewsResources }.first())
            assertEquals(
                niaPreferencesDataSource.userData.map { it.viewedNewsResources }.first(),
                subject.userData.map { it.viewedNewsResources }.first(),
            )
        }

        test("set theme brand delegates to nia preferences") {
            subject.setThemeBrand(ThemeBrand.ANDROID)
            assertEquals(ThemeBrand.ANDROID, subject.userData.map { it.themeBrand }.first())
            assertEquals(ThemeBrand.ANDROID, niaPreferencesDataSource.userData.map { it.themeBrand }.first())
        }

        test("set dynamic color delegates to nia preferences") {
            subject.setDynamicColorPreference(true)
            assertEquals(true, subject.userData.map { it.useDynamicColor }.first())
            assertEquals(true, niaPreferencesDataSource.userData.map { it.useDynamicColor }.first())
        }

        test("set dark theme config delegates to nia preferences") {
            subject.setDarkThemeConfig(DarkThemeConfig.DARK)
            assertEquals(DarkThemeConfig.DARK, subject.userData.map { it.darkThemeConfig }.first())
            assertEquals(DarkThemeConfig.DARK, niaPreferencesDataSource.userData.map { it.darkThemeConfig }.first())
        }

        test("when user completes onboarding and removes all interests should hide onboarding is false") {
            subject.setFollowedTopicIds(setOf("1"))
            subject.setShouldHideOnboarding(true)
            assertTrue(subject.userData.first().shouldHideOnboarding)

            subject.setFollowedTopicIds(emptySet())
            assertFalse(subject.userData.first().shouldHideOnboarding)
        }
    }
}
