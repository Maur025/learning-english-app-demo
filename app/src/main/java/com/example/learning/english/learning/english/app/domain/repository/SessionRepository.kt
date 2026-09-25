package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.SessionId
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun observeRecent(limit: Int): Flow<List<LearningSession>>

    suspend fun getById(sessionId: SessionId): LearningSession?

    suspend fun save(session: LearningSession)
}
