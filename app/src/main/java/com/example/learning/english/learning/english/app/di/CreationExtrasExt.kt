package com.example.learning.english.learning.english.app.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.learning.english.learning.english.app.EnglishTrainerApp
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.ui.navigation.Routes

/**
 * Acceso al contenedor desde la fábrica de ViewModels.
 *
 * Así la UI no recibe el contenedor por parámetro: cada ViewModel declara en su
 * `initializer` lo que necesita del grafo y nada más (README §13, §47).
 */
val CreationExtras.appContainer: AppContainer
    get() = (requireNotNull(this[APPLICATION_KEY]) as EnglishTrainerApp).container

/**
 * Identificador de la sesión de la entrada de navegación actual.
 *
 * Se lee del `SavedStateHandle` en vez de pasarlo como argumento de la fábrica: el
 * `sessionId` ya forma parte de los argumentos de la ruta, así que el handle lo
 * tiene sin que nadie tenga que duplicarlo (README §44).
 *
 * Si falta, la ruta está mal construida y conviene fallar aquí: un
 * `SessionId("")` abriría una sesión vacía en lugar de decir que el error está en
 * el destino.
 */
val CreationExtras.practiceSessionId: SessionId
    get() = SessionId(
        requireNotNull(
            createSavedStateHandle()[Routes.PRACTICE_ARG_SESSION_ID],
        ) { "practice route is missing ${Routes.PRACTICE_ARG_SESSION_ID}" },
    )
