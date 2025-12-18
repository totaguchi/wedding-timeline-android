package com.ttaguchi.weddingtimeline.ui.bestpost

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ttaguchi.weddingtimeline.domain.model.MediaType
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import com.ttaguchi.weddingtimeline.ui.timeline.components.FullscreenImageGallery
import com.ttaguchi.weddingtimeline.ui.timeline.components.TimelinePostCard
import com.ttaguchi.weddingtimeline.ui.timeline.components.VideoPlayerScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestPostScreen(
    roomId: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onToggleBottomBar: (Boolean) -> Unit = {},
    viewModel: BestPostViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var galleryImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var galleryStartIndex by remember { mutableIntStateOf(0) }
    var showGallery by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var showVideo by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(true) }
    val hideBottomBar = showVideo || showGallery

    LaunchedEffect(roomId) { viewModel.fetchBestPosts(roomId) }
    LaunchedEffect(hideBottomBar) { onToggleBottomBar(hideBottomBar) }

    when {
        showVideo && videoUrl != null -> {
            VideoPlayerScreen(
                videoUrl = videoUrl!!,
                onBackClick = { showVideo = false; videoUrl = null }
            )
        }
        showGallery -> {
            FullscreenImageGallery(
                images = galleryImages,
                startIndex = galleryStartIndex,
                onDismiss = { showGallery = false }
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshBestPosts(roomId) },
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                when {
                    uiState.isLoading && uiState.posts.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.posts.isEmpty() && !uiState.isLoading -> {
                        EmptyBestPost()
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF8E9F3)),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            item { HeaderArea() }

                            itemsIndexed(uiState.posts, key = { _, item -> item.id }) { index, post ->
                                RankedCard(
                                    rank = index + 1,
                                    post = post,
                                    onLike = { viewModel.toggleLike(post, roomId) },
                                    onVideo = { url ->
                                        videoUrl = url
                                        showVideo = true
                                    },
                                    onImage = { start ->
                                        val images = post.media.filter { it.type == MediaType.IMAGE }.map { it.url }
                                        if (images.isNotEmpty()) {
                                            galleryImages = images
                                            galleryStartIndex = start
                                            showGallery = true
                                        }
                                    },
                                    isMuted = isMuted,
                                    onMuteToggle = { isMuted = !isMuted }
                                )
                            }

                            if (uiState.isLoading && uiState.posts.isNotEmpty()) {
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

@Composable
private fun EmptyBestPost() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("👑")
            Text("まだベストポストがありません")
            Text("いいねが多い投稿がここに表示されます")
        }
    }
}

@Composable
private fun HeaderArea() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF1F7), Color(0xFFFFE3EF))
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "👑 ベストポスト 👑",
            color = Color(0xFFE91E63),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "みんなに愛されている投稿です",
            color = Color(0xFF7B7B7B),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RankedCard(
    rank: Int,
    post: TimeLinePost,
    onLike: () -> Unit,
    onVideo: (String) -> Unit,
    onImage: (Int) -> Unit,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
) {
    val cardColor = when (rank) {
        1 -> Color(0xFFFFF9C4)
        2 -> Color(0xFFF1F5FF)
        else -> Color.White
    }
    val badgeColor = when (rank) {
        1 -> Color(0xFFFEC107)
        2 -> Color(0xFFB0BEC5)
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.ttaguchi.weddingtimeline.ui.common.resolveAvatarResId(post.userIcon)),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDEDED))
                    )
                    Column {
                        Text(text = post.authorName, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(text = "@${post.authorId}", color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Text(
                    text = "#${rank}位",
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(text = post.content, color = Color.Black)

            // Media/like/comment actions reuse timeline card for consistency
            TimelinePostCard(
                post = post,
                onLikeClick = onLike,
                onVideoClick = onVideo,
                onImageClick = onImage,
                isVisible = false,
                isMuted = isMuted,
                onMuteToggle = onMuteToggle,
                modifier = Modifier.background(Color.Transparent)
            )
        }
    }
}
