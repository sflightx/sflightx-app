package com.sflightx.app.`class`

data class Comment(
    val author: String = "",
    var username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val key: String = "",
    var profilePictureUrl: String? = null
)