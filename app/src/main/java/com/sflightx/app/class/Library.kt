package com.sflightx.app.`class`

data class LibraryEntry(
    val postKey: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)