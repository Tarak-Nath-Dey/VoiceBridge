package com.voicebridge.ui

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import com.voicebridge.data.network.MeshNetworkService
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voicebridge.ui.navigation.Screen
import com.voicebridge.ui.screens.MainScreen
import com.voicebridge.ui.screens.PrivateChatScreen
import com.voicebridge.ui.screens.SetupScreen
import com.voicebridge.ui.theme.VoiceBridgeTheme
import com.voicebridge.ui.viewmodel.ChatViewModel
import com.voicebridge.ui.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var onPermissionsResultCallback: (() -> Unit)? = null
    
    private val pendingNavigation = MutableSharedFlow<String>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Permissions are required for offline device discovery & mesh networking.",
                Toast.LENGTH_LONG
            ).show()
        }
        onPermissionsResultCallback?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)
        
        // Create Notification Channel
        createNotificationChannel()

        // Request standard P2P permissions on startup
        checkAndRequestPermissions()

        setContent {
            VoiceBridgeTheme {
                val navController = rememberNavController()
                
                // Retrieve Hilt ViewModels
                val userViewModel: UserViewModel = viewModel()
                val chatViewModel: ChatViewModel = viewModel()
                

                onPermissionsResultCallback = {
                    startMeshService()
                }
                
                val isProfileCreated by userViewModel.isProfileCreated.collectAsState()

                if (isProfileCreated == null) {
                    // Waiting for database check
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val startRoute = if (isProfileCreated == true) Screen.Main.route else Screen.Setup.route
                    
                    NavHost(
                        navController = navController,
                        startDestination = startRoute
                    ) {
                        composable(Screen.Setup.route) {
                            SetupScreen(
                                userViewModel = userViewModel,
                                onSetupComplete = {
                                    // Start offline networking service
                                    startMeshService()
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.Setup.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.Main.route) {
                            MainScreen(
                                userViewModel = userViewModel,
                                chatViewModel = chatViewModel,
                                onNavigateToChat = { chatId ->
                                    navController.navigate(Screen.PrivateChat.createRoute(chatId))
                                }
                            )
                        }
                        
                        composable(Screen.PrivateChat.route) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                            PrivateChatScreen(
                                chatId = chatId,
                                chatViewModel = chatViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    if (isProfileCreated == true) {
                        LaunchedEffect(Unit) {
                            pendingNavigation.collect { chatId ->
                                if (chatId == "EVERYONE") {
                                    navController.popBackStack(Screen.Main.route, inclusive = false)
                                } else {
                                    navController.navigate(Screen.PrivateChat.createRoute(chatId)) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        // Location is needed for Nearby Connections (all API levels)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Android 13+ Notification and Nearby Wi-Fi Devices Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            onPermissionsResultCallback?.invoke()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VoiceBridge Notifications"
            val descriptionText = "Notifications for incoming messages and friend requests"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("voicebridge_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val chatId = intent?.getStringExtra("chatId")
        if (chatId != null) {
            pendingNavigation.tryEmit(chatId)
            intent.removeExtra("chatId")
        }
    }

    private fun startMeshService() {
        val intent = Intent(this, MeshNetworkService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
