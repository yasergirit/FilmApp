package com.filmapp.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyQuestionDto(
    val id: Long? = null,
    @SerialName("created_date") val createdDate: String,
    val question: String,
    @SerialName("option_a") val optionA: String,
    @SerialName("option_b") val optionB: String,
    @SerialName("option_c") val optionC: String,
    @SerialName("option_d") val optionD: String,
    @SerialName("right_answer") val rightAnswer: String
)
