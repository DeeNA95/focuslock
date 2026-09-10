package com.focuslock.data.repository

import com.focuslock.data.dao.TagDao
import com.focuslock.data.db.toDomain
import com.focuslock.data.db.toEntity
import com.focuslock.domain.model.TagBinding
import com.focuslock.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTagRepository @Inject constructor(
    private val tagDao: TagDao,
) : TagRepository {

    override suspend fun saveBinding(binding: TagBinding) {
        tagDao.upsert(binding.toEntity())
    }

    override suspend fun resolveByToken(token: String): TagBinding? =
        tagDao.getByToken(token)?.toDomain()

    override fun observeBindings(): Flow<List<TagBinding>> =
        tagDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getBindings(): List<TagBinding> =
        tagDao.getAll().map { it.toDomain() }

    override suspend fun deleteBinding(id: UUID) {
        tagDao.delete(id.toString())
    }
}
