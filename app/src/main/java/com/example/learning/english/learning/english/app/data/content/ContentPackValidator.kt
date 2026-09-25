package com.example.learning.english.learning.english.app.data.content

import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty

/**
 * Comprueba que un pack sea coherente antes de importarlo (Fase 3).
 *
 * Devuelve todas las incidencias juntas en vez de fallar en la primera: un pack
 * con tres errores debe reportarlos tres, no obligar a tres importaciones.
 */
object ContentPackValidator {

    /** Versiones del esquema que esta build sabe leer (README §28). */
    const val SUPPORTED_SCHEMA_VERSION = 1

    /** Formato de los ids que acaban formando parte de las claves de Room. */
    private val IDENTIFIER = Regex("[a-z0-9]+(-[a-z0-9]+)*")

    private val DIFFICULTIES = Difficulty.entries.map { it.name }
    private val LEVELS = CefrLevel.entries.map { it.name }

    fun validate(file: ContentPackFile): List<ContentIssue> {
        val schemaIssue = validateSchemaVersion(file.schemaVersion)
        if (schemaIssue != null) {
            // Con una versión desconocida el resto del archivo puede no tener sentido.
            return listOf(schemaIssue)
        }

        return buildList {
            addAll(validatePackMetadata(file.pack))
            addAll(validateExpressions(file.expressions))
        }
    }

    private fun validateSchemaVersion(schemaVersion: Int): ContentIssue? = when {
        schemaVersion == SUPPORTED_SCHEMA_VERSION -> null
        else -> ContentIssue(
            path = "schemaVersion",
            reason = ContentIssueReason.UNSUPPORTED_SCHEMA_VERSION,
            detail = "La app entiende la versión $SUPPORTED_SCHEMA_VERSION y el archivo declara $schemaVersion",
        )
    }

    private fun validatePackMetadata(pack: PackMetadataFile): List<ContentIssue> = buildList {
        addIdentifierIssue("pack.id", pack.id)
        if (pack.name.isBlank()) {
            add(ContentIssue("pack.name", ContentIssueReason.BLANK_FIELD, "El pack necesita un nombre visible"))
        }
        if (pack.version < 1) {
            add(ContentIssue("pack.version", ContentIssueReason.INVALID_VERSION, "La revisión debe ser 1 o mayor"))
        }
    }

    private fun validateExpressions(expressions: List<ExpressionFile>): List<ContentIssue> = buildList {
        if (expressions.isEmpty()) {
            add(ContentIssue("expressions", ContentIssueReason.EMPTY_PACK, "El pack no contiene ninguna expresión"))
            return@buildList
        }

        val duplicatedIds = expressions
            .groupBy { it.id }
            .filterValues { occurrences -> occurrences.size > 1 }
            .keys

        expressions.forEachIndexed { index, expression ->
            val path = "expressions[$index]"
            addIdentifierIssue("$path.id", expression.id)

            if (expression.phrase.isBlank()) {
                add(ContentIssue("$path.expression", ContentIssueReason.BLANK_FIELD, "La expresión no puede estar vacía"))
            }
            if (expression.meaning.isBlank()) {
                add(ContentIssue("$path.meaning", ContentIssueReason.BLANK_FIELD, "La expresión necesita un significado"))
            }
            addDifficultyIssue("$path.difficulty", expression.difficulty, required = true)
            addLevelIssue("$path.level", expression.level)
            addAll(validateExamples(expression, path))
            addAll(validatePatterns(expression, path))
            addAll(validateTags(expression, path))
        }

        // Un id repetido es un problema único, no uno por cada copia.
        duplicatedIds.forEach { duplicatedId ->
            val index = expressions.indexOfFirst { it.id == duplicatedId }
            add(
                ContentIssue(
                    path = "expressions[$index].id",
                    reason = ContentIssueReason.DUPLICATE_EXPRESSION_ID,
                    detail = "El id $duplicatedId está repetido en el pack",
                ),
            )
        }
    }

    private fun validateExamples(expression: ExpressionFile, path: String): List<ContentIssue> = buildList {
        if (expression.examples.isEmpty()) {
            add(ContentIssue("$path.examples", ContentIssueReason.NO_EXAMPLES, "Se necesita al menos un ejemplo para practicar la expresión"))
        }

        if (expression.examples.count { it.isPrimary } > 1) {
            add(ContentIssue("$path.examples", ContentIssueReason.MULTIPLE_PRIMARY_EXAMPLES, "Solo una oración puede marcarse como principal"))
        }

        expression.examples.forEachIndexed { index, example ->
            val examplePath = "$path.examples[$index]"
            if (example.english.isBlank()) {
                add(ContentIssue("$examplePath.english", ContentIssueReason.BLANK_FIELD, "La oración de ejemplo no puede estar vacía"))
            }
            addDifficultyIssue("$examplePath.difficulty", example.difficulty, required = false)
        }
    }

    private fun validatePatterns(expression: ExpressionFile, path: String): List<ContentIssue> = buildList {
        expression.patterns.forEachIndexed { index, pattern ->
            val patternPath = "$path.patterns[$index]"
            if (pattern.isBlank()) {
                add(ContentIssue(patternPath, ContentIssueReason.BLANK_FIELD, "Un patrón no puede estar vacío"))
            }
        }
        if (expression.patterns.distinct().size != expression.patterns.size) {
            add(ContentIssue("$path.patterns", ContentIssueReason.DUPLICATE_PATTERN, "Hay patrones repetidos"))
        }
    }

    private fun validateTags(expression: ExpressionFile, path: String): List<ContentIssue> = buildList {
        expression.tags.forEachIndexed { index, tag ->
            if (tag.isBlank() || tag != tag.trim()) {
                add(ContentIssue("$path.tags[$index]", ContentIssueReason.BLANK_FIELD, "Una etiqueta no puede estar vacía ni llevar espacios sobrantes"))
            }
        }
        if (expression.tags.distinct().size != expression.tags.size) {
            add(ContentIssue("$path.tags", ContentIssueReason.DUPLICATE_TAG, "Hay etiquetas repetidas en la misma expresión"))
        }
    }

    private fun MutableList<ContentIssue>.addIdentifierIssue(path: String, id: String) {
        when {
            id.isBlank() -> add(ContentIssue(path, ContentIssueReason.BLANK_FIELD, "El identificador no puede estar vacío"))
            !IDENTIFIER.matches(id) -> add(
                ContentIssue(path, ContentIssueReason.INVALID_IDENTIFIER, "Se esperaba un id en kebab-case minúsculas, por ejemplo core-english"),
            )
        }
    }

    private fun MutableList<ContentIssue>.addDifficultyIssue(path: String, difficulty: String?, required: Boolean) {
        if (difficulty == null && !required) return
        if (difficulty !in DIFFICULTIES) {
            add(ContentIssue(path, ContentIssueReason.UNKNOWN_DIFFICULTY, "Valores admitidos: ${DIFFICULTIES.joinToString()}"))
        }
    }

    private fun MutableList<ContentIssue>.addLevelIssue(path: String, level: String?) {
        if (level != null && level !in LEVELS) {
            add(ContentIssue(path, ContentIssueReason.UNKNOWN_LEVEL, "Valores admitidos: ${LEVELS.joinToString()}"))
        }
    }
}
