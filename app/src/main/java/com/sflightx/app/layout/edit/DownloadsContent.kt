package com.sflightx.app.layout.edit

import android.content.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import com.sflightx.app.*
import com.sflightx.app.`class`.LibraryEntry
import com.sflightx.app.task.*

@Composable
fun DownloadTabContent(context: Context, snackbarHostState: SnackbarHostState) {
    var library by remember { mutableStateOf<List<LibraryEntry>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        LaunchedEffect(Unit) {
            val result = loadUserLibrary(context)

            if (result.isSuccess) {
                val data = result.getOrDefault(emptyMap())
                library = data.values.sortedByDescending { it.timestamp }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                snackbarHostState.showSnackbar("Error: $error")
            }
        }

        // Handle empty library state
        if (library.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No blueprints in your library yet.")
            }
        } else {
            LibraryList(
                library = library,
            )
        }
    }
}

@Composable
fun GameFileTabContent() {
    LoadPermissionContext()
}