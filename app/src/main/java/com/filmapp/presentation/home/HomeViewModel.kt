package com.filmapp.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.core.util.Resource
import com.filmapp.domain.model.Movie
import com.filmapp.domain.repository.AuthRepository
import com.filmapp.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private val MOVIE_QUERIES = listOf(
            "the", "love", "man", "night", "war", "world", "dark",
            "king", "star", "dead", "last", "black", "fire", "life",
            "day", "time", "back", "girl", "city", "heart", "death",
            "blood", "house", "game", "red", "blue", "lost", "dream",
            "shadow", "secret", "high", "wild", "bad", "good"
        )
        private val TV_QUERIES = listOf(
            "the", "love", "night", "world", "life", "war", "game",
            "star", "man", "dark", "king", "dead", "last", "black",
            "house", "city", "day", "power", "time", "bad", "good",
            "big", "real", "secret", "family", "girl", "fire", "heart",
            "new", "doctor", "law", "high", "wild"
        )
    }

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // Movie pagination tracking
    private var movieQueryIdx = 0
    private var moviePage = 0
    private val movieSeenIds = mutableSetOf<String>()

    // TV Show pagination tracking
    private var tvQueryIdx = 0
    private var tvPage = 0
    private val tvSeenIds = mutableSetOf<String>()

    // Search debounce
    private var searchJob: Job? = null

    init {
        loadMovies()
    }

    fun loadMovies() {
        if (_state.value.movies.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isMoviesLoading = true) }
            val movies = fetchNextMovieBatch()
            _state.update {
                it.copy(
                    movies = movies,
                    isMoviesLoading = false,
                    hasMoreMovies = movies.isNotEmpty()
                )
            }
        }
    }

    fun loadMoreMovies() {
        val current = _state.value
        if (current.isLoadingMoreMovies || !current.hasMoreMovies) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreMovies = true) }
            val newMovies = fetchNextMovieBatch()
            _state.update {
                it.copy(
                    movies = it.movies + newMovies,
                    isLoadingMoreMovies = false,
                    hasMoreMovies = newMovies.isNotEmpty()
                )
            }
        }
    }

    fun loadTvShows() {
        if (_state.value.tvShows.isNotEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isTvShowsLoading = true) }
            val shows = fetchNextTvBatch()
            _state.update {
                it.copy(
                    tvShows = shows,
                    isTvShowsLoading = false,
                    hasMoreTvShows = shows.isNotEmpty()
                )
            }
        }
    }

    fun loadMoreTvShows() {
        val current = _state.value
        if (current.isLoadingMoreTvShows || !current.hasMoreTvShows) return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMoreTvShows = true) }
            val newShows = fetchNextTvBatch()
            _state.update {
                it.copy(
                    tvShows = it.tvShows + newShows,
                    isLoadingMoreTvShows = false,
                    hasMoreTvShows = newShows.isNotEmpty()
                )
            }
        }
    }

    // Fetches next batch of unique movies — 3 pages in parallel
    private suspend fun fetchNextMovieBatch(): List<Movie> {
        val allUnique = mutableListOf<Movie>()
        var rounds = 0
        while (movieQueryIdx < MOVIE_QUERIES.size && rounds < 3) {
            rounds++
            val query = MOVIE_QUERIES[movieQueryIdx]
            val pages = (1..3).map { moviePage + it }
            moviePage += 3

            val results = pages.map { page ->
                viewModelScope.async { searchMoviesDirect(query, page) }
            }.awaitAll().flatten()

            if (results.isEmpty()) {
                movieQueryIdx++
                moviePage = 0
                continue
            }
            allUnique.addAll(results.filter { movie -> movieSeenIds.add(movie.id) })
            if (allUnique.isNotEmpty()) return allUnique
        }
        return allUnique
    }

    // Fetches next batch of unique TV shows — 3 pages in parallel
    private suspend fun fetchNextTvBatch(): List<Movie> {
        val allUnique = mutableListOf<Movie>()
        var rounds = 0
        while (tvQueryIdx < TV_QUERIES.size && rounds < 3) {
            rounds++
            val query = TV_QUERIES[tvQueryIdx]
            val pages = (1..3).map { tvPage + it }
            tvPage += 3

            val results = pages.map { page ->
                viewModelScope.async { searchTvShowsDirect(query, page) }
            }.awaitAll().flatten()

            if (results.isEmpty()) {
                tvQueryIdx++
                tvPage = 0
                continue
            }
            allUnique.addAll(results.filter { show -> tvSeenIds.add(show.id) })
            if (allUnique.isNotEmpty()) return allUnique
        }
        return allUnique
    }

    private suspend fun searchMoviesDirect(query: String, page: Int): List<Movie> {
        var result = emptyList<Movie>()
        movieRepository.searchMovies(query, page = page)
            .collect { resource ->
                if (resource is Resource.Success) result = resource.data
            }
        return result
    }

    private suspend fun searchTvShowsDirect(query: String, page: Int): List<Movie> {
        var result = emptyList<Movie>()
        movieRepository.searchTvShows(query, page = page)
            .collect { resource ->
                if (resource is Resource.Success) result = resource.data
            }
        return result
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onComplete()
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(isSearching = true) }
            val results = searchMoviesDirect(query, 1)
            _state.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }
}
