package com.ttaguchi.weddingtimeline.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import com.ttaguchi.weddingtimeline.domain.model.Session
import com.ttaguchi.weddingtimeline.ui.bestpost.BestPostScreen
import com.ttaguchi.weddingtimeline.ui.settings.SettingsScreen
import com.ttaguchi.weddingtimeline.ui.timeline.TimelineScreen

@Composable
fun MainScreen(
    session: Session,
    modifier: Modifier = Modifier,
    onCreatePost: () -> Unit,
    onBackToLogin: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var hideBottomBar by remember { mutableStateOf(false) }

    val roomId = session.roomId
    if (roomId == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text("部屋情報を取得できませんでした")
                Text("ログイン情報を確認して、もう一度お試しください")
                Button(
                    onClick = onBackToLogin,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("ログインに戻る")
                }
            }
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
        },
        floatingActionButton = {
            // TimelineScreenでのみFABを表示
            if (selectedTab == 0 && !hideBottomBar) {
                FloatingActionButton(onClick = onCreatePost) {
                    Icon(Icons.Default.Add, contentDescription = "投稿作成")
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
                onToggleBottomBar = { hide: Boolean -> hideBottomBar = hide }
            )
            1 -> BestPostScreen(
                roomId = roomId,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                onToggleBottomBar = { hide: Boolean -> hideBottomBar = hide }
            )
            2 -> SettingsScreen(
                session = session,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onSignedOut = onBackToLogin,
                onAccountDeleted = onBackToLogin
            )
        }
    }
}

enum class TabItem(val title: String, val icon: ImageVector) {
    TIMELINE("タイムライン", Icons.Default.Home),
    BEST_POST("ベストポスト", Icons.Default.Star),
    SETTINGS("設定", Icons.Default.Settings),
}
