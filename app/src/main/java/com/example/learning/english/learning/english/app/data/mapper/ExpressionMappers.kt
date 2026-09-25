package com.example.learning.english.learning.english.app.data.mapper

import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExampleEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionTagCrossRef
import com.example.learning.english.learning.english.app.data.persistence.entity.PatternEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.TagEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.ExpressionWithRelations
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ExpressionPattern
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.PatternId
import com.example.learning.english.learning.english.app.domain.model.Tag
import com.example.learning.english.learning.english.app.domain.model.TagId

/**
 * Mapeo entre entidades de Room y modelos de dominio.
 *
 * Son funciones puras sin dependencias de Android: se prueban en JVM sin
 * necesidad de base de datos (README §58).
 */

fun ExpressionWithRelations.toDomain(): Expression = Expression(
    id = ExpressionId(expression.id),
    phrase = expression.phrase,
    primaryMeaning = expression.primaryMeaning,
    packId = PackId(expression.packId),
    difficulty = expression.difficulty,
    level = expression.level,
    explanation = expression.explanation,
    source = expression.source,
    examples = examples.map { it.toDomain() }.sortedByDescending { it.isPrimary },
    patterns = patterns.map { it.toDomain() },
    tags = tags.map { it.toDomain() }.toSet(),
    createdAt = expression.createdAt,
    updatedAt = expression.updatedAt,
)

fun Expression.toEntity(): ExpressionEntity = ExpressionEntity(
    id = id.value,
    phrase = phrase,
    primaryMeaning = primaryMeaning,
    packId = packId.value,
    difficulty = difficulty,
    level = level,
    explanation = explanation,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Expression.toRelation(): ExpressionWithRelations = ExpressionWithRelations(
    expression = toEntity(),
    examples = examples.map { it.toEntity(id) },
    patterns = patterns.map { it.toEntity(id) },
    tags = tags.map { it.toEntity() },
)

fun Example.toEntity(expressionId: ExpressionId): ExampleEntity = ExampleEntity(
    id = id.value,
    expressionId = expressionId.value,
    english = english,
    spanish = spanish,
    context = context,
    difficulty = difficulty,
    isPrimary = isPrimary,
)

fun ExampleEntity.toDomain(): Example = Example(
    id = ExampleId(id),
    expressionId = ExpressionId(expressionId),
    english = english,
    spanish = spanish,
    context = context,
    difficulty = difficulty,
    isPrimary = isPrimary,
)

fun ExpressionPattern.toEntity(expressionId: ExpressionId): PatternEntity = PatternEntity(
    id = id.value,
    expressionId = expressionId.value,
    pattern = pattern,
    explanation = explanation,
)

fun PatternEntity.toDomain(): ExpressionPattern = ExpressionPattern(
    id = PatternId(id),
    expressionId = ExpressionId(expressionId),
    pattern = pattern,
    explanation = explanation,
)

fun Tag.toEntity(): TagEntity = TagEntity(id = id.value, name = name)

fun TagEntity.toDomain(): Tag = Tag(id = TagId(id), name = name)

fun Tag.toCrossRef(expressionId: ExpressionId): ExpressionTagCrossRef = ExpressionTagCrossRef(
    expressionId = expressionId.value,
    tagId = id.value,
)

fun ContentPackEntity.idAsPackId(): PackId = PackId(id)
