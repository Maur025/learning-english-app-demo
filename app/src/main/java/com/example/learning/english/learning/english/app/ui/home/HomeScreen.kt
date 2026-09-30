package com.example.learning.english.learning.english.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.model.SkillAverages

/**
 * Home responde a una pregunta: ¿qué hago ahora?
 *
 * Por eso el orden es fijo y sin adornos: objetivo del día, contadores, los dos
 * ejes de progreso y la acción principal. Las acciones secundarias solo aparecen
 * cuando tienen algo que ofrecer (README §43.2).
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onContinueSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            if (!uiState.isContentReady) {
                ContentLoading(modifier = Modifier.fillMaxWidth())
                return@Column
            }

            DailyGoalCard(uiState = uiState, modifier = Modifier.fillMaxWidth())
            CountersCard(uiState = uiState, modifier = Modifier.fillMaxWidth())
            SkillsCard(uiState.skillAverages, modifier = Modifier.fillMaxWidth())

            if (uiState.isStartingSession) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = onStartSession,
                    enabled = uiState.canStartSession,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.home_start_session))
                }
            }

            if (!uiState.canStartSession && !uiState.isStartingSession) {
                Text(
                    text = stringResource(R.string.home_nothing_pending),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (uiState.hasInProgressSession) {
                ContinueSessionAction(
                    answeredCount = uiState.answeredCount,
                    onClick = onContinueSession,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(uiState: HomeUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = stringResource(R.string.home_todays_goal), style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(
                    R.plurals.home_daily_goal_minutes,
                    uiState.dailyGoalMinutes,
                    uiState.dailyGoalMinutes,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CountersCard(uiState: HomeUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            CounterRow(
                label = stringResource(R.string.home_due_reviews),
                count = uiState.dueCount,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            CounterRow(
                label = stringResource(R.string.home_new_expressions),
                count = uiState.newCount,
            )
        }
    }
}

@Composable
private fun CounterRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SkillsCard(averages: SkillAverages, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SkillRow(
                label = stringResource(R.string.home_recognition),
                percent = averages.recognitionPercent,
            )
            SkillRow(
                label = stringResource(R.string.home_production),
                percent = averages.productionPercent,
            )
        }
    }
}

@Composable
private fun SkillRow(label: String, percent: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = percent?.let { "$it%" } ?: "—", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ContinueSessionAction(
    answeredCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = answeredCount
                ?.let { stringResource(R.string.home_continue_session_progress, it) }
                ?: stringResource(R.string.home_continue_session),
        )
    }
}

@Composable
private fun ContentLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.home_preparing_content),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
