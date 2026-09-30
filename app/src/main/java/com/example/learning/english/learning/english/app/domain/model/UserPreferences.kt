package com.example.learning.english.learning.english.app.domain.model

/**
 * Preferencias ligeras de la aplicación (README §19).
 *
 * Solo lo que V1 consume; `locale` y `exercisePreferences` se añadirán cuando
 * exista un caso de uso real. Nunca se guardan aquí datos de aprendizaje: eso
 * vive en Room.
 */
data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val dailyGoalMinutes: Int = DEFAULT_DAILY_GOAL_MINUTES,
    val newExpressionsPerDay: Int = DEFAULT_NEW_EXPRESSIONS_PER_DAY,
    val preferredPackIds: Set<PackId> = emptySet(),
    val theme: ThemePreference = ThemePreference.SYSTEM,
) {
    init {
        require(dailyGoalMinutes > 0) { "dailyGoalMinutes must be positive" }
        require(newExpressionsPerDay >= 0) { "newExpressionsPerDay must not be negative" }
    }

    companion object {
        const val DEFAULT_DAILY_GOAL_MINUTES = 30
        const val DEFAULT_NEW_EXPRESSIONS_PER_DAY = 5
    }
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Packs con los que se practica.
 *
 * Sin selección explícita se usa todo lo instalado: es el valor por defecto de
 * las preferencias nuevas y evita una sesión vacía por descuido. Los ids que ya
 * no estén instalados se ignoran, porque la lista válida la impone el contenido
 * y no la preferencia.
 */
fun UserPreferences.selectedPackIds(installedPackIds: Set<PackId>): Set<PackId> {
    val selection = preferredPackIds.intersect(installedPackIds)
    return selection.ifEmpty { installedPackIds }
}
