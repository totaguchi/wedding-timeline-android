package com.ttaguchi.weddingtimeline.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ttaguchi.weddingtimeline.domain.model.Session
import com.ttaguchi.weddingtimeline.ui.timeline.TimelineScreen

@Composable
fun MainScreen(
    session: Session,
    modifier: Modifier = Modifier,
    onCreatePost: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var hideBottomBar by remember { mutableStateOf(false) } // 型を明示的に指定

    // roomIdがnullの場合はローディング表示
    val roomId = session.roomId
    if (roomId == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar {
                    TabItem.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> TimelineScreen(
                roomId = roomId,
                onCreatePost = onCreatePost,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                onToggleBottomBar = { hide: Boolean -> hideBottomBar = hide } // ラムダの型を明示
            )
            1 -> BestPostScreen(modifier = Modifier.padding(paddingValues))
            2 -> SettingsScreen(modifier = Modifier.padding(paddingValues))
        }
    }
}

enum class TabItem(val title: String, val icon: ImageVector) {
    TIMELINE("タイムライン", Icons.Default.Home),
    BEST_POST("ベストポスト", Icons.Default.Star),
    SETTINGS("設定", Icons.Default.Settings),
}

@Composable
fun BestPostScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("ベストポスト画面（実装予定）")
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("設定画面（実装予定）")
    }
}
