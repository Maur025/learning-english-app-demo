package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.persistence.dao.ContentPackDao
import com.example.learning.english.learning.english.app.domain.model.ContentPack
import com.example.learning.english.learning.english.app.domain.repository.ContentPackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [ContentPackRepository].
 *
 * Solo lee: los packs se crean al importar el contenido, no desde la UI.
 */
class RoomContentPackRepository(
    private val contentPackDao: ContentPackDao,
) : ContentPackRepository {

    override fun observeAll(): Flow<List<ContentPack>> =
        contentPackDao.observeAllWithCount().map { rows -> rows.map { it.toDomain() } }
}
