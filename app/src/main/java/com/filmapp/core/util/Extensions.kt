package com.filmapp.core.util

import com.filmapp.core.constants.Constants

/** OMDb returns full poster URLs or "N/A" when no poster exists. */
fun String?.toSafePosterUrl(): String {
    return if (this != null && this != Constants.OMDB_NO_POSTER) this else ""
}

fun String?.toRatingString(): String {
    if (this == null || this == "N/A") return "—"
    return this
}
