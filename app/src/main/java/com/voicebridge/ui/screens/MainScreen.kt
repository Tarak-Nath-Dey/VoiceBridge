package com.voicebridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voicebridge.ui.navigation.Screen
import com.voicebridge.ui.viewmodel.ChatViewModel
import com.voicebridge.ui.viewmodel.UserViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userViewModel: UserViewModel,
    chatViewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(3) } // Default to Chats Tab (index 3)
    val userState by userViewModel.userFlow.collectAsState(initial = null)
    
    // Count pending friend requests for badge
    val pendingCount by chatViewModel.pendingRequests.map { it.size }.collectAsState(initial = 0)
    
    val hasUnreadEveryone by chatViewModel.unreadEveryoneBadge.collectAsState(initial = false)

    // Count total unread chat messages for badge (excluding EVERYONE)
    val totalUnreadCount by chatViewModel.chats.map { list -> 
        list.filter { it.chatId != "EVERYONE" }.sumOf { it.unreadCount } 
    }.collectAsState(initial = 0)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Nearby Discovery"
                            1 -> "Everyone Chat"
                            2 -> "Friends"
                            3 -> "Conversations"
                            4 -> "My Profile"
                            else -> "Settings"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                actions = {
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Nearby Tab
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Radar, contentDescription = "Nearby") },
                    label = { Text("Nearby", style = MaterialTheme.typography.labelSmall) }
                )
                
                // Everyone Tab
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1 
                        chatViewModel.clearUnreadCount("EVERYONE")
                    },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (hasUnreadEveryone) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Public, contentDescription = "Everyone") 
                        }
                    },
                    label = { Text("Everyone", style = MaterialTheme.typography.labelSmall) }
                )

                // Friends Tab
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingCount > 0) {
                                    Badge { Text(pendingCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.People, contentDescription = "Friends")
                        }
                    },
                    label = { Text("Friends", style = MaterialTheme.typography.labelSmall) }
                )

                // Chats Tab
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnreadCount > 0) {
                                    Badge { Text(totalUnreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Chats")
                        }
                    },
                    label = { Text("Chats", style = MaterialTheme.typography.labelSmall) }
                )

                // Profile Tab
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", style = MaterialTheme.typography.labelSmall) }
                )

                // Settings Tab
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> NearbyScreen(chatViewModel = chatViewModel, onNavigateToChat = onNavigateToChat)
                1 -> EveryoneScreen(chatViewModel = chatViewModel)
                2 -> FriendsScreen(chatViewModel = chatViewModel, onNavigateToChat = onNavigateToChat)
                3 -> ChatsScreen(chatViewModel = chatViewModel, onNavigateToChat = onNavigateToChat)
                4 -> ProfileScreen(userViewModel = userViewModel)
                5 -> SettingsScreen(userViewModel = userViewModel, chatViewModel = chatViewModel)
            }
        }
    }
}
