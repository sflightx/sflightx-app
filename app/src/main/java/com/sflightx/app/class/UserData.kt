package com.sflightx.app.`class`

import java.io.Serializable

data class UserData(
    val profile: String? = null,
    val username: String? = null,
    val email: String? = null,
    val uid: String? = null,
    val bio: String? = null
) : Serializable