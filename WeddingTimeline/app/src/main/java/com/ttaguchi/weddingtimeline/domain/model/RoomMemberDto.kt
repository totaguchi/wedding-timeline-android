package com.ttaguchi.weddingtimeline.domain.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class RoomMemberDto(
    @DocumentId
    val id: String = "",
    
    @PropertyName("username")
    val username: String = "",
    
    @PropertyName("usernameLower")
    val usernameLower: String = "",
    
    @PropertyName("role")
    val role: String = "member",
    
    @PropertyName("joinedAt")
    val joinedAt: Timestamp? = null,
    
    @PropertyName("lastSignedInAt")
    val lastSignedInAt: Timestamp? = null,
    
    @PropertyName("isBanned")
    val isBanned: Boolean = false,
    
    @PropertyName("mutedUntil")
    val mutedUntil: Timestamp? = null,
    
    @PropertyName("userIcon")
    val userIcon: String = "",
) {
    fun toDomain(): RoomMember {
        return RoomMember(
            id = id,
            username = username,
            usernameLower = usernameLower,
            role = role,
            joinedAt = joinedAt,
            lastSignedInAt = lastSignedInAt,
            isBanned = isBanned,
            mutedUntil = mutedUntil,
            userIcon = userIcon
        )
    }
}