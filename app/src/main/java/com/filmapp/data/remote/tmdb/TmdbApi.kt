package com.filmapp.data.remote.tmdb

import com.filmapp.data.remote.tmdb.dto.OmdbDetailResponse
import com.filmapp.data.remote.tmdb.dto.OmdbSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OMDb API — kept in tmdb package to avoid renaming the whole directory tree.
 */
interface OmdbApi {

    /** Search movies by keyword. Returns up to 10 results per page. */
    @GET("/")
    suspend fun searchMovies(
        @Query("s") query: String,
        @Query("type") type: String = "movie",
        @Query("page") page: Int = 1
    ): OmdbSearchResponse

    /** Get full movie detail by IMDb ID (e.g. "tt0372784"). */
    @GET("/")
    suspend fun getMovieDetail(
        @Query("i") imdbId: String,
        @Query("plot") plot: String = "full"
    ): OmdbDetailResponse
}
