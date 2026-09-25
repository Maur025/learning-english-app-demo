package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.learning.english.learning.english.app.domain.model.LearningStage

/**
 * Estado de aprendizaje de una expresión (README §31).
 *
 * Un registro por expresión (`expressionId` es PK). Los campos de planificación
 * viven aquí para poder migrar de algoritmo de spaced repetition sin tocar el
 * resto del esquema.
 */
@Entity(
    tableName = "learning_state",
    foreignKeys = [
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["expressionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nextReviewAt")],
)
data class LearningStateEntity(
    @PrimaryKey val expressionId: String,
    val stage: LearningStage,
    val recognitionScore: Int,
    val productionScore: Int,
    val nextReviewAt: Long?,
    val lastReviewedAt: Long?,
    val reviewCount: Int,
    val successfulReviewCount: Int,
    val failedReviewCount: Int,
    val currentIntervalDays: Int,
    val easeFactor: Double,
    val updatedAt: Long,
)
