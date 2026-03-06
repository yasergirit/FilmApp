package com.filmapp.domain.repository

import com.filmapp.core.util.Resource
import com.filmapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    fun isLoggedIn(): Boolean

    fun getCurrentUserId(): String?

    suspend fun signUp(email: String, password: String, displayName: String): Resource<Unit>

    suspend fun signIn(email: String, password: String): Resource<Unit>

    suspend fun signOut(): Resource<Unit>

    fun getProfile(): Flow<Resource<UserProfile>>
}
