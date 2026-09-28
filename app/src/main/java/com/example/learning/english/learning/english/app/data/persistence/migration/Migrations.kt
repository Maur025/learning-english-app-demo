package com.example.learning.english.learning.english.app.data.persistence.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migraciones explícitas de esquema (README §48).
 *
 * La historia de aprendizaje debe sobrevivir a cada actualización, así que
 * `fallbackToDestructiveMigration` queda prohibido. Cada vez que suba
 * [com.example.learning.english.learning.english.app.data.persistence.AppDatabase.SCHEMA_VERSION]
 * se añade aquí su migración y se sube en el test de migración de la Fase 10.
 */
object Migrations {

    /**
     * Añade el punto de avance de la sesión. Es aditiva: las sesiones ya
     * guardadas empiezan en 0, que es exactamente donde estaban.
     */
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE learning_sessions ADD COLUMN currentPosition INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(V1_TO_V2)
}
