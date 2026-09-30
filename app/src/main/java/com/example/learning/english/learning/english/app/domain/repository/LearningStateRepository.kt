package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import kotlinx.coroutines.flow.Flow

interface LearningStateRepository {
    fun observeAll(): Flow<List<LearningState>>

    fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?>

    suspend fun getByExpression(expressionId: ExpressionId): LearningState?

    suspend fun getAll(): List<LearningState>

    /** Revisiones vencidas en [packIds], para los contadores de Home. */
    fun observeDueCount(now: Long, packIds: Set<PackId>): Flow<Int>

    /** Media de reconocimiento y producción sobre lo ya revisado en [packIds]. */
    fun observeSkillAverages(packIds: Set<PackId>): Flow<SkillAverages>

    suspend fun upsert(state: LearningState)

    suspend fun upsertAll(states: List<LearningState>)
}
