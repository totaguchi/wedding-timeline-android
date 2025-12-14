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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ttaguchi.weddingtimeline.ui.MainScreen
import com.ttaguchi.weddingtimeline.ui.login.LoginScreen
import com.ttaguchi.weddingtimeline.ui.post.CreatePostScreen
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
    val navController = rememberNavController()

    val startDestination = if (session.isLoggedIn) "main" else "login"

    LaunchedEffect(session.isLoggedIn) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (session.isLoggedIn) {
            if (currentRoute == "login") {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else {
            if (currentRoute != "login") {
                navController.navigate("login") {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = { roomId ->
                    sessionViewModel.onJoinedRoom(roomId)
                }
            )
        }

        composable("main") {
            MainScreen(
                session = session,
                onCreatePost = {
                    navController.navigate("createPost")
                }
            )
        }

        composable("createPost") {
            CreatePostScreen(
                roomId = session.roomId ?: "",
                session = session, // Session全体を渡す
                onPostCreated = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}
