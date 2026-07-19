package com.voicebridge.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voicebridge.data.local.dao.UserDao
import com.voicebridge.domain.repository.ChatRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeshNetworkService : Service() {

    @Inject
    lateinit var nearbyConnectionManager: NearbyConnectionManager

    @Inject
    lateinit var chatRepository: ChatRepository

    @Inject
    lateinit var userDao: UserDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "voicebridge_bg_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceBridge Mesh Network")
            .setContentText("Running in the background to route messages")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Start offline mesh logic
        serviceScope.launch {
            val user = userDao.getUser()
            if (user != null) {
                nearbyConnectionManager.initialize(user.deviceId, user.username)
                nearbyConnectionManager.startP2P()
                chatRepository.sendNodeHello()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyConnectionManager.stopP2P()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding for now
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VoiceBridge Background Service"
            val descriptionText = "Keeps the offline mesh network alive in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
