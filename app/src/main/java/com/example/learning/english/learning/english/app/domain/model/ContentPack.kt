package com.example.learning.english.learning.english.app.domain.model

/**
 * Pack de contenido instalado y elegible desde el onboarding (README §27).
 *
 * Es solo lectura para la UI: no trae expresiones porque Home no las necesita, y
 * cargarlas enteras en cada pantalla que muestra la lista sería tirar trabajo.
 * El contenido se pide a `ExpressionRepository` cuando hace falta.
 *
 * [expressionCount] viene de un `COUNT` sobre la tabla de expresiones, no de un
 * campo almacenado, para que no pueda desincronizarse del contenido real.
 */
data class ContentPack(
    val id: PackId,
    val name: String,
    val description: String?,
    val expressionCount: Int,
    val bundled: Boolean,
) {
    init {
        require(expressionCount >= 0) { "expressionCount must not be negative" }
    }
}
