package com.example.test

import com.example.FakeDataSource
import com.example.DataRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataRepositoryTest {

    private lateinit var dataSource: FakeDataSource
    private lateinit var subject: DataRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        dataSource = FakeDataSource()
        subject = DataRepository(
            dataSource = dataSource,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun getItems_returnsAllItems() = runTest(testDispatcher) {
        val items = subject.getItems()
        assertEquals(3, items.size)
    }

    @Test
    fun getItems_whenEmpty_returnsEmptyList() = runTest(testDispatcher) {
        dataSource.clear()
        val items = subject.getItems()
        assertTrue(items.isEmpty())
    }

    @Test
    fun getItemById_returnsCorrectItem() = runTest(testDispatcher) {
        val item = subject.getItemById("1")
        assertEquals("1", item?.id)
    }
}
