package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Relación N:M entre expresiones y etiquetas. */
@Entity(
    tableName = "expression_tag_cross_ref",
    primaryKeys = ["expressionId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ExpressionEntity::class,
            parentColumns = ["id"],
            childColumns = ["expressionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class ExpressionTagCrossRef(
    val expressionId: String,
    val tagId: String,
)
