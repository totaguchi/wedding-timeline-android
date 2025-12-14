package com.ttaguchi.weddingtimeline.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.ttaguchi.weddingtimeline.domain.model.PostTag
import com.ttaguchi.weddingtimeline.domain.model.Session
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    roomId: String,
    session: Session, // Session全体を受け取る
    onPostCreated: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreatePostViewModel = viewModel(
        factory = CreatePostViewModelFactory(roomId, session)
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var text by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(PostTag.CEREMONY) }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Media picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4),
        onResult = { uris ->
            selectedMediaUris = uris
        }
    )

    // Show error messages
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Handle post creation success
    LaunchedEffect(viewModel.postCreated) {
        if (viewModel.postCreated) {
            onPostCreated()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✨",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "新規投稿",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = Color(0xFF536471)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomBar(
                // メディアがあるかテキストが入力されていれば有効
                canSubmit = selectedMediaUris.isNotEmpty() || text.isNotBlank(),
                isSubmitting = isSubmitting,
                onSubmit = {
                    // 連打防止: すでに送信中の場合は何もしない
                    if (isSubmitting) return@BottomBar
                    
                    isSubmitting = true
                    scope.launch {
                        try {
                            viewModel.createPost(
                                content = text,
                                tag = selectedTag,
                                mediaUris = selectedMediaUris,
                                context = context
                            )
                        } catch (e: Exception) {
                            // エラーが発生したら送信中フラグを解除
                            isSubmitting = false
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF0F5),
                            Color(0xFFFFF5F7)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Text input section
                TextInputSection(
                    text = text,
                    onTextChange = { text = it }
                )

                // Media picker section
                MediaPickerSection(
                    selectedMediaCount = selectedMediaUris.size,
                    onPickMedia = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )

                // Tag picker section
                TagPickerSection(
                    selectedTag = selectedTag,
                    onTagSelected = { selectedTag = it }
                )

                // Media grid
                if (selectedMediaUris.isNotEmpty()) {
                    MediaGrid(
                        mediaUris = selectedMediaUris,
                        context = context,
                        onRemove = { uri ->
                            selectedMediaUris = selectedMediaUris.filter { it != uri }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun TextInputSection(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = {
                Text(
                    "今の気持ちを共有しましょう…",
                    color = Color(0xFF536471)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun MediaPickerSection(
    selectedMediaCount: Int,
    onPickMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPickMedia,
            enabled = selectedMediaCount < 4,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF536471)
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "メディアを選択",
                modifier = Modifier.size(20.dp)
            )
        }

        if (selectedMediaCount > 0) {
            Text(
                text = "$selectedMediaCount / 4",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF536471),
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSection(
    selectedTag: PostTag,
    onTagSelected: (PostTag) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "カテゴリ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF536471)
            )

            SingleChoiceSegmentedButtonRow {
                PostTag.entries.forEachIndexed { index, tag ->
                    SegmentedButton(
                        selected = selectedTag == tag,
                        onClick = { onTagSelected(tag) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = PostTag.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = when (tag) {
                                PostTag.CEREMONY -> Color(0xFFE8D4F8)
                                PostTag.RECEPTION -> Color(0xFFFFE0B2)
                            },
                            activeContentColor = when (tag) {
                                PostTag.CEREMONY -> Color(0xFF9C27B0)
                                PostTag.RECEPTION -> Color(0xFFFF6F00)
                            },
                            inactiveContainerColor = Color.White,
                            inactiveContentColor = Color(0xFF536471)
                        )
                    ) {
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGrid(
    mediaUris: List<Uri>,
    context: android.content.Context,
    onRemove: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.height(350.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaUris, key = { it.toString() }) { uri ->
            MediaThumbnail(
                uri = uri,
                context = context,
                onRemove = { onRemove(uri) }
            )
        }
    }
}

@Composable
private fun MediaThumbnail(
    uri: Uri,
    context: android.content.Context,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mimeType = remember(uri) {
        context.contentResolver.getType(uri) ?: ""
    }
    val isVideo = mimeType.startsWith("video/")

    Box(modifier = modifier) {
        // 画像・動画のサムネイル表示
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = if (isVideo) "選択された動画" else "選択された画像",
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFEFF3F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF536471),
                            strokeWidth = 2.dp
                        )
                    }
                }
                is AsyncImagePainter.State.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFEFF3F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVideo) Icons.Default.PlayCircle else Icons.Default.Image,
                            contentDescription = null,
                            tint = Color(0xFF536471),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }

        // 動画の場合は再生アイコンを表示
        if (isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = "動画",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .padding(4.dp)
            )
        }

        // 削除ボタン
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "削除",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun BottomBar(
    canSubmit: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Button(
            onClick = onSubmit,
            enabled = canSubmit && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE91E63),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color(0xFF9E9E9E)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "投稿する",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}