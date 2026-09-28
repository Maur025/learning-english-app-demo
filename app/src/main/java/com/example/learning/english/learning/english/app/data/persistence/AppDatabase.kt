package com.example.learning.english.learning.english.app.data.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.learning.english.learning.english.app.data.persistence.converter.EnumConverters
import com.example.learning.english.learning.english.app.data.persistence.dao.ContentPackDao
import com.example.learning.english.learning.english.app.data.persistence.dao.ExpressionDao
import com.example.learning.english.learning.english.app.data.persistence.dao.LearningSessionDao
import com.example.learning.english.learning.english.app.data.persistence.dao.LearningStateDao
import com.example.learning.english.learning.english.app.data.persistence.dao.ReviewEventDao
import com.example.learning.english.learning.english.app.data.persistence.dao.TagDao
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExampleEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ExpressionTagCrossRef
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningSessionEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.LearningStateEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.PatternEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.ReviewEventEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.SessionExerciseEntity
import com.example.learning.english.learning.english.app.data.persistence.entity.TagEntity
import com.example.learning.english.learning.english.app.data.persistence.migration.Migrations

/**
 * Fuente de verdad local (README §17–18).
 *
 * El esquema se exporta a `app/schemas` en cada compilación: es la base de las
 * migraciones futuras y de los tests de migración de la Fase 10.
 */
@Database(
    entities = [
        ContentPackEntity::class,
        ExpressionEntity::class,
        ExampleEntity::class,
        PatternEntity::class,
        TagEntity::class,
        ExpressionTagCrossRef::class,
        LearningStateEntity::class,
        ReviewEventEntity::class,
        LearningSessionEntity::class,
        SessionExerciseEntity::class,
    ],
    version = AppDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contentPackDao(): ContentPackDao

    abstract fun expressionDao(): ExpressionDao

    abstract fun tagDao(): TagDao

    abstract fun learningStateDao(): LearningStateDao

    abstract fun reviewEventDao(): ReviewEventDao

    abstract fun learningSessionDao(): LearningSessionDao

    companion object {
        const val SCHEMA_VERSION = 2
        const val DATABASE_NAME = "learning-english.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(*Migrations.ALL)
                .build()
    }
}
