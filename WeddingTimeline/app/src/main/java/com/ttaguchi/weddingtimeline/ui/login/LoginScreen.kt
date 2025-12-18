package com.ttaguchi.weddingtimeline.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ttaguchi.weddingtimeline.ui.common.resolveAvatarResId

@Composable
fun LoginScreen(
    onLoginSuccess: (roomId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showIconPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // エラーが出たときにスナックバー表示し画面に留まる
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE8D4F8).copy(alpha = 0.2f),
                            Color(0xFFB794F4).copy(alpha = 0.15f),
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Wedding Timeline",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "大切な瞬間をみんなでシェア",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Error Message (top)
                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color.Black.copy(alpha = 0.06f),
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Room ID
                        FieldLabel(
                            icon = Icons.Outlined.MeetingRoom,
                            title = "Room ID"
                        )
                        CustomTextField(
                            value = uiState.roomId,
                            onValueChange = viewModel::updateRoomId,
                            placeholder = "ルームIDを入力",
                            enabled = !uiState.isLoading,
                        )

                        // Room Key
                        FieldLabel(
                            icon = Icons.Outlined.Key,
                            title = "Room Key"
                        )
                        CustomTextField(
                            value = uiState.roomKey,
                            onValueChange = viewModel::updateRoomKey,
                            placeholder = "ルームキーを入力",
                            isPassword = true,
                            enabled = !uiState.isLoading,
                        )

                        // Username
                        FieldLabel(
                            icon = Icons.Default.Person,
                            title = "Username"
                        )
                        CustomTextField(
                            value = uiState.username,
                            onValueChange = viewModel::updateUsername,
                            placeholder = "ユーザー名を入力",
                            enabled = !uiState.isLoading,
                        )

                        // Avatar Icon
                        FieldLabel(
                            icon = Icons.Outlined.CameraAlt,
                            title = "プロフィールアイコン"
                        )
                        AvatarSelector(
                            selectedIcon = uiState.selectedIcon,
                            onClick = { showIconPicker = true },
                            enabled = !uiState.isLoading,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Join Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Button(
                        onClick = { viewModel.join(onLoginSuccess) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading &&
                                uiState.roomId.isNotBlank() &&
                                uiState.roomKey.isNotBlank() &&
                                uiState.username.isNotBlank() &&
                                uiState.selectedIcon != null,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (uiState.isLoading ||
                                        uiState.roomId.isBlank() ||
                                        uiState.roomKey.isBlank() ||
                                        uiState.username.isBlank() ||
                                        uiState.selectedIcon == null
                                    ) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Gray.copy(alpha = 0.3f),
                                                Color.Gray.copy(alpha = 0.3f),
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFFE8D4F8),
                                                Color(0xFFB794F4),
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                    )
                                    Text(
                                        text = "入室する",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

        // Icon Picker Dialog
        if (showIconPicker) {
            IconPickerDialog(
                icons = viewModel.icons,
                selectedIcon = uiState.selectedIcon,
                onIconSelected = { icon ->
                    viewModel.selectIcon(icon)
                    showIconPicker = false
                },
                onDismiss = { showIconPicker = false }
            )
        }

        // Login success is handled in ViewModel callback
    }
}

@Composable
private fun FieldLabel(
    icon: ImageVector,
    title: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFE91E63),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE0E0E0).copy(alpha = 0.25f),
            focusedBorderColor = Color(0xFFB794F4),
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedContainerColor = Color(0xFFF5F5F5),
        )
    )
}

@Composable
private fun AvatarSelector(
    selectedIcon: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF5F5F5))
            .border(
                width = 1.dp,
                color = Color(0xFFE0E0E0).copy(alpha = 0.25f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon preview
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (selectedIcon != null) {
                        Color(0xFFE8D4F8)
                    } else {
                        Color(0xFFE0E0E0).copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selectedIcon != null) {
                Image(
                    painter = painterResource(id = resolveAvatarResId(selectedIcon)),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (selectedIcon != null) selectedIcon else "未選択",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (selectedIcon != null) {
                    Color.Unspecified
                } else {
                    Color(0xFF9E9E9E)
                }
            )
            Text(
                text = "タップして変更",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
