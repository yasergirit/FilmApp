package com.filmapp.core.util

/**
 * A generic sealed class representing the state of a data operation.
 * Used throughout the app to propagate loading / success / error states
 * via Kotlin Flow in a type-safe manner.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
