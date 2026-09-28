package com.example.learning.english.learning.english.app.domain.exercise

import java.util.Locale

/**
 * Normalización de texto para comparaciones deterministas (README §36).
 *
 * Sin normalización morfológica: unifica tipografía, mayúsculas, espacios y
 * puntuación terminal. No hace lematización ni compara tokens sueltos, porque
 * "figure out" y "figure of" deben seguir siendo distintas: aceptar por
 * parecido de tokens aceptaría respuestas semánticamente incorrectas (§36).
 */
object TextNormalizer {

    /**
     * Normaliza un texto para compararlo:
     * 1. convierte la tipografía en ASCII (`’` -> `'`, `“”` -> `"`, `–` -> `-`);
     * 2. pasa a minúsculas;
     * 3. colapsa cualquier secuencia de espacios en un único espacio;
     * 4. quita espacios y signos de puntuación de los extremos.
     */
    fun normalize(text: String): String {
        val ascii = text.map { TYPOGRAPHIC_REPLACEMENTS[it] ?: it }.joinToString("")
        return ascii.split(*WHITESPACE)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .lowercase(Locale.ROOT)
            .trim(*TRIMMED_CHARS)
            .trim()
    }

    /** Compara dos textos normalizándolos; la respuesta en blanco nunca coincide. */
    fun matches(answer: String, expected: String): Boolean {
        val normalizedAnswer = normalize(answer)
        return normalizedAnswer.isNotEmpty() && normalizedAnswer == normalize(expected)
    }

    private val TYPOGRAPHIC_REPLACEMENTS: Map<Char, Char> = mapOf(
        '\u2018' to '\'', '\u2019' to '\'', '\u201A' to '\'', '\u201B' to '\'',
        '\u201C' to '"', '\u201D' to '"', '\u201E' to '"', '\u201F' to '"',
        '\u2013' to '-', '\u2014' to '-', '\u2012' to '-', '\u2011' to '-',
        '\u00A0' to ' ', '\u2007' to ' ', '\u202F' to ' ',
    )

    private val WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')

    /** Se quitan de los extremos; dentro de la frase se conservan. */
    private val TRIMMED_CHARS = charArrayOf(
        '.', ',', ';', ':', '!', '?', '¿', '¡',
        '(', ')', '[', ']', '{', '}', '"', '\'',
        ' ', '\t', '\n', '\r',
    )
}
