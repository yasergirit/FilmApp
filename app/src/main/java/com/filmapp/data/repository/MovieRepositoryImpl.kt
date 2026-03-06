package com.filmapp.data.repository

import com.filmapp.core.util.Resource
import com.filmapp.data.mapper.toDomain
import com.filmapp.data.mapper.toWatchlistDto
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.remote.tmdb.OmdbApi
import com.filmapp.di.IoDispatcher
import com.filmapp.domain.model.Movie
import com.filmapp.domain.model.MovieDetail
import com.filmapp.domain.model.WatchlistItem
import com.filmapp.domain.repository.MovieRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central repository that merges OMDb API data with Supabase backend data.
 *
 * Because OMDb has no browse endpoints (trending, popular, etc.), we use
 * curated search keywords for each section to populate the home screen.
 */
@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val omdbApi: OmdbApi,
    private val supabaseDataSource: SupabaseDataSource,
    private val authRepository: AuthRepositoryImpl,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MovieRepository {

    companion object {
        // Curated keywords used to fill each home-screen section
        private const val TRENDING_QUERY = "2024"
        private const val TOP_RATED_QUERY = "dark knight"
        private const val POPULAR_QUERY = "avengers"
        private const val NOW_PLAYING_QUERY = "action"
    }

    private fun currentUserId(): String? {
        return if (authRepository.isDemoSession) AuthRepositoryImpl.DEMO_USER_ID
        else supabaseDataSource.getCurrentUserId()
    }

    private fun searchOmdb(query: String, page: Int): Flow<Resource<List<Movie>>> = flow {
        emit(Resource.Loading)
        try {
            val response = omdbApi.searchMovies(query, page = page)
            if (response.response == "True" && !response.search.isNullOrEmpty()) {
                val movies = response.search.map { it.toDomain() }
                emit(Resource.Success(enrichWithWatchlistStatus(movies)))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Search failed", e))
        }
    }.flowOn(ioDispatcher)

    override fun getTrendingMovies(page: Int) = searchOmdb(TRENDING_QUERY, page)
    override fun getTopRatedMovies(page: Int) = searchOmdb(TOP_RATED_QUERY, page)
    override fun getPopularMovies(page: Int) = searchOmdb(POPULAR_QUERY, page)
    override fun getNowPlayingMovies(page: Int) = searchOmdb(NOW_PLAYING_QUERY, page)

    override fun getMovieDetail(movieId: String): Flow<Resource<MovieDetail>> = flow {
        emit(Resource.Loading)
        try {
            val response = omdbApi.getMovieDetail(movieId)
            if (response.response == "True") {
                val detail = response.toDomain()

                val userId = currentUserId()
                val inWatchlist = if (userId != null && !authRepository.isDemoSession) {
                    supabaseDataSource.isInWatchlist(userId, movieId)
                } else false

                emit(Resource.Success(detail.copy(isInWatchlist = inWatchlist)))
            } else {
                emit(Resource.Error("Movie not found"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to fetch movie detail", e))
        }
    }.flowOn(ioDispatcher)

    override fun searchMovies(query: String, page: Int) = searchOmdb(query, page)

    override fun searchTvShows(query: String, page: Int): Flow<Resource<List<Movie>>> = flow {
        emit(Resource.Loading)
        try {
            val response = omdbApi.searchMovies(query, type = "series", page = page)
            if (response.response == "True" && !response.search.isNullOrEmpty()) {
                val shows = response.search.map { it.toDomain() }
                emit(Resource.Success(enrichWithWatchlistStatus(shows)))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Search failed", e))
        }
    }.flowOn(ioDispatcher)

    // ──────────── Supabase Watchlist ────────────

    override fun getWatchlist(): Flow<Resource<List<WatchlistItem>>> = flow {
        emit(Resource.Loading)
        try {
            if (authRepository.isDemoSession) {
                emit(Resource.Success(emptyList<WatchlistItem>()))
                return@flow
            }
            val userId = currentUserId()
                ?: throw IllegalStateException("User not authenticated")
            val items = supabaseDataSource.getWatchlist(userId).map { it.toDomain() }
            emit(Resource.Success(items))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to fetch watchlist", e))
        }
    }.flowOn(ioDispatcher)

    override suspend fun addToWatchlist(movie: MovieDetail): Resource<Unit> {
        if (authRepository.isDemoSession) return Resource.Success(Unit)
        return try {
            val userId = currentUserId()
                ?: return Resource.Error("User not authenticated")
            val dto = movie.toWatchlistDto(userId)
            supabaseDataSource.addToWatchlist(dto)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add to watchlist", e)
        }
    }

    override suspend fun removeFromWatchlist(movieId: String): Resource<Unit> {
        if (authRepository.isDemoSession) return Resource.Success(Unit)
        return try {
            val userId = currentUserId()
                ?: return Resource.Error("User not authenticated")
            supabaseDataSource.removeFromWatchlist(userId, movieId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to remove from watchlist", e)
        }
    }

    override suspend fun isInWatchlist(movieId: String): Boolean {
        if (authRepository.isDemoSession) return false
        val userId = currentUserId() ?: return false
        return try {
            supabaseDataSource.isInWatchlist(userId, movieId)
        } catch (_: Exception) {
            false
        }
    }

    override fun observeWatchlistChanges(): Flow<Unit> {
        if (authRepository.isDemoSession) return flow {}
        val userId = currentUserId() ?: return flow {}
        return supabaseDataSource.observeWatchlistChanges(userId).map { }
    }

    private suspend fun enrichWithWatchlistStatus(movies: List<Movie>): List<Movie> {
        if (authRepository.isDemoSession) return movies
        val userId = currentUserId() ?: return movies
        return try {
            val watchlistItems = supabaseDataSource.getWatchlist(userId)
            val watchlistMovieIds = watchlistItems.map { it.movieId }.toSet()
            movies.map { movie ->
                movie.copy(isInWatchlist = movie.id in watchlistMovieIds)
            }
        } catch (_: Exception) {
            movies
        }
    }
}
