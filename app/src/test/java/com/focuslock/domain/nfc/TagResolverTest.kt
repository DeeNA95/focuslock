package com.focuslock.domain.nfc

import com.focuslock.domain.model.EnforcementMode
import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.TagBinding
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.repository.TagRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Duration
import java.util.UUID

class TagResolverTest {

    private class FakeTagRepository(
        private val bindings: Map<String, TagBinding> = emptyMap(),
    ) : TagRepository {
        override suspend fun saveBinding(binding: TagBinding) {}
        override suspend fun resolveByToken(token: String): TagBinding? = bindings[token]
        override fun observeBindings(): Flow<List<TagBinding>> = flowOf(emptyList())
        override suspend fun getBindings(): List<TagBinding> = emptyList()
        override suspend fun deleteBinding(id: UUID) {}
    }

    private class FakeProfileRepository(
        private val profiles: Map<UUID, FocusProfile> = emptyMap(),
    ) : ProfileRepository {
        override fun observeProfiles(): Flow<List<FocusProfile>> = flowOf(emptyList())
        override fun observeProfile(id: UUID): Flow<FocusProfile?> = flowOf(null)
        override suspend fun getProfile(id: UUID): FocusProfile? = profiles[id]
        override suspend fun getAllProfiles(): List<FocusProfile> = emptyList()
        override suspend fun saveProfile(profile: FocusProfile) {}
        override suspend fun deleteProfile(id: UUID) {}
    }

    @Test
    fun `unknown token resolves to unknown tag`() = runTest {
        val resolver = TagResolver(FakeTagRepository(), FakeProfileRepository())
        assertThat(resolver.resolve("missing")).isEqualTo(TagResolution.UnknownTag)
    }

    @Test
    fun `known token with missing profile resolves to profile missing`() = runTest {
        val profileId = UUID.randomUUID()
        val binding = TagBinding(UUID.randomUUID(), "token-1", "Desk", profileId)
        val resolver = TagResolver(
            FakeTagRepository(mapOf("token-1" to binding)),
            FakeProfileRepository(emptyMap()),
        )
        assertThat(resolver.resolve("token-1")).isEqualTo(TagResolution.ProfileMissing)
    }

    @Test
    fun `known token resolves to profile`() = runTest {
        val profileId = UUID.randomUUID()
        val binding = TagBinding(UUID.randomUUID(), "token-1", "Desk", profileId)
        val profile = FocusProfile(
            id = profileId,
            name = "Deep Work",
            duration = Duration.ofHours(8),
            blockedPackages = setOf("com.instagram.android"),
            enforcementMode = EnforcementMode.HARD,
        )
        val resolver = TagResolver(
            FakeTagRepository(mapOf("token-1" to binding)),
            FakeProfileRepository(mapOf(profileId to profile)),
        )

        val result = resolver.resolve("token-1")

        assertThat(result).isEqualTo(TagResolution.Resolved(binding, profile))
    }
}
