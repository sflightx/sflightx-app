package com.sflightx.app.`class`

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

enum class SettingType {
    ACTION,
    SWITCH,
    TEXT_INPUT
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class SettingItem(
    val name: String? = null,
    val description: String? = null,
    val iconResId: Int? = null, // made nullable
    val type: SettingType? = null,
    val defaultValue: Boolean = false,
    val onValueChanged: ((Boolean) -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val dialogState: DialogState? = null,
    val content: (@Composable (() -> Unit))? = null
)

data class AppSettings(
    val title: String,
    val description: String,
    val icon: Painter,
    val items: List<SettingItem>
)