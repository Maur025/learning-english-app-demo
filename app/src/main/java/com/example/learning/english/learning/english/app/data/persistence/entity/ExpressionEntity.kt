package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty

/**
 * Fila de la tabla `expressions`.
 *
 * No guarda ejemplos, patrones ni etiquetas: se cargan por relación
 * (`ExpressionWithRelations`). Los timestamps son epoch en milisegundos.
 */
@Entity(
    tableName = "expressions",
    foreignKeys = [
        ForeignKey(
            entity = ContentPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("packId")],
)
data class ExpressionEntity(
    @PrimaryKey val id: String,
    val phrase: String,
    val primaryMeaning: String,
    val packId: String,
    val difficulty: Difficulty,
    val level: CefrLevel?,
    val explanation: String?,
    val source: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
