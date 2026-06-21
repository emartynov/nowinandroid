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

package com.google.samples.apps.nowinandroid.sync.workers

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import de.infix.testBalloon.framework.JUnit4RulesContext
import de.infix.testBalloon.framework.testSuite
import kotlin.test.assertEquals

@HiltAndroidTest
val SyncWorkerTest by testSuite {
    testFixture {
        object : JUnit4RulesContext() {
            val hiltRule = rule(HiltAndroidRule(this), order = 0)
        }
    } asContextForEach {
        val context = InstrumentationRegistry.getInstrumentation().context

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        test("testSyncWork") {
            // Create request
            val request = SyncWorker.startUpSyncWork()

            val workManager = WorkManager.getInstance(context)
            val testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!

            // Enqueue and wait for result.
            workManager.enqueue(request).result.get()

            // Get WorkInfo and outputData
            val preRunWorkInfo = workManager.getWorkInfoById(request.id).get()

            // Assert
            assertEquals(WorkInfo.State.ENQUEUED, preRunWorkInfo?.state)

            // Tells the testing framework that the constraints have been met
            testDriver.setAllConstraintsMet(request.id)

            val postRequirementWorkInfo = workManager.getWorkInfoById(request.id).get()
            assertEquals(WorkInfo.State.RUNNING, postRequirementWorkInfo?.state)
        }
    }
}
