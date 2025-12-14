package com.ttaguchi.weddingtimeline.ui.post

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.PostRepository
import com.ttaguchi.weddingtimeline.model.Media
import com.ttaguchi.weddingtimeline.model.MediaType
import com.ttaguchi.weddingtimeline.model.PostTag
import com.ttaguchi.weddingtimeline.model.Session
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostViewModel(
    private val roomId: String,
    private val session: Session,
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var postCreated by mutableStateOf(false)
        private set

    fun clearError() {
        errorMessage = null
    }

    fun createPost(
        content: String,
        tag: PostTag,
        mediaUris: List<Uri>,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    errorMessage = "ログインが必要です"
                    throw Exception("Not logged in")
                }

                val postId = UUID.randomUUID().toString()
                val authorId = currentUser.uid
                
                // Session から取得（優先順位: AppUser.name > RoomMember.username > "Unknown"）
                val authorName = session.displayName
                val userIcon = session.displayIcon

                // Upload media if any
                val mediaList = mutableListOf<Media>()
                mediaUris.forEachIndexed { index, uri ->
                    try {
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val mediaType = if (mimeType.startsWith("video/")) {
                            MediaType.VIDEO
                        } else {
                            MediaType.IMAGE
                        }

                        val storagePath = "rooms/$roomId/posts/$postId/$authorId/${mediaType.rawValue}_$index"
                        val downloadUrl = repository.uploadMedia(uri, storagePath, context)

                        mediaList.add(
                            Media(
                                id = downloadUrl,
                                url = downloadUrl,
                                type = mediaType,
                                storagePath = storagePath
                            )
                        )
                    } catch (e: Exception) {
                        println("[CreatePostViewModel] Failed to upload media: ${e.message}")
                        // Continue with other media
                    }
                }

                // Create post
                repository.createPost(
                    roomId = roomId,
                    postId = postId,
                    content = content,
                    authorId = authorId,
                    authorName = authorName,
                    userIcon = userIcon,
                    tag = tag,
                    media = mediaList
                )

                postCreated = true
            } catch (e: Exception) {
                errorMessage = e.message ?: "投稿の作成に失敗しました"
                println("[CreatePostViewModel] Error creating post: ${e.message}")
                e.printStackTrace()
                throw e // Re-throw to let the UI handle it
            }
        }
    }
}

class CreatePostViewModelFactory(
    private val roomId: String,
    private val session: Session
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            return CreatePostViewModel(roomId, session) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}