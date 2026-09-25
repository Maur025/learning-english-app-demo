package com.example.learning.english.learning.english.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.learning.english.learning.english.app.ui.navigation.AppNavHost
import com.example.learning.english.learning.english.app.ui.theme.LearningEnglishAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LearningEnglishAppTheme {
                AppNavHost()
            }
        }
    }
}
