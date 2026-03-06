package com.filmapp.presentation.detail

import com.filmapp.data.remote.supabase.dto.ReviewDto
import com.filmapp.domain.model.MovieDetail

data class DetailState(
    val movie: MovieDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isWatchlistLoading: Boolean = false,
    val isFavorite: Boolean = false,
    val isFavoriteLoading: Boolean = false,
    val aiReview: String? = null,
    val isReviewLoading: Boolean = false,
    val reviews: List<ReviewDto> = emptyList(),
    val isReviewsLoading: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitted: Boolean = false,
    val userDisplayName: String = ""
)
