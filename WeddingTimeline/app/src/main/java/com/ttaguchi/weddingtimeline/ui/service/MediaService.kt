package com.ttaguchi.weddingtimeline.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.ttaguchi.weddingtimeline.model.Media
import com.ttaguchi.weddingtimeline.model.MediaType
import com.ttaguchi.weddingtimeline.model.SelectedAttachment
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Service for media conversion and upload.
 */
class MediaService(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {

    /**
     * Upload multiple attachments to Storage and return Media list.
     */
    suspend fun uploadMedia(
        attachments: List<SelectedAttachment>,
        roomId: String,
    ): List<Media> {
        val uid = auth.currentUser?.uid ?: throw AppError.Unauthenticated

        // Generate temporary postId
        val postId = UUID.randomUUID().toString()

        val results = mutableListOf<Media>()

        attachments.forEachIndexed { index, attachment ->
            val media = when (attachment.type) {
                MediaType.IMAGE -> uploadImage(
                    index = index,
                    uri = attachment.uri,
                    providedWidth = attachment.width,
                    providedHeight = attachment.height,
                    roomId = roomId,
                    postId = postId,
                    userId = uid,
                )
                MediaType.VIDEO -> uploadVideo(
                    index = index,
                    uri = attachment.uri,
                    roomId = roomId,
                    postId = postId,
                    userId = uid,
                )
            }
            results.add(media)
        }

        return results
    }

    /**
     * Upload an image and return Media object.
     */
    private suspend fun uploadImage(
        index: Int,
        uri: Uri,
        providedWidth: Int?,
        providedHeight: Int?,
        roomId: String,
        postId: String,
        userId: String,
    ): Media {
        val currentUid = auth.currentUser?.uid ?: throw AppError.Unauthenticated
        if (currentUid != userId) throw AppError.Unauthorized

        // Load bitmap
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw AppError.InvalidData

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Compress
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val data = outputStream.toByteArray()

        if (data.size >= 10 * 1024 * 1024) {
            throw AppError.FileTooLarge("画像サイズが10MBを超えています")
        }

        val fileName = "img_${index}_${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child("rooms/$roomId/posts/$postId/$userId/$fileName")

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        ref.putBytes(data, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        return Media(
            id = downloadUrl,
            url = downloadUrl,
            type = MediaType.IMAGE,
            width = providedWidth ?: bitmap.width,
            height = providedHeight ?: bitmap.height,
            duration = null,
            storagePath = ref.path,
        )
    }

    /**
     * Upload a video and return Media object.
     */
    private suspend fun uploadVideo(
        index: Int,
        uri: Uri,
        roomId: String,
        postId: String,
        userId: String,
    ): Media {
        val currentUid = auth.currentUser?.uid ?: throw AppError.Unauthenticated
        if (currentUid != userId) throw AppError.Unauthorized

        val file = getFileFromUri(uri)

        if (file.length() >= 200 * 1024 * 1024) {
            throw AppError.FileTooLarge("動画サイズが200MBを超えています")
        }

        val fileName = "mov_${index}_${UUID.randomUUID()}.mp4"
        val ref = storage.reference.child("rooms/$roomId/posts/$postId/$userId/$fileName")

        val metadata = StorageMetadata.Builder()
            .setContentType("video/mp4")
            .build()

        ref.putFile(uri, metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()

        // Extract video metadata
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationStr?.toDoubleOrNull()?.div(1000.0)

        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val width = widthStr?.toIntOrNull()
        val height = heightStr?.toIntOrNull()

        retriever.release()

        return Media(
            id = downloadUrl,
            url = downloadUrl,
            type = MediaType.VIDEO,
            width = width,
            height = height,
            duration = duration,
            storagePath = ref.path,
        )
    }

    private fun getFileFromUri(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw AppError.InvalidData

        val tempFile = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        return tempFile
    }
}

/**
 * Application-specific errors.
 */
sealed class AppError : Exception() {
    object Unauthenticated : AppError() {
        override val message: String = "認証されていません"
    }

    object Unauthorized : AppError() {
        override val message: String = "権限がありません"
    }

    object InvalidData : AppError() {
        override val message: String = "無効なデータです"
    }

    data class FileTooLarge(override val message: String) : AppError()

    data class TranscodeError(override val message: String) : AppError()
}