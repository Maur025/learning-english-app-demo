package com.example.learning.english.learning.english.app

import android.app.Application
import com.example.learning.english.learning.english.app.di.AppContainer

/**
 * Punto de acceso al grafo de dependencias. Solo existe una Activity
 * (`MainActivity`); el contenedor vive aquí (README §11, §47).
 */
class EnglishTrainerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
