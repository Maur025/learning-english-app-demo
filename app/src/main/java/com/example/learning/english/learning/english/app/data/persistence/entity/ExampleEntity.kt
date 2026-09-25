package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.learning.english.learning.english.app.domain.model.Difficulty

/** Oración de ejemplo de una expresión. `difficulty` nulo hereda la de la expresión. */
@Entity(
    tableName = "examples",
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
data class ExampleEntity(
    @PrimaryKey val id: String,
    val expressionId: String,
    val english: String,
    val spanish: String?,
    val context: String?,
    val difficulty: Difficulty?,
    val isPrimary: Boolean,
)
