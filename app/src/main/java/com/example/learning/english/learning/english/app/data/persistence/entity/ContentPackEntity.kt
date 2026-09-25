package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadatos de un pack de contenido (README §27).
 *
 * `expressionCount` no se almacena: se deriva con un `COUNT` para que nunca
 * pueda desincronizarse de la tabla de expresiones.
 */
@Entity(tableName = "content_packs")
data class ContentPackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    /** Revisión del pack; el versionado del esquema JSON es otro campo. */
    val version: Int,
    val language: String,
    val source: String?,
    val installedAt: Long,
    val updatedAt: Long,
    val bundled: Boolean,
)
