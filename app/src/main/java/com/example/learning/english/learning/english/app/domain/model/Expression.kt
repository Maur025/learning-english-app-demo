package com.example.learning.english.learning.english.app.domain.model

/**
 * Unidad léxica reutilizable; entidad central del dominio (no `Word`).
 *
 * No corresponde uno a uno a una tabla de Room: el mapeo vive en la capa de datos.
 * Las marcas de tiempo son epoch en milisegundos.
 */
data class Expression(
    val id: ExpressionId,
    val phrase: String,
    val primaryMeaning: String,
    val packId: PackId,
    val difficulty: Difficulty,
    val level: CefrLevel? = null,
    val explanation: String? = null,
    val source: String? = null,
    val examples: List<Example> = emptyList(),
    val patterns: List<ExpressionPattern> = emptyList(),
    val tags: Set<Tag> = emptySet(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    init {
        require(phrase.isNotBlank()) { "phrase must not be blank" }
        require(primaryMeaning.isNotBlank()) { "primaryMeaning must not be blank" }
    }
}
