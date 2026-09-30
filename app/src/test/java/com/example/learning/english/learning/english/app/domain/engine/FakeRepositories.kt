package com.example.learning.english.learning.english.app.domain.engine

import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repositorios en memoria para probar el motor sin Room: el motor no tiene
 * lógica de persistencia propia, así que un `MutableMap` por tabla es
 * suficiente y deja el test legible.
 */
internal class FakeExpressionRepository(
    expressions: List<Expression> = emptyList(),
) : ExpressionRepository {

    private val rows = expressions.associateBy { it.id.value }.toMutableMap()

    override fun observeAll(): Flow<List<Expression>> = flowOf(rows.values.toList())

    override fun observeByPack(packId: PackId): Flow<List<Expression>> =
        flowOf(rows.values.filter { it.packId == packId })

    override fun observeById(expressionId: ExpressionId): Flow<Expression?> = flowOf(rows[expressionId.value])

    override suspend fun getById(expressionId: ExpressionId): Expression? = rows[expressionId.value]

    override suspend fun getAll(): List<Expression> = rows.values.sortedBy { it.phrase }

    override fun observeNewCount(packIds: Set<PackId>): Flow<Int> =
        flowOf(rows.values.count { it.packId in packIds })

    override suspend fun upsertAll(expressions: List<Expression>) {
        expressions.forEach { rows[it.id.value] = it }
    }
}

internal class FakeLearningStateRepository(
    states: List<LearningState> = emptyList(),
) : LearningStateRepository {

    private val rows = states.associateBy { it.expressionId.value }.toMutableMap()

    override fun observeAll(): Flow<List<LearningState>> = flowOf(rows.values.toList())

    override fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?> =
        flowOf(rows[expressionId.value])

    override suspend fun getByExpression(expressionId: ExpressionId): LearningState? = rows[expressionId.value]

    override suspend fun getAll(): List<LearningState> = rows.values.toList()

    override fun observeDueCount(now: Long, packIds: Set<PackId>): Flow<Int> = flowOf(0)

    override fun observeSkillAverages(packIds: Set<PackId>): Flow<SkillAverages> =
        flowOf(SkillAverages.EMPTY)

    override suspend fun upsert(state: LearningState) {
        rows[state.expressionId.value] = state
    }

    override suspend fun upsertAll(states: List<LearningState>) {
        states.forEach { upsert(it) }
    }
}

internal class FakeReviewRepository : ReviewRepository {

    val events = mutableListOf<ReviewEvent>()

    override fun observeHistory(expressionId: ExpressionId): Flow<List<ReviewEvent>> =
        flowOf(events.filter { it.expressionId == expressionId })

    override fun observeAll(): Flow<List<ReviewEvent>> = flowOf(events.toList())

    override suspend fun record(event: ReviewEvent) {
        events += event
    }

    fun eventsOf(expressionId: ExpressionId): List<ReviewEvent> =
        events.filter { it.expressionId == expressionId }
}

internal class FakeSessionRepository : SessionRepository {

    val sessions = mutableMapOf<String, LearningSession>()

    override fun observeRecent(limit: Int): Flow<List<LearningSession>> =
        flowOf(sessions.values.sortedByDescending { it.startedAt }.take(limit))

    override fun observeInProgress(): Flow<LearningSession?> =
        flowOf(sessions.values.filterNot { it.isCompleted }.maxByOrNull { it.startedAt })

    override suspend fun getById(sessionId: SessionId): LearningSession? = sessions[sessionId.value]

    override suspend fun save(session: LearningSession) {
        sessions[session.id.value] = session
    }
}

internal fun exercise(
    sessionId: SessionId,
    position: Int,
    expressionId: String,
    reviewType: ReviewType,
) = Exercise(
    sessionId = sessionId,
    position = position,
    expressionId = ExpressionId(expressionId),
    reviewType = reviewType,
)
