package com.example.learning.english.learning.english.app.data.persistence.migration

import androidx.room.migration.Migration

/**
 * Migraciones explícitas de esquema (README §48).
 *
 * La historia de aprendizaje debe sobrevivir a cada actualización, así que
 * `fallbackToDestructiveMigration` queda prohibido. Cada vez que suba
 * [com.example.learning.english.learning.english.app.data.persistence.AppDatabase.SCHEMA_VERSION]
 * se añade aquí su migración y se sube en el test de migración de la Fase 10.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
