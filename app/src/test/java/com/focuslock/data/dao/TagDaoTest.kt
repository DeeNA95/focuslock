package com.focuslock.data.dao

import com.focuslock.data.db.FocusLockDatabase
import com.focuslock.data.entity.TagBindingEntity
import com.focuslock.testutil.buildInMemoryDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TagDaoTest {

    private lateinit var db: FocusLockDatabase
    private lateinit var dao: TagDao

    @Before
    fun setUp() {
        db = buildInMemoryDatabase()
        dao = db.tagDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `upsert and resolve by token`() = runTest {
        dao.upsert(TagBindingEntity("t1", "token-1", "Desk", "p1"))

        assertThat(dao.getByToken("token-1")!!.label).isEqualTo("Desk")
        assertThat(dao.getByToken("missing")).isNull()
    }

    @Test
    fun `upsert with same id replaces`() = runTest {
        dao.upsert(TagBindingEntity("t1", "token-1", "Desk", "p1"))
        dao.upsert(TagBindingEntity("t1", "token-1", "Office", "p1"))

        assertThat(dao.getByToken("token-1")!!.label).isEqualTo("Office")
        assertThat(dao.getAll()).hasSize(1)
    }

    @Test
    fun `getAll orders by label`() = runTest {
        dao.upsert(TagBindingEntity("t1", "token-1", "Bedside", "p1"))
        dao.upsert(TagBindingEntity("t2", "token-2", "Desk", "p2"))

        assertThat(dao.getAll().map { it.label }).containsExactly("Bedside", "Desk").inOrder()
    }

    @Test
    fun `delete removes binding`() = runTest {
        dao.upsert(TagBindingEntity("t1", "token-1", "Desk", "p1"))
        dao.delete("t1")

        assertThat(dao.getByToken("token-1")).isNull()
    }
}
