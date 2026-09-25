package com.example.learning.english.learning.english.app.domain.model

/** Calificación interna de una respuesta. No depende de los textos mostrados en pantalla (§34). */
enum class ReviewRating {
    FORGOT,
    HARD,
    GOOD,
    EASY,
    ;

    val isCorrect: Boolean
        get() = this != FORGOT
}
