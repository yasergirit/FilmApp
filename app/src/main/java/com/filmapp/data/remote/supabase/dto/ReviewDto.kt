package com.filmapp.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    val id: Long? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("movie_id") val movieId: String,
    @SerialName("display_name") val displayName: String,
    val content: String,
    val rating: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)
