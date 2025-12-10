package com.ttaguchi.weddingtimeline.ui.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Auto-play video player for timeline (muted, loops).
 * Pauses when not visible on screen.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AutoPlayVideoPlayer(
    url: String,
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    height: Int = 200,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycle by remember { mutableStateOf(Lifecycle.Event.ON_CREATE) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(url)
            setMediaItem(mediaItem)
            prepare()
            volume = 0f // Muted
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    // Observe lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycle = event
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Control playback based on visibility and lifecycle
    LaunchedEffect(isVisible, lifecycle) {
        when {
            lifecycle == Lifecycle.Event.ON_PAUSE -> {
                exoPlayer.pause()
            }
            lifecycle == Lifecycle.Event.ON_RESUME && isVisible -> {
                exoPlayer.play()
            }
            isVisible -> {
                exoPlayer.play()
            }
            else -> {
                exoPlayer.pause()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    controllerAutoShow = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}