package com.filmapp.navigation

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object Quiz : Screen("quiz")
    data object Recommendations : Screen("recommendations")
    data object Detail : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
}

enum class BottomTab(val label: String) {
    Movies("Movies"),
    TvShows("TV Shows"),
    Recommendations("Öneriler"),
    Quiz("Quiz")
}
