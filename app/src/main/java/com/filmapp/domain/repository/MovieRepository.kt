package com.filmapp.domain.repository

import com.filmapp.core.util.Resource
import com.filmapp.domain.model.Movie
import com.filmapp.domain.model.MovieDetail
import com.filmapp.domain.model.WatchlistItem
import kotlinx.coroutines.flow.Flow

interface MovieRepository {

    fun getTrendingMovies(page: Int = 1): Flow<Resource<List<Movie>>>

    fun getTopRatedMovies(page: Int = 1): Flow<Resource<List<Movie>>>

    fun getPopularMovies(page: Int = 1): Flow<Resource<List<Movie>>>

    fun getNowPlayingMovies(page: Int = 1): Flow<Resource<List<Movie>>>

    fun getMovieDetail(movieId: String): Flow<Resource<MovieDetail>>

    fun searchMovies(query: String, page: Int = 1): Flow<Resource<List<Movie>>>

    fun searchTvShows(query: String, page: Int = 1): Flow<Resource<List<Movie>>>

    // ── Supabase Watchlist Operations ──

    fun getWatchlist(): Flow<Resource<List<WatchlistItem>>>

    suspend fun addToWatchlist(movie: MovieDetail): Resource<Unit>

    suspend fun removeFromWatchlist(movieId: String): Resource<Unit>

    suspend fun isInWatchlist(movieId: String): Boolean

    fun observeWatchlistChanges(): Flow<Unit>
}
