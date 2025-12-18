package com.ttaguchi.weddingtimeline.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttaguchi.weddingtimeline.BuildConfig
import com.ttaguchi.weddingtimeline.domain.model.Session
import com.ttaguchi.weddingtimeline.ui.common.resolveAvatarResId
import kotlinx.coroutines.launch

private const val LEGAL_URL = "https://weddingtimeline-d67a6.web.app/legal.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    session: Session,
    modifier: Modifier = Modifier,
    onSignedOut: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showLegalSheet by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle error and success messages
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearMessages()
            }
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = msg,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearMessages()
            }
        }
    }

    fun openLegalOnline() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(LEGAL_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar("ブラウザを開けませんでした")
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileHeader(session = session)
            Spacer(modifier = Modifier.height(12.dp))

            SettingsCard {
                SettingRow(
                    icon = Icons.Default.Info,
                    title = "アプリについて",
                    subtitle = "バージョン情報",
                    onClick = { showAboutSheet = true }
                )
                Divider()
                SettingRow(
                    icon = Icons.Default.Link,
                    title = "規約・プライバシー",
                    subtitle = "ポリシーと利用条件",
                    onClick = { 
                        val online = checkOnline(context)
                        if (online) {
                            openLegalOnline()
                        } else {
                            isOnline = false
                            showLegalSheet = true
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ログアウト")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("アカウント削除")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        // Snackbarを下部に配置
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showLogoutDialog) {
        ConfirmDialog(
            title = "ログアウトしますか？",
            message = "ルームからログアウトします。よろしいですか？",
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                viewModel.signOut(
                    context = context,
                    roomId = session.roomId,
                    onSuccess = onSignedOut
                )
            }
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "アカウントを削除しますか？",
            message = "投稿・プロフィール・いいね等の関連データが削除され、復元できません。よろしいですか？",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount(
                    context = context,
                    roomId = session.roomId,
                    onSuccess = onAccountDeleted
                )
            },
            confirmText = "削除する"
        )
    }

    if (uiState.isLoading) {
        LoadingOverlay(text = if (showDeleteDialog) "削除しています…" else "処理中…")
    }

    if (showAboutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAboutSheet = false },
            sheetState = sheetState
        ) {
            AboutContent()
        }
    }

    if (showLegalSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLegalSheet = false },
            sheetState = sheetState
        ) {
            LegalContent(
                isOnline = isOnline,
                onOpenOnline = { openLegalOnline() }
            )
        }
    }
}

@Composable
private fun ProfileHeader(session: Session) {
    val username = session.member?.username ?: session.user?.name ?: "未設定"
    val icon = session.member?.userIcon ?: session.user?.icon
    val tag = session.uid.takeIf { it.isNotEmpty() }?.take(4)?.let { "@${it}" } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8D4F8),
                        Color(0xFFFCE4EC)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(Color.White, CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resolveAvatarResId(icon)),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (tag.isNotEmpty()) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Default.Link,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String = "OK",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

@Composable
private fun LoadingOverlay(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator()
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AboutContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("アプリについて", style = MaterialTheme.typography.titleMedium)
        Text("結婚式の思い出をみんなで共有する、参列者向けのタイムラインアプリです。")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("アプリ名")
            Text("WeddingTimeline", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("バージョン")
            Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LegalContent(
    isOnline: Boolean,
    onOpenOnline: () -> Unit
) {
    val offlineText = remember { DEFAULT_OFFLINE_LEGAL }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("規約・プライバシー", style = MaterialTheme.typography.titleMedium)

        if (isOnline) {
            Text(
                text = "オンライン版を表示中",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        loadUrl(LEGAL_URL)
                    }
                }
            )
            TextButton(onClick = onOpenOnline) {
                Text("ブラウザで開く")
            }
        } else {
            Text(
                text = "オフライン要約版を表示しています",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.height(320.dp)
            ) {
                item {
                    Text(
                        text = offlineText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            TextButton(onClick = onOpenOnline) {
                Text("オンライン版を開く")
            }
        }
    }
}

private fun checkOnline(context: android.content.Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private val DEFAULT_OFFLINE_LEGAL = """
# 利用規約・プライバシーポリシー（オフライン要約）
最終更新: 2025年12月18日

これは通信できない場合に表示する要約版です。

## 利用規約（要点）
- 本アプリはルーム単位の限定タイムラインを提供します。
- 匿名認証（Firebase Authentication）でサインインします。表示名には識別のため `@UID先頭4桁` が付く場合があります。
- ルーム入室には roomId / roomKey が必要です。入室キーの共有は禁止します。
- 投稿の著作権はユーザーに帰属しますが、アプリ運用上の保存・表示・最適化のための利用を許諾いただきます。
- 法令違反・権利侵害・不適切コンテンツは削除やアカウント制限の対象となります。

## プライバシー（要点）
- 収集する主な情報：Firebase UID、表示名・アイコン、参加ルームID、投稿、いいね／通報等の操作、技術ログなど。
- 入室キーは Firestore の `roomSecrets/{roomId}` に保存され、クライアントから直接は読めないルールで保護します。端末ローカルには保存しません（保存する場合はアプリ内で明示）。
- 利用目的：タイムライン提供、不正防止、品質改善、サポート対応。
- 第三者提供：Firebase（Auth/Firestore/Storage 等）を利用。法令に基づく場合を除き本人同意なく第三者提供しません。
- 保存期間：目的達成に必要な期間のみ保持し、不要になった情報は適切に削除・匿名化します。退会はアプリ内から申請可能です。

## お問い合わせ
- 運営者名：田口 友暉（個人開発）
- 連絡先　：ttaguchidevelop@gmail.com
"""
