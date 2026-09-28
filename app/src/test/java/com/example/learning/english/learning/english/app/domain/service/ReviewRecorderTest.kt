package com.example.learning.english.learning.english.app.domain.service

import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.engine.LearningStateProgressor
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import com.example.learning.english.learning.english.app.domain.scheduler.Sm2ReviewScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRecorderTest {

    private val scheduler = Sm2ReviewScheduler()
    private val learningStateRepository = FakeLearningStateRepository()
    private val reviewRepository = FakeReviewRepository()
    private val recorder = ReviewRecorder(
        scheduler = scheduler,
        progressor = LearningStateProgressor(),
        learningStateRepository = learningStateRepository,
        reviewRepository = reviewRepository,
    )

    @Test
    fun `records the review and schedules the expression for the first time`() = runTest {
        val reviewedAt = 1_000_000L

        val updated = recorder.record(EXPRESSION_ID, ReviewType.RECOGNITION, ReviewRating.GOOD, reviewedAt)

        assertEquals(1, updated.reviewCount)
        assertEquals(2, updated.currentIntervalDays)
        assertNotNull(updated.nextReviewAt)
        assertEquals(reviewedAt, updated.lastReviewedAt)
        assertEquals(updated, learningStateRepository.getByExpression(EXPRESSION_ID))

        val event = reviewRepository.events.single()
        assertEquals(EXPRESSION_ID, event.expressionId)
        assertEquals(ReviewType.RECOGNITION, event.reviewType)
        assertEquals(ReviewRating.GOOD, event.rating)
        assertEquals(reviewedAt, event.reviewedAt)
        assertEquals(LearningStage.NEW, event.previousStage)
        assertEquals(LearningStage.SEEN, event.newStage)
        assertNull(event.sessionId)
        assertNull(event.responseTimeMs)
    }

    @Test
    fun `reuses the existing learning state and refines its score`() = runTest {
        learningStateRepository.upsert(
            LearningState.new(EXPRESSION_ID).copy(
                stage = LearningStage.RECOGNIZED,
                recognitionScore = 75,
                reviewCount = 3,
                currentIntervalDays = 2,
            ),
        )

        val updated = recorder.record(EXPRESSION_ID, ReviewType.CLOZE, ReviewRating.EASY, reviewedAt = 2_000L)

        assertEquals(LearningStage.RECOGNIZED, updated.stage)
        assertEquals(84, updated.recognitionScore)
        assertEquals(0, updated.productionScore)
        assertEquals(7, updated.currentIntervalDays)
        assertEquals(ReviewType.CLOZE, reviewRepository.events.single().reviewType)
        assertEquals(LearningStage.RECOGNIZED, reviewRepository.events.single().previousStage)
    }

    @Test
    fun `stores session id and response time in the event`() = runTest {
        val sessionId = SessionId("session-1")

        recorder.record(
            expressionId = EXPRESSION_ID,
            reviewType = ReviewType.PRODUCTION,
            rating = ReviewRating.HARD,
            reviewedAt = 3_000L,
            sessionId = sessionId,
            responseTimeMs = 4_200L,
        )

        val event = reviewRepository.events.single()
        assertEquals(sessionId, event.sessionId)
        assertEquals(4_200L, event.responseTimeMs)
    }

    @Test
    fun `a forgotten expression is scheduled to come back immediately`() = runTest {
        val reviewedAt = 5_000L

        val updated = recorder.record(EXPRESSION_ID, ReviewType.GUIDED_RECALL, ReviewRating.FORGOT, reviewedAt)

        assertEquals(reviewedAt, updated.nextReviewAt)
        assertEquals(1, updated.failedReviewCount)
        assertTrue(reviewRepository.events.single().rating == ReviewRating.FORGOT)
    }

    @Test
    fun `consecutive reviews accumulate history and count`() = runTest {
        recorder.record(EXPRESSION_ID, ReviewType.RECOGNITION, ReviewRating.GOOD, reviewedAt = 1_000L)
        recorder.record(EXPRESSION_ID, ReviewType.PRODUCTION, ReviewRating.EASY, reviewedAt = 2_000L)

        assertEquals(2, reviewRepository.events.size)
        assertEquals(2, learningStateRepository.getByExpression(EXPRESSION_ID)?.reviewCount)
    }

    private class FakeLearningStateRepository : LearningStateRepository {

        private val rows = mutableMapOf<String, LearningState>()

        override fun observeAll(): Flow<List<LearningState>> = flowOf(rows.values.toList())

        override fun observeByExpression(expressionId: ExpressionId): Flow<LearningState?> =
            flowOf(rows[expressionId.value])

        override suspend fun getByExpression(expressionId: ExpressionId): LearningState? =
            rows[expressionId.value]

        override suspend fun getAll(): List<LearningState> = rows.values.toList()

        override suspend fun upsert(state: LearningState) {
            rows[state.expressionId.value] = state
        }

        override suspend fun upsertAll(states: List<LearningState>) {
            for (state in states) upsert(state)
        }
    }

    private class FakeReviewRepository : ReviewRepository {

        val events = mutableListOf<ReviewEvent>()

        override fun observeHistory(expressionId: ExpressionId): Flow<List<ReviewEvent>> =
            flowOf(events.filter { it.expressionId == expressionId })

        override fun observeAll(): Flow<List<ReviewEvent>> = flowOf(events.toList())

        override suspend fun record(event: ReviewEvent) {
            events += event
        }
    }

    private companion object {
        val EXPRESSION_ID = ExpressionId("figure-out")
    }
}