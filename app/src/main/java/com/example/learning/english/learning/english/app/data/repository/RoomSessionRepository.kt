package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.persistence.dao.LearningSessionDao
import com.example.learning.english.learning.english.app.domain.model.LearningSession
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [SessionRepository].
 *
 * Guardar es transaccional y conserva la lista de ejercicios, de modo que
 * recuperar una sesión devuelve exactamente lo que se guardó.
 */
class RoomSessionRepository(
    private val learningSessionDao: LearningSessionDao,
) : SessionRepository {

    override fun observeRecent(limit: Int): Flow<List<LearningSession>> =
        learningSessionDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(sessionId: SessionId): LearningSession? =
        learningSessionDao.getByIdWithExercises(sessionId.value)?.toDomain()

    override suspend fun save(session: LearningSession) {
        learningSessionDao.save(
            session = session.toEntity(),
            exercises = session.exercises.map { it.toEntity(session.id) },
        )
    }
}
