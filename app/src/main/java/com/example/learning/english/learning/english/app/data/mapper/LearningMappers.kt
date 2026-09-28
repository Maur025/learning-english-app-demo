package com.example.learning.english.learning.english.app.data.mapper

import com.example.learning.english.learning.english.app.data.persistence.entity.LearningSessionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningStateEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ReviewEventEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.SessionExerciseEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.LearningSessionWithExercises
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewEvent
import com.example.learning.english.learning.english.app.domain.model.ReviewId
import com.example.learning.english.learning.english.app.domain.model.SessionId

fun LearningStateEntity.toDomain(): LearningState = LearningState(
    expressionId = ExpressionId(expressionId),
    stage = stage,
    recognitionScore = recognitionScore,
    productionScore = productionScore,
    nextReviewAt = nextReviewAt,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    successfulReviewCount = successfulReviewCount,
    failedReviewCount = failedReviewCount,
    currentIntervalDays = currentIntervalDays,
    easeFactor = easeFactor,
    updatedAt = updatedAt,
)

fun LearningState.toEntity(): LearningStateEntity = LearningStateEntity(
    expressionId = expressionId.value,
    stage = stage,
    recognitionScore = recognitionScore,
    productionScore = productionScore,
    nextReviewAt = nextReviewAt,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    successfulReviewCount = successfulReviewCount,
    failedReviewCount = failedReviewCount,
    currentIntervalDays = currentIntervalDays,
    easeFactor = easeFactor,
    updatedAt = updatedAt,
)

fun ReviewEventEntity.toDomain(): ReviewEvent = ReviewEvent(
    id = ReviewId(id),
    expressionId = ExpressionId(expressionId),
    reviewType = reviewType,
    rating = rating,
    reviewedAt = reviewedAt,
    sessionId = sessionId?.let { SessionId(it) },
    responseTimeMs = responseTimeMs,
    previousStage = previousStage,
    newStage = newStage,
)

fun ReviewEvent.toEntity(): ReviewEventEntity = ReviewEventEntity(
    id = id.value,
    expressionId = expressionId.value,
    reviewType = reviewType,
    rating = rating,
    reviewedAt = reviewedAt,
    sessionId = sessionId?.value,
    responseTimeMs = responseTimeMs,
    previousStage = previousStage,
    newStage = newStage,
)

fun LearningSessionWithExercises.toDomain(): LearningSession =
    session.toDomain().copy(exercises = exercises.sortedBy { it.position }.map { it.toDomain() })

/** Sesión sin ejercicios: suficiente para listados recientes. */
fun LearningSessionEntity.toDomain(): LearningSession = LearningSession(
    id = SessionId(id),
    startedAt = startedAt,
    completedAt = completedAt,
    currentPosition = currentPosition,
)

fun LearningSession.toEntity(): LearningSessionEntity = LearningSessionEntity(
    id = id.value,
    startedAt = startedAt,
    completedAt = completedAt,
    currentPosition = currentPosition,
)

fun Exercise.toEntity(sessionId: SessionId): SessionExerciseEntity = SessionExerciseEntity(
    sessionId = sessionId.value,
    position = position,
    expressionId = expressionId.value,
    reviewType = reviewType,
)

fun SessionExerciseEntity.toDomain(): Exercise = Exercise(
    sessionId = SessionId(sessionId),
    position = position,
    expressionId = ExpressionId(expressionId),
    reviewType = reviewType,
)
