package com.ttaguchi.weddingtimeline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ttaguchi.weddingtimeline.data.UserPreferences
import com.ttaguchi.weddingtimeline.ui.legal.TermsAgreementScreen
import com.ttaguchi.weddingtimeline.ui.MainScreen
import com.ttaguchi.weddingtimeline.ui.login.LoginScreen
import com.ttaguchi.weddingtimeline.ui.post.CreatePostScreen
import com.ttaguchi.weddingtimeline.ui.theme.WeddingTimelineTheme
import com.ttaguchi.weddingtimeline.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // アプリ起動時にセッション状態を初期化（Swift の .task { await session.bootstrapOnLaunch() } に相当）
        sessionViewModel.bootstrapOnLaunch(applicationContext)

        setContent {
            WeddingTimelineTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeddingTimelineApp(sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}

@Composable
private fun WeddingTimelineApp(
    sessionViewModel: SessionViewModel,
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val acceptedTerms by userPreferences.acceptedTerms.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    if (!acceptedTerms) {
        TermsAgreementScreen(
            onAgree = {
                scope.launch {
                    userPreferences.setAcceptedTerms(true)
                }
            }
        )
        return
    }

    AppContent(sessionViewModel = sessionViewModel)
}

@Composable
private fun AppContent(
    sessionViewModel: SessionViewModel,
) {
    val context = LocalContext.current
    val session by sessionViewModel.session.collectAsState()
    val isInitialized by sessionViewModel.isInitialized.collectAsState()
    val navController = rememberNavController()

    // 初期化中はローディング表示
    if (!isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // isLoggedIn（member != null）かつ roomId が揃った時のみメイン画面に遷移
    val shouldShowMain = session.isLoggedIn && session.roomId != null
    val startDestination = if (shouldShowMain) "main" else "login"

    LaunchedEffect(shouldShowMain) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (shouldShowMain) {
            if (currentRoute == "login") {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (!session.isLoggedIn) {
            if (currentRoute == "main" || currentRoute == "createPost") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
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
                    sessionViewModel.onJoinedRoom(context, roomId)
                }
            )
        }

        composable("main") {
            MainScreen(
                session = session,
                onCreatePost = {
                    navController.navigate("createPost")
                },
                onBackToLogin = {
                    sessionViewModel.signOut(context)
                }
            )
        }

        composable("createPost") {
            CreatePostScreen(
                roomId = session.roomId ?: "",
                session = session,
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
