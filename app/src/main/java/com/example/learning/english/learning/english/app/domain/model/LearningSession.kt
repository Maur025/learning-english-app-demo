package com.example.learning.english.learning.english.app.domain.model

data class LearningSession(
    val id: SessionId,
    val startedAt: Long,
    val completedAt: Long? = null,
    val exercises: List<Exercise> = emptyList(),
) {
    val isCompleted: Boolean
        get() = completedAt != null
}
