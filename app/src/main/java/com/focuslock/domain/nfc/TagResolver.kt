package com.focuslock.domain.nfc

import com.focuslock.domain.model.FocusProfile
import com.focuslock.domain.model.TagBinding
import com.focuslock.domain.repository.ProfileRepository
import com.focuslock.domain.repository.TagRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves an NFC tag token into a [FocusProfile] via [TagBinding].
 *
 * The tag only carries an identifier; all profile configuration is resolved
 * from the database.
 */
@Singleton
class TagResolver @Inject constructor(
    private val tagRepository: TagRepository,
    private val profileRepository: ProfileRepository,
) {
    suspend fun resolve(token: String): TagResolution {
        val binding = tagRepository.resolveByToken(token)
            ?: return TagResolution.UnknownTag
        val profile = profileRepository.getProfile(binding.profileId)
            ?: return TagResolution.ProfileMissing
        return TagResolution.Resolved(binding, profile)
    }
}

sealed interface TagResolution {
    data class Resolved(val binding: TagBinding, val profile: FocusProfile) : TagResolution
    data object UnknownTag : TagResolution
    data object ProfileMissing : TagResolution
}
