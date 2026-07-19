package com.voicebridge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicebridge.data.local.entity.FriendEntity
import com.voicebridge.data.network.DiscoveredDevice
import com.voicebridge.ui.theme.DarkSurface
import com.voicebridge.ui.theme.Indigo40
import com.voicebridge.ui.theme.Pink40
import com.voicebridge.ui.viewmodel.ChatViewModel

@Composable
fun NearbyScreen(
    chatViewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    val discoveredList by chatViewModel.discoveredDevices.collectAsState()
    val connectedList by chatViewModel.connectedPeers.collectAsState()
    val connectedPeersList by chatViewModel.connectedPeersList.collectAsState()
    val friendsList by chatViewModel.friends.collectAsState(initial = emptyList())
    
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pulse Radar Animation Section
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = pulseAlpha),
                    radius = pulseRadius * 2.5f,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF6366F1).copy(alpha = 0.2f),
                    radius = 40.dp.toPx()
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Indigo40, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Radar Scan",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Scanning for nearby devices...",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { chatViewModel.restartP2P() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Discovery",
                    tint = Indigo40
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = Indigo40,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Active Mesh Nodes Connected: ${connectedList.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Note: Ensure Bluetooth, Wi-Fi & Location are switched ON in Android settings for peer discovery.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        val isListEmpty = discoveredList.isEmpty() && connectedPeersList.isEmpty()

        if (isListEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No devices detected in range.\nMake sure Bluetooth/Wi-Fi are enabled.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section 1: Connected Mesh Nodes
                if (connectedPeersList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Connected Peers (Direct Range)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(connectedPeersList) { peer ->
                        val existingFriend = friendsList.find { it.deviceId == peer.deviceId }
                        ConnectedDeviceRow(
                            peer = peer,
                            friend = existingFriend,
                            onChatDirect = { onNavigateToChat(peer.deviceId) },
                            onAddFriend = {
                                chatViewModel.sendFriendRequest(peer)
                            }
                        )
                    }
                }

                // Section 2: Discovered Peers
                if (discoveredList.isNotEmpty()) {
                    item {
                        Text(
                            text = "Available Nearby Devices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(discoveredList) { device ->
                        val existingFriend = friendsList.find { it.deviceId == device.deviceId }
                        NearbyDeviceRow(
                            device = device,
                            friend = existingFriend,
                            onAddFriend = { chatViewModel.sendFriendRequest(device) },
                            onChatDirect = { onNavigateToChat(device.deviceId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectedDeviceRow(
    peer: com.voicebridge.data.network.ConnectedDevice,
    friend: FriendEntity?,
    onChatDirect: () -> Unit,
    onAddFriend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AvatarView(avatarIndex = friend?.avatarIndex ?: (peer.deviceId.firstOrNull()?.code ?: 0) % 7)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = peer.username,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (friend?.isFriend == true) "Friend • Connected" else "Connected Node (Not Friends)",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 12.sp
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Direct message is allowed to any nearby node
                IconButton(
                    onClick = onChatDirect,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Indigo40
                    )
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White)
                }

                if (friend?.isFriend != true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (friend?.isOutgoingRequest == true) {
                        IconButton(
                            onClick = { },
                            enabled = false,
                            colors = IconButtonDefaults.iconButtonColors(
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Sent")
                        }
                    } else {
                        IconButton(
                            onClick = onAddFriend,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyDeviceRow(
    device: DiscoveredDevice,
    friend: FriendEntity?,
    onAddFriend: () -> Unit,
    onChatDirect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Render avatar using random/derived index if not friend, or friend's avatar index
                AvatarView(avatarIndex = friend?.avatarIndex ?: (device.deviceId.firstOrNull()?.code ?: 0) % 7)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = device.username,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (friend?.isFriend == true) "Friend • Direct Range" else "Available Peer",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Direct message button allowed for any nearby device
                IconButton(
                    onClick = onChatDirect,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Indigo40
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                if (friend?.isFriend != true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    if (friend?.isOutgoingRequest == true) {
                        Button(
                            onClick = { },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = Color.Gray,
                                disabledContentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sent", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onAddFriend,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Add Friend",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
