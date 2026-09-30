package com.example.learning.english.learning.english.app.domain.repository

import com.example.learning.english.learning.english.app.domain.model.ContentPack
import kotlinx.coroutines.flow.Flow

/**
 * Packs de contenido instalados.
 *
 * El onboarding lo consume para poder elegir packs: la lista sale de Room
 * porque el contenido se importa en segundo plano y la UI reacciona por Flow,
 * sin esperar a que termine la instalación.
 */
interface ContentPackRepository {

    fun observeAll(): Flow<List<ContentPack>>
}
