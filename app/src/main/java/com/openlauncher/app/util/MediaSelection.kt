package com.openlauncher.app.util

/** An explicit preference never falls back to an unrelated app. */
fun <T> selectMediaSession(
    sessions: List<T>,
    preferredPackage: String,
    packageName: (T) -> String,
    playing: (T) -> Boolean
): T? {
    val matching = if (preferredPackage.isBlank()) sessions else sessions.filter { packageName(it) == preferredPackage }
    return matching.firstOrNull(playing) ?: matching.firstOrNull()
}

/** Preferred media app, falling back to the last-used one (with a radio sentinel). */
fun musicStartupPackage(preferred: String, lastUsed: String): String = when {
    preferred == "@radio" -> ""
    preferred.isNotBlank() -> preferred
    else -> lastUsed
}
