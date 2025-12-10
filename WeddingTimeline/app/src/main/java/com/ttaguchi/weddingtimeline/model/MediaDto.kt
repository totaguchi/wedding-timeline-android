package com.ttaguchi.weddingtimeline.model

import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for Media.
 */
data class MediaDto(
    @PropertyName("id")
    val id: String = "",

    @PropertyName("url")
    val url: String = "",

    @PropertyName("type")
    val type: String = "image",

    @PropertyName("width")
    val width: Int? = null,

    @PropertyName("height")
    val height: Int? = null,

    @PropertyName("duration")
    val duration: Double? = null,

    @PropertyName("storagePath")
    val storagePath: String? = null,
) {
    /**
     * Convert DTO to domain model.
     */
    fun toDomain(): Media? {
        if (id.isEmpty() || url.isEmpty()) return null

        val mediaType = MediaType.fromRawValue(type)

        return Media(
            id = id,
            url = url,
            type = mediaType,
            width = width,
            height = height,
            duration = duration,
            storagePath = storagePath,
        )
    }
}

/**
 * Extension function to convert Media to DTO.
 * This is the single source of truth for Media -> MediaDto conversion.
 */
fun Media.toDto(): MediaDto {
    return MediaDto(
        id = this.id,
        url = this.url,
        type = this.type.rawValue,
        width = this.width,
        height = this.height,
        duration = this.duration,
        storagePath = this.storagePath,
    )
}