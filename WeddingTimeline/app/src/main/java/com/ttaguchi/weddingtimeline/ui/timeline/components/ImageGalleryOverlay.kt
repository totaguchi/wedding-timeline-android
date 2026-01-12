package com.ttaguchi.weddingtimeline.ui.timeline.components

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Fullscreen image gallery (mimics iOS fullScreenCover).
 */
@Composable
fun FullscreenImageGallery(
    images: List<String>,
    startIndex: Int,
    onDismiss: () -> Unit,
) {
    if (images.isEmpty()) return

    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, images.lastIndex),
        pageCount = { images.size }
    )

    // Simple lifecycle hook to close if lifecycle stops (optional safety)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onDismiss()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(state = pagerState) { page ->
            MediaImageItem(
                url = images[page],
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                height = 0 // ignored when fillMaxSize
            )
        }

        // Top bar with close and menu buttons
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusBarSpacing(), end = 16.dp)
        ) {
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "閉じる",
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }

            // More options button
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "メニュー",
                    tint = Color.White,
                    modifier = Modifier.padding(2.dp)
                )
            }

            // Dropdown menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("画像を保存") },
                    onClick = {
                        showMenu = false
                        val currentImageUrl = images[pagerState.currentPage]
                        downloadImage(context, currentImageUrl)
                    }
                )
            }
        }

        // Page indicator at bottom
        AssistChip(
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
            colors = AssistChipDefaults.assistChipColors(
                containerColor = Color.White.copy(alpha = 0.25f),
                labelColor = Color.White
            )
        )
    }
}

// Simple spacer for status bar height without needing WindowInsets (keeps composable decoupled)
@Composable
private fun statusBarSpacing(): Dp = 24.dp

/**
 * Download image using DownloadManager.
 */
private fun downloadImage(context: Context, imageUrl: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setTitle("画像をダウンロード中")
            .setDescription("Wedding Timeline")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_PICTURES,
                "WeddingTimeline_${System.currentTimeMillis()}.jpg"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

        println("[ImageGallery] Download started for: $imageUrl")
    } catch (e: Exception) {
        println("[ImageGallery] Download failed: ${e.message}")
        e.printStackTrace()
    }
}
