package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.ttaguchi.weddingtimeline.domain.model.Media
import com.ttaguchi.weddingtimeline.domain.model.MediaType

/**
 * Media area for timeline posts (images or video).
 */
@Composable
fun PostMediaView(
    media: List<Media>,
    isVisible: Boolean,
    isMuted: Boolean,
    onImageClick: ((Int) -> Unit)?,
    onVideoClick: ((String) -> Unit)?,
    onMuteToggle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val images = media.filter { it.type == MediaType.IMAGE }
    val video = media.firstOrNull { it.type == MediaType.VIDEO }
    var remaining by remember(video?.id) { mutableStateOf(video?.duration) }

    Column(modifier = modifier) {
        when {
            // Video takes priority
            video != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Auto-play video when visible
                    AutoPlayVideoPlayer(
                        url = video.url,
                        isVisible = isVisible,
                        isMuted = isMuted,
                        onVideoClick = { onVideoClick?.invoke(video.url) },
                        onProgress = { currentMs, totalMs ->
                            val total = if (totalMs > 0) totalMs / 1000.0 else (video.duration ?: 0.0)
                            val left = (total * 1000 - currentMs).coerceAtLeast(0.0) / 1000.0
                            remaining = left
                        }
                    )

                    // Duration overlay (bottom-left)
                    remaining?.let { duration ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mute button (bottom-right)
                    if (onMuteToggle != null) {
                        IconButton(
                            onClick = onMuteToggle,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "ミュート解除" else "ミュート",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            // Images
            images.isNotEmpty() -> {
                PostImagesView(
                    images = images,
                    onImageClick = onImageClick,
                )
            }
        }
    }
}

/**
 * Display images in grid or single layout.
 */
@Composable
private fun PostImagesView(
    images: List<Media>,
    onImageClick: ((Int) -> Unit)?,
) {
    when {
        // Single image - full width with 16:9 aspect ratio
        images.size == 1 -> {
            val image = images[0]
            PostImageItem(
                url = image.url,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick?.invoke(0) },
                contentScale = ContentScale.Crop,
            )
        }
        // 2 images - side by side
        images.size == 2 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                images.forEachIndexed { index, image ->
                    PostImageItem(
                        url = image.url,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick?.invoke(index) },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        // 3 images - 1 on left, 2 stacked on right
        images.size == 3 -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                PostImageItem(
                    url = images[0].url,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick?.invoke(0) },
                    contentScale = ContentScale.Crop,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    PostImageItem(
                        url = images[1].url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick?.invoke(1) },
                        contentScale = ContentScale.Crop,
                    )
                    PostImageItem(
                        url = images[2].url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick?.invoke(2) },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        // 4+ images - 2x2 grid
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(minOf(images.size, 4)) { index ->
                    Box {
                        PostImageItem(
                            url = images[index].url,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick?.invoke(index) },
                            contentScale = ContentScale.Crop,
                        )
                        // Show "+N" overlay for 4th image if more than 4 images
                        if (index == 3 && images.size > 4) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${images.size - 4}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single image item with loading state.
 */
@Composable
private fun PostImageItem(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = "投稿画像",
        modifier = modifier,
        contentScale = contentScale,
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEFF3F4)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF536471),
                        strokeWidth = 2.dp
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFEFF3F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF536471),
                    )
                }
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}
