package com.example.learning.english.learning.english.app.data.content

/**
 * Incidencia de un archivo de contenido, con la ruta exacta donde está.
 *
 * Existir como valor (y no como excepción) permite listar todos los problemas de
 * un pack de una vez: quien escribe el contenido no debería arreglar el archivo
 * de error en error.
 */
data class ContentIssue(
    val path: String,
    val reason: ContentIssueReason,
    val detail: String,
)

enum class ContentIssueReason {
    /** El archivo no se pudo leer (falta el asset o no es legible). */
    UNREADABLE_FILE,

    /** El archivo no es JSON válido o no encaja con el esquema. */
    MALFORMED_JSON,

    /** `schemaVersion` mayor que la que entiende esta versión de la app. */
    UNSUPPORTED_SCHEMA_VERSION,

    BLANK_FIELD,
    INVALID_IDENTIFIER,
    INVALID_VERSION,
    UNKNOWN_DIFFICULTY,
    UNKNOWN_LEVEL,
    EMPTY_PACK,
    DUPLICATE_EXPRESSION_ID,
    DUPLICATE_TAG,
    DUPLICATE_PATTERN,
    MULTIPLE_PRIMARY_EXAMPLES,
    NO_EXAMPLES,
}

/** Resultado de leer un archivo de contenido del disco. */
sealed interface ContentParseResult {
    data class Parsed(val file: ContentPackFile) : ContentParseResult

    data class Malformed(val issue: ContentIssue) : ContentParseResult
}
