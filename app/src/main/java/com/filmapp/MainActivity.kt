package com.filmapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.filmapp.navigation.NavGraph
import com.filmapp.presentation.theme.DarkBackground
import com.filmapp.presentation.theme.FilmAppTheme
import com.filmapp.presentation.theme.LightBackground
import com.filmapp.presentation.theme.ThemeManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeManager = remember { ThemeManager(this@MainActivity) }
            val isDark by themeManager.isDarkTheme.collectAsState(initial = true)

            FilmAppTheme(isDarkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDark) DarkBackground else LightBackground
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
