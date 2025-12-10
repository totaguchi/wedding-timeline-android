package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttaguchi.weddingtimeline.ui.timeline.components.CategoryFilterBar
import com.ttaguchi.weddingtimeline.ui.timeline.components.NewPostsBadge
import com.ttaguchi.weddingtimeline.ui.timeline.components.TimelinePostCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    roomId: String,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = viewModel(),
) {
    println("[TimelineScreen] Rendering with roomId=$roomId")
    
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredPosts = remember(uiState.posts, uiState.selectedFilter) {
        viewModel.getFilteredPosts()
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
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { 
                println("[TimelineScreen] Pull to refresh triggered")
                viewModel.refreshHead(roomId) 
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                                TimelinePostCard(
                                    post = post,
                                    onLikeClick = {
                                        viewModel.toggleLike(post, roomId)
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // Loading indicator at bottom
                            if (uiState.isLoading && filteredPosts.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
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