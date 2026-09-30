package com.example.learning.english.learning.english.app.ui.practice

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.learning.english.learning.english.app.domain.exercise.AnswerEvaluation
import com.example.learning.english.learning.english.app.domain.exercise.AnswerOption
import com.example.learning.english.learning.english.app.domain.exercise.ClozeExercise
import com.example.learning.english.learning.english.app.domain.exercise.MatchKind
import com.example.learning.english.learning.english.app.domain.exercise.ProductionExercise
import com.example.learning.english.learning.english.app.domain.exercise.RecognitionExercise
import com.example.learning.english.learning.english.app.domain.exercise.RevealContent
import com.example.learning.english.learning.english.app.domain.exercise.TranslationExercise
import com.example.learning.english.learning.english.app.domain.model.Exercise
import com.example.learning.english.learning.english.app.domain.model.ExpressionId
import com.example.learning.english.learning.english.app.domain.model.ReviewRating
import com.example.learning.english.learning.english.app.domain.model.ReviewType
import com.example.learning.english.learning.english.app.domain.model.SessionId
import com.example.learning.english.learning.english.app.domain.model.UserAnswer
import com.example.learning.english.learning.english.app.ui.theme.LearningEnglishAppTheme

/**
 * Vistas previas de los cinco tipos de ejercicio y de las dos fases.
 *
 * Están aparte de la pantalla para que los datos de prueba no compiten con el
 * código que dibuja: aquí importa ver el resultado, no mantenerlo.
 *
 * Los datos salen del mismo generador que usa el motor, así que una vista previa
 * no puede enseñar una forma de ejercicio que la app nunca generaría.
 */
private val EXPRESSION = ExpressionId("developer-english/figure-out")
private val SESSION = SessionId("preview")

@Preview(showBackground = true, name = "Recognition · answering")
@Composable
private fun RecognitionAnsweringPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.RECOGNITION,
                prompt = RecognitionExercise(
                    expressionId = EXPRESSION,
                    prompt = "figure out",
                    options = listOf(
                        AnswerOption("a", "to understand something difficult"),
                        AnswerOption("b", "to cancel a plan"),
                        AnswerOption("c", "to run away"),
                    ),
                    correctOptionId = "a",
                    reveal = RevealContent(
                        expectedAnswers = listOf("to understand something difficult"),
                        pattern = "figure out + noun / to figure out that…",
                    ),
                ),
                turn = AnswerTurn.Answering(draft = UserAnswer.Choice("a")),
                introduction = null,
                answered = 2,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, name = "Recognition · revealed correct")
@Composable
private fun RecognitionRevealedPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.RECOGNITION,
                prompt = RecognitionExercise(
                    expressionId = EXPRESSION,
                    prompt = "figure out",
                    options = listOf(
                        AnswerOption("a", "to understand something difficult"),
                        AnswerOption("b", "to cancel a plan"),
                    ),
                    correctOptionId = "a",
                    reveal = RevealContent(expectedAnswers = listOf("to understand something difficult")),
                ),
                turn = AnswerTurn.Revealed(
                    evaluation = AnswerEvaluation.Graded(
                        expectedAnswers = listOf("to understand something difficult"),
                        match = MatchKind.EXACT,
                        normalizedAnswer = null,
                    ),
                    draft = UserAnswer.Choice("a"),
                    suggestedRating = ReviewRating.GOOD,
                ),
                introduction = null,
                answered = 2,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, name = "Cloze · revealed incorrect")
@Composable
private fun ClozeRevealedPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.CLOZE,
                prompt = ClozeExercise(
                    expressionId = EXPRESSION,
                    sentence = "I need to ______ why the build is failing.",
                    hiddenAnswer = "figure out",
                    reveal = RevealContent(
                        expectedAnswers = listOf("figure out"),
                        explanation = "Investigar algo con el fin de entenderlo.",
                    ),
                ),
                turn = AnswerTurn.Revealed(
                    evaluation = AnswerEvaluation.Graded(
                        expectedAnswers = listOf("figure out"),
                        match = MatchKind.INCORRECT,
                        normalizedAnswer = "find out",
                    ),
                    draft = UserAnswer.Text("find out"),
                    suggestedRating = ReviewRating.FORGOT,
                ),
                introduction = null,
                answered = 3,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, name = "Translation · answering")
@Composable
private fun TranslationAnsweringPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.TRANSLATION,
                prompt = TranslationExercise(
                    expressionId = EXPRESSION,
                    prompt = "Hay que averiguar por qué falla la compilación.",
                    reveal = RevealContent(expectedAnswers = listOf("figure out why the build fails")),
                ),
                turn = AnswerTurn.Answering(draft = UserAnswer.Text("You have to ")),
                introduction = null,
                answered = 5,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

/**
 * En producción la vista previa tiene que mostrar lo que verá el usuario: no hay
 * veredicto y no hay calificación sugerida, solo el aviso (§35).
 */
@Preview(showBackground = true, name = "Production · self rated")
@Composable
private fun ProductionRevealedPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.PRODUCTION,
                prompt = ProductionExercise(
                    expressionId = EXPRESSION,
                    situation = "Un compañero pregunta qué significa un error del build.",
                    guidance = "Responde como se lo explicarías a unJunior.",
                    reveal = RevealContent(
                        expectedAnswers = listOf("Let me figure out what's going on."),
                        example = null,
                    ),
                ),
                turn = AnswerTurn.Revealed(
                    evaluation = AnswerEvaluation.SelfAssessed(
                        expectedAnswers = listOf("Let me figure out what's going on."),
                        normalizedAnswer = "i should find out what happened",
                    ),
                    draft = UserAnswer.Text("i should find out what happened"),
                    suggestedRating = null,
                ),
                introduction = null,
                answered = 8,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

@Preview(showBackground = true, name = "New expression · introduction")
@Composable
private fun IntroductionPreview() {
    LearningEnglishAppTheme {
        PracticeScreen(
            uiState = active(
                reviewType = ReviewType.RECOGNITION,
                prompt = RecognitionExercise(
                    expressionId = EXPRESSION,
                    prompt = "figure out",
                    options = listOf(
                        AnswerOption("a", "to understand something difficult"),
                        AnswerOption("b", "to cancel a plan"),
                    ),
                    correctOptionId = "a",
                    reveal = RevealContent(expectedAnswers = listOf("to understand something difficult")),
                ),
                turn = AnswerTurn.Answering(draft = null),
                introduction = ExpressionIntroUiModel(
                    phrase = "figure out",
                    meaning = "entender algo difícil, averiguar una causa",
                    explanation = "Muy común en el inglés del trabajo técnico.",
                    pattern = "figure out + noun / to figure out that…",
                    examples = listOf("I need to figure out why it crashes."),
                ),
                answered = 0,
            ),
            onIntroductionSeen = {},
            onOptionSelected = {},
            onTextChanged = {},
            onCheck = {},
            onRate = {},
            onFinish = {},
        )
    }
}

private fun active(
    reviewType: ReviewType,
    prompt: com.example.learning.english.learning.english.app.domain.exercise.PracticeExercise,
    turn: AnswerTurn,
    introduction: ExpressionIntroUiModel?,
    answered: Int,
) = PracticeUiState.Active(
    exercise = Exercise(
        sessionId = SESSION,
        position = answered,
        expressionId = EXPRESSION,
        reviewType = reviewType,
    ),
    prompt = prompt,
    progress = SessionProgress(answered = answered, total = 10),
    turn = turn,
    introduction = introduction,
)
