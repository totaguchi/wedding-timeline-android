package com.ttaguchi.weddingtimeline.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for AppUser document.
 */
data class AppUserDto(
    @DocumentId
    val id: String = "",

    @PropertyName("name")
    val name: String = "",

    @PropertyName("icon")
    val icon: String = "",

    @PropertyName("createdAt")
    val createdAt: Timestamp? = null,
)