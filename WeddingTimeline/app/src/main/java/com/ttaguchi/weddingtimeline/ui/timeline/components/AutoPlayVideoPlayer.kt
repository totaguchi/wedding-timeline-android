package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Auto-play video player that starts/stops based on visibility.
 * Only ONE video should be playing at a time (X/Twitter behavior).
 */
@Composable
fun AutoPlayVideoPlayer(
    url: String,
    isVisible: Boolean,
    isMuted: Boolean,
    onVideoClick: (() -> Unit)? = null,
    onProgress: ((currentMs: Long, totalMs: Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // Create ExoPlayer instance - unique per URL
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            repeatMode = Player.REPEAT_MODE_ALL
            volume = if (isMuted) 0f else 1f
            playWhenReady = false // Don't auto-play on creation
        }
    }

    // Update mute state
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Control playback based on visibility
    LaunchedEffect(isVisible) {
        if (isVisible) {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } else {
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
            exoPlayer.seekTo(0) // Reset to beginning when not visible
        }
    }

    // Progress callbacks while visible
    LaunchedEffect(isVisible, onProgress) {
        if (!isVisible || onProgress == null) return@LaunchedEffect
        while (true) {
            val total = exoPlayer.duration.coerceAtLeast(0L)
            val current = exoPlayer.currentPosition.coerceAtLeast(0L)
            onProgress.invoke(current, total)
            kotlinx.coroutines.delay(250)
        }
    }

    // Clean up when disposed
    DisposableEffect(url) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = onVideoClick != null) {
                onVideoClick?.invoke()
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // FIT mode to show entire video without cropping
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { playerView ->
                // Update player reference to ensure correct binding
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
