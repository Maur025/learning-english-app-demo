package com.example.learning.english.learning.english.app.data.content

import androidx.room.withTransaction
import com.example.learning.english.learning.english.app.data.mapper.toCrossRef
import com.example.learning.english.learning.english.app.data.mapper.toEntity
import com.example.learning.english.learning.english.app.data.mapper.toPackEntity
import com.example.learning.english.learning.english.app.data.mapper.toRelation
import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.domain.model.PackId

/** Qué pasó con un pack al pasarlo por el importador. */
sealed interface PackImportResult {
    val packId: PackId

    data class Imported(override val packId: PackId, val expressionCount: Int) : PackImportResult

    /** Ya había una revisión igual o superior instalada; no se toca nada. */
    data class Skipped(override val packId: PackId, val installedVersion: Int) : PackImportResult
}

/**
 * Escribe un pack en Room de forma idempotente y versionada (README §29).
 *
 * Reglas:
 * - si la revisión instalada es mayor o igual, no se reimporta;
 * - cada pack se importa en una transacción: o entra entero o no entra;
 * - las expresiones, ejemplos, patrones y etiquetas se reescriben por completo,
 *   así que reimportar converge al contenido del archivo en vez de acumular
 *   filas duplicadas;
 * - nunca se borra el progreso: el estado de aprendizaje vive en su propia
 *   tabla y sobrevive a la reimportación.
 */
class RoomContentImporter(
    private val database: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun import(pack: ParsedContentPack): PackImportResult {
        val installed = database.contentPackDao()
            .getByIds(listOf(pack.metadata.id.value))
            .singleOrNull()

        if (installed != null && installed.version >= pack.metadata.version) {
            return PackImportResult.Skipped(pack.metadata.id, installed.version)
        }

        val now = clock()
        database.withTransaction {
            val packDao = database.contentPackDao()
            val expressionDao = database.expressionDao()
            val tagDao = database.tagDao()

            packDao.upsert(
                pack.toPackEntity(installedAt = installed?.installedAt ?: now, updatedAt = now),
            )

            val expressions = pack.expressions
            tagDao.upsertTags(expressions.flatMap { it.tags }.distinctBy { it.id }.map { it.toEntity() })
            expressions.forEach { expressionDao.upsertExpression(it.toRelation()) }

            // Las etiquetas se reescriben por expresión: si el contenido deja de
            // traer una, deja de estar enlazada.
            tagDao.deleteCrossRefsOf(expressions.map { it.id.value })
            tagDao.upsertCrossRefs(
                expressions.flatMap { expression -> expression.tags.map { it.toCrossRef(expression.id) } },
            )
        }

        return PackImportResult.Imported(pack.metadata.id, pack.expressions.size)
    }
}
