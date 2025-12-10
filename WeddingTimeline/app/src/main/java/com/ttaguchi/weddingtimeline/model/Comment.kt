package com.ttaguchi.weddingtimeline.model

import com.google.firebase.Timestamp

/**
 * Domain model for Comment/Reply.
 */
data class Comment(
    val id: String,
    val postId: String,
    val roomId: String,
    val authorId: String,
    val authorName: String,
    val userIcon: String,
    val content: String,
    val createdAt: Timestamp?,
)