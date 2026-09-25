package com.example.learning.english.learning.english.app.data.repository

import androidx.room.withTransaction
import com.example.learning.english.learning.english.app.data.mapper.toCrossRef
import com.example.learning.english.learning.english.app.data.mapper.toDomain
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.mapper.toRelation
import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.persistence.dao.ContentPackDao
import com.example.learning.english.learning.english.app.data.persistence.dao.ExpressionDao
import com.example.learning.english.learning.english.app.data.persistence.dao.TagDao
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación de Room de [ExpressionRepository].
 *
 * [upsertAll] reescribe ejemplos, patrones y etiquetas de cada expresión dentro de
 * una única transacción, así que importar el mismo contenido dos veces no duplica
 * nada y una reimportación con cambios converge al contenido nuevo (README §29).
 */
class RoomExpressionRepository(
    private val database: AppDatabase,
    private val expressionDao: ExpressionDao = database.expressionDao(),
    private val tagDao: TagDao = database.tagDao(),
    private val contentPackDao: ContentPackDao = database.contentPackDao(),
) : ExpressionRepository {

    override fun observeAll(): Flow<List<Expression>> =
        expressionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByPack(packId: PackId): Flow<List<Expression>> =
        expressionDao.observeByPack(packId.value).map { rows -> rows.map { it.toDomain() } }

    override fun observeById(expressionId: ExpressionId): Flow<Expression?> =
        expressionDao.observeById(expressionId.value).map { it?.toDomain() }

    override suspend fun getById(expressionId: ExpressionId): Expression? =
        expressionDao.getById(expressionId.value)?.toDomain()

    override suspend fun upsertAll(expressions: List<Expression>) {
        if (expressions.isEmpty()) return

        val packIds = expressions.map { it.packId.value }.toSet()
        val pendingTags = expressions.flatMap { it.tags }.distinctBy { it.id.value }
        val crossRefs = expressions.flatMap { expression ->
            expression.tags.map { it.toCrossRef(expression.id) }
        }

        database.withTransaction {
            // Se valida dentro de la transacción: la clave foránea daría igual error,
            // pero este mensaje apunta a la causa real (importar antes que el pack).
            val knownPackIds = contentPackDao.getExistingIds(packIds.toList()).toSet()
            check(packIds.all { it in knownPackIds }) {
                "Unknown content pack: ${packIds - knownPackIds}"
            }

            expressions.forEach { expression -> expressionDao.upsertExpression(expression.toRelation()) }
            tagDao.upsertTags(pendingTags.map { it.toEntity() })
            tagDao.deleteCrossRefsOf(expressions.map { it.id.value })
            tagDao.upsertCrossRefs(crossRefs)
        }
    }
}
