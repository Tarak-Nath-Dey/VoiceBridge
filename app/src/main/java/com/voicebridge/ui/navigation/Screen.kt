package com.voicebridge.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Main : Screen("main")
    object PrivateChat : Screen("private_chat/{chatId}") {
        fun createRoute(chatId: String) = "private_chat/$chatId"
    }
    object QrCode : Screen("qrcode")
}
