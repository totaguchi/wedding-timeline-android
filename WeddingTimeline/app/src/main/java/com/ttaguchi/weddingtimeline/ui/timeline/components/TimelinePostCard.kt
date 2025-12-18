package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ttaguchi.weddingtimeline.domain.model.PostTag
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import androidx.compose.ui.res.painterResource
import com.ttaguchi.weddingtimeline.ui.common.resolveAvatarResId

/**
 * X (Twitter) style post item.
 * Media rendering is delegated to PostMediaView.kt to keep this file small.
 */
@Composable
fun TimelinePostCard(
    post: TimeLinePost,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCommentClick: (() -> Unit)? = null,
    onPostClick: (() -> Unit)? = null,
    onImageClick: ((Int) -> Unit)? = null,
    onVideoClick: ((String) -> Unit)? = null,
    isVisible: Boolean = true,
    isMuted: Boolean = true,
    onMuteToggle: (() -> Unit)? = null,
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
            // User icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8D4F8)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = resolveAvatarResId(post.userIcon)),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Content area
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header
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
                        isMuted = isMuted,
                        onImageClick = onImageClick,
                        onVideoClick = onVideoClick,
                        onMuteToggle = onMuteToggle,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Actions
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
