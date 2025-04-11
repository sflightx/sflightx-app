package com.sflightx.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class Settings(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val items: List<SettingItem>
)


data class SettingItem(
    val name: String,
    val description: String? = null,
    val icon: ImageVector = Icons.Default.Settings,
    val action: (() -> Unit)? = null
)


