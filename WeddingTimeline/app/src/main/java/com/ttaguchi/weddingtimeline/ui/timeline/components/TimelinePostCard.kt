package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttaguchi.weddingtimeline.model.MediaType
import com.ttaguchi.weddingtimeline.model.PostTag
import com.ttaguchi.weddingtimeline.model.TimeLinePost

/**
 * Card displaying a single timeline post.
 */
@Composable
fun TimelinePostCard(
    post: TimeLinePost,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCommentClick: (() -> Unit)? = null,
    onPostClick: (() -> Unit)? = null,
    isVisible: Boolean = true,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f)
            )
            .then(
                if (onPostClick != null) {
                    Modifier.clickable(onClick = onPostClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: User info + Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // User info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // User icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8D4F8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.userIcon.ifEmpty { "👤" },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // User name
                    Column {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = post.formattedCreatedAt(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Tag chip
                TagChip(tag = post.tag)
            }

            // Content
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            // Media
            if (post.hasMedia) {
                PostMediaGrid(
                    media = post.media,
                    isVisible = isVisible
                )
            }

            // Actions: Like, Comment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Like button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "いいね",
                            tint = if (post.isLiked) Color(0xFFE91E63) else Color.Gray,
                        )
                    }
                    Text(
                        text = post.likeCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (post.isLiked) Color(0xFFE91E63) else Color.Gray,
                    )
                }

                // Comment button
                if (onCommentClick != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onCommentClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = "コメント",
                                tint = Color.Gray,
                            )
                        }
                        Text(
                            text = post.replyCount.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = tag.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun PostMediaGrid(
    media: List<com.ttaguchi.weddingtimeline.model.Media>,
    isVisible: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        media.forEach { item ->
            when (item.type) {
                MediaType.IMAGE -> {
                    MediaImageItem(url = item.url)
                }
                MediaType.VIDEO -> {
                    if (isVisible) {
                        AutoPlayVideoPlayer(
                            url = item.url,
                            isVisible = isVisible,
                        )
                    } else {
                        MediaVideoItem(
                            url = item.url,
                            duration = item.duration,
                            autoPlay = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaImageItem(url: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Replace with AsyncImage from Coil
        // AsyncImage(
        //     model = url,
        //     contentDescription = "投稿画像",
        //     modifier = Modifier.fillMaxSize(),
        //     contentScale = ContentScale.Crop
        // )
        Text("画像", color = Color.Gray)
    }
}

@Composable
private fun MediaVideoItem(url: String, duration: Double?, autoPlay: Boolean = true) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Implement video player
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("動画", color = Color.White)
            duration?.let {
                Text(
                    text = formatDuration(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
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