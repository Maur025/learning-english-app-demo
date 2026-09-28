package com.example.learning.english.learning.english.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.learning.english.learning.english.app.data.content.AssetContentSource
import com.example.learning.english.learning.english.app.data.content.BundledContentInstaller
import com.example.learning.english.learning.english.app.data.content.ContentPackParser
import com.example.learning.english.learning.english.app.data.content.RoomContentImporter
import com.example.learning.english.learning.english.app.data.persistence.AppDatabase
import com.example.learning.english.learning.english.app.data.preferences.DataStoreUserPreferencesRepository
import com.example.learning.english.learning.english.app.data.repository.RoomExpressionRepository
import com.example.learning.english.learning.english.app.data.repository.RoomLearningStateRepository
import com.example.learning.english.learning.english.app.data.repository.RoomReviewRepository
import com.example.learning.english.learning.english.app.data.repository.RoomSessionRepository
import com.example.learning.english.learning.english.app.domain.engine.LearningStateProgressor
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluator
import com.example.learning.english.learning.english.app.domain.exercise.DefaultExerciseGenerator
import com.example.learning.english.learning.english.app.domain.exercise.ExerciseGenerator
import com.example.learning.english.learning.english.app.domain.repository.ExpressionRepository
import com.example.learning.english.learning.english.app.domain.repository.LearningStateRepository
import com.example.learning.english.learning.english.app.domain.repository.ReviewRepository
import com.example.learning.english.learning.english.app.domain.repository.SessionRepository
import com.example.learning.english.learning.english.app.domain.repository.UserPreferencesRepository
import com.example.learning.english.learning.english.app.domain.scheduler.ReviewScheduler
import com.example.learning.english.learning.english.app.domain.scheduler.Sm2ReviewScheduler
import com.example.learning.english.learning.english.app.domain.service.ReviewRecorder

/**
 * Grafo de dependencias con inyección manual.
 *
 * El proyecto no usa todavía Hilt: con este tamaño de grafo la construcción
 * centralizada aquí es suficiente y deja los repositorios reemplazables en
 * tests (README §47). Todo es lazy para no abrir la base de datos al arrancar.
 */
class AppContainer(context: Context) {

    private val applicationContext: Context = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.create(applicationContext) }

    private val preferencesDataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            applicationContext.preferencesDataStoreFile(PREFERENCES_FILE_NAME)
        }
    }

    val expressionRepository: ExpressionRepository by lazy {
        RoomExpressionRepository(database)
    }

    val learningStateRepository: LearningStateRepository by lazy {
        RoomLearningStateRepository(database.learningStateDao())
    }

    val reviewRepository: ReviewRepository by lazy {
        RoomReviewRepository(database.reviewEventDao())
    }

    val sessionRepository: SessionRepository by lazy {
        RoomSessionRepository(database.learningSessionDao())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        DataStoreUserPreferencesRepository(preferencesDataStore)
    }

    val reviewScheduler: ReviewScheduler by lazy { Sm2ReviewScheduler() }

    val exerciseGenerator: ExerciseGenerator by lazy { DefaultExerciseGenerator() }

    val answerEvaluator: AnswerEvaluator by lazy { AnswerEvaluator() }

    val learningStateProgressor: LearningStateProgressor by lazy { LearningStateProgressor() }

    val reviewRecorder: ReviewRecorder by lazy {
        ReviewRecorder(
            scheduler = reviewScheduler,
            progressor = learningStateProgressor,
            learningStateRepository = learningStateRepository,
            reviewRepository = reviewRepository,
        )
    }

    val contentImporter: RoomContentImporter by lazy { RoomContentImporter(database) }

    val contentInstaller: BundledContentInstaller by lazy {
        BundledContentInstaller(
            contentSource = AssetContentSource(applicationContext),
            parser = ContentPackParser(),
            importer = contentImporter,
        )
    }

    private companion object {
        const val PREFERENCES_FILE_NAME = "user_preferences"
    }
}
