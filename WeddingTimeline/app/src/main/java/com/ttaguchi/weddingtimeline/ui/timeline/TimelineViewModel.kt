package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.ttaguchi.weddingtimeline.data.PostRepository
import com.ttaguchi.weddingtimeline.domain.model.PostTag
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import com.ttaguchi.weddingtimeline.domain.model.TimelineFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for Timeline screen.
 */
data class TimelineUiState(
    val posts: List<TimeLinePost> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedFilter: TimelineFilter = TimelineFilter.ALL,
    val newBadgeCount: Int = 0,
    val error: String? = null,
)

/**
 * ViewModel for Timeline screen.
 */
class TimelineViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    private var lastSnapshot: DocumentSnapshot? = null
    private var listenerJob: Job? = null
    private var pendingPosts: List<TimeLinePost> = emptyList()
    private var isAtTop: Boolean = true
    private var currentRoomId: String? = null

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
                _uiState.update { it.copy(isLoading = true, error = null) }
                println("[TimelineViewModel] Starting fetch...")

                val startAfter = if (reset) {
                    lastSnapshot = null
                    null
                } else {
                    lastSnapshot
                }

                val result = postRepository.fetchPosts(
                    roomId = roomId,
                    limit = 50,
                    startAfter = startAfter
                )

                println("[TimelineViewModel] Fetched ${result.posts.size} posts")

                _uiState.update { state ->
                    val newPosts = if (reset) {
                        result.posts
                    } else {
                        (state.posts + result.posts).distinctBy { it.id }
                    }
                    state.copy(
                        posts = newPosts,
                        isLoading = false,
                        error = null
                    )
                }

                lastSnapshot = result.lastSnapshot
                currentRoomId = roomId
            } catch (e: Exception) {
                println("[TimelineViewModel] Error fetching posts: ${e.message}")
                e.printStackTrace()
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "投稿の取得に失敗しました: ${e.message}"
                    ) 
                }
            }
        }
    }

    /**
     * Start listening to latest posts.
     */
    fun startListening(roomId: String) {
        println("[TimelineViewModel] startListening called: roomId=$roomId")
        
        listenerJob?.cancel()
        listenerJob = viewModelScope.launch {
            postRepository.listenLatestWithIsLiked(roomId, limit = 20)
                .catch { e ->
                    println("[TimelineViewModel] Listener error: ${e.message}")
                    e.printStackTrace()
                    _uiState.update { it.copy(error = "リアルタイム更新エラー: ${e.message}") }
                }
                .collect { result ->
                    println("[TimelineViewModel] Received ${result.posts.size} posts from listener")
                    handleNewPosts(result.posts)
                }
        }
    }

    /**
     * Refresh head (pull-to-refresh).
     */
    fun refreshHead(roomId: String) {
        println("[TimelineViewModel] refreshHead called")
        
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
                
                val result = postRepository.fetchPosts(roomId, limit = 50, startAfter = null)
                
                println("[TimelineViewModel] Refresh completed with ${result.posts.size} posts")
                
                _uiState.update { state ->
                    state.copy(
                        posts = result.posts,
                        isRefreshing = false,
                        newBadgeCount = 0,
                        error = null
                    )
                }
                
                lastSnapshot = result.lastSnapshot
                pendingPosts = emptyList()
            } catch (e: Exception) {
                println("[TimelineViewModel] Refresh error: ${e.message}")
                e.printStackTrace()
                _uiState.update { 
                    it.copy(
                        isRefreshing = false,
                        error = "更新に失敗しました: ${e.message}"
                    ) 
                }
            }
        }
    }

    private fun handleNewPosts(latestPosts: List<TimeLinePost>) {
        val currentPosts = _uiState.value.posts
        val newPosts = latestPosts.filter { new -> currentPosts.none { it.id == new.id } }

        if (newPosts.isEmpty()) return

        println("[TimelineViewModel] Found ${newPosts.size} new posts, isAtTop=$isAtTop")

        if (isAtTop) {
            // Merge immediately
            _uiState.update { state ->
                val merged = (newPosts + currentPosts).distinctBy { it.id }
                state.copy(posts = merged)
            }
        } else {
            // Store as pending
            pendingPosts = (newPosts + pendingPosts).distinctBy { it.id }
            _uiState.update { it.copy(newBadgeCount = pendingPosts.size) }
        }
    }

    fun revealPending() {
        val pending = pendingPosts
        if (pending.isEmpty()) return

        println("[TimelineViewModel] Revealing ${pending.size} pending posts")

        _uiState.update { state ->
            val merged = (pending + state.posts).distinctBy { it.id }
            state.copy(posts = merged, newBadgeCount = 0)
        }
        pendingPosts = emptyList()
    }

    fun markAtTop(atTop: Boolean) {
        isAtTop = atTop
        if (atTop && pendingPosts.isNotEmpty()) {
            revealPending()
        }
    }

    fun setFilter(filter: TimelineFilter) {
        println("[TimelineViewModel] Filter changed to: $filter")
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun getFilteredPosts(): List<TimeLinePost> {
        val posts = _uiState.value.posts
        return when (_uiState.value.selectedFilter) {
            TimelineFilter.ALL -> posts
            TimelineFilter.CEREMONY -> posts.filter { it.tag == PostTag.CEREMONY }
            TimelineFilter.RECEPTION -> posts.filter { it.tag == PostTag.RECEPTION }
        }
    }

    fun toggleLike(post: TimeLinePost, roomId: String) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            println("[TimelineViewModel] Cannot toggle like: user not authenticated")
            return
        }

        println("[TimelineViewModel] Toggling like for post ${post.id}")

        viewModelScope.launch {
            try {
                val newIsLiked = !post.isLiked
                val newLikeCount = postRepository.toggleLike(roomId, post.id, uid, newIsLiked)

                _uiState.update { state ->
                    val updatedPosts = state.posts.map { p ->
                        if (p.id == post.id) {
                            p.copy(isLiked = newIsLiked, likeCount = newLikeCount)
                        } else {
                            p
                        }
                    }
                    state.copy(posts = updatedPosts)
                }
                
                println("[TimelineViewModel] Like toggled successfully: isLiked=$newIsLiked, count=$newLikeCount")
            } catch (e: Exception) {
                println("[TimelineViewModel] Toggle like error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("[TimelineViewModel] onCleared")
        listenerJob?.cancel()
    }
}