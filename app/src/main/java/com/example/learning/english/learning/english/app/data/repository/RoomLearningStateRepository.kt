package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.persistence.dao.LearningStateDao
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [LearningStateRepository].
 *
 * Reconocimiento y producción se guardan por separado tal como llegan del
 * dominio: Room no calcula scores (README §3.3, §31).
 */
class RoomLearningStateRepository(
    private val learningStateDao: LearningStateDao,
) : LearningStateRepository {

    override fun observeAll(): Flow<List<LearningState>> =
        learningStateDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?> =
        learningStateDao.observeByExpression(expressionId.value).map { it?.toDomain() }

    override suspend fun getByExpression(expressionId: ExpressionId): LearningState? =
        learningStateDao.getByExpression(expressionId.value)?.toDomain()

    override suspend fun getAll(): List<LearningState> =
        learningStateDao.getAll().map { it.toDomain() }

    override suspend fun upsert(state: LearningState) {
        learningStateDao.upsert(state.toEntity())
    }

    override suspend fun upsertAll(states: List<LearningState>) {
        if (states.isEmpty()) return
        learningStateDao.upsertAll(states.map { it.toEntity() })
    }
}
