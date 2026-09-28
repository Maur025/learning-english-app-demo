package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Estado de aprendizaje, una fila por expresión.
 *
 * [observeDue] es la primitiva de consulta para el `ReviewScheduler` (Fase 4):
 * expresiones ya vistas cuya revisión corresponde en o antes de `now`.
 */
@Dao
interface LearningStateDao {

    @Query("SELECT * FROM learning_state")
    fun observeAll(): Flow<List<LearningStateEntity>>

    /**
     * Instantánea del estado de aprendizaje. El motor de sesión la cruza con el
     * contenido en un único momento, en vez de leer expresión a expresión.
     */
    @Query("SELECT * FROM learning_state")
    suspend fun getAll(): List<LearningStateEntity>

    @Query("SELECT * FROM learning_state WHERE expressionId = :expressionId")
    fun observeByExpression(expressionId: String): Flow<LearningStateEntity?>

    @Query("SELECT * FROM learning_state WHERE expressionId = :expressionId")
    suspend fun getByExpression(expressionId: String): LearningStateEntity?

    @Query(
        """
        SELECT * FROM learning_state
        WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now
        ORDER BY nextReviewAt
        """,
    )
    fun observeDue(now: Long): Flow<List<LearningStateEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM learning_state
        WHERE nextReviewAt IS NOT NULL AND nextReviewAt <= :now
        """,
    )
    suspend fun countDue(now: Long): Int

    @Query("SELECT COUNT(*) FROM learning_state WHERE stage = :stage")
    fun observeCountByStage(stage: String): Flow<Int>

    @Upsert
    suspend fun upsert(state: LearningStateEntity)

    @Upsert
    suspend fun upsertAll(states: List<LearningStateEntity>)

    @Query("DELETE FROM learning_state WHERE expressionId = :expressionId")
    suspend fun deleteByExpression(expressionId: String)
}
