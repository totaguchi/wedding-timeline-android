package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Badge showing number of new posts (X/Twitter-style).
 */
@Composable
fun NewPostsBadge(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = count > 0,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.1f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE8D4F8),
                                Color(0xFFB794F4),
                            )
                        )
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = Color.White,
                )
                Text(
                    text = "新しい投稿 $count 件",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}