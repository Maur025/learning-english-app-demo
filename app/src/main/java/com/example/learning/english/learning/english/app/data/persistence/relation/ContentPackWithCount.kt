package com.example.learning.english.learning.english.app.data.persistence.relation

import androidx.room.Embedded
import com.example.learning.english.learning.english.app.data.persistence.entity.ContentPackEntity

/**
 * Pack junto al número de expresiones que contiene.
 *
 * El conteo se calcula en la misma consulta con un subselect, así la UI no
 * necesita un Flow por pack ni un `combine` que puede desfasarse entre sí.
 */
data class ContentPackWithCount(
    @Embedded val pack: ContentPackEntity,
    val expressionCount: Int,
)
