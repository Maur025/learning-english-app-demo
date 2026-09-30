package com.example.learning.english.learning.english.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.model.SessionSummary
import com.example.learning.english.learning.english.app.ui.theme.LearningEnglishAppTheme
import java.util.Locale

/**
 * Resumen de la sesión terminada (README §43.5).
 *
 * Solo se pinta lo que se sabe. Los porcentajes vienen en `null` cuando la sesión
 * no tuvo ejercicios de ese eje, y aquí simplemente no se muestran: un 0 % sobre
 * cero ejercicios sería un dato falso, no un dato pequeño.
 */
@Composable
fun SessionSummaryCard(
    summary: SessionSummary,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.practice_summary_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (summary.reviewedCount == 0) {
                Text(text = stringResource(R.string.practice_summary_empty))
            }
            summary.durationMillis?.let { millis ->
                LabelledText(
                    label = stringResource(R.string.practice_summary_duration_label),
                    value = formatDuration(millis),
                )
            }
            LabelledText(
                label = stringResource(R.string.practice_summary_reviewed_label),
                value = summary.reviewedCount.toString(),
            )
            LabelledText(
                label = stringResource(R.string.practice_summary_new_label),
                value = summary.newExpressions.toString(),
            )
            summary.recognitionPercent?.let { percent ->
                LabelledText(
                    label = stringResource(R.string.practice_summary_recognition_label),
                    value = percent.toString(),
                )
            }
            summary.productionPercent?.let { percent ->
                LabelledText(
                    label = stringResource(R.string.practice_summary_production_label),
                    value = percent.toString(),
                )
            }
            LabelledText(
                label = stringResource(R.string.practice_summary_improved_label),
                value = summary.improvedExpressions.toString(),
            )
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.practice_close))
            }
        }
    }
}

/**
 * Duración legible: minutos y segundos.
 *
 * Se formatea aquí y no en el modelo porque es una decisión de presentación, y el
 * modelo no debería saber de cadenas. Además, la duración es el tiempo que la
 * sesión estuvo abierta, no el tiempo practicando (§43.5): por eso el resumen lo
 * enseña como "duración de la sesión" y no como tiempo de estudio.
 */
private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds == 0L) {
        String.format(Locale.getDefault(), "%d min", minutes)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryPreview() {
    LearningEnglishAppTheme {
        SessionSummaryCard(
            summary = SessionSummary(
                durationMillis = 312_000L,
                reviewedCount = 10,
                newExpressions = 3,
                recognitionPercent = 80,
                productionPercent = null,
                improvedExpressions = 4,
            ),
            onFinish = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptySummaryPreview() {
    LearningEnglishAppTheme {
        SessionSummaryCard(summary = SessionSummary.EMPTY, onFinish = {})
    }
}
