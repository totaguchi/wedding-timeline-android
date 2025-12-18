package com.ttaguchi.weddingtimeline.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.ttaguchi.weddingtimeline.ui.common.resolveAvatarResId

@Composable
fun IconPickerDialog(
    icons: List<String>,
    selectedIcon: String?,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                        )
                        Text(
                            text = "アバターを選択",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.Gray.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "閉じる",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Subtitle
                Text(
                    text = "お好きなアバターを選んでください",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(icons) { icon ->
                        AvatarCell(
                            icon = icon,
                            isSelected = icon == selectedIcon,
                            onClick = {
                                onIconSelected(icon)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarCell(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFB794F4) else Color(0xFFE0E0E0).copy(alpha = 0.25f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Avatar Icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.08f)
                )
                .clip(CircleShape)
                .background(Color(0xFFF5F5F5))
                .border(
                    width = 2.dp,
                    color = if (isSelected) Color(0xFFB794F4) else Color.White,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = resolveAvatarResId(icon)),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Badge
        if (isSelected) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(10.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f)
                    )
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "選択中",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63),
                )
            }
        }
    }
}

// Helper function to map icon names to drawable resources
// TODO: Implement this based on your avatar resources
// Avatar resource mapping moved to ui/common/AvatarResources.kt
