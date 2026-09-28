package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sesión de práctica: cabecera de una sesión, su punto de avance y su marca de
 * finalización.
 *
 * [currentPosition] es el índice del siguiente ejercicio sin responder, así que
 * una sesión interrumpida se retoma donde se quedó (README §37, §72).
 */
@Entity(tableName = "learning_sessions")
data class LearningSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long?,
    val currentPosition: Int,
)
