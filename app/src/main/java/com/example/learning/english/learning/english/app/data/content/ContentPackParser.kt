package com.example.learning.english.learning.english.app.data.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

/**
 * Lee el esquema JSON de un pack (README §28, Fase 3).
 *
 * Solo se ocupa de la forma del archivo: la corrección del contenido la
 * comprueba [ContentPackValidator].
 */
class ContentPackParser(
    private val json: Json = DEFAULT_JSON,
) {

    fun parse(rawJson: String): ContentParseResult = try {
        ContentParseResult.Parsed(json.decodeFromString<ContentPackFile>(rawJson))
    } catch (exception: SerializationException) {
        ContentParseResult.Malformed(
            ContentIssue(
                path = "$",
                reason = ContentIssueReason.MALFORMED_JSON,
                detail = exception.message.orEmpty().ifBlank { "El archivo no encaja con el esquema de contenido" },
            ),
        )
    }

    private companion object {
        val DEFAULT_JSON = Json {
            // Un campo opcional nuevo no debe invalidar los packs ya instalados.
            ignoreUnknownKeys = true
        }
    }
}
