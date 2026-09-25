package com.example.learning.english.learning.english.app.domain.model

/** Etiqueta multidimensional (`phrasal-verb`, `software-development`, `B1`, …). */
data class Tag(
    val id: TagId,
    val name: String,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
    }
}
