package com.filmapp.data.remote.tmdb.dto

import com.google.gson.annotations.SerializedName

// ── OMDb Search Response ──

data class OmdbSearchResponse(
    @SerializedName("Search") val search: List<OmdbSearchItem>?,
    val totalResults: String?,
    @SerializedName("Response") val response: String  // "True" or "False"
)

data class OmdbSearchItem(
    @SerializedName("Title") val title: String,
    @SerializedName("Year") val year: String?,
    val imdbID: String,
    @SerializedName("Type") val type: String?,
    @SerializedName("Poster") val poster: String?
)

// ── OMDb Detail Response ──

data class OmdbDetailResponse(
    @SerializedName("Title") val title: String?,
    @SerializedName("Year") val year: String?,
    @SerializedName("Rated") val rated: String?,
    @SerializedName("Released") val released: String?,
    @SerializedName("Runtime") val runtime: String?,
    @SerializedName("Genre") val genre: String?,
    @SerializedName("Director") val director: String?,
    @SerializedName("Writer") val writer: String?,
    @SerializedName("Actors") val actors: String?,
    @SerializedName("Plot") val plot: String?,
    @SerializedName("Poster") val poster: String?,
    @SerializedName("Ratings") val ratings: List<OmdbRating>?,
    @SerializedName("Metascore") val metascore: String?,
    val imdbRating: String?,
    val imdbVotes: String?,
    val imdbID: String?,
    @SerializedName("Type") val type: String?,
    @SerializedName("BoxOffice") val boxOffice: String?,
    @SerializedName("Response") val response: String
)

data class OmdbRating(
    @SerializedName("Source") val source: String,
    @SerializedName("Value") val value: String
)
