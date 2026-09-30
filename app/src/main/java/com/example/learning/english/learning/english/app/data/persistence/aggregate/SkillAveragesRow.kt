package com.example.learning.english.learning.english.app.data.persistence.aggregate

/**
 * Fila de un agregado: las medias de los dos ejes, tal como las devuelve SQL.
 *
 * `AVG` sobre un conjunto vacío es `NULL`, no 0, y esa diferencia importa: sin
 * revisiones no hay porcentaje que mostrar, sino ausencia de dato.
 */
data class SkillAveragesRow(
    val avgRecognition: Double?,
    val avgProduction: Double?,
)
