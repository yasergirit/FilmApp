package com.filmapp.data.repository

import com.filmapp.core.util.Resource
import com.filmapp.data.local.UserPreferencesManager
import com.filmapp.data.mapper.toDomain
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.remote.supabase.dto.ProfileDto
import com.filmapp.di.IoDispatcher
import com.filmapp.domain.model.UserProfile
import com.filmapp.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseDataSource: SupabaseDataSource,
    private val userPreferencesManager: UserPreferencesManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    companion object {
        private const val DEMO_EMAIL = "demo"
        private const val DEMO_PASSWORD = "demo"
        const val DEMO_USER_ID = "demo-user-00000000-0000-0000-0000-000000000000"
    }

    @Volatile
    var isDemoSession: Boolean = false
        private set

    private fun isDemo(email: String, password: String) =
        email == DEMO_EMAIL && password == DEMO_PASSWORD

    override fun isLoggedIn(): Boolean {
        return isDemoSession || supabaseDataSource.getCurrentUser() != null
    }

    override fun getCurrentUserId(): String? {
        return if (isDemoSession) DEMO_USER_ID else supabaseDataSource.getCurrentUserId()
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Resource<Unit> {
        if (isDemo(email, password)) {
            isDemoSession = true
            userPreferencesManager.setDisplayName("Demo User")
            return Resource.Success(Unit)
        }
        return try {
            val randomName = UserPreferencesManager.generateRandomUsername()
            supabaseDataSource.signUp(email, password, randomName)
            userPreferencesManager.setDisplayName(randomName)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign up failed", e)
        }
    }

    override suspend fun signIn(email: String, password: String): Resource<Unit> {
        if (isDemo(email, password)) {
            isDemoSession = true
            userPreferencesManager.setDisplayName("Demo User")
            return Resource.Success(Unit)
        }
        return try {
            supabaseDataSource.signIn(email, password)
            // Fetch profile and cache display name
            val userId = supabaseDataSource.getCurrentUserId()
            if (userId != null) {
                try {
                    val profile = supabaseDataSource.getProfile(userId)
                    val name = profile.displayName
                    if (!name.isNullOrBlank()) {
                        userPreferencesManager.setDisplayName(name)
                    } else {
                        val randomName = UserPreferencesManager.generateRandomUsername()
                        supabaseDataSource.updateProfile(ProfileDto(id = userId, displayName = randomName))
                        userPreferencesManager.setDisplayName(randomName)
                    }
                } catch (_: Exception) {
                    val randomName = UserPreferencesManager.generateRandomUsername()
                    userPreferencesManager.setDisplayName(randomName)
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign in failed", e)
        }
    }

    override suspend fun signOut(): Resource<Unit> {
        if (isDemoSession) {
            isDemoSession = false
            userPreferencesManager.clear()
            return Resource.Success(Unit)
        }
        return try {
            supabaseDataSource.signOut()
            userPreferencesManager.clear()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign out failed", e)
        }
    }

    override fun getProfile(): Flow<Resource<UserProfile>> = flow {
        emit(Resource.Loading)
        if (isDemoSession) {
            emit(Resource.Success(UserProfile(
                id = DEMO_USER_ID,
                username = "demo",
                displayName = "Demo User",
                avatarUrl = null
            )))
            return@flow
        }
        try {
            val userId = supabaseDataSource.getCurrentUserId()
                ?: throw IllegalStateException("Not authenticated")
            val profile = supabaseDataSource.getProfile(userId).toDomain()
            emit(Resource.Success(profile))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load profile", e))
        }
    }.flowOn(ioDispatcher)
}
