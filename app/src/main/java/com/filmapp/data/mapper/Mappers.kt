package com.filmapp.data.mapper

import com.filmapp.core.constants.Constants
import com.filmapp.data.remote.supabase.dto.ProfileDto
import com.filmapp.data.remote.supabase.dto.WatchlistItemDto
import com.filmapp.data.remote.tmdb.dto.OmdbDetailResponse
import com.filmapp.data.remote.tmdb.dto.OmdbSearchItem
import com.filmapp.domain.model.*

fun OmdbSearchItem.toDomain(): Movie = Movie(
    id = imdbID,
    title = title,
    year = year,
    posterUrl = poster?.takeIf { it != Constants.OMDB_NO_POSTER }
)

fun OmdbDetailResponse.toDomain(): MovieDetail = MovieDetail(
    id = imdbID.orEmpty(),
    title = title.orEmpty(),
    overview = plot.orEmpty(),
    posterUrl = poster?.takeIf { it != Constants.OMDB_NO_POSTER },
    year = year,
    rated = rated,
    runtime = runtime,
    genre = genre,
    director = director,
    writer = writer,
    actors = actors,
    imdbRating = imdbRating,
    imdbVotes = imdbVotes,
    boxOffice = boxOffice
)

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl
)

fun WatchlistItemDto.toDomain(): WatchlistItem = WatchlistItem(
    movieId = movieId,
    title = title,
    posterPath = posterPath,
    addedAt = addedAt
)

fun MovieDetail.toWatchlistDto(userId: String): WatchlistItemDto = WatchlistItemDto(
    userId = userId,
    movieId = id,
    title = title,
    posterPath = posterUrl
)
