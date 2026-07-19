package com.voicebridge.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicebridge.ui.theme.DarkBg
import com.voicebridge.ui.theme.DarkSurface
import com.voicebridge.ui.theme.Indigo40
import com.voicebridge.ui.theme.Pink40
import com.voicebridge.ui.viewmodel.UserViewModel

val Avatars = listOf("🦊", "🐨", "🐼", "🦁", "🐱", "🐸", "🦄")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    userViewModel: UserViewModel,
    onSetupComplete: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E38),
                        Color(0xFF0F0F1A)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo/App Name
            Text(
                text = "VoiceBridge",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Secure Offline Peer-to-Peer Messenger",
                fontSize = 14.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
                textAlign = TextAlign.Center
            )

            // Setup Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose Your Avatar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Selected Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = getAvatarGradient(selectedAvatar)
                                ),
                                shape = CircleShape
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Avatars[selectedAvatar],
                            fontSize = 60.sp
                        )
                    }

                    // Avatar Selection Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .height(130.dp)
                            .padding(top = 16.dp)
                    ) {
                        items(Avatars.size) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedAvatar == index) Color.White.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedAvatar = index }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Avatars[index],
                                    fontSize = 28.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            showError = false
                        },
                        label = { Text("Username", color = Color.LightGray) },
                        placeholder = { Text("Enter nickname", color = Color.Gray) },
                        singleLine = true,
                        isError = showError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Indigo40,
                            unfocusedBorderColor = Color.Gray,
                            errorBorderColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        Text(
                            text = "Username cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            }

            // Get Started Button
            Button(
                onClick = {
                    if (username.trim().isEmpty()) {
                        showError = true
                    } else {
                        userViewModel.createProfile(username.trim(), selectedAvatar)
                        onSetupComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo40
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Forward",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// Avatar color gradient mapper
fun getAvatarGradient(index: Int): List<Color> {
    return when (index) {
        0 -> listOf(Color(0xFFFF9F43), Color(0xFFFF5252)) // Fox
        1 -> listOf(Color(0xFFADB5BD), Color(0xFF495057)) // Koala
        2 -> listOf(Color(0xFFE9ECEF), Color(0xFF212529)) // Panda
        3 -> listOf(Color(0xFFFFD32A), Color(0xFFFF8008)) // Lion
        4 -> listOf(Color(0xFF70A1FF), Color(0xFF1E90FF)) // Cat
        5 -> listOf(Color(0xFF7BED9F), Color(0xFF2ED573)) // Frog
        6 -> listOf(Color(0xFFE84393), Color(0xFF6C5CE7)) // Unicorn
        99 -> listOf(Color(0xFF38EF7D), Color(0xFF11998E)) // Everyone Globe
        else -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
    }
}

@Composable
fun AvatarView(
    avatarIndex: Int,
    size: Int = 48,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                Brush.linearGradient(
                    colors = getAvatarGradient(avatarIndex)
                ),
                shape = CircleShape
            )
            .padding((size / 10).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (avatarIndex == 99) "🌐" else Avatars.getOrNull(avatarIndex) ?: "👤",
            fontSize = (size * 0.6).sp
        )
    }
}
