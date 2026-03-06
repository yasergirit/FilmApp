package com.filmapp.data.remote.supabase

import com.filmapp.core.constants.Constants
import com.filmapp.data.remote.supabase.dto.FavoriteItemDto
import com.filmapp.data.remote.supabase.dto.ProfileDto
import com.filmapp.data.remote.supabase.dto.WatchlistItemDto
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val client: SupabaseClient
) {

    // ──────────────── Auth ────────────────

    suspend fun signUp(email: String, password: String, displayName: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("display_name", displayName)
            }
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): UserInfo? = auth.currentUserOrNull()

    fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

    // ──────────────── Profile ────────────────

    suspend fun getProfile(userId: String): ProfileDto {
        return postgrest.from(Constants.TABLE_PROFILES)
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileDto>()
    }

    suspend fun updateProfile(profile: ProfileDto) {
        postgrest.from(Constants.TABLE_PROFILES)
            .update(profile) {
                filter { eq("id", profile.id) }
            }
    }

    // ──────────────── Watchlist ────────────────

    suspend fun getWatchlist(userId: String): List<WatchlistItemDto> {
        return postgrest.from(Constants.TABLE_WATCHLIST)
            .select {
                filter { eq("user_id", userId) }
                order("added_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<WatchlistItemDto>()
    }

    suspend fun addToWatchlist(item: WatchlistItemDto) {
        postgrest.from(Constants.TABLE_WATCHLIST)
            .insert(item)
    }

    suspend fun removeFromWatchlist(userId: String, movieId: String) {
        postgrest.from(Constants.TABLE_WATCHLIST)
            .delete {
                filter {
                    eq("user_id", userId)
                    eq("movie_id", movieId)
                }
            }
    }

    suspend fun isInWatchlist(userId: String, movieId: String): Boolean {
        val result = postgrest.from(Constants.TABLE_WATCHLIST)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("movie_id", movieId)
                }
            }
            .decodeList<WatchlistItemDto>()
        return result.isNotEmpty()
    }

    // ──────────────── Favorites ────────────────

    suspend fun getFavorites(userId: String): List<FavoriteItemDto> {
        return postgrest.from(Constants.TABLE_FAVORITES)
            .select {
                filter { eq("user_id", userId) }
                order("added_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<FavoriteItemDto>()
    }

    suspend fun addToFavorites(item: FavoriteItemDto) {
        postgrest.from(Constants.TABLE_FAVORITES)
            .insert(item)
    }

    suspend fun removeFromFavorites(userId: String, movieId: String) {
        postgrest.from(Constants.TABLE_FAVORITES)
            .delete {
                filter {
                    eq("user_id", userId)
                    eq("movie_id", movieId)
                }
            }
    }

    suspend fun isFavorite(userId: String, movieId: String): Boolean {
        val result = postgrest.from(Constants.TABLE_FAVORITES)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("movie_id", movieId)
                }
            }
            .decodeList<FavoriteItemDto>()
        return result.isNotEmpty()
    }

    // ──────────────── Realtime ────────────────

    fun observeWatchlistChanges(userId: String): Flow<PostgresAction> {
        val channel = client.channel("watchlist-$userId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = Constants.TABLE_WATCHLIST
            filter = "user_id=eq.$userId"
        }
        return flow
    }

    // ──────────────── Daily Questions ────────────────

    suspend fun getTodayQuestions(today: String): List<com.filmapp.data.remote.supabase.dto.DailyQuestionDto> {
        return postgrest[Constants.TABLE_DAILY_QUESTIONS]
            .select {
                filter { eq("created_date", today) }
                order("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(10)
            }
            .decodeList()
    }

    suspend fun insertDailyQuestions(questions: List<com.filmapp.data.remote.supabase.dto.DailyQuestionDto>) {
        postgrest[Constants.TABLE_DAILY_QUESTIONS].insert(questions)
    }

    // ──────────────── Reviews ────────────────

    suspend fun getReviews(movieId: String): List<com.filmapp.data.remote.supabase.dto.ReviewDto> {
        return postgrest[Constants.TABLE_REVIEWS]
            .select {
                filter { eq("movie_id", movieId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(50)
            }
            .decodeList()
    }

    suspend fun addReview(review: com.filmapp.data.remote.supabase.dto.ReviewDto) {
        postgrest[Constants.TABLE_REVIEWS].insert(review)
    }

    // ──────────────── Leaderboard ────────────────

    suspend fun getLeaderboard(): List<com.filmapp.data.remote.supabase.dto.UserScoreDto> {
        return postgrest[Constants.TABLE_USER_SCORES]
            .select {
                order("weekly_score", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(100)
            }
            .decodeList()
    }

    suspend fun updateUserScore(userScore: com.filmapp.data.remote.supabase.dto.UserScoreDto) {
        postgrest[Constants.TABLE_USER_SCORES].upsert(userScore)
    }
}

// Helper to build JSON objects for sign-up metadata
private fun buildJsonObject(builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): kotlinx.serialization.json.JsonObject {
    return kotlinx.serialization.json.buildJsonObject(builder)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: String) {
    put(key, kotlinx.serialization.json.JsonPrimitive(value))
}
