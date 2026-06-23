/*
 * Copyright 2023 The Android Open Source Project
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
import com.google.samples.apps.nowinandroid.core.testing.util.mainDispatcherTestConfig
import com.google.samples.apps.nowinandroid.feature.search.impl.RecentSearchQueriesUiState.Success
import com.google.samples.apps.nowinandroid.feature.search.impl.SearchResultUiState.EmptyQuery
import com.google.samples.apps.nowinandroid.feature.search.impl.SearchResultUiState.Loading
import com.google.samples.apps.nowinandroid.feature.search.impl.SearchResultUiState.SearchNotReady
import de.infix.testBalloon.framework.core.testSuite
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * To learn more about how this test handles Flows created with stateIn, see
 * https://developer.android.com/kotlin/flow/test#statein
 */
val SearchViewModelTest by testSuite(testConfig = mainDispatcherTestConfig) {
    testFixture {
        object {
            val userDataRepository = TestUserDataRepository()
            val searchContentsRepository = TestSearchContentsRepository()
            val getSearchContentsUseCase = GetSearchContentsUseCase(
                searchContentsRepository = searchContentsRepository,
                userDataRepository = userDataRepository,
            )
            val recentSearchRepository = TestRecentSearchRepository()
            val getRecentQueryUseCase = GetRecentSearchQueriesUseCase(recentSearchRepository)
            val viewModel = SearchViewModel(
                getSearchContentsUseCase = getSearchContentsUseCase,
                recentSearchQueriesUseCase = getRecentQueryUseCase,
                searchContentsRepository = searchContentsRepository,
                savedStateHandle = SavedStateHandle(),
                recentSearchRepository = recentSearchRepository,
                userDataRepository = userDataRepository,
                analyticsHelper = NoOpAnalyticsHelper(),
            )
        }
    } asContextForEach {
        userDataRepository.setUserData(emptyUserData)

        test("stateIsInitiallyLoading") {
            assertEquals(Loading, viewModel.searchResultUiState.value)
        }

        test("stateIsEmptyQuery withEmptySearchQuery") {
            searchContentsRepository.addNewsResources(newsResourcesTestData)
            searchContentsRepository.addTopics(topicsTestData)
            backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

            viewModel.onSearchQueryChanged("")

            assertEquals(EmptyQuery, viewModel.searchResultUiState.value)
        }

        test("emptyResultIsReturned withNotMatchingQuery") {
            backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

            viewModel.onSearchQueryChanged("XXX")
            searchContentsRepository.addNewsResources(newsResourcesTestData)
            searchContentsRepository.addTopics(topicsTestData)

            val result = viewModel.searchResultUiState.value
            assertIs<SearchResultUiState.Success>(result)
        }

        test("recentSearches verifyUiStateIsSuccess") {
            backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.recentSearchQueriesUiState.collect() }
            viewModel.onSearchTriggered("kotlin")

            val result = viewModel.recentSearchQueriesUiState.value
            assertIs<Success>(result)
        }

        test("searchNotReady withNoFtsTableEntity") {
            backgroundScope.launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

            viewModel.onSearchQueryChanged("")

            assertEquals(SearchNotReady, viewModel.searchResultUiState.value)
        }

        test("emptySearchText isNotAddedToRecentSearches") {
            viewModel.onSearchTriggered("")

            val recentSearchQueriesStream = getRecentQueryUseCase()
            val recentSearchQueries = recentSearchQueriesStream.first()
            val recentSearchQuery = recentSearchQueries.firstOrNull()

            assertNull(recentSearchQuery)
        }

        test("searchTextWithThreeSpaces isEmptyQuery") {
            searchContentsRepository.addNewsResources(newsResourcesTestData)
            searchContentsRepository.addTopics(topicsTestData)
            val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

            viewModel.onSearchQueryChanged("   ")

            assertIs<EmptyQuery>(viewModel.searchResultUiState.value)

            collectJob.cancel()
        }

        test("searchTextWithThreeSpacesAndOneLetter isEmptyQuery") {
            searchContentsRepository.addNewsResources(newsResourcesTestData)
            searchContentsRepository.addTopics(topicsTestData)
            val collectJob = launch(UnconfinedTestDispatcher()) { viewModel.searchResultUiState.collect() }

            viewModel.onSearchQueryChanged("   a")

            assertIs<EmptyQuery>(viewModel.searchResultUiState.value)

            collectJob.cancel()
        }

        test("whenToggleNewsResourceSavedIsCalled bookmarkStateIsUpdated") {
            val newsResourceId = "123"
            viewModel.setNewsResourceBookmarked(newsResourceId, true)

            assertEquals(
                expected = setOf(newsResourceId),
                actual = userDataRepository.userData.first().bookmarkedNewsResources,
            )

            viewModel.setNewsResourceBookmarked(newsResourceId, false)

            assertEquals(
                expected = emptySet(),
                actual = userDataRepository.userData.first().bookmarkedNewsResources,
            )
        }
    }
}
