@file:Suppress("DEPRECATION")

package com.sflightx.app


import android.Manifest
import android.annotation.*
import android.app.*
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.*
import android.graphics.*
import android.net.*
import android.os.*
import android.provider.*
import android.util.*
import android.widget.*
import androidx.activity.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.*
import androidx.activity.result.contract.*
import androidx.browser.customtabs.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.app.*
import androidx.core.content.*
import androidx.core.net.*
import coil.compose.*
import com.google.firebase.*
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.storage.*
import com.google.gson.*
import com.google.gson.reflect.*
import com.sflightx.app.bottomsheet.*
import com.sflightx.app.ui.theme.*
import com.sflightx.library.imagecrop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.*
import java.io.*
import java.text.*
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    private val STORAGE_PERMISSION_CODE = 101
    private val PREFS_NAME = "sflightx.settings"
    private val KEY_FIRST_BOOT = "first_boot"
    private lateinit var cropImageLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        ensureDirectoryExists(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                GlobalBottomSheetHost {
                    MainAppLayout(
                        STORAGE_PERMISSION_CODE,
                        onOpenCropActivity = { imageUri ->
                            val intent = Intent(this, CropActivity::class.java).apply {
                                putExtra("imageUri", imageUri)
                            }
                            cropImageLauncher.launch(intent)
                        }
                    )
                }

            }
        }

        if (isFirstBoot(this)) {
            val intent = Intent(this, FirstBootActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, // Fixed to match exact expected type
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isFirstBoot(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (!sharedPreferences.contains(KEY_FIRST_BOOT)) return true
        return sharedPreferences.getBoolean(KEY_FIRST_BOOT, true)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
    storagePermissionCode: Int,
    onOpenCropActivity: (Uri) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomSheetController = LocalBottomSheetController.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val color = MaterialTheme.colorScheme.background.toArgb()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangeNameDialog by remember { mutableStateOf(false) }
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val croppedImageUri = data?.getParcelableExtra<Uri>("croppedImageUri")
                if (croppedImageUri != null) {
                    val inputStream = context.contentResolver.openInputStream(croppedImageUri)
                    val croppedBitmap = BitmapFactory.decodeStream(inputStream)
                    Log.d("ImageReceiver", "Cropped image received in MainAppLayout.")

                    // Upload the cropped image to Firebase
                    uploadProfilePictureToFirebase(
                        bitmap = croppedBitmap,
                        onSuccess = { imageUrl ->
                            Log.d("ImageReceiver", "Image uploaded successfully!")
                            Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { exception ->
                            Log.d("ImageReceiver", "Error uploading image: ${exception.message}")
                            Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Log.d("ImageReceiver", "No result received or action was canceled.")
            }
        }
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val imageUri: Uri = uri
                val intent = Intent(context, CropActivity::class.java).apply {
                    putExtra("imageUri", imageUri)
                }
                cropLauncher.launch(intent) // Use cropLauncher to launch CropActivity
            }
        }
    )

    bottomSheetController.show {
        CheckUpdates()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("Are you sure you want to log out of your account?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    (context as? Activity)?.finish()
                    showLogoutDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBottomSheet) {

        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },

            sheetState = sheetState
        ) {
            ProfileBottomSheetContent(
                onChangeName = {
                    showChangeNameDialog = true
                },
                onChangeProfilePicture = {
                    launcher.launch("image/*")
                },
                onLogout = {
                    showLogoutDialog = true
                },
                onClose = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }

    if (showChangeNameDialog) {
        UpdateDisplayNameDialog(
            onDismiss = { showChangeNameDialog = false },
            onSuccess = {
                Toast.makeText(context, "Change profile name success", Toast.LENGTH_SHORT).show()
            }
        )
    }

    FirebaseAuth.getInstance().currentUser
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SFlightX",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    val user = FirebaseAuth.getInstance().currentUser

                    IconButton(modifier = Modifier.padding(end = 8.dp), onClick = {
                        if (user != null) {
                            showBottomSheet = true
                        } else {
                            context.startActivity(Intent(context, LoginActivity::class.java))
                        }
                    }) {
                        if (user?.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.account_circle_24px),
                                contentDescription = "Account",
                                tint = colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.settings_24px),
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Modifier.height(80.dp)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        val iconRes = if (selectedTab == 0) {
                            R.drawable.home_filled_24px
                        } else {
                            R.drawable.home_24px
                        }
                        Icon(
                            painter = painterResource(id = iconRes), // Replace with your drawable resource
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        val iconRes = if (selectedTab == 1) {
                            R.drawable.edit_filled_24px
                        } else {
                            R.drawable.edit_24px
                        }
                        Icon(
                            painter = painterResource(id = iconRes), // Replace with your drawable resource
                            contentDescription = "Blueprint"
                        )
                    },
                    label = { Text("Blueprints") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        val iconRes = if (selectedTab == 2) {
                            R.drawable.newsmode_filled_24px
                        } else {
                            R.drawable.newsmode_24px
                        }
                        Icon(
                            painter = painterResource(id = iconRes), // Replace with your drawable resource
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("News") }
                )
            }
        },
        floatingActionButton = {
            ExpandableFab(
                onUploadClick = {
                    var url = "https://help.sflightx.com/sflightx-app/upload-using-app"
                    openCustomTab(context, url, color)
                },
                onCreateClick = {
                    Toast.makeText(context, "Soon!", Toast.LENGTH_SHORT)
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> HomeContent(context, storagePermissionCode)
                1 -> EditContent(context, snackbarHostState)
                2 -> NewsContent()
            }
        }
        FirebaseNotificationSnackbar()
    }
}

@Composable
fun HomeContent(context: Context, storagePermissionCode: Int) {
    val activity = context as? Activity
    var showAllItems by remember { mutableStateOf(false) }
    val database = FirebaseDatabase.getInstance()

    // Live data to hold the fetched blueprints
    var blueprints by remember { mutableStateOf<List<Blueprint>>(emptyList()) }

    // Fetch data from Firebase Realtime DatabaseManager when composable is first loaded
    LaunchedEffect(Unit) {
        val blueprintRef = database.getReference("upload/blueprint")

        blueprintRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Assuming the data is structured as a map of items
                val blueprintList = snapshot.children.mapNotNull { it.getValue(Blueprint::class.java) }
                blueprints = blueprintList.reversed()
            }
        }.addOnFailureListener {
            // Handle errors
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header and See More Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Latest SFS Blueprints",
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .weight(1f)
            )
            /*
            OutlinedButton(
                onClick = {},
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("See More")
            }
            */
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())

        ) {
            val itemsToShow = if (showAllItems) blueprints else blueprints.take(10)

            itemsToShow.forEach { blueprint ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.animateContentSize()
                            .background(colorScheme.surfaceContainerLow)
                            .clickable {
                                val intent = Intent(context, ViewPostActivity::class.java)
                                intent.putExtra("key", blueprint.key)
                                intent.putExtra("data", blueprint)
                                context.startActivity(intent)
                            }
                    ) {
                        // Image on top
                        Image(
                            painter = rememberAsyncImagePainter(blueprint.image_url),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .height(120.dp)
                                .fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = blueprint.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val result = remember(blueprint.author) {
                            mutableStateOf<Result<Map<String, Any>?>>(Result.success(null))
                        }

                        LaunchedEffect(blueprint.author) {
                            try {
                                val snapshot = FirebaseDatabase.getInstance()
                                    .getReference("userdata")
                                    .child(blueprint.author)
                                    .get()
                                    .await()

                                if (snapshot.exists()) {
                                    @Suppress("UNCHECKED_CAST")
                                    val data = snapshot.value as? Map<String, Any>
                                    result.value = Result.success(data)
                                } else {
                                    result.value = Result.success(null) // No data found
                                }
                            } catch (e: Exception) {
                                result.value = Result.failure(e)
                            }
                        }

                        result.value.onSuccess { data ->
                            val profileImageUrl = data?.get("profile") as? String
                            val authorName = data?.get("username") as? String

                            if (profileImageUrl != null && authorName != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    // User Profile Image in Circle
                                    Image(
                                        painter = rememberAsyncImagePainter(profileImageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Author Name Text
                                    Text(
                                        text = authorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface
                                    )
                                }
                            }
                        }.onFailure {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                text = "Error loading author data",
                                color = colorScheme.error
                            )
                        }

                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Handle permissions
    if (ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                storagePermissionCode
            )
        } else {
            openSettings(activity)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun EditContent(context: Context, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf<List<LibraryEntry>>(emptyList()) }

    Column (
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "Blueprints",
            color = colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box (
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Log and load the user library inside LaunchedEffect
            LaunchedEffect(Unit) {
                Log.d("EditContent", "LaunchedEffect: Loading user library...")
                val result = loadUserLibrary(context)

                if (result.isSuccess) {
                    val data = result.getOrDefault(emptyMap())
                    Log.d("EditContent", "Library loaded: ${data.size} items")
                    library = data.values.sortedByDescending { it.timestamp }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("EditContent", "Failed to load library: $error")
                    snackbarHostState.showSnackbar("Error: $error")
                    Log.d("EditContent", "Snackbar shown with error message")
                }
            }

            // Handle empty library state
            if (library.isEmpty()) {
                Log.d("EditContent", "Library is empty — showing empty state")
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No blueprints in your library yet.")
                }
            } else {
                // Use LibraryList composable here
                Log.d("EditContent", "Rendering LibraryList with ${library.size} items")
                LibraryList(
                    library = library,
                )
            }
        }
    }
}

@Composable
fun NewsContent() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Latest News",
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}


//Background Functions
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun getFilePaths(directoryPath: String, snackbarHostState: SnackbarHostState): List<String> {
    val scope = rememberCoroutineScope()
    val fileList = mutableListOf<String>()
    val directory = File(directoryPath)

    if (directory.exists() && directory.isDirectory) {
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                fileList.add(file.absolutePath)
            }
        }
    } else {
        scope.launch {
            snackbarHostState.showSnackbar("The specified path is not a directory or does not exist.")
        }
    }

    return fileList
}

@SuppressLint("QueryPermissionsNeeded")
private fun openSettings(activity: Activity) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${activity.packageName}".toUri()
    }
    if (intent.resolveActivity(activity.packageManager) != null) {
        activity.startActivity(intent)
    }
}

fun ensureDirectoryExists(context: Context): File? {
    // Access the private directory: Android/data/<your_app_package_name>/files/blob/blueprints
    val targetDirectory = File(context.getExternalFilesDir(null), "blob/blueprints")

    // Check if the directory exists; if not, create it
    if (!targetDirectory.exists()) {
        val isCreated = targetDirectory.mkdirs()
        if (isCreated) {
            println("Directory created: ${targetDirectory.absolutePath}")
        } else {
            println("Failed to create directory: ${targetDirectory.absolutePath}")
            return null // Return null if directory creation failed
        }
    } else {
        println("Directory already exists: ${targetDirectory.absolutePath}")
    }

    return targetDirectory // Return the directory object
}

@Composable
fun ProfileBottomSheetContent(
    onChangeName: () -> Unit,
    onChangeProfilePicture: () -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            val user = FirebaseAuth.getInstance().currentUser

            IconButton(onClick = {
                if (user != null) {
                    TODO()
                } else {
                    TODO()
                }
            }) {
                if (user?.photoUrl != null) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.account_circle_24px),
                        contentDescription = "Account",
                        tint = colorScheme.onSurface
                    )
                }
            }
            Column (
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Hello, ${user?.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "UID: ${user?.uid}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(user?.uid.toString()))
                            Toast.makeText(context, "UID copied to clipboard", Toast.LENGTH_SHORT)
                                .show()
                        }
                )
            }
        }
        HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            text = "Profile Options",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                onChangeName()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Change Name")
        }

        Button(
            onClick = {
                onChangeProfilePicture()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("Change Profile Picture")
        }

        OutlinedButton(
            onClick = {
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Log Out")
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Close")
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpandableFab(
    onUploadClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "FAB Rotation")

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Upload") },
                    icon = { Icon(painter = painterResource(id = R.drawable.share_24px), contentDescription = "Upload") },
                    onClick = {
                        expanded = false
                        onUploadClick()
                    }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Create") },
                    icon = { Icon(painter = painterResource(id = R.drawable.add_24px), contentDescription = "Add") },
                    onClick = {
                        expanded = false
                        onCreateClick()
                    }
                )
            }

            FloatingActionButton(
                onClick = { expanded = !expanded }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.add_24px),
                    contentDescription = "Toggle",
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

@Composable
fun UpdateDisplayNameDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    updateFirebaseDisplayName(
                        newName = newName.trim(),
                        onSuccess = {
                            isLoading = false
                            onSuccess()
                            onDismiss()
                        },
                        onFailure = {
                            isLoading = false
                            errorMessage = it.message ?: "Update failed"
                        }
                    )
                },
                enabled = newName.isNotBlank() && !isLoading
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun updateFirebaseDisplayName(
    newName: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val uid = user.uid
        val profileUpdates = userProfileChangeRequest {
            displayName = newName
        }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val databaseRef = FirebaseDatabase.getInstance().getReference("userdata").child(uid).child("username")

                    databaseRef.setValue(newName)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { dbError -> onFailure(dbError) }

                } else {
                    onFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    } else {
        onFailure(Exception("No authenticated user"))
    }
}

fun uploadProfilePictureToFirebase(
    bitmap: Bitmap?,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val uid = user.uid
        val storageRef = FirebaseStorage.getInstance().getReference("profile/$uid.jpeg")

        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos) // Compress to JPEG format
        val byteArray = baos.toByteArray()

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        storageRef.putBytes(byteArray, metadata)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Upload failed.")
                }
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUrl ->
                // 1. Update Firebase Auth
                val profileUpdates = userProfileChangeRequest {
                    photoUri = downloadUrl
                }

                user.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        // 2. Save to database
                        val dbRef = FirebaseDatabase.getInstance()
                            .getReference("userdata").child(uid).child("profile")

                        dbRef.setValue(downloadUrl.toString())
                            .addOnSuccessListener { onSuccess(downloadUrl.toString()) }
                            .addOnFailureListener { onFailure(it) }
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    } else {
        onFailure(Exception("User not logged in!"))
    }
}

@SuppressLint("UseKtx")
@Composable
fun FirebaseNotificationSnackbar() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val prefs = context.getSharedPreferences("sflightx.settings", Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()
    var notification by remember { mutableStateOf<InAppNotification?>(null) }

    // Fetch notification only once
    LaunchedEffect(Unit) {
        fetchInAppNotification { notif ->
            if (notif != null) {
                Log.d("InAppNotification", "Fetched Notification: ${notif}")

                // Check if visible field is true
                if (notif.visible) {
                    // Log and proceed with showing the snackbar
                    Log.d("InAppNotification", "Notification visible: ${notif.message}")

                    // Define the key for SharedPreferences to track whether the user has seen this notification
                    val seenKey = "notif_seen_${notif.title}_${notif.timestamp}"

                    // Check if the key exists in SharedPreferences
                    val hasSeen = prefs.contains(seenKey) && prefs.getBoolean(seenKey, false)

                    // If the notification hasn't been seen, show it
                    if (!hasSeen) {
                        notification = notif

                        // Use coroutineScope to show the snackbar
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = notif.message,
                                actionLabel = "Dismiss",
                                duration = SnackbarDuration.Indefinite
                            )
                            // When user dismisses the snackbar, mark it as seen
                            if (result == SnackbarResult.ActionPerformed) {
                                prefs.edit().putBoolean(seenKey, true).apply()
                            }
                        }
                    }
                } else {
                    Log.d("InAppNotification", "Notification is not visible: ${notif.message}")
                }
            } else {
                Log.d("InAppNotification", "Notification is null")
            }
        }
    }
}


fun fetchInAppNotification(onNotification: (InAppNotification?) -> Unit) {
    val ref = Firebase.database.reference.child("notification").child("update").child("app")
    ref.get().addOnSuccessListener {
        val notif = it.getValue(InAppNotification::class.java)
        onNotification(notif)
    }.addOnFailureListener {
        onNotification(null)
    }
}

fun loadUserLibrary(context: Context): Result<Map<String, LibraryEntry>> {
    return try {
        val file = File(context.getExternalFilesDir(null), "library/blueprint/library.json")
        Log.d("loadUserLibrary", "Looking for file at: ${file.absolutePath}")

        if (file.exists()) {
            val gson = Gson()
            val type = object : TypeToken<Map<String, LibraryEntry>>() {}.type
            val json = file.readText()

            Log.d("loadUserLibrary", "File found. Raw JSON: $json")

            val data = gson.fromJson<Map<String, LibraryEntry>>(json, type)

            if (data == null) {
                Log.w("loadUserLibrary", "Parsed data is null, returning empty map.")
                Result.success(emptyMap())
            } else {
                Log.d("loadUserLibrary", "Parsed ${data.size} entries from library.")
                Result.success(data)
            }
        } else {
            Log.e("loadUserLibrary", "File not found.")
            Result.failure(FileNotFoundException("Library file not found."))
        }
    } catch (e: Exception) {
        Log.e("loadUserLibrary", "Exception occurred: ${e.message}", e)
        Result.failure(e)
    }
}

@Composable
fun LibraryEntryItem(
    entry: LibraryEntry,
    isSelected: Boolean,
    onSelectedChanged: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelectedChanged() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleLarge)
            Text("Saved at: ${formatTimestamp(entry.timestamp)}", style = MaterialTheme.typography.bodySmall)

            Column(modifier = Modifier.animateContentSize()) {
                if (isSelected) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {}) {
                            Text("Edit")
                        }
                        val color = colorScheme.background.toArgb()
                        val context = LocalContext.current
                        Button(onClick = {
                            Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show()
                            var key = entry.postKey
                            val dataRef = FirebaseDatabase.getInstance().reference
                                .child("upload")
                                .child("blueprint")
                                .child(key)
                                .child("file_link")

                            dataRef.get().addOnSuccessListener { snapshot ->

                                val link = snapshot.getValue(String::class.java)
                                retreiveFile(context, link.toString(), color)

                            }.addOnFailureListener {
                                showDialog = true
                            }
                        }) {
                            Text("Download")
                        }
                        SimpleAlertDialog(
                            title = "No Blueprint Found",
                            text = "The original blueprint could not be found. It may have been deleted, moved or relocated.",
                            confirmText = "OK",
                            showDialog = showDialog,
                            onDismiss = { showDialog = false },
                            onConfirm = {
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryList(library: List<LibraryEntry>) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LazyColumn {
        itemsIndexed(library) { index, entry ->
            LibraryEntryItem(
                entry = entry,
                isSelected = selectedIndex == index,
                onSelectedChanged = {
                    selectedIndex = if (selectedIndex == index) null else index
                }
            )
        }
    }
}

fun retreiveFile(context: Context, url: String, color: Int) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setToolbarColor(color)  // Directly use the ARGB color value
        .build()
    customTabsIntent.launchUrl(context, url.toUri())
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SimpleAlertDialog(
    title: String? = "Confirm Action",
    text: String? = "The developer did not put any description.",
    confirmText: String? = "Yes",
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title.toString()) },
            text = { Text(text.toString()) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(confirmText.toString())
                }
            }
        )
    }
}
