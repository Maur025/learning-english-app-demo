package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Estructura reutilizable de la expresión, por ejemplo `figure + object + out`. */
@Entity(
    tableName = "patterns",
    foreignKeys = [
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["expressionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("expressionId")],
)
data class PatternEntity(
    @PrimaryKey val id: String,
    val expressionId: String,
    val pattern: String,
    val explanation: String?,
)
