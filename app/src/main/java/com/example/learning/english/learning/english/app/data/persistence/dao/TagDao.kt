package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionTagCrossRef
import com.example.learning.english.learning.english.app.data.persistence.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Etiquetas y su relación con expresiones.
 *
 * Las etiquetas son globales y se comparten entre packs, por eso el alta es
 * idempotente por id.
 */
@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM expression_tag_cross_ref WHERE expressionId = :expressionId")
    suspend fun getCrossRefsOf(expressionId: String): List<ExpressionTagCrossRef>

    @Upsert
    suspend fun upsertTags(tags: List<TagEntity>)

    @Upsert
    suspend fun upsertCrossRefs(crossRefs: List<ExpressionTagCrossRef>)

    @Query("DELETE FROM expression_tag_cross_ref WHERE expressionId IN (:expressionIds)")
    suspend fun deleteCrossRefsOf(expressionIds: List<String>)

    @Query("SELECT COUNT(*) FROM tags")
    suspend fun count(): Int
}
