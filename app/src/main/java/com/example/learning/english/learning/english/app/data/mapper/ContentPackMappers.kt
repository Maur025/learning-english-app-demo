package com.example.learning.english.learning.english.app.data.mapper

import com.example.learning.english.learning.english.app.data.content.ContentPack
import com.example.learning.english.learning.english.app.data.content.PackMetadata
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity

/**
 * El pack ya trae sus metadatos de archivo: aquí solo se decide qué marca de
 * tiempo se guarda y si viene empaquetado con la app.
 */
fun ContentPack.toPackEntity(installedAt: Long, updatedAt: Long): ContentPackEntity =
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
