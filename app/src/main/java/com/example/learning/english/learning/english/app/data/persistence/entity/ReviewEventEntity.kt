package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Evento de revisión histórico (README §32). Solo se agrega, nunca se actualiza.
 *
 * `isCorrect` no se persiste: se deriva de [rating] para que ambos nunca
 * puedan contradecirse.
 */
@Entity(
    tableName = "reviews",
    foreignKeys = [
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["expressionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("expressionId"), Index("reviewedAt")],
)
data class ReviewEventEntity(
    @PrimaryKey val id: String,
    val expressionId: String,
    val reviewType: ReviewType,
    val rating: ReviewRating,
    val reviewedAt: Long,
    val sessionId: String? = null,
    val responseTimeMs: Long? = null,
    val previousStage: LearningStage,
    val newStage: LearningStage,
)
