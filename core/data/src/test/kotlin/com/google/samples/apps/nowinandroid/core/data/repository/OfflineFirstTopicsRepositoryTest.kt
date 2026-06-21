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

import com.google.samples.apps.nowinandroid.core.data.Synchronizer
import com.google.samples.apps.nowinandroid.core.data.model.asEntity
import com.google.samples.apps.nowinandroid.core.data.testdoubles.CollectionType
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestTopicDao
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import com.google.samples.apps.nowinandroid.core.datastore.NiaPreferencesDataSource
import com.google.samples.apps.nowinandroid.core.datastore.UserPreferences
import com.google.samples.apps.nowinandroid.core.datastore.test.InMemoryDataStore
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkTopic
import de.infix.testBalloon.framework.testSuite
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals

val OfflineFirstTopicsRepositoryTest by testSuite {
    testFixture {
        object {
            val topicDao = TestTopicDao()
            val network = TestNiaNetworkDataSource()
            val synchronizer: Synchronizer = TestSynchronizer(
                NiaPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance())),
            )
            val subject = OfflineFirstTopicsRepository(
                topicDao = topicDao,
                network = network,
            )
        }
    } asContextForEach {
        test("topics stream is backed by topics dao") {
            subject.syncWith(synchronizer)

            assertEquals(
                topicDao.getTopicEntities().first().map(TopicEntity::asExternalModel),
                subject.getTopics().first(),
            )
        }

        test("sync pulls from network") {
            subject.syncWith(synchronizer)

            val networkTopics = network.getTopics().map(NetworkTopic::asEntity)
            val dbTopics = topicDao.getTopicEntities().first()

            assertEquals(
                networkTopics.map(TopicEntity::id),
                dbTopics.map(TopicEntity::id),
            )
            assertEquals(
                network.latestChangeListVersion(CollectionType.Topics),
                synchronizer.getChangeListVersions().topicVersion,
            )
        }

        test("incremental sync pulls from network") {
            synchronizer.updateChangeListVersions { copy(topicVersion = 10) }
            subject.syncWith(synchronizer)

            val networkTopics = network.getTopics().map(NetworkTopic::asEntity).drop(10)
            val dbTopics = topicDao.getTopicEntities().first()

            assertEquals(
                networkTopics.map(TopicEntity::id),
                dbTopics.map(TopicEntity::id),
            )
            assertEquals(
                network.latestChangeListVersion(CollectionType.Topics),
                synchronizer.getChangeListVersions().topicVersion,
            )
        }

        test("sync deletes items marked deleted on network") {
            val networkTopics = network.getTopics()
                .map(NetworkTopic::asEntity)
                .map(TopicEntity::asExternalModel)

            val deletedItems = networkTopics
                .map(Topic::id)
                .partition { it.chars().sum() % 2 == 0 }
                .first
                .toSet()

            deletedItems.forEach {
                network.editCollection(collectionType = CollectionType.Topics, id = it, isDelete = true)
            }

            subject.syncWith(synchronizer)

            val dbTopics = topicDao.getTopicEntities().first().map(TopicEntity::asExternalModel)

            assertEquals(
                networkTopics.map(Topic::id) - deletedItems,
                dbTopics.map(Topic::id),
            )
            assertEquals(
                network.latestChangeListVersion(CollectionType.Topics),
                synchronizer.getChangeListVersions().topicVersion,
            )
        }
    }
}
