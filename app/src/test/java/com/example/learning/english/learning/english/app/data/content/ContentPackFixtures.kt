package com.example.learning.english.learning.english.app.data.content

/** JSON mínimo y válido como punto de partida de los tests de contenido. */
internal object ContentPackFixtures {

    const val PACK_ID = "core-english"

    val validPack: String = """
        {
          "schemaVersion": 1,
          "pack": {
            "id": "core-english",
            "name": "Core English",
            "description": "Expresiones del inglés general",
            "version": 1,
            "language": "en"
          },
          "expressions": [
            {
              "id": "figure-out",
              "expression": "figure out",
              "meaning": "averiguar, resolver",
              "explanation": "Use it when you find the answer.",
              "difficulty": "MEDIUM",
              "level": "B1",
              "patterns": ["figure + object + out"],
              "examples": [
                {
                  "english": "I'm trying to figure out what happened.",
                  "spanish": "Estoy intentando averiguar qué pasó.",
                  "context": "general"
                }
              ],
              "tags": ["phrasal-verb", "problem-solving"]
            }
          ]
        }
    """.trimIndent()

    /** Aplica una transformación textual sobre un pack válido. */
    fun validPackWith(replacement: String, target: String): String = validPack.replace(target, replacement)
}
