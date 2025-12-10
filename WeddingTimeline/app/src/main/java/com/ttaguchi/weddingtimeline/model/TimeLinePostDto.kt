package com.ttaguchi.weddingtimeline.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for TimelinePost document.
 * Matches Swift's TimelinePostDTO structure.
 */
data class TimeLinePostDto(
    @DocumentId
    var id: String? = null,  // Mutable for Firestore to set

    @PropertyName("content")
    val content: String = "",

    @PropertyName("authorId")
    val authorId: String = "",

    @PropertyName("authorName")
    val authorName: String = "",

    @PropertyName("userIcon")
    val userIcon: String? = null,

    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,

    @PropertyName("media")
    val media: List<MediaDto> = emptyList(),

    @PropertyName("replyCount")
    val replyCount: Int = 0,

    @PropertyName("retweetCount")
    val retweetCount: Int = 0,

    @PropertyName("likeCount")
    val likeCount: Int? = null,

    @PropertyName("tag")
    val tag: String? = null,
) {
    /**
     * Convert DTO to domain model.
     * Returns null if required fields are missing.
     */
    fun toDomain(roomId: String, isLiked: Boolean = false): TimeLinePost? {
        // Validate required fields
        val docId = id ?: return null
        if (authorId.isEmpty()) return null
        
        val postTag = when (tag) {
            "ceremony" -> PostTag.CEREMONY
            "reception" -> PostTag.RECEPTION
            else -> PostTag.CEREMONY // Default
        }

        val domainMedia = media.mapNotNull { it.toDomain() }

        return TimeLinePost(
            id = docId,
            roomId = roomId,
            authorId = authorId,
            authorName = authorName,
            userIcon = userIcon ?: "",
            content = content,
            media = domainMedia,
            tag = postTag,
            createdAt = createdAt?.toDate() ?: java.util.Date(),
            replyCount = replyCount,
            retweetCount = retweetCount,
            likeCount = likeCount ?: 0,
            isLiked = isLiked,
        )
    }
}