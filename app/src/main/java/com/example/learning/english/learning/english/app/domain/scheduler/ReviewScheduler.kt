package com.example.learning.english.learning.english.app.domain.scheduler

import com.example.learning.english.learning.english.app.domain.model.LearningState
import com.example.learning.english.learning.english.app.domain.model.ReviewRating

/**
 * Programador de repetición espaciada (§4.3).
 *
 * Calcula el próximo repaso de una expresión a partir de su estado actual y
 * la calificación del usuario. La interfaz aísla el algoritmo (SM-2 hoy,
 * FSRS en el futuro) para que pueda cambiar sin tocar la UI ni la
 * persistencia: la entrada y la salida son [LearningState] puro.
 *
 * La firma usa [ReviewRating] directamente; cuando `LearningEngine` exista
 * (Fase 6), el resultado del ejercicio lo envolverá sin cambiar esta
 * interfaz.
 */
fun interface ReviewScheduler {
    fun schedule(state: LearningState, rating: ReviewRating, reviewedAt: Long): LearningState
}