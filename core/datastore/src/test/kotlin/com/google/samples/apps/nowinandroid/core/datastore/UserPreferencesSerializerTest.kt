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

package com.google.samples.apps.nowinandroid.core.datastore

import androidx.datastore.core.CorruptionException
import de.infix.testBalloon.framework.core.testSuite
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

val UserPreferencesSerializerTest by testSuite {
    val userPreferencesSerializer = UserPreferencesSerializer()

    test("defaultUserPreferences_isEmpty") {
        assertEquals(
            userPreferences {
                // Default value
            },
            userPreferencesSerializer.defaultValue,
        )
    }

    test("writingAndReadingUserPreferences_outputsCorrectValue") {
        val expectedUserPreferences = userPreferences {
            followedTopicIds.put("0", true)
            followedTopicIds.put("1", true)
        }

        val outputStream = ByteArrayOutputStream()

        expectedUserPreferences.writeTo(outputStream)

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())

        val actualUserPreferences = userPreferencesSerializer.readFrom(inputStream)

        assertEquals(
            expectedUserPreferences,
            actualUserPreferences,
        )
    }

    test("readingInvalidUserPreferences_throwsCorruptionException") {
        assertFailsWith<CorruptionException> {
            userPreferencesSerializer.readFrom(ByteArrayInputStream(byteArrayOf(0)))
        }
    }
}
