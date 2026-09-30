package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.ContentPackWithCount
import kotlinx.coroutines.flow.Flow

/**
 * Packs de contenido. La Fase 3 (sistema de contenido) es su primer consumidor;
 * el conteo de expresiones se deriva con [countExpressions] en vez de almacenar
 * un campo que podría desincronizarse.
 */
@Dao
interface ContentPackDao {

    @Query("SELECT * FROM content_packs ORDER BY name")
    fun observeAll(): Flow<List<ContentPackEntity>>

    /**
     * Packs con su conteo de expresiones, para las pantallas que ofrecen elegir
     * contenido. El conteo sale de un subselect en la misma consulta: una fila por
     * pack y sin Flows adicionales que puedan desincronizarse entre sí.
     */
    @Query(
        """
        SELECT p.*, (
            SELECT COUNT(*) FROM expressions e WHERE e.packId = p.id
        ) AS expressionCount
        FROM content_packs p
        ORDER BY p.name
        """,
    )
    fun observeAllWithCount(): Flow<List<ContentPackWithCount>>

    @Query("SELECT * FROM content_packs WHERE id = :packId")
    fun observeById(packId: String): Flow<ContentPackEntity?>

    @Query("SELECT COUNT(*) FROM expressions WHERE packId = :packId")
    fun countExpressions(packId: String): Flow<Int>

    @Upsert
    suspend fun upsert(pack: ContentPackEntity)

    @Upsert
    suspend fun upsertAll(packs: List<ContentPackEntity>)

    @Query("SELECT id FROM content_packs WHERE id IN (:packIds)")
    suspend fun getExistingIds(packIds: List<String>): List<String>

    /** Lectura puntual para la importación, que compara revisiones antes de escribir. */
    @Query("SELECT * FROM content_packs WHERE id IN (:packIds)")
    suspend fun getByIds(packIds: List<String>): List<ContentPackEntity>

    @Query("SELECT COUNT(*) FROM content_packs")
    suspend fun count(): Int

    @Query("DELETE FROM content_packs WHERE id = :packId")
    suspend fun deleteById(packId: String)
}
