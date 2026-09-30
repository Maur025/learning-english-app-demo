package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.persistence.dao.ReviewEventDao
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [ReviewRepository].
 *
 * Solo inserta: la historia de revisiones es append-only para que las
 * métricas futuras puedan recalcularse (README §32).
 */
class RoomReviewRepository(
    private val reviewEventDao: ReviewEventDao,
) : ReviewRepository {

    override fun observeHistory(expressionId: ExpressionId): Flow<List<ReviewEvent>> =
        reviewEventDao.observeByExpression(expressionId.value).map { rows -> rows.map { it.toDomain() } }

    override fun observeAll(): Flow<List<ReviewEvent>> =
        reviewEventDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getBySession(sessionId: SessionId): List<ReviewEvent> =
        reviewEventDao.getBySession(sessionId.value).map { row -> row.toDomain() }

    override suspend fun record(event: ReviewEvent) {
        reviewEventDao.insert(event.toEntity())
    }
}
