package com.example.learning.english.learning.english.app.data.mapper

import com.example.learning.english.learning.english.app.data.persistence.relation.LearningSessionWithExercises
import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ExpressionPattern
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.PatternId
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.Tag
import com.example.learning.english.learning.english.app.domain.model.TagId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mapeos puros: se prueban sin Room ni Android (README §58). */
class MappersTest {

    @Test
    fun `expression survives a domain to entity round trip`() {
        val expression = fullExpression()

        assertEquals(expression, expression.toRelation().toDomain())
    }

    @Test
    fun `optional expression fields are preserved as nulls`() {
        val expression = fullExpression(level = null, explanation = null, source = null)

        val restored = expression.toRelation().toDomain()

        assertNull(restored.level)
        assertNull(restored.explanation)
        assertNull(restored.source)
    }

    @Test
    fun `tags and patterns are restored`() {
        val expression = fullExpression()

        val restored = expression.toRelation().toDomain()

        assertEquals(expression.tags, restored.tags)
        assertEquals(expression.patterns, restored.patterns)
    }

    @Test
    fun `learning state keeps both scores and the scheduling fields`() {
        val state = LearningState(
            expressionId = EXPRESSION_ID,
            stage = LearningStage.RECALLABLE,
            recognitionScore = 80,
            productionScore = 35,
            nextReviewAt = 12_345L,
            lastReviewedAt = 12_000L,
            reviewCount = 4,
            successfulReviewCount = 3,
            failedReviewCount = 1,
            currentIntervalDays = 7,
            easeFactor = 2.6,
            updatedAt = 12_345L,
        )

        assertEquals(state, state.toEntity().toDomain())
    }

    @Test
    fun `unscheduled learning state keeps a null next review`() {
        val state = LearningState.new(EXPRESSION_ID)

        assertNull(state.toEntity().toDomain().nextReviewAt)
    }

    @Test
    fun `review event keeps its type, rating and stages`() {
        val event = fullReviewEvent()

        assertEquals(event, event.toEntity().toDomain())
        assertTrue(event.toEntity().toDomain().isCorrect)
    }

    @Test
    fun `forgotten review is reported as incorrect`() {
        val event = fullReviewEvent(rating = ReviewRating.FORGOT)

        assertFalse(event.toEntity().toDomain().isCorrect)
    }

    @Test
    fun `enums are mapped by name, not by ordinal`() {
        val entity = fullReviewEvent().toEntity()

        assertEquals(ReviewType.PRODUCTION.name, entity.reviewType.name)
        assertEquals(ReviewRating.EASY.name, entity.rating.name)
        assertEquals(Difficulty.MEDIUM.name, fullExpression().toEntity().difficulty.name)
    }

    @Test
    fun `session exercises are mapped with their order`() {
        val session = fullSession()

        val restored = LearningSessionWithExercises(
            session = session.toEntity(),
            exercises = session.exercises.map { it.toEntity(session.id) },
        ).toDomain()

        assertEquals(session, restored)
    }

    private fun fullExpression(
        level: CefrLevel? = CefrLevel.B2,
        explanation: String? = "Used when discovering an answer.",
        source: String? = "core pack",
    ) = Expression(
        id = EXPRESSION_ID,
        phrase = "figure out",
        primaryMeaning = "averiguar, resolver",
        packId = PackId("core-english"),
        difficulty = Difficulty.MEDIUM,
        level = level,
        explanation = explanation,
        source = source,
        examples = listOf(
            Example(
                id = ExampleId("figure-out-example-1"),
                expressionId = EXPRESSION_ID,
                english = "I'm trying to figure out what happened.",
                spanish = "Estoy intentando averiguar qué pasó.",
                context = "general",
                difficulty = Difficulty.EASY,
                isPrimary = true,
            ),
        ),
        patterns = listOf(
            ExpressionPattern(
                id = PatternId("figure-out-pattern-1"),
                expressionId = EXPRESSION_ID,
                pattern = "figure + object + out",
                explanation = null,
            ),
        ),
        tags = setOf(Tag(TagId("phrasal-verb"), "phrasal-verb")),
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    private fun fullReviewEvent(rating: ReviewRating = ReviewRating.EASY) = ReviewEvent(
        id = ReviewId("review-1"),
        expressionId = EXPRESSION_ID,
        reviewType = ReviewType.PRODUCTION,
        rating = rating,
        reviewedAt = 1_000L,
        sessionId = SessionId("session-1"),
        responseTimeMs = 4_200L,
        previousStage = LearningStage.RECOGNIZED,
        newStage = LearningStage.RECALLABLE,
    )

    private fun fullSession() = LearningSession(
        id = SESSION_ID,
        startedAt = 1_000L,
        completedAt = 2_000L,
        exercises = listOf(
            Exercise(
                sessionId = SESSION_ID,
                position = 0,
                expressionId = EXPRESSION_ID,
                reviewType = ReviewType.RECOGNITION,
            ),
        ),
    )

    private companion object {
        val EXPRESSION_ID = ExpressionId("figure-out")
        val SESSION_ID = SessionId("session-1")
    }
}
