package com.ttaguchi.weddingtimeline.domain.model

/**
 * Media model.
 * MediaType enum is defined in MediaType.kt
 */
data class Media(
    val id: String,
    val url: String,
    val type: MediaType,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val storagePath: String? = null,
)