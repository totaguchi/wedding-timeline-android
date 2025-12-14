package com.ttaguchi.weddingtimeline.domain.model

import com.google.firebase.firestore.PropertyName

/**
 * Firestore DTO for Media.
 * Matches Swift's MediaDTO structure.
 */
data class MediaDto(
    @PropertyName("id")
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @PropertyName("mediaUrl")
    @get:PropertyName("mediaUrl")
    @set:PropertyName("mediaUrl")
    var url: String = "",

    @PropertyName("type")
    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "image",

    @PropertyName("width")
    @get:PropertyName("width")
    @set:PropertyName("width")
    var width: Int? = null,

    @PropertyName("height")
    @get:PropertyName("height")
    @set:PropertyName("height")
    var height: Int? = null,

    @PropertyName("duration")
    @get:PropertyName("duration")
    @set:PropertyName("duration")
    var duration: Double? = null,

    @PropertyName("storagePath")
    @get:PropertyName("storagePath")
    @set:PropertyName("storagePath")
    var storagePath: String? = null,
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "image", null, null, null, null)

    /**
     * Convert DTO to domain model.
     */
    fun toDomain(): Media? {
        println("[MediaDto] toDomain called: id=$id, url=$url, type=$type")
        
        if (id.isEmpty()) {
            println("[MediaDto] Invalid media: id is empty")
            return null
        }
        
        if (url.isEmpty()) {
            println("[MediaDto] Invalid media: url is empty")
            return null
        }

        val mediaType = MediaType.fromRawValue(type)
        println("[MediaDto] Converting to domain: id=$id, type=$type -> $mediaType, url length=${url.length}")

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