package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttaguchi.weddingtimeline.domain.model.MediaType
import com.ttaguchi.weddingtimeline.ui.timeline.components.CategoryFilterBar
import com.ttaguchi.weddingtimeline.ui.timeline.components.MediaImageItem
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
    val hideBottomBar = showVideo

    val filteredPosts = remember(uiState.posts, uiState.selectedFilter) {
        viewModel.getFilteredPosts()
    }

    // Detect visible items
    val visibleItemIndices by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.map { it.index }.toSet()
        }
    }

    // Calculate visible range (middle 50% of screen)
    val density = LocalDensity.current
    val screenHeightPx = with(density) { 
        listState.layoutInfo.viewportSize.height
    }
    val autoPlayRangeStart = screenHeightPx * 0.25f // Top 25%
    val autoPlayRangeEnd = screenHeightPx * 0.75f   // Bottom 75%

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

    LaunchedEffect(hideBottomBar) {
        onToggleBottomBar(hideBottomBar)
    }

    // Show video player fullscreen (without Scaffold to hide bottom bar)
    if (showVideo && videoUrl != null) {
        VideoPlayerScreen(
            videoUrl = videoUrl!!,
            onBackClick = {
                showVideo = false
                videoUrl = null
            }
        )
    } else if (showGallery) {
        // Show image gallery fullscreen (without Scaffold)
        ImageGalleryOverlay(
            images = galleryImages,
            startIndex = galleryStartIndex,
            onDismiss = { showGallery = false }
        )
    } else {
        // Main timeline UI with Scaffold (shows bottom bar in parent)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("タイムライン") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreatePost) {
                Icon(Icons.Default.Add, contentDescription = "投稿作成")
            }
        }
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { 
                println("[TimelineScreen] Pull to refresh triggered")
                viewModel.refreshHead(roomId) 
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
                Column {
                    // Filter bar
                    CategoryFilterBar(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = viewModel::setFilter,
                    )

                    // Posts list
                    when {
                        uiState.isLoading && filteredPosts.isEmpty() -> {
                            println("[TimelineScreen] Showing initial loading")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        filteredPosts.isEmpty() && !uiState.isLoading -> {
                            println("[TimelineScreen] No posts available")
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("まだ投稿がありません")
                            }
                        }

                        else -> {
                            println("[TimelineScreen] Showing ${filteredPosts.size} posts")
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // New badge
                                if (uiState.newBadgeCount > 0 && !isAtTop) {
                                    item {
                                        NewPostsBadge(
                                            count = uiState.newBadgeCount,
                                            onClick = {
                                                viewModel.revealPending()
                                            }
                                        )
                                    }
                                }

                                // Posts
                                items(
                                    items = filteredPosts,
                                    key = { it.id }
                                ) { post ->
                                    // Calculate if this item is in auto-play range
                                    val itemInfo = listState.layoutInfo.visibleItemsInfo
                                        .find { it.key == post.id }
                                    
                                    val isInAutoPlayRange = itemInfo?.let { info ->
                                        val itemTop = info.offset.toFloat()
                                        val itemBottom = itemTop + info.size
                                        val itemCenter = (itemTop + itemBottom) / 2f
                                        
                                        itemCenter in autoPlayRangeStart..autoPlayRangeEnd
                                    } ?: false

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
                                        isVisible = isInAutoPlayRange,
                                        isMuted = isMuted,
                                        onMuteToggle = { isMuted = !isMuted },
                                        modifier = Modifier
                                    )
                                }

                                // Loading indicator at bottom
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageGalleryOverlay(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(state = pagerState) { page ->
            MediaImageItem(
                url = images[page],
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                height = 0
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "閉じる",
                tint = Color.White
            )
        }

        androidx.compose.material3.AssistChip(
            onClick = {},
            label = {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                labelColor = Color.White
            )
        )
    }
}
