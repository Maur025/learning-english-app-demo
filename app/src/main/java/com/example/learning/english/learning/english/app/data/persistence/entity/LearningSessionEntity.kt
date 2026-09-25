package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sesión de práctica: cabecera de una sesión y su marcador de finalización. */
@Entity(tableName = "learning_sessions")
data class LearningSessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val completedAt: Long?,
)
