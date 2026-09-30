package com.example.learning.english.learning.english.app.di

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.learning.english.learning.english.app.EnglishTrainerApp

/**
 * Acceso al contenedor desde la fábrica de ViewModels.
 *
 * Así la UI no recibe el contenedor por parámetro: cada ViewModel declara en su
 * `initializer` lo que necesita del grafo y nada más (README §13, §47).
 */
val CreationExtras.appContainer: AppContainer
    get() = (requireNotNull(this[APPLICATION_KEY]) as EnglishTrainerApp).container
