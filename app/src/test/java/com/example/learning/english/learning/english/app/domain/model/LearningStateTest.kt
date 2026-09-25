package com.example.learning.english.learning.english.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningStateTest {

    @Test
    fun `new state starts at NEW with zeroed scores`() {
        val state = LearningState.new(ExpressionId("figure-out"))

        assertEquals(LearningStage.NEW, state.stage)
        assertEquals(0, state.recognitionScore)
        assertEquals(0, state.productionScore)
        assertNull(state.nextReviewAt)
        assertEquals(0, state.reviewCount)
        assertEquals(LearningState.DEFAULT_EASE_FACTOR, state.easeFactor, 0.0)
    }

    @Test
    fun `recognition and production scores are tracked independently`() {
        val state = LearningState.new(ExpressionId("figure-out")).copy(
            recognitionScore = 95,
            productionScore = 62,
        )

        assertEquals(95, state.recognitionScore)
        assertEquals(62, state.productionScore)
    }

    @Test
    fun `boundary scores are accepted`() {
        val state = LearningState.new(ExpressionId("figure-out")).copy(
            recognitionScore = LearningState.SCORE_MAX,
            productionScore = LearningState.SCORE_MIN,
        )

        assertEquals(100, state.recognitionScore)
        assertEquals(0, state.productionScore)
    }

    @Test
    fun `scores outside the range are rejected`() {
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(recognitionScore = 101) }
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(productionScore = -1) }
    }

    @Test
    fun `negative counters are rejected`() {
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(reviewCount = -1) }
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(successfulReviewCount = -1) }
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(failedReviewCount = -1) }
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(currentIntervalDays = -1) }
    }

    @Test
    fun `non positive ease factor is rejected`() {
        assertIllegalArgument { LearningState.new(EXPRESSION_ID).copy(easeFactor = 0.0) }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        val result = runCatching(block)
        assertTrue("expected IllegalArgumentException", result.exceptionOrNull() is IllegalArgumentException)
    }

    private companion object {
        val EXPRESSION_ID = ExpressionId("figure-out")
    }
}
