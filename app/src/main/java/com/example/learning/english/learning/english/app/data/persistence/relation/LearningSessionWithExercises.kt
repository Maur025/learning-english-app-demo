package com.example.learning.english.learning.english.app.data.persistence.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningSessionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.SessionExerciseEntity

/** Sesión con sus ejercicios, en el orden persistido por `position`. */
data class LearningSessionWithExercises(
    @Embedded val session: LearningSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseEntity>,
)
