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
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.*
import com.google.firebase.*
import com.google.firebase.auth.*
import com.sflightx.app.animation.*
import com.sflightx.app.dialog.*
import com.sflightx.app.layout.*
import com.sflightx.app.layout.components.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*


class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        super.onCreate(savedInstanceState)
        setContent {
            SFlightXTheme {
                AnimatedGradientBackground(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LoginLayout()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LoginLayout() {
    val context = LocalContext.current
    val activity = context as? Activity
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        showDialog = true
    }

    if (showDialog) {
        M3ConfirmationDialog(
            title = "Skip Login?",
            message = "You may not be able to use all functionality throughout the app.",
            confirmButtonText = "Yes",
            dismissButtonText = "Cancel",
            onConfirm = {
                context.startActivity(Intent(context, MainActivity::class.java))
                activity?.finish()
            },
            onDismiss = {
                showDialog = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
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
                    text = "Hello!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = (MaterialTheme.typography.headlineSmall.fontSize.value * 3f).sp
                )
                Text(
                    text = "Log In to our services to get started.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                MainLayout(snackbarHostState = snackbarHostState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
        M3ConfirmationDialog(
            title = "Skip Login?",
            message = "You may not be able to use all functionality throughout the app.",
            confirmButtonText = "Yes",
            dismissButtonText = "Cancel",
            onConfirm = {
                context.startActivity(Intent(context, MainActivity::class.java))
                activity.finish()
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
                    snackbarHostState.showSnackbar(
                        "Authentication Failed: ${e.message} ${
                            GoogleSignInStatusCodes.getStatusCodeString(
                                e.statusCode
                            )
                        }"
                    )
                }
            }
        }
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge, // Expressive
            tonalElevation = 8.dp,                   // Expressive/Elevated
            dragHandle = {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .padding(top = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        ) {
            LoginPanelContent(
                onDismiss = { scope.launch { sheetState.hide(); showSheet = false } }
            )
        }
    }
    Column {
        LoginButtonGroupWithIcons(
            onEmail = {
                scope.launch {
                    showSheet = true
                }
            },
            onPhone = {
                scope.launch {
                    snackbarHostState.showSnackbar("Phone sign-in coming soon")
                }
            },
            onGoogle = {
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            }
        )
        LegalFooter(context)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginButtonGroupWithIcons(
    onEmail: () -> Unit,
    onPhone: () -> Unit,
    onGoogle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        M3Button(
            iconResId = R.drawable.mail_24px,
            contentDescription = "Sign in with Email",
            onClick = onEmail,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            buttonType = M3ButtonType.FilledTonal
        )
        M3Button(
            iconResId = R.drawable.call_24px,
            contentDescription = "Sign in with Phone",
            onClick = onPhone,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            buttonType = M3ButtonType.FilledTonal
        )
        M3Button(
            iconResId = R.drawable.google,
            contentDescription = "Sign in with Google",
            onClick = onGoogle,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            buttonType = M3ButtonType.FilledTonal
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginPanelContent(
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            enabled = !loading
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            enabled = !loading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
            M3Button(
                onClick = {
                    loading = true
                    error = null
                    if (email.isBlank() || password.isBlank()) {
                        error = "Please fill in all fields"
                        loading = false
                    } else {

                    }
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .height(64.dp)
                    .weight(1f),
                enabled = !loading,
                contentDescription = "Confirm",
                text = "Log In",
                buttonType = M3ButtonType.FilledTonal
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        M3Button(
            onClick = { loading = false },
            contentDescription = "Cancel",
            text = "Cancel",
            buttonType = M3ButtonType.Text
        )
    }
}