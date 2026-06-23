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
import com.google.samples.apps.nowinandroid.core.data.model.topicCrossReferences
import com.google.samples.apps.nowinandroid.core.data.model.topicEntityShells
import com.google.samples.apps.nowinandroid.core.data.testdoubles.CollectionType
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNewsResourceDao
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestNiaNetworkDataSource
import com.google.samples.apps.nowinandroid.core.data.testdoubles.TestTopicDao
import com.google.samples.apps.nowinandroid.core.data.testdoubles.filteredInterestsIds
import com.google.samples.apps.nowinandroid.core.data.testdoubles.nonPresentInterestsIds
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceEntity
import com.google.samples.apps.nowinandroid.core.database.model.NewsResourceTopicCrossRef
import com.google.samples.apps.nowinandroid.core.database.model.PopulatedNewsResource
import com.google.samples.apps.nowinandroid.core.database.model.TopicEntity
import com.google.samples.apps.nowinandroid.core.database.model.asExternalModel
import com.google.samples.apps.nowinandroid.core.datastore.NiaPreferencesDataSource
import com.google.samples.apps.nowinandroid.core.datastore.UserPreferences
import com.google.samples.apps.nowinandroid.core.datastore.test.InMemoryDataStore
import com.google.samples.apps.nowinandroid.core.model.data.NewsResource
import com.google.samples.apps.nowinandroid.core.model.data.Topic
import com.google.samples.apps.nowinandroid.core.network.model.NetworkChangeList
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsResource
import com.google.samples.apps.nowinandroid.core.testing.notifications.TestNotifier
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.first
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val OfflineFirstNewsRepositoryTest by testSuite {
    testFixture {
        object {
            val prefs = NiaPreferencesDataSource(InMemoryDataStore(UserPreferences.getDefaultInstance()))
            val newsDao = TestNewsResourceDao()
            val topicDao = TestTopicDao()
            val net = TestNiaNetworkDataSource()
            val notif = TestNotifier()
            val synchronizer: Synchronizer = TestSynchronizer(prefs)
            val subject = OfflineFirstNewsRepository(
                niaPreferencesDataSource = prefs,
                newsResourceDao = newsDao,
                topicDao = topicDao,
                network = net,
                notifier = notif,
            )
        }
    } asContextForEach {
        test("news resources stream is backed by news resource dao") {
            subject.syncWith(synchronizer)
            assertEquals(
                newsDao.getNewsResources().first().map(PopulatedNewsResource::asExternalModel),
                subject.getNewsResources().first(),
            )
        }

        test("news resources for topic is backed by news resource dao") {
            assertEquals(
                expected = newsDao.getNewsResources(
                    filterTopicIds = filteredInterestsIds,
                    useFilterTopicIds = true,
                ).first().map(PopulatedNewsResource::asExternalModel),
                actual = subject.getNewsResources(query = NewsResourceQuery(filterTopicIds = filteredInterestsIds)).first(),
            )
            assertEquals(
                expected = emptyList(),
                actual = subject.getNewsResources(query = NewsResourceQuery(filterTopicIds = nonPresentInterestsIds)).first(),
            )
        }

        test("sync pulls from network") {
            prefs.setShouldHideOnboarding(false)
            subject.syncWith(synchronizer)

            val fromNetwork = net.getNewsResources().map(NetworkNewsResource::asEntity).map(NewsResourceEntity::asExternalModel)
            val fromDb = newsDao.getNewsResources().first().map(PopulatedNewsResource::asExternalModel)

            assertEquals(fromNetwork.map(NewsResource::id).sorted(), fromDb.map(NewsResource::id).sorted())
            assertEquals(net.latestChangeListVersion(CollectionType.NewsResources), synchronizer.getChangeListVersions().newsResourceVersion)
            assertTrue(notif.addedNewsResources.isEmpty())
        }

        test("sync deletes items marked deleted on network") {
            prefs.setShouldHideOnboarding(false)

            val fromNetwork = net.getNewsResources().map(NetworkNewsResource::asEntity).map(NewsResourceEntity::asExternalModel)
            val deletedItems = fromNetwork.map(NewsResource::id).partition { it.chars().sum() % 2 == 0 }.first.toSet()

            deletedItems.forEach {
                net.editCollection(collectionType = CollectionType.NewsResources, id = it, isDelete = true)
            }
            subject.syncWith(synchronizer)

            val fromDb = newsDao.getNewsResources().first().map(PopulatedNewsResource::asExternalModel)
            assertEquals(
                (fromNetwork.map(NewsResource::id) - deletedItems).sorted(),
                fromDb.map(NewsResource::id).sorted(),
            )
            assertEquals(net.latestChangeListVersion(CollectionType.NewsResources), synchronizer.getChangeListVersions().newsResourceVersion)
            assertTrue(notif.addedNewsResources.isEmpty())
        }

        test("incremental sync pulls from network") {
            prefs.setShouldHideOnboarding(false)
            synchronizer.updateChangeListVersions { copy(newsResourceVersion = 7) }
            subject.syncWith(synchronizer)

            val changeList = net.changeListsAfter(CollectionType.NewsResources, version = 7)
            val changeListIds = changeList.map(NetworkChangeList::id).toSet()
            val fromNetwork = net.getNewsResources()
                .map(NetworkNewsResource::asEntity)
                .map(NewsResourceEntity::asExternalModel)
                .filter { it.id in changeListIds }
            val fromDb = newsDao.getNewsResources().first().map(PopulatedNewsResource::asExternalModel)

            assertEquals(fromNetwork.map(NewsResource::id).sorted(), fromDb.map(NewsResource::id).sorted())
            assertEquals(changeList.last().changeListVersion, synchronizer.getChangeListVersions().newsResourceVersion)
            assertTrue(notif.addedNewsResources.isEmpty())
        }

        test("sync saves shell topic entities") {
            subject.syncWith(synchronizer)
            assertEquals(
                expected = net.getNewsResources().map(NetworkNewsResource::topicEntityShells).flatten()
                    .distinctBy(TopicEntity::id).sortedBy(TopicEntity::toString),
                actual = newsDao.getNewsResources().first().map { it.topics }.flatten()
                    .map { TopicEntity(it.id, it.name, it.shortDescription, it.longDescription, it.url, it.imageUrl) }
                    .distinctBy(TopicEntity::id).sortedBy(TopicEntity::toString),
            )
        }

        test("sync saves topic cross references") {
            subject.syncWith(synchronizer)
            assertEquals(
                expected = net.getNewsResources().map(NetworkNewsResource::topicCrossReferences).flatten()
                    .distinct().sortedBy(NewsResourceTopicCrossRef::toString),
                actual = newsDao.topicCrossReferences.sortedBy(NewsResourceTopicCrossRef::toString),
            )
        }

        test("sync marks as read on first run") {
            subject.syncWith(synchronizer)
            assertEquals(
                net.getNewsResources().map { it.id }.toSet(),
                prefs.userData.first().viewedNewsResources,
            )
        }

        test("sync does not mark as read on subsequent run") {
            synchronizer.updateChangeListVersions { copy(newsResourceVersion = 7) }
            subject.syncWith(synchronizer)
            assertEquals(emptySet(), prefs.userData.first().viewedNewsResources)
        }

        test("sends notifications for newly synced news that is followed") {
            prefs.setShouldHideOnboarding(true)
            val networkNewsResources = net.getNewsResources()
            val followedTopicIds = networkNewsResources
                .flatMap(NetworkNewsResource::topicEntityShells)
                .mapNotNull { topic -> if (topic.id.chars().sum() % 2 == 0) topic.id else null }
                .toSet()

            prefs.setFollowedTopicIds(followedTopicIds)
            subject.syncWith(synchronizer)

            val followedIds = networkNewsResources
                .filter { (it.topics intersect followedTopicIds).isNotEmpty() }
                .map(NetworkNewsResource::id).sorted()

            assertEquals(followedIds, notif.addedNewsResources.first().map(NewsResource::id).sorted())
        }

        test("does not send notifications for existing news resources") {
            prefs.setShouldHideOnboarding(true)
            val networkNewsResources = net.getNewsResources().map(NetworkNewsResource::asEntity)
            newsDao.upsertNewsResources(networkNewsResources)

            val followedTopicIds = networkNewsResources.map(NewsResourceEntity::asExternalModel)
                .flatMap(NewsResource::topics).map(Topic::id).toSet()
            prefs.setFollowedTopicIds(followedTopicIds)
            subject.syncWith(synchronizer)

            assertTrue(notif.addedNewsResources.isEmpty())
        }
    }
}
