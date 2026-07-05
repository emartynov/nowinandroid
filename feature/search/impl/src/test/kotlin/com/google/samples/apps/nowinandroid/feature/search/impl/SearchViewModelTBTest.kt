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

package com.google.samples.apps.nowinandroid.feature.search.impl

import androidx.lifecycle.SavedStateHandle
import com.google.samples.apps.nowinandroid.core.analytics.NoOpAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.domain.GetRecentSearchQueriesUseCase
import com.google.samples.apps.nowinandroid.core.domain.GetSearchContentsUseCase
import com.google.samples.apps.nowinandroid.core.testing.data.newsResourcesTestData
import com.google.samples.apps.nowinandroid.core.testing.data.topicsTestData
import com.google.samples.apps.nowinandroid.core.testing.repository.TestRecentSearchRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestSearchContentsRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.TestUserDataRepository
import com.google.samples.apps.nowinandroid.core.testing.repository.emptyUserData
import com.google.samples.apps.nowinandroid.feature.search.impl.SearchResultUiState.EmptyQuery
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.aroundEachTest
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

val SearchViewModelBTTest by testSuite(
    testConfig = TestConfig
        .aroundEachTest { action ->
            Dispatchers.setMain(UnconfinedTestDispatcher())
            try {
                action()
            } finally {
                Dispatchers.resetMain()
            }
        },
) {
    testFixture {
        object {
            val userDataRepository = TestUserDataRepository()
            val searchContentsRepository = TestSearchContentsRepository()
            val getSearchContentsUseCase = GetSearchContentsUseCase(
                searchContentsRepository = searchContentsRepository,
                userDataRepository = userDataRepository,
            )
            val recentSearchRepository = TestRecentSearchRepository()
            val getRecentQueryUseCase =
                GetRecentSearchQueriesUseCase(recentSearchRepository)

            val viewModel = SearchViewModel(
                getSearchContentsUseCase = getSearchContentsUseCase,
                recentSearchQueriesUseCase = getRecentQueryUseCase,
                searchContentsRepository = searchContentsRepository,
                savedStateHandle = SavedStateHandle(),
                recentSearchRepository = recentSearchRepository,
                userDataRepository = userDataRepository,
                analyticsHelper = NoOpAnalyticsHelper(),
            ).also {
                userDataRepository.setUserData(emptyUserData)
            }
        }
    } asParameterForEach {

        test("stateIsInitiallyLoading") {
            assertEquals(SearchResultUiState.Loading, it.viewModel.searchResultUiState.value)
        }


        test("stateIsEmptyQuery_withEmptySearchQuery") {
            it.searchContentsRepository.addNewsResources(newsResourcesTestData)
            it.searchContentsRepository.addTopics(topicsTestData)
            testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.searchResultUiState.collect() }

            it.viewModel.onSearchQueryChanged("")

            assertEquals(EmptyQuery, it.viewModel.searchResultUiState.value)
        }

        test("emptyResultIsReturned_withNotMatchingQuery") {
            testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.searchResultUiState.collect() }

            it.viewModel.onSearchQueryChanged("XXX")
            it.searchContentsRepository.addNewsResources(newsResourcesTestData)
            it.searchContentsRepository.addTopics(topicsTestData)

            val result = it.viewModel.searchResultUiState.value
            assertIs<SearchResultUiState.Success>(result)
        }

        test("recentSearches_verifyUiStateIsSuccess") {
            testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.recentSearchQueriesUiState.collect() }
            it.viewModel.onSearchTriggered("kotlin")

            val result = it.viewModel.recentSearchQueriesUiState.value
            assertIs<RecentSearchQueriesUiState.Success>(result)
        }

        test("searchNotReady_withNoFtsTableEntity") {
            testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.searchResultUiState.collect() }

            it.viewModel.onSearchQueryChanged("")

            assertEquals(SearchResultUiState.SearchNotReady, it.viewModel.searchResultUiState.value)
        }

        test("emptySearchText_isNotAddedToRecentSearches") {
            it.viewModel.onSearchTriggered("")

            val recentSearchQueriesStream = it.getRecentQueryUseCase()
            val recentSearchQueries = recentSearchQueriesStream.first()
            val recentSearchQuery = recentSearchQueries.firstOrNull()

            assertNull(recentSearchQuery)
        }

        test("searchTextWithThreeSpaces_isEmptyQuery") {
            it.searchContentsRepository.addNewsResources(newsResourcesTestData)
            it.searchContentsRepository.addTopics(topicsTestData)
            val collectJob =
                testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.searchResultUiState.collect() }

            it.viewModel.onSearchQueryChanged("   ")

            assertIs<EmptyQuery>(it.viewModel.searchResultUiState.value)

            collectJob.cancel()
        }

        test("searchTextWithThreeSpacesAndOneLetter_isEmptyQuery") {
            it.searchContentsRepository.addNewsResources(newsResourcesTestData)
            it.searchContentsRepository.addTopics(topicsTestData)
            val collectJob =
                testScope.backgroundScope.launch(UnconfinedTestDispatcher()) { it.viewModel.searchResultUiState.collect() }

            it.viewModel.onSearchQueryChanged("   a")

            assertIs<EmptyQuery>(it.viewModel.searchResultUiState.value)

            collectJob.cancel()
        }

        test("whenToggleNewsResourceSavedIsCalled_bookmarkStateIsUpdated") {
            val newsResourceId = "123"
            it.viewModel.setNewsResourceBookmarked(newsResourceId, true)

            assertEquals(
                expected = setOf(newsResourceId),
                actual = it.userDataRepository.userData.first().bookmarkedNewsResources,
            )

            it.viewModel.setNewsResourceBookmarked(newsResourceId, false)

            assertEquals(
                expected = emptySet(),
                actual = it.userDataRepository.userData.first().bookmarkedNewsResources,
            )
        }
    }
}
