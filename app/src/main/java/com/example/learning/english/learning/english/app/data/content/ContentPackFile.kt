package com.example.learning.english.learning.english.app.data.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Esquema en disco de un pack de contenido, versión 1 (README §28).
 *
 * Los tipos viajan como texto, no como enums, para que [ContentPackValidator]
 * pueda reportar un valor desconocido como incidencia en lugar de romper el
 * archivo entero. Las claves desconocidas se ignoran: añadir un campo opcional
 * no debe invalidar los packs ya instalados.
 */
@Serializable
data class ContentPackFile(
    val schemaVersion: Int,
    val pack: PackMetadataFile,
    val expressions: List<ExpressionFile> = emptyList(),
)

/** Metadatos del pack (README §27). `installedAt` y `updatedAt` los pone la app. */
@Serializable
data class PackMetadataFile(
    val id: String,
    val name: String,
    val description: String? = null,
    val version: Int = 1,
    val language: String = "en",
    val source: String? = null,
)

/**
 * Una expresión del pack.
 *
 * `id` es único dentro del pack; la app lo namespacea con el id del pack para
 * que dos packs puedan usar ids locales iguales.
 */
@Serializable
data class ExpressionFile(
    val id: String,
    @SerialName("expression") val phrase: String,
    val meaning: String,
    val explanation: String? = null,
    val source: String? = null,
    /** `EASY`, `MEDIUM` o `HARD`. */
    val difficulty: String,
    /** `A1`–`C2`, opcional. */
    val level: String? = null,
    val patterns: List<String> = emptyList(),
    val examples: List<ExampleFile> = emptyList(),
    val tags: List<String> = emptyList(),
)

/**
 * Oración de ejemplo.
 *
 * [isPrimary] marca la que explica mejor la expresión. Si ninguna lo está, el
 * mapper usa la primera: los ejercicioscontextuales la prefieren.
 */
@Serializable
data class ExampleFile(
    val english: String,
    val spanish: String? = null,
    val context: String? = null,
    /** Dificultad de esta oración; `null` hereda la de la expresión. */
    val difficulty: String? = null,
    val isPrimary: Boolean = false,
)
