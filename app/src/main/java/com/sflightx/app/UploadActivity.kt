package com.sflightx.app

import android.annotation.*
import android.app.*
import android.content.*
import android.net.*
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import coil.compose.*
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.storage.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*
import kotlin.Any
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.OptIn
import kotlin.Pair
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.to
import kotlin.toString

@Suppress("DEPRECATION")
class UploadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val (sharedLink, sharedImageUri) = getSharedContent(intent)
        setContent {
            SFlightXTheme {
                UploadLayout(sharedLink = sharedLink, sharedImageUri = sharedImageUri)
            }
        }
    }
    private fun getSharedContent(intent: Intent): Pair<String?, Uri?> {
        val action = intent.action
        val type = intent.type

        var sharedText: String? = null
        var sharedImage: Uri? = null

        if (Intent.ACTION_SEND == action && type != null) {
            if (type.startsWith("image/")) {
                sharedImage = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        }
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

        return Pair(sharedText, sharedImage)
    }


}

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadLayout(sharedLink: String?, sharedImageUri: Uri?) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? Activity
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var fileName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf(sharedLink ?: "") }
    var imageUri by remember { mutableStateOf(sharedImageUri) }
    var selectedGame by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var blueprintData by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }

    var fileNameError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var linkError by remember { mutableStateOf<String?>(null) }
    var gameSelectionError by remember { mutableStateOf<String?>(null) }
    var typeSelectionError by remember { mutableStateOf<String?>(null) }

    var currentStep by remember { mutableIntStateOf(1) } // Track the current step
    val totalSteps = 4
    var stepIsValid by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    fun validateStep(): Boolean {
        var valid = true

        when (currentStep) {
            1 -> {
                if (fileName.isEmpty()) {
                    fileNameError = "File name is required"
                    valid = false
                } else {
                    fileNameError = null
                }

                if (description.isEmpty()) {
                    descriptionError = "Description is required"
                    valid = false
                } else {
                    descriptionError = null
                }
            }

            2 -> {
                if (link.isEmpty()) {
                    linkError = "Link is required"
                    valid = false
                } else {
                    linkError = null
                }
            }

            3 -> {
                if (selectedGame == null) {
                    gameSelectionError = "You must select a game."
                    valid = false
                } else {
                    gameSelectionError = null
                }

                if (selectedType == null) {
                    typeSelectionError = "You must select the blueprint type."
                    valid = false
                } else {
                    typeSelectionError = null
                }
            }

            4 -> {
                // Add validation for step 4 if needed
            }
        }

        return valid
    }


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Upload a file",
                        fontSize = (30 - 10 * scrollBehavior.state.collapsedFraction).sp, // Refined title size change
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        activity?.finish() // Close activity
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            BottomAppBar(
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        IconButton(
                            onClick = {
                                if (currentStep > 1) {
                                    currentStep -= 1
                                }
                                showErrors = true
                                stepIsValid = validateStep()
                                showErrors = false
                            },
                            enabled = currentStep > 1, // Disable the button when currentStep <= 1
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous"
                            )
                        }

                        Box (
                            modifier = Modifier.weight(1f)
                        ) {
                            BottomAppBarWithProgress(currentStep = currentStep, totalSteps = totalSteps)
                        }

                        IconButton(
                            onClick = {
                                if (stepIsValid) {
                                    if (currentStep == 1) currentStep = 2
                                    else if (currentStep == 2) currentStep = 3
                                    else if (currentStep == 3) currentStep = 4
                                }
                                showErrors = true
                                stepIsValid = validateStep()
                                showErrors = false
                            },
                            enabled = stepIsValid && currentStep != 4,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next"
                            )
                        }

                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        // Body content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Use innerPadding here
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                if (currentStep == 1) {
                    if (imageUri != null) {
                        Box(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .wrapContentSize()
                        ) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Shared Image",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .aspectRatio(1f)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            stepIsValid = validateStep()
                        },
                        label = { Text("File Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true,
                        isError = fileNameError != null
                    )

                    if (!fileNameError.isNullOrEmpty()) {
                        Text(
                            text = fileNameError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            stepIsValid = validateStep()
                        },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        maxLines = 3,
                        isError = descriptionError != null
                    )

                    if (!descriptionError.isNullOrEmpty()) {
                        Text(
                            text = descriptionError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    if (link.isNotEmpty()) {
                        Text(
                            text = link,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                }

                if (currentStep == 2) {

                    OutlinedTextField(
                        value = link,
                        onValueChange = {
                            link = it
                            stepIsValid = validateStep()
                        },
                        label = { Text("Spaceflight Simulator Link") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        singleLine = true,
                        isError = linkError != null
                    )

                    if (linkError != null) {
                        Text(
                            text = linkError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    WarningCard(
                        title = "Newer Sharing Method",
                        message = "For SFS sharing, open Spaceflight Simulator, upload blueprint and share link. You must share to sFLightX App in order to upload to our community.",
                        onProceed = {

                        },
                        onSecondaryProceed = {

                        }
                    )
                }

                // Step 3: Additional Form Fields or Details
                if (currentStep == 3) {
                    // Add more fields, for example, category or tags
                    GameSelectionStep(
                        selectedGame = selectedGame,
                        onGameSelected = {
                            selectedGame = it
                            stepIsValid = validateStep()
                        }
                    )
                    TypeSelectionStep(
                        selectedType = selectedType,
                        onTypeSelected = {
                            selectedType = it
                            stepIsValid = validateStep()
                        }
                    )
                    if (gameSelectionError != null) {
                        if (showErrors) {
                            scope.launch {
                                snackbarHostState.showSnackbar(gameSelectionError!!)
                            }
                        }
                    }
                    if (typeSelectionError != null) {
                        if (showErrors) {
                            scope.launch {
                                snackbarHostState.showSnackbar(typeSelectionError!!)
                            }
                        }
                    }
                }

                // Step 4: Review and Confirm
                if (currentStep == 4) {
                    // Display summary or confirmation message
                    Text(
                        "Review your details and submit",
                        modifier = Modifier.padding(bottom = 32.dp),
                        style = MaterialTheme.typography.headlineLarge
                    )

                    Text(
                        fileName,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        description,
                        modifier = Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.padding(end = 16.dp))
                    Text(
                        link,
                        modifier = Modifier.padding(bottom = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row {
                        AssistChip(
                            onClick = {},
                            label = { Text(text = selectedGame.toString()) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(text = selectedType.toString()) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val user = FirebaseAuth.getInstance().currentUser
                            val userId = user?.uid ?: "unknown_user"
                            val imageUriString = imageUri?.toString() ?: "null"
                            showDialog = true
                            blueprintData = mapOf<String, Any>(
                                "name" to fileName,
                                "link" to link,
                                "desc" to description,
                                "uri" to imageUriString,
                                "downloads" to 0,
                                "author" to userId,
                                "game" to selectedGame.toString(),
                                "type" to selectedType.toString()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Submit")
                        if (showDialog) {
                            ConfirmationDialog(
                                context = context,
                                blueprintData = blueprintData,
                                onDismiss = { showDialog = false } // Close the dialog when dismissed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(
    title: String = "Warning",
    message: String = "Are you sure you want to continue? This action may have consequences.",
    onProceed: () -> Unit,
    onSecondaryProceed: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onProceed) {
                    Text("Proceed")
                }
                TextButton(onClick = onSecondaryProceed) {
                    Text("Learn More")
                }
            }
        }
    }
}

@Composable
fun BottomAppBarWithProgress(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        // Create progress dots for each step
        for (i in 1..totalSteps) {
            val isActive = i <= currentStep
            val dotColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .padding(4.dp)
            )

            // Add space between dots
            if (i < totalSteps) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSelectionStep(
    selectedGame: String?,
    onGameSelected: (String) -> Unit,
) {
    val games = listOf("JNO", "SFS", "Ellipse")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select the game you are posting to:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            games.forEach { game ->
                val selectedGameState = selectedGame == game
                FilterChip(
                    selected = selectedGame == game,
                    onClick = { onGameSelected(game) },
                    label = { Text(text = game) },
                    modifier = Modifier.padding(4.dp),
                    leadingIcon = if (selectedGameState) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeSelectionStep(
    selectedType: String?,
    onTypeSelected: (String) -> Unit,
) {
    val games = listOf("Vanilla", "DLC")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Select the type of your blueprint:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            games.forEach { game ->
                val selectedTypeState = selectedType == game
                FilterChip(
                    selected = selectedType == game,
                    onClick = { onTypeSelected(game) },
                    label = { Text(text = game) },
                    modifier = Modifier.padding(4.dp),
                    leadingIcon = if (selectedTypeState) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

fun handleSubmission(context: Context, blueprintData: Map<String, Any>) {
    val activity = context as? Activity
    val user = FirebaseAuth.getInstance().currentUser
    val postKey = FirebaseDatabase.getInstance().reference.child("upload/blueprint").push().key

    val userId = user?.uid ?: "unknown_user"
    val imageUri = blueprintData["uri"] as? String
    val fileName = blueprintData["name"] as? String
    val link = blueprintData["link"] as? String
    val description = blueprintData["desc"] as? String
    val selectedGame = blueprintData["game"] as? String
    val selectedType = blueprintData["type"] as? String

    Log.d("Blueprint Upload", "imageUri: $imageUri")
    Log.d("Blueprint Upload", "postKey: $postKey")
    Log.d("Blueprint Upload", "fileName: $fileName")
    Log.d("Blueprint Upload", "link: $link")
    Log.d("Blueprint Upload", "description: $description")
    Log.d("Blueprint Upload", "userId: $userId")
    Log.d("Blueprint Upload", "selectedGame: $selectedGame")
    Log.d("Blueprint Upload", "selectedType: $selectedType")

    if (imageUri != null && postKey != null && fileName != null && link != null && description != null) {
        uploadImageToFirebaseStorage(imageUri.toUri(), postKey,
            onSuccess = { imageDownloadUrl ->
                val blueprintDataWithImage = mapOf<String, Any>(
                    "name" to fileName,
                    "file_link" to link,
                    "desc" to description,
                    "timestamp" to ServerValue.TIMESTAMP,
                    "downloads" to 0,
                    "author" to userId,
                    "image_url" to imageDownloadUrl,
                    "req_game" to selectedGame.toString(),
                    "req_type" to selectedType.toString(),
                    "key" to postKey
                )

                // Store the data in Firebase Realtime DatabaseManager
                val databaseRef = FirebaseDatabase.getInstance().reference
                databaseRef.child("upload/blueprint/$postKey").setValue(blueprintDataWithImage)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Uploaded Successfully!", Toast.LENGTH_SHORT).show()
                        activity?.finish()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Blueprint Upload", "Failed to upload data", exception)
                    }
            },
            onFailure = { exception ->
                Log.e("Image Upload", "Failed to upload image", exception)
            }
        )
    } else {
        Log.e("Blueprint Upload", "Missing required data.")
    }
}

fun uploadImageToFirebaseStorage(
    imageUri: Uri,
    postKey: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storageRef = FirebaseStorage.getInstance().reference
    val fileRef = storageRef.child("uploads/blueprint/bp-$postKey.jpg")

    fileRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                val imageDownloadUrl = uri.toString()
                onSuccess(imageDownloadUrl)
            }
        }
        .addOnFailureListener { exception ->
            onFailure(exception)
        }
}

@Composable
fun ConfirmationDialog(context: Context, blueprintData: Map<String, Any>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Submission") },
        text = { Text("Upon confirmation, the blueprint will be uploaded shortly") },
        confirmButton = {
            TextButton(onClick = {
                handleSubmission(context, blueprintData)
                onDismiss()
            }) {
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
