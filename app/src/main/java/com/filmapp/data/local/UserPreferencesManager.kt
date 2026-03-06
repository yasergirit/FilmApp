package com.filmapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

data class LocalFavorite(
    val movieId: String,
    val title: String,
    val posterPath: String?
)

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val FAVORITES_JSON = stringPreferencesKey("favorites_json")
        private val RECOMMENDATIONS_JSON = stringPreferencesKey("recommendations_json")
        private val RECOMMENDATIONS_DATE = stringPreferencesKey("recommendations_date")

        fun generateRandomUsername(): String {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            val suffix = (1..6).map { chars.random() }.joinToString("")
            return "Kullanici_$suffix"
        }
    }

    private val gson = Gson()

    val displayName: Flow<String> = context.userPrefsDataStore.data.map { prefs ->
        prefs[DISPLAY_NAME] ?: ""
    }

    suspend fun getDisplayName(): String {
        return context.userPrefsDataStore.data.first()[DISPLAY_NAME] ?: ""
    }

    suspend fun setDisplayName(name: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[DISPLAY_NAME] = name
        }
    }

    suspend fun clear() {
        context.userPrefsDataStore.edit { it.clear() }
    }

    // ── Local Favorites (for demo users) ──

    private suspend fun getFavoritesList(): MutableList<LocalFavorite> {
        val json = context.userPrefsDataStore.data.first()[FAVORITES_JSON] ?: return mutableListOf()
        val type = object : TypeToken<MutableList<LocalFavorite>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { mutableListOf() }
    }

    private suspend fun saveFavoritesList(list: List<LocalFavorite>) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[FAVORITES_JSON] = gson.toJson(list)
        }
    }

    suspend fun getLocalFavorites(): List<LocalFavorite> = getFavoritesList()

    suspend fun isLocalFavorite(movieId: String): Boolean {
        return getFavoritesList().any { it.movieId == movieId }
    }

    suspend fun addLocalFavorite(favorite: LocalFavorite) {
        val list = getFavoritesList()
        if (list.none { it.movieId == favorite.movieId }) {
            list.add(0, favorite)
            saveFavoritesList(list)
        }
    }

    suspend fun removeLocalFavorite(movieId: String) {
        val list = getFavoritesList()
        list.removeAll { it.movieId == movieId }
        saveFavoritesList(list)
    }

    // ── Cached Recommendations ──

    suspend fun getCachedRecommendations(): List<CachedRecommendation> {
        val json = context.userPrefsDataStore.data.first()[RECOMMENDATIONS_JSON] ?: return emptyList()
        val type = object : TypeToken<List<CachedRecommendation>>() {}.type
        return try { gson.fromJson(json, type) } catch (_: Exception) { emptyList() }
    }

    suspend fun getRecommendationsDate(): String? {
        return context.userPrefsDataStore.data.first()[RECOMMENDATIONS_DATE]
    }

    suspend fun saveRecommendations(recs: List<CachedRecommendation>, date: String) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[RECOMMENDATIONS_JSON] = gson.toJson(recs)
            prefs[RECOMMENDATIONS_DATE] = date
        }
    }
}

data class CachedRecommendation(
    val title: String,
    val year: String,
    val reason: String,
    val genre: String,
    val imdbId: String
)
