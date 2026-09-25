package com.example.learning.english.learning.english.app.data.content

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Un pack que no se pudo leer ni importar, con el motivo. */
data class ContentImportFailure(
    val fileName: String,
    val issues: List<ContentIssue>,
)

/** Resumen de una pasada de importación: qué entró, qué se saltó y qué falló. */
data class ContentInstallReport(
    val imported: List<PackImportResult.Imported> = emptyList(),
    val skipped: List<PackImportResult.Skipped> = emptyList(),
    val failures: List<ContentImportFailure> = emptyList(),
) {
    val hasFailures: Boolean get() = failures.isNotEmpty()

    val importedExpressionCount: Int get() = imported.sumOf { it.expressionCount }
}

/**
 * Importa el contenido empaquetado en Room (README §29).
 *
 * Es idempotente por sí mismo: volver a llamarlo tras cada arranque no duplica
 * nada, porque [RoomContentImporter] salta los packs cuya revisión ya está
 * instalada. Un pack inválido no detiene a los demás: se reporta como incidencia
 * y se sigue.
 */
class BundledContentInstaller(
    private val contentSource: ContentSource,
    private val parser: ContentPackParser,
    private val importer: RoomContentImporter,
    private val files: List<String> = BundledContent.FILES,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun install(): ContentInstallReport {
        val results = files.map { fileName -> install(fileName) }
        return ContentInstallReport(
            imported = results.filterIsInstance<FileResult.Imported>().map { it.result },
            skipped = results.filterIsInstance<FileResult.Skipped>().map { it.result },
            failures = results.filterIsInstance<FileResult.Failed>().map { it.failure },
        )
    }

    private suspend fun install(fileName: String): FileResult = withContext(dispatcher) {
        val rawJson = try {
            contentSource.readText(fileName)
        } catch (exception: IOException) {
            return@withContext FileResult.Failed(
                ContentImportFailure(
                    fileName,
                    listOf(ContentIssue(fileName, ContentIssueReason.UNREADABLE_FILE, exception.message.orEmpty())),
                ),
            )
        }

        val file = when (val parsed = parser.parse(rawJson)) {
            is ContentParseResult.Malformed -> return@withContext FileResult.Failed(
                ContentImportFailure(fileName, listOf(parsed.issue)),
            )

            is ContentParseResult.Parsed -> parsed.file
        }

        val issues = ContentPackValidator.validate(file)
        if (issues.isNotEmpty()) {
            return@withContext FileResult.Failed(ContentImportFailure(fileName, issues))
        }

        val result = importer.import(ContentPackMapper.toContentPack(file, clock()))
        when (result) {
            is PackImportResult.Imported -> FileResult.Imported(result)
            is PackImportResult.Skipped -> FileResult.Skipped(result)
        }
    }

    private sealed interface FileResult {
        data class Imported(val result: PackImportResult.Imported) : FileResult

        data class Skipped(val result: PackImportResult.Skipped) : FileResult

        data class Failed(val failure: ContentImportFailure) : FileResult
    }
}
