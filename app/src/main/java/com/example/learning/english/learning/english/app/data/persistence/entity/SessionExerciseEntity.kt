package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Ejercicio de una sesión. La posición define el orden y hace de PK compuesta
 * junto al id de sesión, de modo que reordenar es determinista.
 */
@Entity(
    tableName = "session_exercises",
    primaryKeys = ["sessionId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = LearningSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("expressionId")],
)
data class SessionExerciseEntity(
    val sessionId: String,
    val position: Int,
    val expressionId: String,
    val reviewType: ReviewType,
)
