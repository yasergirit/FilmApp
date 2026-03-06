package com.filmapp.domain.model

data class Movie(
    val id: String,           // imdbID e.g. "tt0372784"
    val title: String,
    val year: String?,
    val posterUrl: String?,   // OMDb provides full poster URL
    val imdbRating: String? = null,
    val isInWatchlist: Boolean = false
)

data class MovieDetail(
    val id: String,           // imdbID
    val title: String,
    val overview: String,
    val posterUrl: String?,
    val year: String?,
    val rated: String?,
    val runtime: String?,
    val genre: String?,       // comma-separated
    val director: String?,
    val writer: String?,
    val actors: String?,
    val imdbRating: String?,
    val imdbVotes: String?,
    val boxOffice: String?,
    val isInWatchlist: Boolean = false
)

data class UserProfile(
    val id: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?
)

data class WatchlistItem(
    val movieId: String,
    val title: String,
    val posterPath: String?,
    val addedAt: String?
)
