package com.example.learning.english.learning.english.app.ui.practice

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.model.ReviewRating

/**
 * Práctica: un ejercicio, una respuesta y una calificación por paso (README §43.4).
 *
 * La pantalla no decide nada. Quién sabe si la expresión es nueva, si la respuesta
 * era correcta o cuál es la siguiente ya viene resuelto en [PracticeUiState], y la
 * única decisión que queda —qué botón se pulsa— se reporta hacia fuera.
 *
 * Todo cabe en una columna con scroll porque la fuente es texto: con tipografía
 * grande un ejercicio escrito puede no caber en pantalla, y recortarlo sería peor
 * que permitir el desplazamiento (§65).
 */
@Composable
fun PracticeScreen(
    uiState: PracticeUiState,
    onIntroductionSeen: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { PracticeTopBar(uiState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState) {
                PracticeUiState.Loading -> BusyIndicator()

                is PracticeUiState.Active -> ActiveTurn(
                    state = uiState,
                    onIntroductionSeen = onIntroductionSeen,
                    onOptionSelected = onOptionSelected,
                    onTextChanged = onTextChanged,
                    onCheck = onCheck,
                    onRate = onRate,
                )

                is PracticeUiState.Completed -> SessionSummaryCard(
                    summary = uiState.summary,
                    onFinish = onFinish,
                )

                PracticeUiState.NothingToPractice -> ClosingSection(
                    messageRes = R.string.practice_nothing_to_do,
                    onFinish = onFinish,
                )

                is PracticeUiState.Failed -> ClosingSection(
                    messageRes = uiState.messageRes,
                    onFinish = onFinish,
                )
            }
        }
    }
}

/**
 * Un paso de la sesión: o se presenta la expresión, o se practica.
 *
 * Son excluyentes por diseño. La presentación se salta al responder, así que no
 * hace falta un botón aparte: el gesto de responder ya la descarta.
 */
@Composable
private fun ActiveTurn(
    state: PracticeUiState.Active,
    onIntroductionSeen: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onRate: (ReviewRating) -> Unit,
) {
    val introduction = state.introduction
    if (introduction != null) {
        ExpressionIntroductionCard(
            introduction = introduction,
            onUnderstood = onIntroductionSeen,
        )
        return
    }
    ExerciseCard(
        prompt = state.prompt,
        turn = state.turn,
        onOptionSelected = onOptionSelected,
        onTextChanged = onTextChanged,
        onCheck = onCheck,
        onRate = onRate,
    )
}

/**
 * Barra superior con el avance de la sesión.
 *
 * Solo aparece mientras se practica: en el resumen o en un error el avance ya no
 * aporta nada y solo ocupa sitio.
 */
@Composable
private fun PracticeTopBar(uiState: PracticeUiState) {
    val progress = (uiState as? PracticeUiState.Active)?.progress
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.practice_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (progress != null) {
                Text(
                    text = stringResource(
                        R.string.practice_progress,
                        progress.current,
                        progress.total,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Presentación de una expresión que el usuario no ha visto nunca (README §43.3).
 *
 * Es lo único que la app enseña sin que nadie pregunte: significado, patrón y
 * ejemplo. No califica ni registra nada — presentar no es recordar (§35) — por eso
 * el ViewModel solo la descarta cuando el usuario responde.
 */
@Composable
private fun ExpressionIntroductionCard(
    introduction: ExpressionIntroUiModel,
    onUnderstood: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = introduction.phrase,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            LabelledText(
                label = stringResource(R.string.practice_meaning),
                value = introduction.meaning,
            )
            introduction.pattern?.let {
                LabelledText(label = stringResource(R.string.practice_pattern), value = it)
            }
            introduction.explanation?.let {
                LabelledText(label = stringResource(R.string.practice_explanation), value = it)
            }
            introduction.examples.forEach { example ->
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text = stringResource(R.string.practice_introduction_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onUnderstood,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.practice_intro_understand))
            }
        }
    }
}

/** Resumen de la sesión terminada, o el motivo por el que no hay nada que ver. */
@Composable
private fun ClosingSection(@StringRes messageRes: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.practice_close))
        }
    }
}

@Composable
internal fun BusyIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun LabelledText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

