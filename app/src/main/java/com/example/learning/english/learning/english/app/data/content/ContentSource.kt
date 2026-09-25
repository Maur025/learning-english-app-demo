package com.example.learning.english.learning.english.app.data.content

import android.content.Context
import java.io.IOException

/**
 * Origen de los archivos de contenido empaquetados.
 *
 * Existe para que la importación se pueda probar sin assets de Android. Si el
 * archivo no existe, [readText] lanza [IOException] y el instalador lo reporta
 * como fallo: un asset que falta es un error de build, no un motivo para
 * tumbar el arranque.
 */
interface ContentSource {
    fun readText(fileName: String): String
}

/** Lee los packs de `app/src/main/assets/content` (README §28). */
class AssetContentSource(
    context: Context,
    private val directory: String = BundledContent.DIRECTORY,
) : ContentSource {

    private val assets = context.applicationContext.assets

    override fun readText(fileName: String): String =
        assets.open("$directory/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
}

/** Contenido que viaja dentro del APK. */
object BundledContent {
    const val DIRECTORY = "content"

    val FILES: List<String> = listOf(
        "core-english.json",
        "developer-english.json",
    )
}
