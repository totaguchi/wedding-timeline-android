package com.ttaguchi.weddingtimeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ttaguchi.weddingtimeline.ui.MainScreen
import com.ttaguchi.weddingtimeline.ui.login.LoginScreen
import com.ttaguchi.weddingtimeline.ui.theme.WeddingTimelineTheme
import com.ttaguchi.weddingtimeline.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeddingTimelineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}

@Composable
private fun AppContent(
    sessionViewModel: SessionViewModel,
) {
    val session by sessionViewModel.session.collectAsState()

    if (session.isLoggedIn) {
        // ログイン済み: メイン画面を表示
        MainScreen(session = session)
    } else {
        // 未ログイン: ログイン画面を表示
        LoginScreen(
            onLoginSuccess = { roomId ->
                sessionViewModel.onJoinedRoom(roomId)
            }
        )
    }
}
