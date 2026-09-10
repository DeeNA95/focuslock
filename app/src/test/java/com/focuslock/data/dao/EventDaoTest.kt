package com.focuslock.data.dao

import com.focuslock.data.db.FocusLockDatabase
import com.focuslock.data.entity.SessionEventEntity
import com.focuslock.testutil.buildInMemoryDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventDaoTest {

    private lateinit var db: FocusLockDatabase
    private lateinit var dao: EventDao

    @Before
    fun setUp() {
        db = buildInMemoryDatabase()
        dao = db.eventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and read recent events ordered by timestamp desc`() = runTest {
        dao.insert(SessionEventEntity(timestampMillis = 100L, type = "A", sessionId = null, detail = null))
        dao.insert(SessionEventEntity(timestampMillis = 300L, type = "C", sessionId = null, detail = null))
        dao.insert(SessionEventEntity(timestampMillis = 200L, type = "B", sessionId = null, detail = null))

        val recent = dao.getRecent(10)
        assertThat(recent.map { it.type }).containsExactly("C", "B", "A").inOrder()
    }

    @Test
    fun `getRecent respects limit`() = runTest {
        dao.insert(SessionEventEntity(timestampMillis = 100L, type = "A", sessionId = null, detail = null))
        dao.insert(SessionEventEntity(timestampMillis = 200L, type = "B", sessionId = null, detail = null))
        dao.insert(SessionEventEntity(timestampMillis = 300L, type = "C", sessionId = null, detail = null))

        assertThat(dao.getRecent(2)).hasSize(2)
    }

    @Test
    fun `insert auto-generates id`() = runTest {
        val id = dao.insert(SessionEventEntity(timestampMillis = 100L, type = "A", sessionId = null, detail = null))
        assertThat(id).isGreaterThan(0L)
    }
}
