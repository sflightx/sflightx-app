package com.sflightx.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.database.core.Context
import java.io.Serializable

class AppDataClass {

}

data class LibraryEntry(
    val postKey: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
data class UserData(
    val name: String = "",
    val profileImage: String = ""
)
data class User(
    val profile: String? = null,
    val username: String? = null,
    val email: String? = null,
    val uid: String? = null
)
data class Comment(
    val author: String = "",
    var username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    var profilePictureUrl: String? = null
)
data class InAppNotification(
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val visible: Boolean = false
)
data class Blueprint(
    val name: String = "",
    val file_link: String = "",
    val image_url: String = "",
    val author: String = "",
    val key: String = "",
    val req_type: String = "",
    val req_game: String = "",
    val downloads: Long = 0,
    val rating: Double = 0.0
) : Serializable

data class AppSettings(
    val title: String,
    val description: String,
    val icon: Painter,
    val items: List<SettingItem>
)

enum class SettingType {
    ACTION,
    SWITCH,
    CHECKBOX,
    DROPDOWN,
    SLIDER,
    TEXT_INPUT,
    DATE_PICKER,
    TIME_PICKER,
    FILE_PICKER,
    DIALOG
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


enum class DialogType {
    ALERT,
    SWITCH,
    TEXT_INPUT
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

