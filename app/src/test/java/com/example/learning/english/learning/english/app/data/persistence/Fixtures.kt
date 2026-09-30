package com.example.learning.english.learning.english.app.data.persistence

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExampleEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningStateEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.PatternEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ReviewEventEntity
import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ExpressionPattern
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

/** Base de datos en memoria para tests de DAO y repositorio (README §59). */
internal fun createInMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
    ).allowMainThreadQueries().build()

internal object Fixtures {

    const val PACK_ID = "core-english"
    const val EXPRESSION_ID = "figure-out"

    val pack = ContentPackEntity(
        id = PACK_ID,
        name = "Core English",
        description = "Expresiones de base",
        version = 1,
        language = "en",
        source = null,
        installedAt = 1_000L,
        updatedAt = 1_000L,
        bundled = true,
    )

    fun expressionEntity(
        id: String = EXPRESSION_ID,
        packId: String = PACK_ID,
        phrase: String = "figure out",
    ) = ExpressionEntity(
        id = id,
        phrase = phrase,
        primaryMeaning = "averiguar, resolver",
        packId = packId,
        difficulty = Difficulty.MEDIUM,
        level = CefrLevel.B1,
        explanation = "Used when discovering an answer.",
        source = null,
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    fun domainExpression(
        id: String = EXPRESSION_ID,
        packId: String = PACK_ID,
        phrase: String = "figure out",
        examples: List<Example>? = null,
        patterns: List<ExpressionPattern>? = null,
        tags: Set<Tag> = setOf(Tag(TagId("phrasal-verb"), "phrasal-verb")),
    ) = Expression(
        id = ExpressionId(id),
        phrase = phrase,
        primaryMeaning = "averiguar, resolver",
        packId = PackId(packId),
        difficulty = Difficulty.MEDIUM,
        level = CefrLevel.B1,
        explanation = "Used when discovering an answer.",
        source = null,
        examples = examples ?: listOf(domainExample(id)),
        patterns = patterns ?: listOf(domainPattern(id)),
        tags = tags,
        createdAt = 1_000L,
        updatedAt = 2_000L,
    )

    fun domainExample(expressionId: String = EXPRESSION_ID) = Example(
        id = ExampleId("$expressionId-example-1"),
        expressionId = ExpressionId(expressionId),
        english = "I'm trying to figure out what happened.",
        spanish = "Estoy intentando averiguar qué pasó.",
        context = "general",
        difficulty = null,
        isPrimary = true,
    )

    fun tag(id: String) = Tag(TagId(id), id)

    fun domainPattern(expressionId: String = EXPRESSION_ID) = ExpressionPattern(
        id = PatternId("$expressionId-pattern-1"),
        expressionId = ExpressionId(expressionId),
        pattern = "figure + object + out",
        explanation = null,
    )

    fun exampleEntity(
        expressionId: String = EXPRESSION_ID,
        english: String = "I'm trying to figure out what happened.",
    ) = ExampleEntity(
        id = "$expressionId-example-1",
        expressionId = expressionId,
        english = english,
        spanish = "Estoy intentando averiguar qué pasó.",
        context = "general",
        difficulty = null,
        isPrimary = true,
    )

    fun patternEntity(expressionId: String = EXPRESSION_ID) = PatternEntity(
        id = "$expressionId-pattern-1",
        expressionId = expressionId,
        pattern = "figure + object + out",
        explanation = null,
    )

    fun learningStateEntity(
        expressionId: String = EXPRESSION_ID,
        nextReviewAt: Long? = null,
    ) = LearningStateEntity(
        expressionId = expressionId,
        stage = LearningStage.SEEN,
        recognitionScore = 10,
        productionScore = 0,
        nextReviewAt = nextReviewAt,
        lastReviewedAt = null,
        reviewCount = 0,
        successfulReviewCount = 0,
        failedReviewCount = 0,
        currentIntervalDays = 0,
        easeFactor = LearningState.DEFAULT_EASE_FACTOR,
        updatedAt = 1_000L,
    )

    fun reviewEventEntity(
        id: String = "review-1",
        expressionId: String = EXPRESSION_ID,
        reviewedAt: Long = 1_000L,
        rating: ReviewRating = ReviewRating.GOOD,
    ) = ReviewEventEntity(
        id = id,
        expressionId = expressionId,
        reviewType = ReviewType.RECOGNITION,
        rating = rating,
        reviewedAt = reviewedAt,
        sessionId = null,
        responseTimeMs = 2_500L,
        previousStage = LearningStage.NEW,
        newStage = LearningStage.SEEN,
    )

    fun domainReviewEvent(
        id: String = "review-1",
        expressionId: String = EXPRESSION_ID,
        reviewedAt: Long = 1_000L,
        rating: ReviewRating = ReviewRating.GOOD,
        sessionId: String? = null,
    ) = ReviewEvent(
        id = ReviewId(id),
        expressionId = ExpressionId(expressionId),
        reviewType = ReviewType.RECOGNITION,
        rating = rating,
        reviewedAt = reviewedAt,
        sessionId = sessionId?.let { SessionId(it) },
        responseTimeMs = 2_500L,
        previousStage = LearningStage.NEW,
        newStage = LearningStage.SEEN,
    )

    fun domainLearningState(
        expressionId: String = EXPRESSION_ID,
        nextReviewAt: Long? = null,
    ) = LearningState.new(ExpressionId(expressionId)).copy(
        stage = LearningStage.SEEN,
        recognitionScore = 10,
        nextReviewAt = nextReviewAt,
        updatedAt = 1_000L,
    )
}
