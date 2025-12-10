package com.ttaguchi.weddingtimeline.model

/**
 * Filter options for timeline view.
 */
enum class TimelineFilter(val displayName: String) {
    ALL("すべて"),
    CEREMONY("挙式"),
    RECEPTION("披露宴");

    companion object {
        fun fromPostTag(tag: PostTag): TimelineFilter {
            return when (tag) {
                PostTag.CEREMONY -> CEREMONY
                PostTag.RECEPTION -> RECEPTION
            }
        }
    }
}