package com.focuslock.data.dao

import com.focuslock.data.db.FocusLockDatabase
import com.focuslock.data.entity.FocusSessionEntity
import com.focuslock.data.entity.SessionBlockedPackageEntity
import com.focuslock.domain.model.SessionStatus
import com.focuslock.testutil.buildInMemoryDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionDaoTest {

    private lateinit var db: FocusLockDatabase
    private lateinit var dao: SessionDao

    @Before
    fun setUp() {
        db = buildInMemoryDatabase()
        dao = db.sessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sessionEntity(
        id: String = "s1",
        status: SessionStatus = SessionStatus.ACTIVE,
    ) = FocusSessionEntity(
        id = id,
        profileId = "p1",
        profileNameSnapshot = "Deep Work",
        startedAtWallClockMillis = 1_000_000L,
        startedAtElapsedRealtimeMs = 5_000_000L,
        expiresAtWallClockMillis = 1_000_000L + 8 * 60 * 60 * 1000L,
        durationMillis = 8 * 60 * 60 * 1000L,
        enforcementMode = "HARD",
        fortressModeEnabled = false,
        mottoSnapshot = "",
        status = status.name,
    )

    @Test
    fun `save and load session with blocked packages`() = runTest {
        dao.saveSession(
            sessionEntity(),
            listOf(
                SessionBlockedPackageEntity("s1", "com.instagram.android"),
                SessionBlockedPackageEntity("s1", "com.reddit.frontpage"),
            ),
        )

        val loaded = dao.getById("s1")!!
        assertThat(loaded.session.profileNameSnapshot).isEqualTo("Deep Work")
        assertThat(loaded.blockedPackages.map { it.packageName })
            .containsExactly("com.instagram.android", "com.reddit.frontpage")
    }

    @Test
    fun `getActive returns the single non-terminal session`() = runTest {
        dao.saveSession(sessionEntity(id = "s1", status = SessionStatus.COMPLETED), emptyList())
        dao.saveSession(sessionEntity(id = "s2", status = SessionStatus.ACTIVE), emptyList())

        val active = dao.getActive()
        assertThat(active).isNotNull()
        assertThat(active!!.session.id).isEqualTo("s2")
    }

    @Test
    fun `getActive returns null when no active session`() = runTest {
        dao.saveSession(sessionEntity(id = "s1", status = SessionStatus.COMPLETED), emptyList())

        assertThat(dao.getActive()).isNull()
    }

    @Test
    fun `updateStatus changes session status`() = runTest {
        dao.saveSession(sessionEntity(id = "s1", status = SessionStatus.ACTIVE), emptyList())

        dao.updateStatus("s1", SessionStatus.COMPLETED.name)

        val loaded = dao.getById("s1")!!
        assertThat(SessionStatus.valueOf(loaded.session.status)).isEqualTo(SessionStatus.COMPLETED)
        assertThat(dao.getActive()).isNull()
    }

    @Test
    fun `observeActive emits active session`() = runTest {
        dao.saveSession(sessionEntity(id = "s1", status = SessionStatus.ACTIVE), emptyList())

        val observed = dao.observeActive().first()
        assertThat(observed).isNotNull()
        assertThat(observed!!.session.id).isEqualTo("s1")
    }
}
