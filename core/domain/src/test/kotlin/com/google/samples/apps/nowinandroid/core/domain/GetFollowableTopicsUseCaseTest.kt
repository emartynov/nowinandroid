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

package com.google.samples.apps.nowinandroid.core.domain

import com.google.samples.apps.nowinandroid.core.domain.TopicSortField.NAME
import com.google.samples.apps.nowinandroid.core.model.data.FollowableTopic
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.testing.repository.TestTopicsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.util.mainDispatcherTestConfig
import de.infix.testBalloon.framework.testSuite
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals

val GetFollowableTopicsUseCaseTest by testSuite(testConfig = mainDispatcherTestConfig) {
    testFixture {
        object {
            val topicsRepository = TestTopicsRepository()
            val userDataRepository = TestUserDataRepository()
            val useCase = GetFollowableTopicsUseCase(topicsRepository, userDataRepository)
        }
    } asContextForEach {
        test("when no params followable topics are returned with no sorting") {
            val followableTopics = useCase()

            topicsRepository.sendTopics(testTopics)
            userDataRepository.setFollowedTopicIds(setOf(testTopics[0].id, testTopics[2].id))

            assertEquals(
                listOf(
                    FollowableTopic(testTopics[0], true),
                    FollowableTopic(testTopics[1], false),
                    FollowableTopic(testTopics[2], true),
                ),
                followableTopics.first(),
            )
        }

        test("when sort order is by name topics sorted by name are returned") {
            val followableTopics = useCase(sortBy = NAME)

            topicsRepository.sendTopics(testTopics)
            userDataRepository.setFollowedTopicIds(setOf())

            assertEquals(
                followableTopics.first(),
                testTopics.sortedBy { it.name }.map { FollowableTopic(it, false) },
            )
        }
    }
}

private val testTopics = listOf(
    Topic("1", "Headlines", "", "", "", ""),
    Topic("2", "Android Studio", "", "", "", ""),
    Topic("3", "Compose", "", "", "", ""),
)
