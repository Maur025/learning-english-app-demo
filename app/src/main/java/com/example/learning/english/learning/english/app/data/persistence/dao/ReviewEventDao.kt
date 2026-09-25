package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.learning.english.learning.english.app.data.persistence.entity.ReviewEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Historial de revisiones. Solo hay inserciones: cada evento se conserva
 * (README §32) para alimentar progreso, debugging y futuras versiones del
 * algoritmo de repetición espaciada.
 */
@Dao
interface ReviewEventDao {

    @Query("SELECT * FROM reviews WHERE expressionId = :expressionId ORDER BY reviewedAt")
    fun observeByExpression(expressionId: String): Flow<List<ReviewEventEntity>>

    @Query("SELECT * FROM reviews ORDER BY reviewedAt")
    fun observeAll(): Flow<List<ReviewEventEntity>>

    @Query("SELECT * FROM reviews WHERE sessionId = :sessionId ORDER BY reviewedAt")
    suspend fun getBySession(sessionId: String): List<ReviewEventEntity>

    @Insert
    suspend fun insert(event: ReviewEventEntity)

    @Query("SELECT COUNT(*) FROM reviews WHERE expressionId = :expressionId")
    suspend fun countByExpression(expressionId: String): Int
}
