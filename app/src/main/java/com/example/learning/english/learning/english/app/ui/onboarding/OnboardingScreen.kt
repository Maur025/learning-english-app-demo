package com.example.learning.english.learning.english.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.learning.english.learning.english.app.R
import com.example.learning.english.learning.english.app.domain.model.PackId

/**
 * Primera pantalla: cómo se practica, cuánto tiempo al día y con qué contenido.
 *
 * Es una sola pantalla y no un asistente por pasos: son tres decisiones cortas y
 * un botón. Un wizard obligaría a más toques para la misma información (README
 * §43.1, §86).
 */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onDailyGoalSelected: (Int) -> Unit,
    onPackToggled: (PackId) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        if (uiState.isContentReady) {
            OnboardingContent(
                uiState = uiState,
                onDailyGoalSelected = onDailyGoalSelected,
                onPackToggled = onPackToggled,
                onStart = onStart,
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LoadingContent(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onDailyGoalSelected: (Int) -> Unit,
    onPackToggled: (PackId) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.onboarding_approach),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.onboarding_daily_goal),
                style = MaterialTheme.typography.titleMedium,
            )
            DailyGoalOptions(
                selectedMinutes = uiState.dailyGoalMinutes,
                isEditable = uiState.isEditable,
                onSelected = onDailyGoalSelected,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.onboarding_content),
                style = MaterialTheme.typography.titleMedium,
            )
            PackOptions(
                uiState = uiState,
                onPackToggled = onPackToggled,
            )
        }

        Button(
            onClick = onStart,
            enabled = uiState.canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_start))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyGoalOptions(
    selectedMinutes: Int,
    isEditable: Boolean,
    onSelected: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OnboardingUiState.DAILY_GOAL_OPTIONS.forEach { minutes ->
            FilterChip(
                selected = minutes == selectedMinutes,
                onClick = { onSelected(minutes) },
                enabled = isEditable,
                label = { Text(text = pluralStringResource(R.plurals.onboarding_minutes, minutes, minutes)) },
            )
        }
    }
}

@Composable
private fun PackOptions(
    uiState: OnboardingUiState,
    onPackToggled: (PackId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uiState.packs.forEach { pack ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = pack.id in uiState.selectedPackIds,
                        onCheckedChange = { onPackToggled(pack.id) },
                        enabled = uiState.isEditable,
                    )
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(text = pack.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = pluralStringResource(
                                R.plurals.onboarding_expression_count,
                                pack.expressionCount,
                                pack.expressionCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.onboarding_preparing_content),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
