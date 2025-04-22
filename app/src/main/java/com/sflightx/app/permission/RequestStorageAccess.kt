package com.sflightx.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile

@Composable
fun BlueprintFolderAccess() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            // Persist permission
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            // Process the selected directory
            val pickedDir = DocumentFile.fromTreeUri(context, uri)
            pickedDir?.listFiles()?.forEach { folder ->
                val blueprintJson = folder.findFile("blueprint.json") ?: folder.findFile("blueprint.txt")
                if (blueprintJson != null && blueprintJson.isFile) {
                    val inputStream = context.contentResolver.openInputStream(blueprintJson.uri)
                    val json = inputStream?.bufferedReader()?.use { it.readText() }
                    Log.d("Blueprint", json ?: "Empty")
                }
            }
        }
    }

    // UI to show instructions and allow folder access
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Access your Spaceflight Simulator blueprints!",
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = """
                1. Tap "Allow Folder Access" below.
                2. In the file picker, navigate to:
                   Android > data > com.StefMorojna.SpaceflightSimulator > Blueprints
                3. Tap "Use this folder".
            """.trimIndent(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { launcher.launch(null) }
        ) {
            Text("Allow Folder Access")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You will be able to browse your blueprints once the folder is selected.",
        )
    }
}

@Preview
@Composable
fun BlueprintFolderAccessPreview() {
    BlueprintFolderAccess()
}
