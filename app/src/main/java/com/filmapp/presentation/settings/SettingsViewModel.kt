package com.filmapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.data.local.UserPreferencesManager
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.remote.supabase.dto.ProfileDto
import com.filmapp.data.repository.AuthRepositoryImpl
import com.filmapp.domain.model.WatchlistItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val displayName: String = "",
    val isEditing: Boolean = false,
    val editText: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val favoriteMovies: List<WatchlistItem> = emptyList(),
    val isFavoritesLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager,
    private val supabaseDataSource: SupabaseDataSource,
    private val authRepository: AuthRepositoryImpl
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesManager.displayName.collect { name ->
                _state.update { it.copy(displayName = name) }
            }
        }
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            try {
                if (authRepository.isDemoSession) {
                    val localFavs = userPreferencesManager.getLocalFavorites()
                    val items = localFavs.map { fav ->
                        WatchlistItem(
                            movieId = fav.movieId,
                            title = fav.title,
                            posterPath = fav.posterPath,
                            addedAt = null
                        )
                    }
                    _state.update { it.copy(favoriteMovies = items, isFavoritesLoading = false) }
                } else {
                    val userId = supabaseDataSource.getCurrentUserId()
                    if (userId != null) {
                        val items = supabaseDataSource.getFavorites(userId).map { dto ->
                            WatchlistItem(
                                movieId = dto.movieId,
                                title = dto.title,
                                posterPath = dto.posterPath,
                                addedAt = dto.addedAt
                            )
                        }
                        _state.update { it.copy(favoriteMovies = items, isFavoritesLoading = false) }
                    } else {
                        _state.update { it.copy(isFavoritesLoading = false) }
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isFavoritesLoading = false) }
            }
        }
    }

    fun removeFavorite(movieId: String) {
        viewModelScope.launch {
            try {
                if (authRepository.isDemoSession) {
                    userPreferencesManager.removeLocalFavorite(movieId)
                } else {
                    val userId = supabaseDataSource.getCurrentUserId()
                    if (userId != null) {
                        supabaseDataSource.removeFromFavorites(userId, movieId)
                    }
                }
                _state.update { current ->
                    current.copy(favoriteMovies = current.favoriteMovies.filter { it.movieId != movieId })
                }
            } catch (_: Exception) { }
        }
    }

    fun startEditing() {
        _state.update { it.copy(isEditing = true, editText = it.displayName, saveSuccess = false) }
    }

    fun cancelEditing() {
        _state.update { it.copy(isEditing = false, editText = "") }
    }

    fun onEditTextChange(text: String) {
        _state.update { it.copy(editText = text) }
    }

    fun saveDisplayName() {
        val newName = _state.value.editText.trim()
        if (newName.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                val userId = supabaseDataSource.getCurrentUserId()
                if (userId != null) {
                    supabaseDataSource.updateProfile(ProfileDto(id = userId, displayName = newName))
                }
                userPreferencesManager.setDisplayName(newName)
                _state.update { it.copy(displayName = newName, isEditing = false, isSaving = false, saveSuccess = true) }
            } catch (_: Exception) {
                // Still save locally even if Supabase fails
                userPreferencesManager.setDisplayName(newName)
                _state.update { it.copy(displayName = newName, isEditing = false, isSaving = false, saveSuccess = true) }
            }
        }
    }
}
