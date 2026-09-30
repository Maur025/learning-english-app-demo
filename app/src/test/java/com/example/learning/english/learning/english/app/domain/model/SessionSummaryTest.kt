package com.example.learning.english.learning.english.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionSummaryTest {

    @Test
    fun `the duration is the time the session was open`() {
        val summary = SessionSummary.from(completedSession(), reviews = emptyList())

        assertEquals(SESSION_MILLIS, summary.durationMillis)
    }

    @Test
    fun `an unfinished session has no duration yet`() {
        val summary = SessionSummary.from(session(completedAt = null), reviews = emptyList())

        assertNull(summary.durationMillis)
        assertEquals(0, summary.reviewedCount)
    }

    @Test
    fun `only the reviews of this session are counted`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(review("a"), review("b")) + review("c", sessionId = SessionId("other")),
        )

        assertEquals(2, summary.reviewedCount)
    }

    @Test
    fun `an expression reviewed for the first time counts as new`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(review("a", previousStage = LearningStage.NEW), review("b")),
        )

        assertEquals(1, summary.newExpressions)
    }

    @Test
    fun `recognition accuracy counts only graded recognition exercises`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(
                review("a", reviewType = ReviewType.RECOGNITION),
                review("b", reviewType = ReviewType.CLOZE),
                review("c", reviewType = ReviewType.GUIDED_RECALL, rating = ReviewRating.FORGOT),
                review("d", reviewType = ReviewType.PRODUCTION),
            ),
        )

        assertEquals(67, summary.recognitionPercent)
        assertEquals(100, summary.productionPercent)
    }

    @Test
    fun `production accuracy counts a hard recall as produced`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(
                review("a", reviewType = ReviewType.PRODUCTION, rating = ReviewRating.EASY),
                review("b", reviewType = ReviewType.PRODUCTION, rating = ReviewRating.HARD),
                review("c", reviewType = ReviewType.PRODUCTION, rating = ReviewRating.FORGOT),
            ),
        )

        // Solo FORGOT cuenta como fallo: HARD sí significa que lo.recordó.
        assertEquals(67, summary.productionPercent)
    }

    @Test
    fun `an axis with no exercises reports no percentage instead of zero`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(review("a", reviewType = ReviewType.RECOGNITION)),
        )

        assertEquals(100, summary.recognitionPercent)
        assertNull(summary.productionPercent)
    }

    @Test
    fun `an expression that goes back a stage is not counted as improved`() {
        val summary = SessionSummary.from(
            completedSession(),
            reviews = listOf(
                review("a", previousStage = LearningStage.SEEN, newStage = LearningStage.RECOGNIZED),
                review("b", previousStage = LearningStage.RECALLABLE, newStage = LearningStage.SEEN),
                review("c", previousStage = LearningStage.PRODUCTIVE, newStage = LearningStage.PRODUCTIVE),
            ),
        )

        assertEquals(1, summary.improvedExpressions)
    }

    @Test
    fun `a session with nothing to practise summarises as empty`() {
        val summary = SessionSummary.from(session(completedAt = STARTED_AT), reviews = emptyList())

        assertEquals(
            SessionSummary.EMPTY.copy(durationMillis = 0L),
            summary,
        )
    }

    private fun session(completedAt: Long?) = LearningSession(
        id = SESSION_ID,
        startedAt = STARTED_AT,
        completedAt = completedAt,
    )

    private fun completedSession() = session(completedAt = STARTED_AT + SESSION_MILLIS)

    private fun review(
        id: String,
        sessionId: SessionId = SESSION_ID,
        reviewType: ReviewType = ReviewType.RECOGNITION,
        rating: ReviewRating = ReviewRating.GOOD,
        previousStage: LearningStage = LearningStage.SEEN,
        newStage: LearningStage = LearningStage.RECOGNIZED,
    ) = ReviewEvent(
        id = ReviewId(id),
        expressionId = ExpressionId("expression-$id"),
        reviewType = reviewType,
        rating = rating,
        reviewedAt = STARTED_AT + 1_000,
        sessionId = sessionId,
        previousStage = previousStage,
        newStage = newStage,
    )

    private companion object {
        const val STARTED_AT = 1_700_000_000_000L
        const val SESSION_MILLIS = 4_300_000L
        val SESSION_ID = SessionId("session-1")
    }
}