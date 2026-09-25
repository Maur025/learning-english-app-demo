package com.example.learning.english.learning.english.app.data.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Etiqueta multidimensional compartida entre packs (`phrasal-verb`, `B1`, …). */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
)
