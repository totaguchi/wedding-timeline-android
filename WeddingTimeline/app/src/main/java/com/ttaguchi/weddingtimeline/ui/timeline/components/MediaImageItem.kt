package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/**
 * Display image using Coil library with loading state.
 */
@Composable
fun MediaImageItem(
    url: String,
    modifier: Modifier = Modifier,
    height: Int = 200,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = "投稿画像",
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Crop,
    ) {
        val state = painter.state
        when (state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "画像の読み込みに失敗しました",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            else -> {
                SubcomposeAsyncImageContent()
            }
        }
    }
}