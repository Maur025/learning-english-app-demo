package com.example.learning.english.learning.english.app.data.persistence.converter

import androidx.room.TypeConverter
import com.example.learning.english.learning.english.app.domain.model.CefrLevel
import com.example.learning.english.learning.english.app.domain.model.Difficulty
import com.example.learning.english.learning.english.app.domain.model.LearningStage
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType

/**
 * Persistencia de enums como `String` por nombre.
 *
 * Se usa `name` (y no el ordinal) para que un cambio de orden no corrompa datos.
 */
class EnumConverters {

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String = value.name

    @TypeConverter
    fun toDifficulty(value: String): Difficulty = enumValueOf(value)

    @TypeConverter
    fun fromCefrLevel(value: CefrLevel?): String? = value?.name

    @TypeConverter
    fun toCefrLevel(value: String?): CefrLevel? = value?.let { enumValueOf<CefrLevel>(it) }

    @TypeConverter
    fun fromLearningStage(value: LearningStage): String = value.name

    @TypeConverter
    fun toLearningStage(value: String): LearningStage = enumValueOf(value)

    @TypeConverter
    fun fromReviewType(value: ReviewType): String = value.name

    @TypeConverter
    fun toReviewType(value: String): ReviewType = enumValueOf(value)

    @TypeConverter
    fun fromReviewRating(value: ReviewRating): String = value.name

    @TypeConverter
    fun toReviewRating(value: String): ReviewRating = enumValueOf(value)
}
