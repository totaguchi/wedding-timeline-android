package com.ttaguchi.weddingtimeline.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timeline post model.
 * Matches Swift's TimelinePost structure.
 */
data class TimeLinePost(
    val id: String,
    val roomId: String,
    val authorId: String,
    val authorName: String,
    val userIcon: String,
    val content: String,
    val createdAt: Date,
    val replyCount: Int,
    val retweetCount: Int,
    val likeCount: Int,
    val isLiked: Boolean,
    val media: List<Media>,
    val tag: PostTag,
) {
    val hasMedia: Boolean get() = media.isNotEmpty()

    fun formattedCreatedAt(): String {
        val now = Date()
        val diffMillis = now.time - createdAt.time
        val diffSeconds = diffMillis / 1000
        val diffMinutes = diffSeconds / 60
        val diffHours = diffMinutes / 60
        val diffDays = diffHours / 24

        return when {
            diffSeconds < 60 -> "${diffSeconds}秒前"
            diffMinutes < 60 -> "${diffMinutes}分前"
            diffHours < 24 -> "${diffHours}時間前"
            diffDays < 7 -> "${diffDays}日前"
            else -> {
                val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
                formatter.format(createdAt)
            }
        }
    }

    companion object {
        /**
         * Extension function to create TimeLinePost from DTO.
         * Matches Swift's init?(dto: TimelinePostDTO) extension.
         */
        fun fromDto(dto: TimeLinePostDto, roomId: String, isLiked: Boolean = false): TimeLinePost? {
            return dto.toDomain(roomId, isLiked)
        }
    }
}
