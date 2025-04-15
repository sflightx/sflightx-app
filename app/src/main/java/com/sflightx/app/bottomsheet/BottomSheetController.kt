package com.sflightx.app.bottomsheet

import androidx.compose.runtime.*

class BottomSheetController {
    var isVisible by mutableStateOf(false)
        private set

    var content: (@Composable () -> Unit)? by mutableStateOf(null)
        private set

    fun show(content: @Composable () -> Unit) {
        this.content = content
        isVisible = true
    }

    fun hide() {
        isVisible = false
        content = null
    }
}