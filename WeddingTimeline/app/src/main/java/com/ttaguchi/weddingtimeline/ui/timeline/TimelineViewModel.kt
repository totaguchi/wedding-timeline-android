package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.ttaguchi.weddingtimeline.data.PostRepository
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import com.ttaguchi.weddingtimeline.domain.model.TimelineFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * UI state for Timeline screen.
 */
data class TimelineUiState(
    val posts: List<TimeLinePost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedFilter: TimelineFilter = TimelineFilter.ALL,
    val newBadgeCount: Int = 0,
    val isAtTop: Boolean = true,
    val activeVideoPostId: String? = null,
)

/**
 * ViewModel for Timeline screen.
 */
class TimelineViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private var lastSnapshot: DocumentSnapshot? = null
    private val pendingPosts = mutableListOf<TimeLinePost>()
    private var listenJob: Job? = null

    /**
     * Fetch posts with pagination.
     */
    fun fetchPosts(roomId: String, reset: Boolean = false) {
        println("[TimelineViewModel] fetchPosts called: roomId=$roomId, reset=$reset, isLoading=${_uiState.value.isLoading}")
        
        if (_uiState.value.isLoading) {
            println("[TimelineViewModel] Already loading, skipping...")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                println("[TimelineViewModel] Starting fetch...")

                val result = repository.fetchPosts(
                    roomId = roomId,
                    limit = 50,
                    startAfter = if (reset) null else lastSnapshot
                )

                println("[TimelineViewModel] Fetched ${result.posts.size} posts")

                val newList = if (reset) {
                    result.posts
                } else {
                    (_uiState.value.posts + result.posts).distinctBy { it.id }
                }

                _uiState.value = _uiState.value.copy(
                    posts = newList,
                    isLoading = false,
                    error = null
                )

                lastSnapshot = result.lastSnapshot
            } catch (e: Exception) {
                println("[TimelineViewModel] Error fetching posts: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = "投稿の取得に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * Refresh head (pull-to-refresh).
     */
    fun refreshHead(roomId: String) {
        println("[TimelineViewModel] refreshHead called for roomId=$roomId")
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
                
                val result = repository.fetchPosts(roomId = roomId, limit = 50, startAfter = null)
                
                println("[TimelineViewModel] Refresh completed with ${result.posts.size} posts")
                
                _uiState.value = _uiState.value.copy(
                    posts = result.posts,
                    isRefreshing = false,
                    newBadgeCount = 0,
                    error = null
                )
                
                lastSnapshot = result.lastSnapshot
                pendingPosts.clear()
            } catch (e: Exception) {
                println("[TimelineViewModel] Refresh error: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "更新に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * Start listening for new posts.
     */
    fun startListening(roomId: String) {
        println("[TimelineViewModel] Starting real-time listener for roomId=$roomId")
        
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            repository.listenLatestWithIsLiked(roomId).collectLatest { payload ->
                handleNewPosts(payload.posts)
            }
        }
    }

    private fun handleNewPosts(newPosts: List<TimeLinePost>) {
        val existingIds = _uiState.value.posts.map { it.id }.toSet()
        val actuallyNew = newPosts.filter { it.id !in existingIds }
        
        if (actuallyNew.isEmpty()) {
            println("[TimelineViewModel] No new posts to handle")
            return
        }
        
        println("[TimelineViewModel] Handling ${actuallyNew.size} new posts, isAtTop=${_uiState.value.isAtTop}")
        
        if (_uiState.value.isAtTop) {
            _uiState.value = _uiState.value.copy(
                posts = (actuallyNew + _uiState.value.posts).distinctBy { it.id }
            )
        } else {
            pendingPosts.addAll(actuallyNew)
            _uiState.value = _uiState.value.copy(
                newBadgeCount = pendingPosts.size
            )
        }
    }

    fun revealPending() {
        println("[TimelineViewModel] Revealing ${pendingPosts.size} pending posts")
        
        if (pendingPosts.isEmpty()) return
        
        _uiState.value = _uiState.value.copy(
            posts = (pendingPosts.toList() + _uiState.value.posts).distinctBy { it.id },
            newBadgeCount = 0
        )
        pendingPosts.clear()
    }

    fun markAtTop(isAtTop: Boolean) {
        _uiState.value = _uiState.value.copy(isAtTop = isAtTop)
        
        if (isAtTop && pendingPosts.isNotEmpty()) {
            revealPending()
        }
    }

    fun toggleLike(post: TimeLinePost, roomId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        viewModelScope.launch {
            try {
                val newIsLiked = !post.isLiked
                val newCount = repository.toggleLike(roomId, post.id, userId, newIsLiked)

                val updatedPosts = _uiState.value.posts.map { p ->
                    if (p.id == post.id) {
                        p.copy(isLiked = newIsLiked, likeCount = newCount)
                    } else p
                }
                _uiState.value = _uiState.value.copy(posts = updatedPosts)
            } catch (e: Exception) {
                println("[TimelineViewModel] Error toggling like: ${e.message}")
            }
        }
    }

    fun setFilter(filter: TimelineFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredPosts(): List<TimeLinePost> {
        val filter = _uiState.value.selectedFilter
        return if (filter == TimelineFilter.ALL) {
            _uiState.value.posts
        } else {
            _uiState.value.posts.filter { post ->
                when (filter) {
                    TimelineFilter.CEREMONY -> post.tag == com.ttaguchi.weddingtimeline.domain.model.PostTag.CEREMONY
                    TimelineFilter.RECEPTION -> post.tag == com.ttaguchi.weddingtimeline.domain.model.PostTag.RECEPTION
                    TimelineFilter.ALL -> true
                }
            }
        }
    }

    /**
     * Set the currently active video post ID.
     * Only one video should be playing at a time.
     */
    fun setActiveVideoPost(postId: String?) {
        _uiState.value = _uiState.value.copy(activeVideoPostId = postId)
    }

    override fun onCleared() {
        super.onCleared()
        listenJob?.cancel()
        println("[TimelineViewModel] Listener removed")
    }
}
