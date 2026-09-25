package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeHistory(expressionId: ExpressionId): Flow<List<ReviewEvent>>

    fun observeAll(): Flow<List<ReviewEvent>>

    suspend fun record(event: ReviewEvent)
}
