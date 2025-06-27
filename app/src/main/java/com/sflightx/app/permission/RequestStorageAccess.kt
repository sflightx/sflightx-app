package com.sflightx.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun BlueprintFolderAccess(
    onBlueprintsLoaded: (List<DocumentFile>) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val pickedDir = DocumentFile.fromTreeUri(context, uri)
                val blueprintDir = pickedDir
                    ?.findFile("files")
                    ?.findFile("Saving")
                    ?.findFile("Blueprints")

                if (blueprintDir != null && blueprintDir.isDirectory) {
                    val blueprints = blueprintDir.listFiles().filter { it.isDirectory }
                    if (blueprints.isNotEmpty()) {
                        onBlueprintsLoaded(blueprints)
                    } else {
                        snackbarMessage = "No blueprints found in the folder."
                    }
                } else {
                    snackbarMessage = "Please select the correct game folder."
                }
            }.onFailure { e ->
                Log.e("BlueprintAccess", "Error accessing blueprints", e)
                snackbarMessage = "Failed to access blueprints."
            }
        }
    }

    // Handle showing snackbar
    snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Access your Spaceflight Simulator blueprints!")

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = """
                1. Tap "Allow Folder Access" below.
                2. Navigate to:
                   Android > data > com.StefMorojna.SpaceflightSimulator
                3. Tap "Use this folder".
                (We will find the Saving/Blueprints folder automatically!)
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val initialUri = DocumentsContract.buildRootUri(
                    "com.android.externalstorage.documents", "primary"
                )
                launcher.launch(initialUri)
            }
        ) {
            Text("Allow Folder Access")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "After access is granted, your blueprints will be loaded.")
    }
}
