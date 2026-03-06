package com.filmapp.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.core.util.Resource
import com.filmapp.data.local.LocalFavorite
import com.filmapp.data.local.UserPreferencesManager
import com.filmapp.data.remote.gemini.GeminiService
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.remote.supabase.dto.FavoriteItemDto
import com.filmapp.data.remote.supabase.dto.ReviewDto
import com.filmapp.data.repository.AuthRepositoryImpl
import com.filmapp.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val geminiService: GeminiService,
    private val supabaseDataSource: SupabaseDataSource,
    private val authRepository: AuthRepositoryImpl,
    private val userPreferencesManager: UserPreferencesManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    init {
        loadMovie()
        loadUserDisplayName()
        loadFavoriteStatus()
    }

    private fun loadUserDisplayName() {
        viewModelScope.launch {
            userPreferencesManager.displayName.collect { name ->
                _state.update { it.copy(userDisplayName = name) }
            }
        }
    }

    private fun loadMovie() {
        viewModelScope.launch {
            movieRepository.getMovieDetail(movieId)
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> _state.update { it.copy(isLoading = true) }
                        is Resource.Success -> {
                            _state.update {
                                it.copy(movie = result.data, isLoading = false, error = null)
                            }
                            result.data?.let { movie ->
                                loadAiReview(movie)
                                loadReviews(movie.id)
                            }
                        }
                        is Resource.Error -> _state.update {
                            it.copy(isLoading = false, error = result.message)
                        }
                    }
                }
        }
    }

    private fun loadAiReview(movie: com.filmapp.domain.model.MovieDetail) {
        viewModelScope.launch {
            _state.update { it.copy(isReviewLoading = true) }
            val review = geminiService.getFilmReview(
                title = movie.title,
                year = movie.year,
                genre = movie.genre,
                director = movie.director,
                imdbRating = movie.imdbRating,
                overview = movie.overview
            )
            _state.update { it.copy(aiReview = review, isReviewLoading = false) }
        }
    }

    fun toggleWatchlist() {
        val movie = _state.value.movie ?: return

        viewModelScope.launch {
            _state.update { it.copy(isWatchlistLoading = true) }

            val result = if (movie.isInWatchlist) {
                movieRepository.removeFromWatchlist(movie.id)
            } else {
                movieRepository.addToWatchlist(movie)
            }

            when (result) {
                is Resource.Success -> {
                    _state.update {
                        it.copy(
                            movie = movie.copy(isInWatchlist = !movie.isInWatchlist),
                            isWatchlistLoading = false
                        )
                    }
                }
                is Resource.Error -> {
                    _state.update { it.copy(isWatchlistLoading = false, error = result.message) }
                }
                is Resource.Loading -> { /* no-op */ }
            }
        }
    }

    private fun currentUserId(): String? {
        return if (authRepository.isDemoSession) AuthRepositoryImpl.DEMO_USER_ID
        else supabaseDataSource.getCurrentUserId()
    }

    private fun loadFavoriteStatus() {
        viewModelScope.launch {
            try {
                if (authRepository.isDemoSession) {
                    val isFav = userPreferencesManager.isLocalFavorite(movieId)
                    _state.update { it.copy(isFavorite = isFav) }
                } else {
                    val userId = supabaseDataSource.getCurrentUserId() ?: return@launch
                    val isFav = supabaseDataSource.isFavorite(userId, movieId)
                    _state.update { it.copy(isFavorite = isFav) }
                }
            } catch (_: Exception) { }
        }
    }

    fun toggleFavorite() {
        val movie = _state.value.movie ?: return
        val currentFav = _state.value.isFavorite

        viewModelScope.launch {
            _state.update { it.copy(isFavoriteLoading = true) }
            try {
                if (authRepository.isDemoSession) {
                    // Demo mode: save locally
                    if (currentFav) {
                        userPreferencesManager.removeLocalFavorite(movie.id)
                    } else {
                        userPreferencesManager.addLocalFavorite(
                            LocalFavorite(
                                movieId = movie.id,
                                title = movie.title,
                                posterPath = movie.posterUrl
                            )
                        )
                    }
                    _state.update { it.copy(isFavorite = !currentFav, isFavoriteLoading = false) }
                } else {
                    val userId = supabaseDataSource.getCurrentUserId()
                    if (userId == null) {
                        _state.update { it.copy(isFavoriteLoading = false) }
                        return@launch
                    }
                    if (currentFav) {
                        supabaseDataSource.removeFromFavorites(userId, movie.id)
                    } else {
                        supabaseDataSource.addToFavorites(
                            FavoriteItemDto(
                                userId = userId,
                                movieId = movie.id,
                                title = movie.title,
                                posterPath = movie.posterUrl
                            )
                        )
                    }
                    _state.update { it.copy(isFavorite = !currentFav, isFavoriteLoading = false) }
                }
            } catch (e: Exception) {
                android.util.Log.e("DetailVM", "toggleFavorite FAILED: ${e.message}", e)
                _state.update { it.copy(isFavoriteLoading = false) }
            }
        }
    }

    private fun loadReviews(movieId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isReviewsLoading = true) }
            try {
                val reviews = supabaseDataSource.getReviews(movieId)
                _state.update { it.copy(reviews = reviews, isReviewsLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isReviewsLoading = false) }
            }
        }
    }

    fun submitReview(displayName: String, content: String) {
        val movie = _state.value.movie ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReview = true) }
            try {
                val review = ReviewDto(
                    movieId = movie.id,
                    displayName = displayName,
                    content = content.trim()
                )
                supabaseDataSource.addReview(review)
                _state.update { it.copy(isSubmittingReview = false, reviewSubmitted = true) }
                loadReviews(movie.id)
            } catch (e: Exception) {
                _state.update { it.copy(isSubmittingReview = false) }
            }
        }
    }

    fun resetReviewSubmitted() {
        _state.update { it.copy(reviewSubmitted = false) }
    }
}
