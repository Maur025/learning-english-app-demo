package com.example.learning.english.learning.english.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learning.english.learning.english.app.domain.engine.LearningEngine
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.SkillAverages
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.model.selectedPackIds
import com.example.learning.english.learning.english.app.domain.repository.ContentPackRepository
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Home: la siguiente acción del día, evidente de un vistazo (README §43.2).
 *
 * Los contadores no se calculan aquí: llegan agregados desde la capa de datos.
 * La única decisión de este ViewModel es resolver qué packs están en juego,
 * porque es lo que hace que las cifras y la sesión de hoy hablen del mismo
 * contenido.
 *
 * `now` se fija cada vez que los contadores se suscriben, así que abrir Home
 * recalcula qué está vencido. No hay temporizador: la app es offline y de un
 * solo uso diario, un refresco al entrar es suficiente.
 */
class HomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    contentPackRepository: ContentPackRepository,
    private val learningStateRepository: LearningStateRepository,
    private val expressionRepository: ExpressionRepository,
    sessionRepository: SessionRepository,
    private val learningEngine: LearningEngine,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val isStartingSession = MutableStateFlow(false)

    private val _effects = Channel<HomeEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val counters = combine(
        userPreferencesRepository.observe(),
        contentPackRepository.observeAll(),
    ) { preferences, packs ->
        preferences.selectedPackIds(packs.mapTo(mutableSetOf()) { it.id })
    }.flatMapLatest { packIds ->
        combine(
            learningStateRepository.observeDueCount(now(), packIds),
            expressionRepository.observeNewCount(packIds),
            learningStateRepository.observeSkillAverages(packIds),
        ) { dueCount, newCount, averages -> Triple(dueCount, newCount, averages) }
    }

    val uiState = combine(
        userPreferencesRepository.observe(),
        contentPackRepository.observeAll(),
        sessionRepository.observeInProgress(),
        counters,
        isStartingSession,
    ) { preferences, packs, inProgressSession, (dueCount, newCount, averages), starting ->
        HomeUiState(
            dailyGoalMinutes = preferences.dailyGoalMinutes,
            isContentReady = packs.isNotEmpty(),
            dueCount = dueCount,
            newCount = newCount,
            skillAverages = averages,
            inProgressSessionId = inProgressSession?.id,
            answeredCount = inProgressSession?.currentPosition,
            isStartingSession = starting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    fun onStartSession() {
        if (isStartingSession.value || !uiState.value.canStartSession) return
        // El guardia se levanta antes de lanzar el coroutine: leer el valor
        // después dejaría pasar un segundo toque dentro del mismo frame, y cada
        // toque crearía una sesión distinta.
        isStartingSession.value = true
        viewModelScope.launch {
            try {
                val session = learningEngine.createDailySession(
                    preferences = userPreferencesRepository.current(),
                    now = now(),
                )
                _effects.send(HomeEffect.OpenSession(session.id))
            } finally {
                isStartingSession.value = false
            }
        }
    }

    fun onContinueSession() {
        val sessionId = uiState.value.inProgressSessionId ?: return
        _effects.trySend(HomeEffect.OpenSession(sessionId))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Estado de Home.
 *
 * Los contadores valen 0 antes de que el contenido esté instalado: no es un
 * error, es lo que la base de datos sabe hasta que termina la importación.
 */
data class HomeUiState(
    val dailyGoalMinutes: Int = UserPreferences.DEFAULT_DAILY_GOAL_MINUTES,
    val isContentReady: Boolean = false,
    val dueCount: Int = 0,
    val newCount: Int = 0,
    val skillAverages: SkillAverages = SkillAverages.EMPTY,
    val inProgressSessionId: SessionId? = null,
    val answeredCount: Int? = null,
    val isStartingSession: Boolean = false,
) {
    val hasInProgressSession: Boolean
        get() = inProgressSessionId != null

    /** Sin nada vencido ni nuevo no hay sesión que valga la pena empezar. */
    val canStartSession: Boolean
        get() = isContentReady && dueCount + newCount > 0 && !isStartingSession
}

/** Navegaciones que Home decide y la pantalla solo ejecuta. */
sealed interface HomeEffect {
    data class OpenSession(val sessionId: SessionId) : HomeEffect
}
