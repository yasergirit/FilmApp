package com.filmapp.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserScoreDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("weekly_score") val weeklyScore: Int,
    @SerialName("latest_updated") val latestUpdated: String? = null
)
