package com.sflightx.app.`class`

import java.io.Serializable

data class BlueprintData(
    val name: String = "",
    val desc: String = "",
    val file_link: String = "",
    val image_url: String = "",
    val author: String = "",
    val key: String = "",
    val req_type: String = "",
    val req_game: String = "",
    val downloads: Long = 0,
    val rating: Double = 0.0
) : Serializable