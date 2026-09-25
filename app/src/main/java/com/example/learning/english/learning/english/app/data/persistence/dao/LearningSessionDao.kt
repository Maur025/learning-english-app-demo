package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningSessionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.SessionExerciseEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.LearningSessionWithExercises
import kotlinx.coroutines.flow.Flow

/**
 * Sesiones de práctica y sus ejercicios.
 *
 * [save] es transaccional para que una sesión nunca quede a medias: o se
 * guarda completa con su lista de ejercicios, o no se guarda.
 */
@Dao
interface LearningSessionDao {

    @Query("SELECT * FROM learning_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LearningSessionEntity>>

    @Transaction
    @Query("SELECT * FROM learning_sessions WHERE id = :sessionId")
    suspend fun getByIdWithExercises(sessionId: String): LearningSessionWithExercises?

    @Query("SELECT COUNT(*) FROM learning_sessions")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(session: LearningSessionEntity)

    @Query("DELETE FROM session_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercisesOf(sessionId: String)

    @Upsert
    suspend fun upsertExercises(exercises: List<SessionExerciseEntity>)

    @Transaction
    suspend fun save(session: LearningSessionEntity, exercises: List<SessionExerciseEntity>) {
        upsert(session)
        deleteExercisesOf(session.id)
        if (exercises.isNotEmpty()) {
            upsertExercises(exercises.sortedBy { it.position })
        }
    }
}
