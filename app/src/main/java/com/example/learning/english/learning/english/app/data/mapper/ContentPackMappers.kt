package com.example.learning.english.learning.english.app.data.mapper

import com.example.learning.english.learning.english.app.data.content.PackMetadata
import com.example.learning.english.learning.english.app.data.content.ParsedContentPack
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.data.persistence.relation.ContentPackWithCount
import com.example.learning.english.learning.english.app.domain.model.ContentPack
import com.example.learning.english.learning.english.app.domain.model.PackId

/**
 * El pack ya trae sus metadatos de archivo: aquí solo se decide qué marca de
 * tiempo se guarda y si viene empaquetado con la app.
 */
fun ParsedContentPack.toPackEntity(installedAt: Long, updatedAt: Long): ContentPackEntity =
    metadata.toEntity(installedAt = installedAt, updatedAt = updatedAt, bundled = true)

fun PackMetadata.toEntity(installedAt: Long, updatedAt: Long, bundled: Boolean): ContentPackEntity =
    ContentPackEntity(
        id = id.value,
        name = name,
        description = description,
        version = version,
        language = language,
        source = source,
        installedAt = installedAt,
        updatedAt = updatedAt,
        bundled = bundled,
    )

/**
 * Proyecta el pack instalado al modelo de dominio.
 *
 * El conteo de expresiones no se guarda en la fila del pack justamente para que
 * esta proyección no pueda quedar vieja: siempre viene del subselect.
 */
fun ContentPackWithCount.toDomain(): ContentPack = ContentPack(
    id = PackId(pack.id),
    name = pack.name,
    description = pack.description,
    expressionCount = expressionCount,
    bundled = pack.bundled,
)
