package com.ttaguchi.weddingtimeline.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for RoomMember document.
 */
data class RoomMemberDto(
    @DocumentId
    val id: String = "",

    @PropertyName("username")
    val username: String = "",

    @PropertyName("usernameLower")
    val usernameLower: String = "",

    @PropertyName("userIcon")
    val userIcon: String = "",

    @PropertyName("role")
    val role: String = "member", // "owner" | "admin" | "member"

    @PropertyName("joinedAt")
    val joinedAt: Timestamp? = null,

    @PropertyName("lastSignedInAt")
    val lastSignedInAt: Timestamp? = null,

    @PropertyName("isBanned")
    val isBanned: Boolean = false,

    @PropertyName("mutedUntil")
    val mutedUntil: Timestamp? = null,
) {
    /**
     * Convert DTO to domain model.
     */
    fun toDomain(): RoomMember {
        return RoomMember(
            id = id,
            username = username,
            userIcon = userIcon,
            role = role,
            joinedAt = joinedAt,
            lastSignedInAt = lastSignedInAt,
            isBanned = isBanned,
            mutedUntil = mutedUntil,
        )
    }
}