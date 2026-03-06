package com.filmapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.core.util.Resource
import com.filmapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        if (authRepository.isLoggedIn()) {
            _state.update { it.copy(isSuccess = true) }
        }
    }

    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }

    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun onDisplayNameChange(name: String) {
        _state.update { it.copy(displayName = name, error = null) }
    }

    fun toggleMode() {
        _state.update { it.copy(isSignUp = !it.isSignUp, error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = "Email and password are required") }
            return
        }
        if (current.isSignUp && current.displayName.isBlank()) {
            _state.update { it.copy(error = "Display name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = if (current.isSignUp) {
                authRepository.signUp(current.email, current.password, current.displayName)
            } else {
                authRepository.signIn(current.email, current.password)
            }

            when (result) {
                is Resource.Success -> _state.update { it.copy(isLoading = false, isSuccess = true) }
                is Resource.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
                is Resource.Loading -> { /* handled above */ }
            }
        }
    }
}
