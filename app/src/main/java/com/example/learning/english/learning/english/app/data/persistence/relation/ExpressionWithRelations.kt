package com.example.learning.english.learning.english.app.data.persistence.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.learning.english.learning.english.app.data.persistence.entity.ExampleEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionTagCrossRef
import com.example.learning.english.learning.english.app.data.persistence.entity.PatternEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.TagEntity

/**
 * Expresión con sus colecciones dependientes.
 *
 * Una sola consulta transaccional evita consultas N+1 al observar contenido.
 */
data class ExpressionWithRelations(
    @Embedded val expression: ExpressionEntity,
    @Relation(parentColumn = "id", entityColumn = "expressionId")
    val examples: List<ExampleEntity>,
    @Relation(parentColumn = "id", entityColumn = "expressionId")
    val patterns: List<PatternEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ExpressionTagCrossRef::class,
            parentColumn = "expressionId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
)
