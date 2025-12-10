package com.ttaguchi.weddingtimeline.model

import android.net.Uri
import java.util.UUID

/**
 * Represents a selected media attachment (image or video) before upload.
 */
data class SelectedAttachment(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val type: MediaType,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
) {
    /**
     * Check if this is an image.
     */
    val isImage: Boolean
        get() = type == MediaType.IMAGE

    /**
     * Check if this is a video.
     */
    val isVideo: Boolean
        get() = type == MediaType.VIDEO
}