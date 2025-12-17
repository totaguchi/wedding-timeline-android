package com.ttaguchi.weddingtimeline.ui.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.domain.model.TimeLinePost
import com.ttaguchi.weddingtimeline.ui.timeline.components.TimelinePostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: TimeLinePost,
    onBackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onImageClick: (Int) -> Unit,
    onVideoClick: (String) -> Unit,
    onDelete: () -> Unit,
    onReport: (String) -> Unit,
    onMuteAuthor: (Boolean) -> Unit,
    isAuthorMuted: Boolean,
    isVideoVisible: Boolean,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val isMyPost = FirebaseAuth.getInstance().currentUser?.uid == post.authorId

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = "ポスト") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "その他")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isMyPost) {
                            DropdownMenuItem(
                                text = { Text("ポストを削除") },
                                onClick = {
                                    showMenu = false
                                    confirmDelete = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("ポストを報告") },
                            onClick = {
                                showMenu = false
                                showReportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isAuthorMuted) "ミュートを解除" else "このユーザーをミュート") },
                            onClick = {
                                showMenu = false
                                onMuteAuthor(!isAuthorMuted)
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            TimelinePostCard(
                post = post,
                onLikeClick = onLikeClick,
                onImageClick = onImageClick,
                onVideoClick = onVideoClick,
                onPostClick = null,
                isVisible = isVideoVisible,
                isMuted = isMuted,
                onMuteToggle = onMuteToggle,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("削除する") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") }
            },
            title = { Text("ポストを削除しますか？") },
            text = { Text("この操作は取り消せません。") }
        )
    }

    if (showReportDialog) {
        val reasons = listOf("スパム/宣伝", "不適切な表現", "プライバシーの侵害", "その他")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("キャンセル") }
            },
            title = { Text("報告の理由を選択") },
            text = {
                Column {
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                onReport(reason)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason)
                        }
                    }
                }
            }
        )
    }
}
