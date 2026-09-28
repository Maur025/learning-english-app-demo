package com.example.learning.english.learning.english.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTypeTest {

    @Test
    fun `recognition exercises measure recognition skill`() {
        listOf(ReviewType.RECOGNITION, ReviewType.CLOZE, ReviewType.GUIDED_RECALL).forEach { type ->
            assertEquals(ReviewSkill.RECOGNITION, type.skill)
        }
    }

    @Test
    fun `production exercises measure production skill`() {
        listOf(ReviewType.TRANSLATION, ReviewType.PRODUCTION).forEach { type ->
            assertEquals(ReviewSkill.PRODUCTION, type.skill)
        }
    }

    @Test
    fun `only FORGOT is an incorrect rating`() {
        assertEquals(false, ReviewRating.FORGOT.isCorrect)
        assertEquals(true, ReviewRating.HARD.isCorrect)
        assertEquals(true, ReviewRating.GOOD.isCorrect)
        assertEquals(true, ReviewRating.EASY.isCorrect)
    }

    @Test
    fun `review event derives isCorrect from its rating`() {
        val forgotten = ReviewEvent(
            id = ReviewId("review-1"),
            expressionId = ExpressionId("figure-out"),
            reviewType = ReviewType.RECOGNITION,
            rating = ReviewRating.FORGOT,
            reviewedAt = 1_000L,
        )
        val easy = forgotten.copy(id = ReviewId("review-2"), rating = ReviewRating.EASY)

        assertEquals(false, forgotten.isCorrect)
        assertEquals(true, easy.isCorrect)
    }

    @Test
    fun `exercise exposes the skill of its review type`() {
        val exercise = Exercise(
            sessionId = SessionId("session-1"),
            position = 0,
            expressionId = ExpressionId("figure-out"),
            reviewType = ReviewType.PRODUCTION,
        )

        assertEquals(ReviewSkill.PRODUCTION, exercise.skill)
    }

    @Test
    fun `every review type has a positive time estimate`() {
        ReviewType.entries.forEach { type ->
            assertTrue("$type must have a time estimate", type.estimatedSeconds > 0)
        }
    }

    @Test
    fun `production costs more than recognition`() {
        assertTrue(ReviewType.PRODUCTION.estimatedSeconds > ReviewType.RECOGNITION.estimatedSeconds)
    }
}
