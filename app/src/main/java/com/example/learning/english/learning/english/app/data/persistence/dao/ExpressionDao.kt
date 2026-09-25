package com.example.learning.english.learning.english.app.data.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.learning.english.learning.english.app.data.persistence.entity.ExampleEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.PatternEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.ExpressionWithRelations
import kotlinx.coroutines.flow.Flow

/**
 * Lecturas y escrituras de contenido.
 *
 * [upsertExpression] es transaccional y reemplaza las colecciones dependientes:
 * volver a insertar la misma expresión no duplica ejemplos ni etiquetas, que es
 * lo que exige la importación idempotente (README §29).
 */
@Dao
interface ExpressionDao {

    @Transaction
    @Query("SELECT * FROM expressions ORDER BY phrase")
    fun observeAll(): Flow<List<ExpressionWithRelations>>

    @Transaction
    @Query("SELECT * FROM expressions WHERE packId = :packId ORDER BY phrase")
    fun observeByPack(packId: String): Flow<List<ExpressionWithRelations>>

    @Transaction
    @Query("SELECT * FROM expressions WHERE id = :expressionId")
    fun observeById(expressionId: String): Flow<ExpressionWithRelations?>

    @Transaction
    @Query("SELECT * FROM expressions WHERE id = :expressionId")
    suspend fun getById(expressionId: String): ExpressionWithRelations?

    @Transaction
    @Query("SELECT COUNT(*) FROM expressions")
    suspend fun count(): Int

    /**
     * Guarda la expresión con sus colecciones dependientes.
     *
     * Las colecciones se reescriben por completo: si el contenido deja de traer un
     * ejemplo, un patrón o una etiqueta, deja de estar en la base de datos. Así una
     * reimportación converge al contenido en vez de acumular filas (README §29).
     */
    @Transaction
    suspend fun upsertExpression(withRelations: ExpressionWithRelations) {
        val expressionId = withRelations.expression.id

        upsertExpressionRow(withRelations.expression)
        deleteExamplesOf(expressionId)
        deletePatternsOf(expressionId)

        if (withRelations.examples.isNotEmpty()) {
            upsertExamples(withRelations.examples.distinctBy { it.id })
        }
        if (withRelations.patterns.isNotEmpty()) {
            upsertPatterns(withRelations.patterns.distinctBy { it.id })
        }
    }

    @Upsert
    suspend fun upsertExpressionRow(expression: ExpressionEntity)

    @Query("DELETE FROM examples WHERE expressionId = :expressionId")
    suspend fun deleteExamplesOf(expressionId: String)

    @Query("DELETE FROM patterns WHERE expressionId = :expressionId")
    suspend fun deletePatternsOf(expressionId: String)

    @Upsert
    suspend fun upsertExamples(examples: List<ExampleEntity>)

    @Upsert
    suspend fun upsertPatterns(patterns: List<PatternEntity>)

    @Query("DELETE FROM expressions WHERE id = :expressionId")
    suspend fun deleteById(expressionId: String)
}
