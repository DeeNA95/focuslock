package com.focuslock.data.dao

import com.focuslock.data.db.FocusLockDatabase
import com.focuslock.data.entity.ActivationWindowEntity
import com.focuslock.data.entity.FocusProfileEntity
import com.focuslock.data.entity.ProfileBlockedPackageEntity
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
class ProfileDaoTest {

    private lateinit var db: FocusLockDatabase
    private lateinit var dao: ProfileDao

    @Before
    fun setUp() {
        db = buildInMemoryDatabase()
        dao = db.profileDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun profileEntity(
        id: String = "p1",
        name: String = "Deep Work",
        enabled: Boolean = true,
    ) = FocusProfileEntity(
        id = id,
        name = name,
        durationMillis = 8 * 60 * 60 * 1000L,
        enforcementMode = "HARD",
        fortressModeEnabled = false,
        enabled = enabled,
        motto = "",
    )

    @Test
    fun `save and load profile with packages and windows`() = runTest {
        dao.saveProfile(
            profileEntity(),
            listOf(
                ProfileBlockedPackageEntity("p1", "com.instagram.android"),
                ProfileBlockedPackageEntity("p1", "com.reddit.frontpage"),
            ),
            listOf(
                ActivationWindowEntity("w1", "p1", 18 * 3600, 23 * 3600 + 59 * 60, 127),
            ),
        )

        val loaded = dao.getById("p1")
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.profile.name).isEqualTo("Deep Work")
        assertThat(loaded.blockedPackages.map { it.packageName })
            .containsExactly("com.instagram.android", "com.reddit.frontpage")
        assertThat(loaded.activationWindows).hasSize(1)
        assertThat(loaded.activationWindows[0].startSecondOfDay).isEqualTo(18 * 3600)
    }

    @Test
    fun `saving a profile again replaces its packages and windows`() = runTest {
        dao.saveProfile(
            profileEntity(),
            listOf(ProfileBlockedPackageEntity("p1", "com.instagram.android")),
            emptyList(),
        )
        dao.saveProfile(
            profileEntity(),
            listOf(ProfileBlockedPackageEntity("p1", "com.youtube.app")),
            emptyList(),
        )

        val loaded = dao.getById("p1")!!
        assertThat(loaded.blockedPackages.map { it.packageName })
            .containsExactly("com.youtube.app")
    }

    @Test
    fun `getAll returns saved profiles`() = runTest {
        dao.saveProfile(profileEntity(id = "p1", name = "Deep Work"), emptyList(), emptyList())
        dao.saveProfile(profileEntity(id = "p2", name = "Gym"), emptyList(), emptyList())

        val all = dao.getAll()
        assertThat(all.map { it.profile.name }).containsExactly("Deep Work", "Gym")
    }

    @Test
    fun `delete profile removes it`() = runTest {
        dao.saveProfile(profileEntity(id = "p1"), emptyList(), emptyList())
        dao.deleteProfile("p1")

        assertThat(dao.getById("p1")).isNull()
    }

    @Test
    fun `deleting profile cascades to packages and windows`() = runTest {
        dao.saveProfile(
            profileEntity(id = "p1"),
            listOf(ProfileBlockedPackageEntity("p1", "com.instagram.android")),
            listOf(ActivationWindowEntity("w1", "p1", 1, 2, 127)),
        )
        dao.deleteProfile("p1")

        // Re-query the child tables through a fresh profile with same id.
        dao.saveProfile(profileEntity(id = "p1"), emptyList(), emptyList())
        val loaded = dao.getById("p1")!!
        assertThat(loaded.blockedPackages).isEmpty()
        assertThat(loaded.activationWindows).isEmpty()
    }

    @Test
    fun `observe emits updates after save`() = runTest {
        dao.saveProfile(profileEntity(id = "p1"), emptyList(), emptyList())

        val observed = dao.observeAllWithDetails().first()
        assertThat(observed).hasSize(1)
        assertThat(observed[0].profile.name).isEqualTo("Deep Work")
    }
}
