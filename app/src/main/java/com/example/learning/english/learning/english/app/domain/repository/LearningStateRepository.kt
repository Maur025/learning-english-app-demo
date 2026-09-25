package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningState
import kotlinx.coroutines.flow.Flow

interface LearningStateRepository {
    fun observeAll(): Flow<List<LearningState>>

    fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?>

    suspend fun getByExpression(expressionId: ExpressionId): LearningState?

    suspend fun upsert(state: LearningState)

    suspend fun upsertAll(states: List<LearningState>)
}
