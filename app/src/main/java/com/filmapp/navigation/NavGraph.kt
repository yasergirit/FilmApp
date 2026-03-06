package com.filmapp.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.filmapp.presentation.auth.AuthScreen
import com.filmapp.presentation.detail.DetailScreen
import com.filmapp.presentation.home.HomeScreen
import com.filmapp.presentation.quiz.QuizScreen
import com.filmapp.presentation.recommendations.RecommendationsScreen
import com.filmapp.presentation.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
                onSignOut = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onQuizClick = {
                    navController.navigate(Screen.Quiz.route)
                },
                onRecommendationsClick = {
                    navController.navigate(Screen.Recommendations.route)
                }
            )
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut()
            }
        ) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Quiz.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut()
            }
        ) {
            QuizScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Recommendations.route,
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut()
            }
        ) {
            RecommendationsScreen(
                onBack = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("movieId") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(350)) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(350)) + fadeOut()
            }
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
