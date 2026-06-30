/*
 * Copyright 2026 The Android Open Source Project
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

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.samples.apps.nowinandroid.core.database.NiaDatabase
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.first
import kotlin.time.Instant
import kotlin.test.assertEquals

val DatabaseTBTest by testSuite {
    testFixture {
        object {
            val db = run {
                val context = ApplicationProvider.getApplicationContext<Context>()
                Room.inMemoryDatabaseBuilder(
                    context,
                    NiaDatabase::class.java,
                ).build()
            }

            val newsResourceDao = db.newsResourceDao()
            val topicDao = db.topicDao()
        }
    } closeWith {
        db.close()
    } asParameterForEach {
        fun testNewsResource(
            id: String = "0",
            millisSinceEpoch: Long = 0,
        ) = NewsResourceEntity(
            id = id,
            title = "",
            content = "",
            url = "",
            headerImageUrl = "",
            publishDate = Instant.fromEpochMilliseconds(millisSinceEpoch),
            type = "Article 📚",
        )

        testSuite("New resource DAO") {
            test("Get new resources - all entries are ordered by publish date description") {
                with (it.newsResourceDao) {
                    val newsResourceEntities = listOf(
                        testNewsResource(
                            id = "0",
                            millisSinceEpoch = 0,
                        ),
                        testNewsResource(
                            id = "1",
                            millisSinceEpoch = 3,
                        ),
                        testNewsResource(
                            id = "2",
                            millisSinceEpoch = 1,
                        ),
                        testNewsResource(
                            id = "3",
                            millisSinceEpoch = 2,
                        ),
                    )
                    upsertNewsResources(
                        newsResourceEntities,
                    )

                    val savedNewsResourceEntities = getNewsResources().first()

                    assertEquals(
                        listOf(3L, 2L, 1L, 0L),
                        savedNewsResourceEntities.map {
                            it.asExternalModel().publishDate.toEpochMilliseconds()
                        },
                    )
                }
            }
        }

        testSuite("Topic DAO") {
            fun testTopicEntity(
                id: String = "0",
                name: String,
            ) = TopicEntity(
                id = id,
                name = name,
                shortDescription = "",
                longDescription = "",
                url = "",
                imageUrl = "",
            )

            suspend fun insertTopics(topics: TopicDao) {
                val topicEntities = listOf(
                    testTopicEntity("1", "compose"),
                    testTopicEntity("2", "performance"),
                    testTopicEntity("3", "headline"),
                )
                topics.insertOrIgnoreTopics(topicEntities)
            }

            test("Get Topics") {
                with (it.topicDao) {
                    insertTopics(this)

                    val savedTopics = getTopicEntities().first()

                    assertEquals(
                        listOf("1", "2", "3"),
                        savedTopics.map { it.id },
                    )
                }
            }
        }
    }
}
