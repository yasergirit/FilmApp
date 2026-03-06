package com.filmapp.presentation.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.data.local.CachedRecommendation
import com.filmapp.data.local.UserPreferencesManager
import com.filmapp.data.remote.gemini.GeminiService
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.repository.AuthRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RecommendationsState(
    val recommendations: List<GeminiService.MovieRecommendation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val hasFavorites: Boolean = true,
    val isCached: Boolean = false
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val supabaseDataSource: SupabaseDataSource,
    private val authRepository: AuthRepositoryImpl,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(RecommendationsState())
    val state: StateFlow<RecommendationsState> = _state.asStateFlow()

    init {
        loadRecommendations()
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val today = LocalDate.now().toString()
                val cachedDate = userPreferencesManager.getRecommendationsDate()
                val cachedRecs = userPreferencesManager.getCachedRecommendations()

                // If same day and cache exists, use cached recommendations
                if (cachedDate == today && cachedRecs.isNotEmpty()) {
                    _state.update {
                        it.copy(
                            recommendations = cachedRecs.map { rec ->
                                GeminiService.MovieRecommendation(
                                    title = rec.title, year = rec.year,
                                    reason = rec.reason, genre = rec.genre, imdbId = rec.imdbId
                                )
                            },
                            isLoading = false,
                            hasFavorites = true,
                            isCached = true
                        )
                    }
                    return@launch
                }

                // Fetch favorites
                val movieTitles: List<String>
                if (authRepository.isDemoSession) {
                    val localFavs = userPreferencesManager.getLocalFavorites()
                    if (localFavs.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasFavorites = false,
                                error = "Öneri alabilmek için önce birkaç filmi favorilere ekleyin."
                            )
                        }
                        return@launch
                    }
                    movieTitles = localFavs.map { it.title }
                } else {
                    val userId = supabaseDataSource.getCurrentUserId()
                    if (userId == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasFavorites = false,
                                error = "Öneri alabilmek için giriş yapmalısınız."
                            )
                        }
                        return@launch
                    }
                    val watchlist = supabaseDataSource.getFavorites(userId)
                    if (watchlist.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                hasFavorites = false,
                                error = "Öneri alabilmek için önce birkaç filmi favorilere ekleyin."
                            )
                        }
                        return@launch
                    }
                    movieTitles = watchlist.map { it.title }
                }

                // Call Gemini (once per day)
                val recommendations = geminiService.getRecommendations(movieTitles)

                // Cache recommendations with today's date
                userPreferencesManager.saveRecommendations(
                    recs = recommendations.map { rec ->
                        CachedRecommendation(
                            title = rec.title, year = rec.year,
                            reason = rec.reason, genre = rec.genre, imdbId = rec.imdbId
                        )
                    },
                    date = today
                )

                _state.update {
                    it.copy(
                        recommendations = recommendations,
                        isLoading = false,
                        hasFavorites = true,
                        isCached = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Öneriler yüklenirken hata oluştu. Tekrar deneyin."
                    )
                }
            }
        }
    }
}
