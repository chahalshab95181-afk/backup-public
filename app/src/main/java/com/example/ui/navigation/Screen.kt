package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.CloudUpload)
    data object Channels : Screen("channels", "Channels", Icons.Default.Send)
    data object Activity : Screen("activity", "Activity", Icons.Default.Assessment)
    data object VaultTools : Screen("vault_tools", "Vault Tools", Icons.Default.Lock)
}
