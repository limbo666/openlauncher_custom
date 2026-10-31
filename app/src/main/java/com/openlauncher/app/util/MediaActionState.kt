package com.openlauncher.app.util

import android.media.Rating
import android.os.Bundle
import java.util.Locale

internal enum class MediaToggle { LIKE, DISLIKE, SAVE, SHUFFLE, REPEAT, OTHER }

internal fun mediaToggle(label: String): MediaToggle {
    val name = label.lowercase(Locale.ROOT)
    return when {
        "dislike" in name || "thumb down" in name -> MediaToggle.DISLIKE
        listOf("like", "favorite", "heart", "thumb up").any { it in name } -> MediaToggle.LIKE
        "save" in name || "library" in name -> MediaToggle.SAVE
        "shuffle" in name -> MediaToggle.SHUFFLE
        "repeat" in name -> MediaToggle.REPEAT
        else -> MediaToggle.OTHER
    }
}

/** Use reported state, never an optimistic local toggle that could disagree with the player. */
internal fun mediaActionActive(label: String, iconName: String = "", extras: Bundle? = null, rating: Rating? = null): Boolean? {
    val kind = mediaToggle(label)
    if (kind == MediaToggle.OTHER) return null
    for (key in listOf("selected", "isSelected", "checked", "isChecked", "active", "isActive")) {
        val value = runCatching { extras?.get(key) }.getOrNull()
        if (value is Boolean) return value
    }
    if (kind == MediaToggle.LIKE && rating != null) return when (rating.ratingStyle) {
        Rating.RATING_HEART -> rating.isRated && rating.hasHeart()
        Rating.RATING_THUMB_UP_DOWN -> rating.isRated && rating.isThumbUp
        else -> null
    }
    val icon = iconName.lowercase(Locale.ROOT)
    if (listOf("inactive", "unchecked", "unselected", "not_saved", "not_liked", "outline", "_off").any { it in icon }) return false
    if (Regex("(^|_)(active|checked|selected|filled|on)($|_)").containsMatchIn(icon)) return true
    val name = label.lowercase(Locale.ROOT)
    if (listOf("unlike", "remove like", "remove favorite", "remove from", "unsave", "disable", "turn off").any { it in name }) return true
    if (listOf("add to", "enable", "turn on").any { it in name } && kind != MediaToggle.DISLIKE) return false
    return null
}

internal fun ratingActive(rating: Rating?, style: Int): Boolean? = rating?.takeIf { it.ratingStyle == style }?.let {
    when (style) {
        Rating.RATING_HEART -> it.isRated && it.hasHeart()
        Rating.RATING_THUMB_UP_DOWN -> it.isRated && it.isThumbUp
        Rating.RATING_3_STARS, Rating.RATING_4_STARS, Rating.RATING_5_STARS -> it.isRated && it.starRating > 0
        else -> null
    }
}
