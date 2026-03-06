package com.filmapp.presentation.auth

data class AuthState(
    val isLoading: Boolean = false,
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val error: String? = null,
    val isSuccess: Boolean = false
)
