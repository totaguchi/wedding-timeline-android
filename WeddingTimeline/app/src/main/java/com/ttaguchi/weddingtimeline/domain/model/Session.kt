package com.ttaguchi.weddingtimeline.domain.model

/**
 * Represents the current user session state.
 */
data class Session(
    val uid: String,
    val user: AppUser?,
    val roomId: String?,
    val member: RoomMember?,
) {
    /**
     * Check if user is logged in.
     */
    val isLoggedIn: Boolean
        get() = uid.isNotEmpty()

    /**
     * Check if user has joined a room.
     */
    val hasJoinedRoom: Boolean
        get() = roomId != null && member != null

    /**
     * Get display name for the user.
     */
    val displayName: String
        get() = user?.name ?: member?.username ?: "ゲスト"

    /**
     * Get icon for the user.
     */
    val displayIcon: String
        get() = user?.icon ?: member?.userIcon ?: "👤"

    companion object {
        /**
         * Empty session for logged out state.
         */
        val empty = Session(
            uid = "",
            user = null,
            roomId = null,
            member = null,
        )
    }
}