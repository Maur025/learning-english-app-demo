package com.example.learning.english.learning.english.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.ClozeExercise
import com.example.learning.english.learning.english.app.domain.exercise.ProductionExercise
import com.example.learning.english.learning.english.app.domain.exercise.PracticeExercise
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.exercise.RevealContent
import com.example.learning.english.learning.english.app.domain.exercise.TranslationExercise
import com.example.learning.english.learning.english.app.domain.exercise.WrittenExercise
import com.example.learning.english.learning.english.app.domain.exercise.GuidedRecallExercise
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.UserAnswer

/**
 * Un ejercicio en la fase en la que esté (README §43.4).
 *
 * Los cinco tipos comparten columna, botón y estructura: solo cambian el
 * enunciado y si se responde eligiendo o escribiendo. La diferencia la marca el
 * modelo ([AnswerTurn]), nunca la pantalla, para que sea imposible pintar una
 * respuesta donde solo cabría elegir.
 *
 * El texto domina: el enunciado va en tamaño grande y la explicación en cuerpo
 * (§65).
 */
@Composable
fun ExerciseCard(
    prompt: PracticeExercise,
    turn: AnswerTurn,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (turn) {
            is AnswerTurn.Answering -> Answering(
                prompt = prompt,
                draft = turn.draft,
                isChecking = turn.checking,
                onOptionSelected = onOptionSelected,
                onTextChanged = onTextChanged,
                onCheck = onCheck,
            )

            is AnswerTurn.Revealed -> Revealed(
                turn = turn,
                reveal = prompt.reveal,
                isRegistering = turn.registering,
                onRate = onRate,
            )
        }
    }
}

/** Fase de respuesta: el enunciado y, debajo, la forma de contestar. */
@Composable
private fun Answering(
    prompt: PracticeExercise,
    draft: UserAnswer?,
    isChecking: Boolean,
    onOptionSelected: (String) -> Unit,
    onTextChanged: (String) -> Unit,
    onCheck: () -> Unit,
) {
    when (prompt) {
        is RecognitionExercise -> {
            Text(
                text = stringResource(R.string.practice_pick_meaning),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = prompt.prompt,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prompt.options.forEach { option ->
                    AnswerOptionRow(
                        text = option.text,
                        isSelected = draft == UserAnswer.Choice(option.id),
                        onClick = { onOptionSelected(option.id) },
                    )
                }
            }
        }

        is WrittenExercise -> {
            WrittenPrompt(prompt)
            OutlinedTextField(
                value = (draft as? UserAnswer.Text)?.value.orEmpty(),
                onValueChange = onTextChanged,
                label = { Text(text = stringResource(R.string.practice_answer_label)) },
                supportingText = { Text(text = stringResource(R.string.practice_answer_supporting)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    CheckButton(
        enabled = draft.hasContent() && !isChecking,
        isChecking = isChecking,
        onCheck = onCheck,
    )
}

/**
 * Enunciado de los ejercicios escritos.
 *
 * Cada tipo enseña algo distinto: la cloze ya trae la frase escondida, el
 * significado, la pista o el enunciado en español. Traducirlos aquí sería
 * duplicar el modelo.
 */
@Composable
private fun WrittenPrompt(prompt: WrittenExercise) {
    Text(
        text = stringResource(prompt.instructionRes()),
        style = MaterialTheme.typography.labelLarge,
    )
    when (prompt) {
        is ClozeExercise -> Text(
            text = prompt.sentence,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
        )

        is GuidedRecallExercise -> {
            Text(text = prompt.meaning, style = MaterialTheme.typography.headlineSmall)
            LabelledText(
                label = stringResource(R.string.practice_hint),
                value = prompt.hint,
            )
        }

        is TranslationExercise -> Text(
            text = prompt.prompt,
            style = MaterialTheme.typography.headlineSmall,
        )

        is ProductionExercise -> {
            Text(text = prompt.situation, style = MaterialTheme.typography.headlineSmall)
            prompt.guidance?.let { guidance ->
                Text(
                    text = guidance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Fase revelada: veredicto, respuesta natural y las cuatro calificaciones.
 *
 * La respuesta del usuario se muestra solo si la escribió; en recognition se elige
 * una opción y no hay nada que repetir.
 */
@Composable
private fun Revealed(
    turn: AnswerTurn.Revealed,
    reveal: RevealContent,
    isRegistering: Boolean,
    onRate: (ReviewRating) -> Unit,
) {
    AnswerVerdict(evaluation = turn.evaluation)
    turn.draft?.asText()?.takeIf { it.isNotBlank() }?.let { answer ->
        LabelledText(
            label = stringResource(R.string.practice_your_answer),
            value = answer,
        )
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabelledText(
                label = stringResource(R.string.practice_expected_answer),
                value = reveal.expectedAnswer,
            )
            reveal.pattern?.let {
                LabelledText(label = stringResource(R.string.practice_pattern), value = it)
            }
            reveal.explanation?.let {
                LabelledText(label = stringResource(R.string.practice_explanation), value = it)
            }
            reveal.example?.let {
                LabelledText(label = stringResource(R.string.practice_example), value = it.english)
            }
        }
    }
    RatingBar(
        suggestedRating = turn.suggestedRating,
        isRegistering = isRegistering,
        onRate = onRate,
    )
}

/**
 * Veredicto del ejercicio ya revelado.
 *
 * En producción no hay veredicto: la app no puede saber si una frase en inglés es
 * correcta, así que lo dice y deja que el usuario califique (§35). En el resto el
 * veredicto lleva icono además de color, para que no dependa solo del color (§64).
 */
@Composable
private fun AnswerVerdict(evaluation: AnswerEvaluation) {
    when (evaluation) {
        is AnswerEvaluation.Graded -> {
            val correct = evaluation.isCorrect
            Text(
                text = stringResource(
                    if (correct) R.string.practice_correct else R.string.practice_incorrect,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (correct) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = ""
                },
            )
        }

        is AnswerEvaluation.SelfAssessed -> {
            Text(
                text = stringResource(R.string.practice_self_assessed_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Las cuatro calificaciones, con la que el motor sugiere destacada.
 *
 * La sugerencia solo destaca: el usuario puede elegir otra, porque saber la
 * respuesta despacio es información que la app no tiene (§35). Se usa
 * [FlowRow] para que con tipografía grande las cuatro quepan en dos filas en vez
 * de recortarse.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingBar(
    suggestedRating: ReviewRating?,
    isRegistering: Boolean,
    onRate: (ReviewRating) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.practice_rating_question),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReviewRating.entries.forEach { rating ->
                RatingButton(
                    rating = rating,
                    isSuggested = rating == suggestedRating,
                    enabled = !isRegistering,
                    onClick = { onRate(rating) },
                )
            }
        }
        if (isRegistering) {
            BusyIndicator()
        }
    }
}

@Composable
private fun RatingButton(
    rating: ReviewRating,
    isSuggested: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = if (isSuggested) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Text(
            text = stringResource(rating.labelRes()),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .then(
                    if (isSuggested) {
                        Modifier.clearAndSetSemantics { }
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

@Composable
private fun CheckButton(enabled: Boolean, isChecking: Boolean, onCheck: () -> Unit) {
    if (isChecking) {
        BusyIndicator()
        return
    }
    Button(
        onClick = onCheck,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET),
    ) {
        Text(text = stringResource(R.string.practice_check))
    }
}

@Composable
private fun AnswerOptionRow(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOUCH_TARGET)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

private fun WrittenExercise.instructionRes(): Int = when (this) {
    is ClozeExercise -> R.string.practice_instruction_cloze
    is GuidedRecallExercise -> R.string.practice_instruction_guided_recall
    is TranslationExercise -> R.string.practice_instruction_translation
    is ProductionExercise -> R.string.practice_instruction_production
}

private fun ReviewRating.labelRes(): Int = when (this) {
    ReviewRating.FORGOT -> R.string.practice_rating_forgot
    ReviewRating.HARD -> R.string.practice_rating_hard
    ReviewRating.GOOD -> R.string.practice_rating_good
    ReviewRating.EASY -> R.string.practice_rating_easy
}

/** La opción elegida no es texto; lo escrito sí. */
private fun UserAnswer.asText(): String? = (this as? UserAnswer.Text)?.value

/**
 * Hay algo que comprobar.
 *
 * Un hueco vacío no cuenta como respuesta: se compararía contra nada y saldría
 * «incorrecto» sin que el usuario hiciera nada.
 */
private fun UserAnswer?.hasContent(): Boolean = when (this) {
    is UserAnswer.Choice -> optionId.isNotBlank()
    is UserAnswer.Text -> value.isNotBlank()
    null -> false
}

/** Suficiente para el mínimo del área táctil, y de sobra con tipografía grande. */
private val MIN_TOUCH_TARGET = 48.dp
