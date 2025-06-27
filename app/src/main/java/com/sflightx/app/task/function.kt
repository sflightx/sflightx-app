package com.sflightx.app.task

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.documentfile.provider.*
import com.google.firebase.database.*
import com.sflightx.app.`class`.*
import com.sflightx.app.ui.*
import kotlinx.coroutines.*
import java.time.*


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun getUserByUid(uid: String): UserData? = suspendCancellableCoroutine { cont ->
    val userRef = FirebaseDatabase.getInstance().getReference("userdata").child(uid)

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val userData = snapshot.getValue(UserData::class.java)
            cont.resume(userData) {}
        }

        override fun onCancelled(error: DatabaseError) {
            cont.resume(null) {}
        }
    }

    userRef.addListenerForSingleValueEvent(listener)

    cont.invokeOnCancellation {
        userRef.removeEventListener(listener)
    }
}

@Composable
fun LoadPermissionContext() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var blueprints by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    val hasPermission by rememberUpdatedState(
        newValue = context.contentResolver.persistedUriPermissions.any { it.isReadPermission }
    )

    if (!hasPermission) {
        // Show the folder access request UI
        BlueprintFolderAccess(
            onBlueprintsLoaded = { loadedBlueprints ->
                blueprints = loadedBlueprints
            },
            snackbarHostState = snackbarHostState
        )
    } else {
        BlueprintFolderAccess(
            onBlueprintsLoaded = { loadedBlueprints ->
                blueprints = loadedBlueprints
            },
            snackbarHostState = snackbarHostState
        )
    }

    // You can also show SnackbarHost somewhere in your scaffold
    SnackbarHost(hostState = snackbarHostState)
}

@Composable
fun LaunchCountdownChip(endTimeMillis: Long) {
    val currentTime by rememberUpdatedState(System.currentTimeMillis())
    var remainingTime by remember { mutableLongStateOf(endTimeMillis - currentTime) }

    // Update every second
    LaunchedEffect(endTimeMillis) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime = endTimeMillis - System.currentTimeMillis()
        }
    }

    val duration = Duration.ofMillis(remainingTime.coerceAtLeast(0L))
    val days = duration.toDays()
    val hours = duration.toHours() % 24
    val minutes = duration.toMinutes() % 60
    val seconds = duration.seconds % 60

    val label = when {
        remainingTime <= 0 -> "Launched"
        days > 0 -> "$days d $hours h"
        hours > 0 -> "$hours h $minutes m"
        minutes > 0 -> "$minutes m $seconds s"
        else -> "$seconds s"
    }

    AssistChip(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(50),
        onClick = {},
        label = { Text(label) }
    )
}
