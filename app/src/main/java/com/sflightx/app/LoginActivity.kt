package com.sflightx.app

import android.annotation.*
import android.app.*
import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.core.net.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.*
import com.google.firebase.*
import com.google.firebase.auth.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        setContent {
            SFlightXTheme {
                LoginLayout()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginLayout() {
    val context = LocalContext.current
    val activity = context as? Activity // Cast safely to Activity
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle physical back press
    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        ConfirmationDialog(
            onConfirm = {
                context.startActivity(Intent(context, MainActivity::class.java))
                activity?.finish() // Finish LoginActivity safely
            },
            onDismiss = {
                showDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * 3f).sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Log In to our services to get started.",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.padding(bottom = 24.dp),
                    thickness = 1.dp,
                )
                MainLayout(snackbarHostState = snackbarHostState) // <-- Pass snackbarHostState if needed
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun MainLayout(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        ConfirmationDialog(
            onConfirm = {
                context.startActivity(Intent(context, MainActivity::class.java))
                context.finish()
            },
            onDismiss = {
                showDialog = false
            }
        )
    }


    val signInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.gcp_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, signInOptions)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authResult ->
                        if (authResult.isSuccessful) {
                            context.startActivity(Intent(context, MainActivity::class.java))
                            activity.finish()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Authentication Failed.")
                            }
                        }
                    }

            } catch (e: ApiException) {
                scope.launch {
                    snackbarHostState.showSnackbar("Authentication Failed: ${e.message} ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}")
                }
            }
        }
    )

    Column {
        Button(
            onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("Email sign-in coming soon")
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Continue using Email")
        }

        Button(
            onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("Phone sign-in coming soon")
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Continue using Phone")
        }

        OutlinedButton(
            onClick = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier.padding(bottom = 24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
        ) {
            Text(
                text = "Sign In with Google",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Row {
            Text(
                text = "Terms and Conditions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * 0.9f).sp,
                modifier = Modifier.padding(end = 8.dp).clickable{
                    val intent = Intent(Intent.ACTION_VIEW, "https://sflightx.com/legal/terms".toUri())
                    context.startActivity(intent)
                },
            )
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * 0.9f).sp,
                modifier = Modifier.clickable{
                    val intent = Intent(Intent.ACTION_VIEW, "https://sflightx.com/legal/privacy".toUri())
                    context.startActivity(intent)
                },
            )
        }
    }
}

@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Skip Login?") },
        text = { Text("You may not be able to use all functionality throughout the app.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
