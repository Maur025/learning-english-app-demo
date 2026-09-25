package com.example.learning.english.learning.english.app.data.content

import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.Example
import com.example.learning.english.learning.english.app.domain.model.ExampleId
import com.example.learning.english.learning.english.app.domain.model.Expression
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ExpressionPattern
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.PatternId
import com.example.learning.english.learning.english.app.domain.model.Tag
import com.example.learning.english.learning.english.app.domain.model.TagId

/** Metadatos de un pack ya validado, listos para persistir (README §27). */
data class PackMetadata(
    val id: PackId,
    val name: String,
    val description: String?,
    val version: Int,
    val language: String,
    val source: String?,
)

/** Un pack de contenido traducido al dominio, sin referencias a Room. */
data class ContentPack(
    val metadata: PackMetadata,
    val expressions: List<Expression>,
)

/**
 * Convierte el archivo de contenido en modelos de dominio (Fase 3).
 *
 * Los ids se namespacean con el pack (`core-english/figure-out`) porque en Room
 * son clave primaria: dos packs pueden usar ids locales iguales sin pisarse.
 * Los ids derivados (`/examples/1`) dependen solo del orden del archivo, así que
 * reimportar el mismo pack produce siempre las mismas claves.
 *
 * Espera un archivo ya validado: [Difficulty.valueOf] y [CefrLevel.valueOf]
 * presuponen que el validador aceptó los valores.
 */
object ContentPackMapper {

    fun toContentPack(file: ContentPackFile, timestamp: Long): ContentPack {
        val packId = PackId(file.pack.id)
        return ContentPack(
            metadata = PackMetadata(
                id = packId,
                name = file.pack.name,
                description = file.pack.description,
                version = file.pack.version,
                language = file.pack.language,
                source = file.pack.source,
            ),
            expressions = file.expressions.map { it.toDomain(packId, timestamp) },
        )
    }

    private fun ExpressionFile.toDomain(packId: PackId, timestamp: Long): Expression {
        val expressionId = ExpressionId(scopedId(packId.value, id))
        val primaryExampleIndex = examples.indexOfFirst { it.isPrimary }.takeIf { it >= 0 } ?: 0

        return Expression(
            id = expressionId,
            phrase = phrase.trim(),
            primaryMeaning = meaning.trim(),
            packId = packId,
            difficulty = Difficulty.valueOf(difficulty),
            level = level?.let(CefrLevel::valueOf),
            explanation = explanation?.trim(),
            source = source?.trim(),
            examples = examples.mapIndexed { index, example ->
                example.toDomain(expressionId, index, isPrimary = index == primaryExampleIndex)
            },
            patterns = patterns.mapIndexed { index, pattern ->
                ExpressionPattern(
                    id = PatternId(scopedId(expressionId.value, "patterns/${index + 1}")),
                    expressionId = expressionId,
                    pattern = pattern.trim(),
                )
            },
            tags = tags.map { tag -> Tag(id = TagId(tag), name = tag) }.toSet(),
            createdAt = timestamp,
            updatedAt = timestamp,
        )
    }

    private fun ExampleFile.toDomain(expressionId: ExpressionId, index: Int, isPrimary: Boolean): Example = Example(
        id = ExampleId(scopedId(expressionId.value, "examples/${index + 1}")),
        expressionId = expressionId,
        english = english.trim(),
        spanish = spanish?.trim(),
        context = context?.trim(),
        difficulty = difficulty?.let(Difficulty::valueOf),
        isPrimary = isPrimary,
    )

    private fun scopedId(packId: String, localId: String): String = "$packId/$localId"
}
