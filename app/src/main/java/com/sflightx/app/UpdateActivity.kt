@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.sflightx.app

import android.content.*
import android.graphics.Color
import android.net.*
import android.os.*
import android.provider.*
import android.util.*
import android.widget.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.content.*
import androidx.core.net.*
import androidx.core.view.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*

@Suppress("DEPRECATION")
class UpdateActivity : ComponentActivity() {
    private var downloadJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val version = intent.getStringExtra("version") ?: "Unknown"
        val url = intent.getStringExtra("url") ?: "https://sflightx.com"

        setContent {
            SFlightXTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(WindowInsets.systemBars),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpdateLayout(
                        version = version,
                        url = url,
                        onExitConfirmed = {
                            stopDownload()
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)

                            // Optionally, finish the current activity if needed
                            finish()
                        }
                    )
                }
            }
        }

        // Handle back press using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this) {
            // This will be handled in the composable
        }
    }

    private fun stopDownload() {
        downloadJob?.cancel()
        Toast.makeText(this, "Download canceled", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateLayout(
    version: String,
    url: String,
    onExitConfirmed: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    val showDialog = remember { mutableStateOf(false) }

    // Handle back press
    BackHandler {
        showDialog.value = true
    }

    // Exit confirmation dialog
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text("Exit Confirmation") },
            text = { Text("Are you sure you want to exit? The current download will be stopped.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog.value = false
                        onExitConfirmed()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text("No")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Updating...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Box(modifier = Modifier.padding(24.dp)) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimary,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(WindowInsets.safeContent.asPaddingValues())
                .fillMaxSize()
        ) {
            Box (
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Downloading Version $version")
            }
        }

        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                    return@LaunchedEffect
                }
            }

            downloadApk(
                apkUrl = url,
                onSuccess = { file ->
                },
                onError = { e ->
                },
                context = context,
                onProgress = { value -> progress = value }
            )
        }
    }
}

fun downloadApk(
    apkUrl: String,
    fileName: String? = "sflightx-update.apk",
    onSuccess: (File) -> Unit,
    onError: (Exception) -> Unit,
    context: Context,
    onProgress: (Float) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL(apkUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Server returned HTTP ${connection.responseCode}")
            }

            val fileLength = connection.contentLength

            val inputStream = connection.inputStream
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)

            var totalBytesRead = 0L
            val buffer = ByteArray(4096)
            var bytesRead: Int

            inputStream.use { input ->
                outputStream.use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileLength > 0) {
                            val progress = totalBytesRead / fileLength.toFloat()
                            withContext(Dispatchers.Main) {
                                onProgress(progress.coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApk(context, file)
                onSuccess(file)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }
}

fun installApk(context: Context, apkFile: File) {
    val apkUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }

    context.startActivity(intent)
}