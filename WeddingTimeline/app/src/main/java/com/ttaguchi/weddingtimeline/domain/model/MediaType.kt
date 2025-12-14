package com.ttaguchi.weddingtimeline.domain.model

/**
 * Media type enum.
 */
enum class MediaType(val rawValue: String) {
    IMAGE("image"),
    VIDEO("video");

    companion object {
        fun fromRawValue(value: String): MediaType {
            return entries.find { it.rawValue.equals(value, ignoreCase = true) } ?: IMAGE
        }
    }
}