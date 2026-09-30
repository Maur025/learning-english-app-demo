package com.example.learning.english.learning.english.app.data.repository

import com.example.learning.english.learning.english.app.domain.model.PackId

/**
 * Los ids de pack viajan a Room como `String` plano porque son claves de tabla,
 * no valores de dominio.
 *
 * Vive en la capa de datos y es interno: ningún DAO debe recibir un [PackId].
 */
internal fun Set<PackId>.toStorageIds(): List<String> = map { it.value }
