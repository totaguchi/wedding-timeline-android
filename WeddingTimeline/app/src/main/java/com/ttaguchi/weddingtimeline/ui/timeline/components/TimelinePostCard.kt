package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ttaguchi.weddingtimeline.model.Media
import com.ttaguchi.weddingtimeline.model.MediaType
import com.ttaguchi.weddingtimeline.model.PostTag
import com.ttaguchi.weddingtimeline.model.TimeLinePost

/**
 * X (Twitter) style post item.
 */
@Composable
fun TimelinePostCard(
    post: TimeLinePost,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCommentClick: (() -> Unit)? = null,
    onPostClick: (() -> Unit)? = null,
    onImageClick: ((Int) -> Unit)? = null,
    isVisible: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(
                enabled = onPostClick != null,
                onClick = { onPostClick?.invoke() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User icon (left side, fixed width)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8D4F8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = post.userIcon.ifEmpty { "👤" },
                    style = MaterialTheme.typography.titleLarge
                )
            }

            // Content area (right side, flexible width)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header: Name + Time + Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "· ${post.formattedCreatedAt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }

                    // Tag chip (smaller, inline)
                    TagChip(tag = post.tag)
                }

                // Content text
                if (post.content.isNotEmpty()) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                    )
                }

                // Media
                if (post.hasMedia) {
                    PostMediaView(
                        media = post.media,
                        isVisible = isVisible,
                        onImageClick = onImageClick,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Actions: Like, Comment (X style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    // Comment button
                    if (onCommentClick != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = onCommentClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "コメント",
                                    tint = Color(0xFF536471),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (post.replyCount > 0) {
                                Text(
                                    text = post.replyCount.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF536471),
                                )
                            }
                        }
                    }

                    // Like button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onLikeClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "いいね",
                                tint = if (post.isLiked) Color(0xFFF91880) else Color(0xFF536471),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (post.likeCount > 0) {
                            Text(
                                text = post.likeCount.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (post.isLiked) Color(0xFFF91880) else Color(0xFF536471),
                            )
                        }
                    }
                }
            }
        }

        // Divider (X style)
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFFEFF3F4)
        )
    }
}

@Composable
private fun TagChip(tag: PostTag) {
    val (backgroundColor, textColor) = when (tag) {
        PostTag.CEREMONY -> Pair(Color(0xFFE8D4F8), Color(0xFF9C27B0))
        PostTag.RECEPTION -> Pair(Color(0xFFFFE0B2), Color(0xFFFF6F00))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = tag.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
        )
    }
}

/**
 * Display media (images or video) based on Swift's MediaView pattern.
 */
@Composable
private fun PostMediaView(
    media: List<Media>,
    isVisible: Boolean,
    onImageClick: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val images = media.filter { it.type == MediaType.IMAGE }
    val video = media.firstOrNull { it.type == MediaType.VIDEO }

    Column(modifier = modifier) {
        when {
            // Video takes priority (Swift pattern)
            video != null -> {
                if (isVisible) {
                    AutoPlayVideoPlayer(
                        url = video.url,
                        isVisible = isVisible,
                    )
                } else {
                    MediaVideoItem(
                        url = video.url,
                        duration = video.duration,
                        autoPlay = false,
                    )
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
 * Display images in grid or single layout (matches Swift's PostImagesView).
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
 * Single image item with loading state (matches Swift's LazyImage with ShimmerPlaceholder).
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

@Composable
private fun MediaVideoItem(url: String, duration: Double?, autoPlay: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("▶ 動画", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            duration?.let {
                Text(
                    text = formatDuration(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val minutes = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%d:%02d".format(minutes, secs)
}