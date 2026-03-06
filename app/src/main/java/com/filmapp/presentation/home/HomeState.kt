package com.filmapp.presentation.home

import com.filmapp.domain.model.Movie

data class HomeState(
    val movies: List<Movie> = emptyList(),
    val tvShows: List<Movie> = emptyList(),
    val isMoviesLoading: Boolean = true,
    val isTvShowsLoading: Boolean = false,
    val isLoadingMoreMovies: Boolean = false,
    val isLoadingMoreTvShows: Boolean = false,
    val hasMoreMovies: Boolean = true,
    val hasMoreTvShows: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)
