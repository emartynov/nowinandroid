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

package com.google.samples.apps.nowinandroid.core.database.dao

import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceTopicCrossRef
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlin.test.assertEquals

internal val NewsResourceDaoTest by testSuite {
    niaDbFixture() asContextForEach {
        val newsResourceEntities = listOf(
            testNewsResource(id = "0", millisSinceEpoch = 0),
            testNewsResource(id = "1", millisSinceEpoch = 3),
            testNewsResource(id = "2", millisSinceEpoch = 1),
            testNewsResource(id = "3", millisSinceEpoch = 2),
        )
        val topicEntities = listOf(
            testTopicEntity(id = "1", name = "1"),
            testTopicEntity(id = "2", name = "2"),
        )

        suspend fun insertTopicsAndResources() {
            topicDao.insertOrIgnoreTopics(topicEntities = topicEntities)
            newsResourceDao.upsertNewsResources(newsResourceEntities)
            newsResourceDao.insertOrIgnoreTopicCrossRefEntities(
                topicEntities.mapIndexed { index, topic ->
                    NewsResourceTopicCrossRef(newsResourceId = index.toString(), topicId = topic.id)
                },
            )
        }

        test("getNewsResources_allEntries_areOrderedByPublishDateDesc") {
            newsResourceDao.upsertNewsResources(newsResourceEntities)

            assertEquals(
                listOf(3L, 2L, 1L, 0L),
                newsResourceDao.getNewsResources().first()
                    .map { it.asExternalModel().publishDate.toEpochMilliseconds() },
            )
        }

        test("getNewsResources_filteredById_areOrderedByDescendingPublishDate") {
            newsResourceDao.upsertNewsResources(newsResourceEntities)

            assertEquals(
                listOf("3", "0"),
                newsResourceDao.getNewsResources(
                    useFilterNewsIds = true,
                    filterNewsIds = setOf("3", "0"),
                ).first().map { it.entity.id },
            )
        }

        test("getNewsResources_filteredByTopicId_areOrderedByDescendingPublishDate") {
            insertTopicsAndResources()

            assertEquals(
                listOf("1", "0"),
                newsResourceDao.getNewsResources(
                    useFilterTopicIds = true,
                    filterTopicIds = topicEntities.map(TopicEntity::id).toSet(),
                ).first().map { it.entity.id },
            )
        }

        test("getNewsResources_filteredByIdAndTopicId_areOrderedByDescendingPublishDate") {
            insertTopicsAndResources()

            assertEquals(
                listOf("1"),
                newsResourceDao.getNewsResources(
                    useFilterTopicIds = true,
                    filterTopicIds = topicEntities.map(TopicEntity::id).toSet(),
                    useFilterNewsIds = true,
                    filterNewsIds = setOf("1"),
                ).first().map { it.entity.id },
            )
        }

        test("deleteNewsResources_byId") {
            newsResourceDao.upsertNewsResources(newsResourceEntities)

            val (toDelete, toKeep) = newsResourceEntities.partition { it.id.toInt() % 2 == 0 }
            newsResourceDao.deleteNewsResources(toDelete.map(NewsResourceEntity::id))

            assertEquals(
                toKeep.map(NewsResourceEntity::id).toSet(),
                newsResourceDao.getNewsResources().first().map { it.entity.id }.toSet(),
            )
        }
    }
}

private fun testNewsResource(
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
