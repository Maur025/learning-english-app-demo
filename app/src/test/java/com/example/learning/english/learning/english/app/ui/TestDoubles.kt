package com.example.learning.english.learning.english.app.ui

import com.example.learning.english.learning.english.app.domain.model.ContentPack
import com.example.learning.english.learning.english.app.domain.model.PackId
import com.example.learning.english.learning.english.app.domain.model.UserPreferences
import com.example.learning.english.learning.english.app.domain.repository.ContentPackRepository
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Los ViewModels trabajan en `viewModelScope`, que despacha en el hilo principal.
 *
 * Sin esto, cualquier test que llame a una acción suspendida se queda esperando
 * un hilo que en un test JVM no existe.
 */
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** Preferencias en memoria: los tests de UI no necesitan DataStore. */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
    /** Al completarse, la escritura termina. Permite simular un guardado lento. */
    private val saveGate: CompletableDeferred<Unit>? = null,
) : UserPreferencesRepository {

    private val preferences = MutableStateFlow(initial)

    /** Veces que se ha escrito: permite comprobar que no se guarda dos veces. */
    var saveCount = 0
        private set

    override fun observe(): Flow<UserPreferences> = preferences.asStateFlow()

    override suspend fun current(): UserPreferences = preferences.value

    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        saveGate?.await()
        preferences.value = transform(preferences.value)
        saveCount++
    }
}

/** Packs instalados en memoria, para las pantallas que eligen contenido. */
class FakeContentPackRepository(
    packs: List<ContentPack> = emptyList(),
) : ContentPackRepository {

    private val installed = MutableStateFlow(packs)

    override fun observeAll(): Flow<List<ContentPack>> = installed.asStateFlow()

    fun install(packs: List<ContentPack>) {
        installed.value = packs
    }
}

fun contentPack(
    id: String,
    name: String = id,
    expressionCount: Int = 10,
): ContentPack = ContentPack(
    id = PackId(id),
    name = name,
    description = null,
    expressionCount = expressionCount,
    bundled = true,
)
