/*
 * Copyright 2025 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFailsWith

private object TestFirstTopLevelKey : NavKey
private object TestSecondTopLevelKey : NavKey
private object TestThirdTopLevelKey : NavKey
private object TestKeyFirst : NavKey
private object TestKeySecond : NavKey

val NavigatorTest by testSuite {
    testFixture {
        val topLevelKeys = listOf(TestFirstTopLevelKey, TestSecondTopLevelKey, TestThirdTopLevelKey)
        object {
            val state = NavigationState(
                startKey = TestFirstTopLevelKey,
                topLevelStack = NavBackStack<NavKey>(TestFirstTopLevelKey),
                subStacks = topLevelKeys.associateWith { key -> NavBackStack(key) },
            )
            val navigator = Navigator(state)
        }
    } asContextForEach {
        test("start key") {
            assertThat(state.startKey).isEqualTo(TestFirstTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("navigate") {
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
            assertThat(state.subStacks[TestFirstTopLevelKey]?.last()).isEqualTo(TestKeyFirst)
        }

        test("navigate top level") {
            navigator.navigate(TestSecondTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestSecondTopLevelKey)
        }

        test("navigate single top") {
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst,
            ).inOrder()

            navigator.navigate(TestKeyFirst)
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst,
            ).inOrder()
        }

        test("navigate top level single top") {
            navigator.navigate(TestSecondTopLevelKey)
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentSubStack).containsExactly(
                TestSecondTopLevelKey, TestKeyFirst,
            ).inOrder()

            navigator.navigate(TestSecondTopLevelKey)
            assertThat(state.currentSubStack).containsExactly(
                TestSecondTopLevelKey,
            ).inOrder()
        }

        test("sub stack") {
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentKey).isEqualTo(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)

            navigator.navigate(TestKeySecond)
            assertThat(state.currentKey).isEqualTo(TestKeySecond)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("multi stack") {
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentKey).isEqualTo(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)

            navigator.navigate(TestSecondTopLevelKey)
            assertThat(state.currentKey).isEqualTo(TestSecondTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestSecondTopLevelKey)

            navigator.navigate(TestKeySecond)
            assertThat(state.currentKey).isEqualTo(TestKeySecond)
            assertThat(state.currentTopLevelKey).isEqualTo(TestSecondTopLevelKey)

            navigator.navigate(TestFirstTopLevelKey)
            assertThat(state.currentKey).isEqualTo(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("pop one non top level") {
            navigator.navigate(TestKeyFirst)
            navigator.navigate(TestKeySecond)
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst, TestKeySecond,
            ).inOrder()

            navigator.goBack()
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst,
            ).inOrder()
            assertThat(state.currentKey).isEqualTo(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("pop one top level") {
            navigator.navigate(TestKeyFirst)
            navigator.navigate(TestSecondTopLevelKey)
            assertThat(state.currentSubStack).containsExactly(TestSecondTopLevelKey).inOrder()
            assertThat(state.currentKey).isEqualTo(TestSecondTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestSecondTopLevelKey)

            navigator.goBack()
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst,
            ).inOrder()
            assertThat(state.currentKey).isEqualTo(TestKeyFirst)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("pop multiple non top level") {
            navigator.navigate(TestKeyFirst)
            navigator.navigate(TestKeySecond)
            assertThat(state.currentSubStack).containsExactly(
                TestFirstTopLevelKey, TestKeyFirst, TestKeySecond,
            ).inOrder()

            navigator.goBack()
            navigator.goBack()
            assertThat(state.currentSubStack).containsExactly(TestFirstTopLevelKey).inOrder()
            assertThat(state.currentKey).isEqualTo(TestFirstTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("pop multiple top level") {
            navigator.navigate(TestSecondTopLevelKey)
            navigator.navigate(TestKeyFirst)
            assertThat(state.currentSubStack).containsExactly(
                TestSecondTopLevelKey, TestKeyFirst,
            ).inOrder()

            navigator.navigate(TestThirdTopLevelKey)
            navigator.navigate(TestKeySecond)
            assertThat(state.currentSubStack).containsExactly(
                TestThirdTopLevelKey, TestKeySecond,
            ).inOrder()

            repeat(4) { navigator.goBack() }
            assertThat(state.currentSubStack).containsExactly(TestFirstTopLevelKey).inOrder()
            assertThat(state.currentKey).isEqualTo(TestFirstTopLevelKey)
            assertThat(state.currentTopLevelKey).isEqualTo(TestFirstTopLevelKey)
        }

        test("throw on empty back stack") {
            assertFailsWith<IllegalStateException> {
                navigator.goBack()
            }
        }
    }
}
