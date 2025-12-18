package com.ttaguchi.weddingtimeline.domain.model

import com.google.firebase.Timestamp

/**
 * Room member model.
 */
data class RoomMember(
    val id: String,
    val username: String,
    val usernameLower: String = "",
    val userIcon: String = "",
    val role: String = "member",
    val joinedAt: Timestamp? = null,
    val lastSignedInAt: Timestamp? = null,
    val isBanned: Boolean = false,
    val mutedUntil: Timestamp? = null,
) {
    val isOwner: Boolean
        get() = role == "owner"

    val isAdmin: Boolean
        get() = role == "admin"

    val isMember: Boolean
        get() = role == "member"

    val isMuted: Boolean
        get() = mutedUntil?.let { it.toDate().time > System.currentTimeMillis() } ?: false
}