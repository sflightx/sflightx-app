package com.sflightx.app.`class`

import androidx.compose.runtime.MutableState

enum class DialogType {
    ALERT,
    SWITCH,
    TEXT_INPUT,
    SELECTION
}

data class DialogState(
    val show: MutableState<Boolean>,
    val title: MutableState<String>,
    val message: MutableState<String>,
    val actionText: MutableState<String>,
    val type: MutableState<DialogType>,
    val inputText: MutableState<String>,
    val switchState: MutableState<Boolean>,
    val onConfirm: MutableState<() -> Unit>
)