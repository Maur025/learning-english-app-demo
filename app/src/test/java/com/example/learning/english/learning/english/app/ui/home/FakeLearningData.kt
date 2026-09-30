package com.example.learning.english.learning.english.app.ui.home

import com.example.learning.english.learning.english.app.domain.engine.LearningEngine
import com.example.learning.english.learning.english.app.domain.engine.ReviewResult
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Contenido y estado de aprendizaje en memoria para probar Home.
 *
 * A diferencia de los dobles del motor, aquí los agregados sí respetan el filtro
 * por packs: el cruce expresión-estado es justo lo que Home necesita comprobar.
 */
class FakeLearningData(
    initialExpressions: List<Expression> = emptyList(),
    initialStates: List<LearningState> = emptyList(),
) {

    private val expressions = initialExpressions.associateBy { it.id.value }.toMutableMap()
    private val states = initialStates.associateBy { it.expressionId.value }.toMutableMap()
    private val sessions = MutableStateFlow<List<LearningSession>>(emptyList())

    val expressionRepository = object : ExpressionRepository {
        override fun observeAll(): Flow<List<Expression>> = MutableStateFlow(expressions.values.sortedBy { it.phrase }).asStateFlow()

        override fun observeByPack(packId: PackId): Flow<List<Expression>> =
            MutableStateFlow(expressions.values.filter { it.packId == packId }).asStateFlow()

        override fun observeById(expressionId: ExpressionId): Flow<Expression?> =
            MutableStateFlow(expressions[expressionId.value]).asStateFlow()

        override suspend fun getById(expressionId: ExpressionId): Expression? =
            expressions[expressionId.value]

        override suspend fun getAll(): List<Expression> = expressions.values.sortedBy { it.phrase }

        override fun observeNewCount(packIds: Set<PackId>): Flow<Int> = MutableStateFlow(
            expressions.values.count { expression ->
                expression.packId in packIds && !isReviewed(expression)
            },
        ).asStateFlow()

        override suspend fun upsertAll(expressions: List<Expression>) {
            expressions.forEach { this@FakeLearningData.expressions[it.id.value] = it }
        }
    }

    val learningStateRepository = object : LearningStateRepository {
        override fun observeAll(): Flow<List<LearningState>> =
            MutableStateFlow(states.values.toList()).asStateFlow()

        override fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?> =
            MutableStateFlow(states[expressionId.value]).asStateFlow()

        override suspend fun getByExpression(expressionId: ExpressionId): LearningState? =
            states[expressionId.value]

        override suspend fun getAll(): List<LearningState> = states.values.toList()

        override fun observeDueCount(now: Long, packIds: Set<PackId>): Flow<Int> =
            MutableStateFlow(
                states.values.count { state ->
                    val review = state.nextReviewAt
                    review != null && review <= now && isInPacks(state, packIds)
                },
            ).asStateFlow()

        override fun observeSkillAverages(packIds: Set<PackId>): Flow<SkillAverages> =
            MutableStateFlow(averagesOf(packIds)).asStateFlow()

        override suspend fun upsert(state: LearningState) {
            states[state.expressionId.value] = state
        }

        override suspend fun upsertAll(states: List<LearningState>) {
            states.forEach { upsert(it) }
        }
    }

    val sessionRepository = object : SessionRepository {
        override fun observeRecent(limit: Int): Flow<List<LearningSession>> =
            sessions.map { list -> list.sortedByDescending { it.startedAt }.take(limit) }

        override fun observeInProgress(): Flow<LearningSession?> =
            sessions.map { list -> list.filterNot { it.isCompleted }.maxByOrNull { it.startedAt } }

        override suspend fun getById(sessionId: SessionId): LearningSession? =
            sessions.value.firstOrNull { it.id == sessionId }

        override suspend fun save(session: LearningSession) {
            sessions.value = sessions.value.filterNot { it.id == session.id } + session
        }
    }

    fun withSession(session: LearningSession) {
        sessions.value = sessions.value + session
    }

    private fun isReviewed(expression: Expression): Boolean =
        (states[expression.id.value]?.reviewCount ?: 0) > 0

    private fun isInPacks(state: LearningState, packIds: Set<PackId>): Boolean =
        expressions[state.expressionId.value]?.packId in packIds

    private fun averagesOf(packIds: Set<PackId>): SkillAverages {
        val reviewed = states.values.filter { it.reviewCount > 0 && isInPacks(it, packIds) }
        if (reviewed.isEmpty()) return SkillAverages.EMPTY
        return SkillAverages.fromScores(
            recognition = reviewed.sumOf { it.recognitionScore }.toDouble() / reviewed.size,
            production = reviewed.sumOf { it.productionScore }.toDouble() / reviewed.size,
        )
    }
}

/** Motor que devuelve la sesión que se le indique, sin planificador detrás. */
class FakeLearningEngine(
    private val session: LearningSession,
    /** Al completarse, la creación de la sesión sigue adelante. */
    private val gate: CompletableDeferred<Unit>? = null,
) : LearningEngine {

    var createdSessions = 0
        private set

    var requestedPreferences: UserPreferences? = null
        private set

    var requestedNow: Long? = null
        private set

    override suspend fun createDailySession(preferences: UserPreferences, now: Long): LearningSession {
        createdSessions++
        // El gate deja la creación a medias, como la suspendería el trabajo real.
        gate?.await()
        requestedPreferences = preferences
        requestedNow = now
        return session
    }

    override suspend fun registerAnswer(
        session: LearningSession,
        exercise: Exercise,
        answer: UserAnswer,
        reviewedAt: Long,
        selfRating: ReviewRating?,
        responseTimeMs: Long?,
    ): ReviewResult = error("Home never registers answers")
}
