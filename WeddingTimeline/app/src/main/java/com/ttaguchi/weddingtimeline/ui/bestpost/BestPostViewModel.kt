package com.ttaguchi.weddingtimeline.ui.bestpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.PostRepository
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for BestPost screen.
 */
data class BestPostUiState(
    val posts: List<TimeLinePost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel for BestPost screen.
 * Shows all posts sorted by like count.
 */
class BestPostViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BestPostUiState())
    val uiState: StateFlow<BestPostUiState> = _uiState.asStateFlow()

    /**
     * Fetch best posts (sorted by like count).
     */
    fun fetchBestPosts(roomId: String) {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val result = repository.fetchPosts(
                    roomId = roomId,
                    limit = 100,
                    startAfter = null
                )

                // Sort by like count (descending)
                val bestPosts = result.posts
                    .sortedByDescending { it.likeCount }
                    .take(3)

                _uiState.value = _uiState.value.copy(
                    posts = bestPosts,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                println("[BestPostViewModel] Error fetching best posts: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "投稿の取得に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh best posts (pull-to-refresh).
     */
    fun refreshBestPosts(roomId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

                val result = repository.fetchPosts(
                    roomId = roomId,
                    limit = 100,
                    startAfter = null
                )

                val bestPosts = result.posts
                    .sortedByDescending { it.likeCount }
                    .take(3)

                _uiState.value = _uiState.value.copy(
                    posts = bestPosts,
                    isRefreshing = false,
                    error = null
                )
            } catch (e: Exception) {
                println("[BestPostViewModel] Refresh error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "更新に失敗しました: ${e.message}"
                )
            }
        }
    }

    fun toggleLike(post: TimeLinePost, roomId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        viewModelScope.launch {
            try {
                val wasLiked = post.isLiked
                repository.toggleLike(roomId, post.id, userId, !wasLiked)

                // Optimistic update
                val updatedPosts = _uiState.value.posts.map { p ->
                    if (p.id == post.id) {
                        val newLikeCount = if (wasLiked) {
                            p.likeCount - 1
                        } else {
                            p.likeCount + 1
                        }
                        p.copy(
                            likeCount = newLikeCount,
                            isLiked = !wasLiked
                        )
                    } else {
                        p
                    }
                }
                    .sortedByDescending { it.likeCount } // Re-sort after like toggle

                _uiState.value = _uiState.value.copy(posts = updatedPosts)
            } catch (e: Exception) {
                println("[BestPostViewModel] Error toggling like: ${e.message}")
            }
        }
    }
}
