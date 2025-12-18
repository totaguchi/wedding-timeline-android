package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttaguchi.weddingtimeline.domain.model.MediaType
import com.ttaguchi.weddingtimeline.ui.timeline.components.CategoryFilterBar
import com.ttaguchi.weddingtimeline.ui.timeline.components.FullscreenImageGallery
import com.ttaguchi.weddingtimeline.ui.timeline.components.NewPostsBadge
import com.ttaguchi.weddingtimeline.ui.timeline.components.TimelinePostCard
import com.ttaguchi.weddingtimeline.ui.timeline.components.VideoPlayerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    roomId: String,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(),
    onToggleBottomBar: (Boolean) -> Unit = {},
    viewModel: TimelineViewModel = viewModel(),
) {
    println("[TimelineScreen] Rendering with roomId=$roomId")
    
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var galleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var galleryStartIndex by remember { mutableIntStateOf(0) }
    var showGallery by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var showVideo by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val selectedPost = remember(uiState.posts, selectedPostId) {
        uiState.posts.firstOrNull { it.id == selectedPostId }
    }
    val hideBottomBar = showVideo || showGallery || selectedPost != null

    BackHandler(enabled = selectedPost != null && !showVideo && !showGallery) {
        selectedPostId = null
    }

    LaunchedEffect(selectedPostId, selectedPost) {
        if (selectedPostId != null && selectedPost == null) {
            selectedPostId = null
        }
    }

    val filteredPosts = remember(uiState.posts, uiState.selectedFilter) {
        viewModel.getFilteredPosts()
    }

    // Calculate visible range (middle 50% of screen)
    val density = LocalDensity.current
    val screenHeightPx = with(density) { 
        listState.layoutInfo.viewportSize.height
    }
    val autoPlayRangeStart = screenHeightPx * 0.25f // Top 25%
    val autoPlayRangeEnd = screenHeightPx * 0.75f   // Bottom 75%

    // Find the post with video in the auto-play range
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.firstNotNullOfOrNull { itemInfo ->
                val post = filteredPosts.getOrNull(itemInfo.index)
                if (post != null && post.media.any { it.type == MediaType.VIDEO }) {
                    val itemTop = itemInfo.offset.toFloat()
                    val itemBottom = itemTop + itemInfo.size
                    val itemCenter = (itemTop + itemBottom) / 2f

                    if (itemCenter in autoPlayRangeStart..autoPlayRangeEnd) {
                        post.id
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        }.collect { activePostId ->
            viewModel.setActiveVideoPost(activePostId)
        }
    }

    // Detect if at top
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isAtTop) {
        viewModel.markAtTop(isAtTop)
    }

    // Initial fetch and start listening
    LaunchedEffect(roomId) {
        println("[TimelineScreen] LaunchedEffect triggered for roomId=$roomId")
        viewModel.fetchPosts(roomId, reset = true)
        viewModel.startListening(roomId)
    }

    LaunchedEffect(hideBottomBar) {
        onToggleBottomBar(hideBottomBar)
    }

    // Show error in Snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(error)
            }
        }
    }

    // Handle scroll to end (pagination)
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastIndex ->
                if (lastIndex != null && lastIndex >= filteredPosts.size - 5 && filteredPosts.isNotEmpty()) {
                    println("[TimelineScreen] Near end, triggering pagination")
                    viewModel.fetchPosts(roomId, reset = false)
                }
            }
    }

    // Show video player fullscreen
    when {
        showVideo && videoUrl != null -> {
            VideoPlayerScreen(
                videoUrl = videoUrl!!,
                onBackClick = {
                    showVideo = false
                    videoUrl = null
                }
            )
        }
        showGallery -> {
            FullscreenImageGallery(
                images = galleryImages,
                startIndex = galleryStartIndex,
                onDismiss = { showGallery = false }
            )
        }
        selectedPost != null -> {
            val post = selectedPost!!
            PostDetailScreen(
                post = post,
                onBackClick = { selectedPostId = null },
                onLikeClick = { viewModel.toggleLike(post, roomId) },
                onImageClick = { index ->
                    val images = post.media
                        .filter { it.type == MediaType.IMAGE }
                        .map { it.url }
                    if (images.isNotEmpty()) {
                        galleryImages = images
                        galleryStartIndex = index
                        showGallery = true
                    }
                },
                onVideoClick = { url ->
                    videoUrl = url
                    showVideo = true
                },
                onDelete = {
                    viewModel.deletePost(post.roomId, post.id)
                    selectedPostId = null
                },
                onReport = { reason ->
                    viewModel.reportPost(post.roomId, post.id, reason)
                    selectedPostId = null
                },
                onMuteAuthor = { mute ->
                    viewModel.muteAuthor(post.roomId, post.authorId, mute)
                    selectedPostId = null
                },
                isAuthorMuted = viewModel.isAuthorMuted(post.authorId),
                isVideoVisible = true,
                isMuted = isMuted,
                onMuteToggle = { isMuted = !isMuted }
            )
        }
        else -> {
            Scaffold(
                modifier = modifier,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(onClick = onCreatePost) {
                        Icon(Icons.Default.Add, contentDescription = "投稿作成")
                    }
                }
            ) { scaffoldPadding ->
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { 
                        println("[TimelineScreen] Pull to refresh triggered")
                        viewModel.refreshHead(roomId) 
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding) // MainScreenからのpadding（下部ナビゲーションバー分）
                        .padding(scaffoldPadding) // Scaffold自身のpadding
                ) {
                    Column {
                        CategoryFilterBar(
                            selectedFilter = uiState.selectedFilter,
                            onFilterSelected = viewModel::setFilter,
                        )

                        when {
                            uiState.isLoading && filteredPosts.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            filteredPosts.isEmpty() && !uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("まだ投稿がありません")
                                }
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (uiState.newBadgeCount > 0 && !isAtTop) {
                                        item {
                                            NewPostsBadge(
                                                count = uiState.newBadgeCount,
                                                onClick = { viewModel.revealPending() }
                                            )
                                        }
                                    }

                                    items(
                                        items = filteredPosts,
                                        key = { it.id }
                                    ) { post ->
                                        // Only this post's video should play if it's the active one
                                        val shouldPlayVideo = uiState.activeVideoPostId == post.id

                                        TimelinePostCard(
                                            post = post,
                                            onLikeClick = {
                                                viewModel.toggleLike(post, roomId)
                                            },
                                            onVideoClick = { url ->
                                                videoUrl = url
                                                showVideo = true
                                            },
                                            onImageClick = { index ->
                                                val images = post.media
                                                    .filter { it.type == MediaType.IMAGE }
                                                    .map { it.url }
                                                if (images.isNotEmpty()) {
                                                    galleryImages = images
                                                    galleryStartIndex = index
                                                    showGallery = true
                                                }
                                            },
                                            onPostClick = {
                                                selectedPostId = post.id
                                            },
                                            isVisible = shouldPlayVideo,
                                            isMuted = isMuted,
                                            onMuteToggle = { isMuted = !isMuted },
                                            modifier = Modifier
                                        )
                                    }

                                    if (uiState.isLoading && filteredPosts.isNotEmpty()) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
