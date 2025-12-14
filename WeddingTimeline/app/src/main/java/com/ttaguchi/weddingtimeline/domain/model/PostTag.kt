package com.ttaguchi.weddingtimeline.domain.model

/**
 * Post tag enum for categorizing posts.
 */
enum class PostTag(val rawValue: String, val displayName: String) {
    CEREMONY("ceremony", "挙式"),
    RECEPTION("reception", "披露宴");

    companion object {
        fun fromRawValue(value: String): PostTag {
            return entries.find { it.rawValue == value } ?: CEREMONY
        }
    }
}