package com.focuslock.domain.repository

import com.focuslock.domain.model.TagBinding
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TagRepository {
    suspend fun saveBinding(binding: TagBinding)
    suspend fun resolveByToken(token: String): TagBinding?
    fun observeBindings(): Flow<List<TagBinding>>
    suspend fun getBindings(): List<TagBinding>
    suspend fun deleteBinding(id: UUID)
}
