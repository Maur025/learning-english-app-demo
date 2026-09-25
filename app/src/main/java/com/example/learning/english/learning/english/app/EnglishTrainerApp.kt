package com.example.learning.english.learning.english.app

import android.app.Application
import android.util.Log
import com.example.learning.english.learning.english.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de acceso al grafo de dependencias. Solo existe una Activity
 * (`MainActivity`); el contenedor vive aquí (README §11, §47).
 */
class EnglishTrainerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // El contenido empaquetado se importa en segundo plano: Room es la fuente
        // de verdad y las pantallas lo observan por Flow, así que no hace falta
        // bloquear el arranque (README §29).
        applicationScope.launch {
            val report = container.contentInstaller.install()
            if (report.hasFailures) {
                report.failures.forEach { failure ->
                    Log.w(TAG, "No se pudo importar ${failure.fileName}: ${failure.issues.joinToString()}")
                }
            }
        }
    }

    private companion object {
        const val TAG = "EnglishTrainerApp"
    }
}
