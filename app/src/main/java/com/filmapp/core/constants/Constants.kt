package com.filmapp.core.constants

object Constants {
    // OMDb
    const val OMDB_BASE_URL = "https://www.omdbapi.com/"
    const val OMDB_NO_POSTER = "N/A"

    // Supabase table names
    const val TABLE_PROFILES = "profiles"
    const val TABLE_WATCHLIST = "watchlist"
    const val TABLE_USER_SCORES = "leaderboard"
    const val TABLE_DAILY_QUESTIONS = "daily_questions"
    const val TABLE_REVIEWS = "reviews"
    const val TABLE_FAVORITES = "favorites"

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val FIRST_PAGE = 1
}
