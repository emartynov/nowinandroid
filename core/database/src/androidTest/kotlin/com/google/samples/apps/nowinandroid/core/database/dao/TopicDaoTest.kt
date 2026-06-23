/*
 * Copyright 2024 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.core.database.dao

import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals

internal val TopicDaoTest by testSuite {
    niaDbFixture() asContextForEach {
        suspend fun insertTopics() {
            val topicEntities = listOf(
                testTopicEntity("1", "compose"),
                testTopicEntity("2", "performance"),
                testTopicEntity("3", "headline"),
            )
            topicDao.insertOrIgnoreTopics(topicEntities)
        }

        test("getTopics") {
            insertTopics()

            val savedTopics = topicDao.getTopicEntities().first()

            assertEquals(
                listOf("1", "2", "3"),
                savedTopics.map { it.id },
            )
        }

        test("getTopic") {
            insertTopics()

            val savedTopicEntity = topicDao.getTopicEntity("2").first()

            assertEquals("performance", savedTopicEntity.name)
        }

        test("getTopics_oneOff") {
            insertTopics()

            val savedTopics = topicDao.getOneOffTopicEntities()

            assertEquals(
                listOf("1", "2", "3"),
                savedTopics.map { it.id },
            )
        }

        test("getTopics_byId") {
            insertTopics()

            val savedTopics = topicDao.getTopicEntities(setOf("1", "2"))
                .first()

            assertEquals(listOf("compose", "performance"), savedTopics.map { it.name })
        }

        test("insertTopic_newEntryIsIgnoredIfAlreadyExists") {
            insertTopics()
            topicDao.insertOrIgnoreTopics(
                listOf(testTopicEntity("1", "compose")),
            )

            val savedTopics = topicDao.getOneOffTopicEntities()

            assertEquals(3, savedTopics.size)
        }

        test("upsertTopic_existingEntryIsUpdated") {
            insertTopics()
            topicDao.upsertTopics(
                listOf(testTopicEntity("1", "newName")),
            )

            val savedTopics = topicDao.getOneOffTopicEntities()

            assertEquals(3, savedTopics.size)
            assertEquals("newName", savedTopics.first().name)
        }

        test("deleteTopics_byId_existingEntriesAreDeleted") {
            insertTopics()
            topicDao.deleteTopics(listOf("1", "2"))

            val savedTopics = topicDao.getOneOffTopicEntities()

            assertEquals(1, savedTopics.size)
            assertEquals("3", savedTopics.first().id)
        }
    }
}

