package com.sflightx.app.`class`

data class InAppNotification(
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val visible: Boolean = false
)