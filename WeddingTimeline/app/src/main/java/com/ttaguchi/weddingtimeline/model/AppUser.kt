package com.ttaguchi.weddingtimeline.model

import com.google.firebase.Timestamp

/**
 * Domain model for AppUser.
 */
data class AppUser(
    val id: String,
    val name: String,
    val icon: String,
    val createdAt: Timestamp?,
)