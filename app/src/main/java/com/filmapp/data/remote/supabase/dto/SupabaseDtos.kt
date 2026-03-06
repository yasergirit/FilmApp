package com.filmapp.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class FavoriteItemDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("movie_id") val movieId: String,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("added_at") val addedAt: String? = null
)

@Serializable
data class WatchlistItemDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("movie_id") val movieId: String,
    val title: String,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("added_at") val addedAt: String? = null
)
