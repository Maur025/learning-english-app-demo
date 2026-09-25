package com.example.learning.english.learning.english.app.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.learning.english.learning.english.app.R

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        Text(
            text = stringResource(R.string.home_title),
            modifier = Modifier.padding(innerPadding)
        )
    }
}
