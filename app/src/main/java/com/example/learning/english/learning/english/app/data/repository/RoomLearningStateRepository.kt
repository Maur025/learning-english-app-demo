package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.persistence.dao.LearningStateDao
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [LearningStateRepository].
 *
 * Reconocimiento y producción se guardan por separado tal como llegan del
 * dominio: Room no calcula scores (README §3.3, §31).
 *
 * Los agregados se calculan en SQL porque la UI los observa en cada cambio de
 * estado; recorrer todas las filas en memoria para contar lo mismo sería trabajo
 * desperdiciado en la capa de datos.
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

    override fun observeDueCount(now: Long, packIds: Set<PackId>): Flow<Int> =
        if (packIds.isEmpty()) {
            flowOf(0)
        } else {
            learningStateDao.observeDueCount(now, packIds.toStorageIds()).map { it }
        }

    override fun observeSkillAverages(packIds: Set<PackId>): Flow<SkillAverages> =
        if (packIds.isEmpty()) {
            flowOf(SkillAverages.EMPTY)
        } else {
            learningStateDao.observeSkillAverages(packIds.toStorageIds()).map { row ->
                SkillAverages.fromScores(row.avgRecognition, row.avgProduction)
            }
        }

    override suspend fun upsert(state: LearningState) {
        learningStateDao.upsert(state.toEntity())
    }

    override suspend fun upsertAll(states: List<LearningState>) {
        if (states.isEmpty()) return
        learningStateDao.upsertAll(states.map { it.toEntity() })
    }
}
