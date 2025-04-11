@file:Suppress("DEPRECATION")

package com.sflightx.app


import android.Manifest
import android.annotation.*
import android.app.*
import android.app.Activity.RESULT_OK
import android.content.*
import android.content.pm.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.*
import android.os.*
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.activity.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
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
import com.sflightx.library.imagecrop.*
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.storage.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.*


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
                MainAppLayout(
                    STORAGE_PERMISSION_CODE,
                    onOpenCropActivity = { imageUri ->
                        val intent = Intent(this, com.sflightx.library.imagecrop.CropActivity::class.java).apply {
                            putExtra("imageUri", imageUri)
                        }
                        cropImageLauncher.launch(intent)
                    }
                )
            }
        }

        if (isFirstBoot(this)) {
            Toast.makeText(this, "App hasn't booted before", Toast.LENGTH_SHORT).show()
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
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Account",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
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
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = "Blueprint") },
                    label = { Text("Blueprints") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.newsmode_24px),
                            contentDescription = "News"
                        )
                    },
                    label = { Text("News") }
                )
            }
        },
        floatingActionButton = {
            ExpandableFab(
                onUploadClick = {
                    context.startActivity(Intent(context, UploadActivity::class.java))
                },
                onCreateClick = {
                    TODO()
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
                blueprints = blueprintList
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .weight(1f)
            )
            OutlinedButton(
                onClick = {},
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("See More")
            }
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
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
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
                            color = MaterialTheme.colorScheme.onSurface,
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
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    // User Profile Image in Circle
                                    Image(
                                        painter = rememberAsyncImagePainter(profileImageUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            ),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Author Name Text
                                    Text(
                                        text = authorName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }.onFailure {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                text = "Error loading author data",
                                color = MaterialTheme.colorScheme.error
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
    val directoryPath = File(context.getExternalFilesDir(null), "blob/blueprints")
    val scope = rememberCoroutineScope()
    val filePaths = getFilePaths(directoryPath.toString(), snackbarHostState)
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("SFS", "JNO", "Ellipse")
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = { selectedIndex = index },
                    selected = index == selectedIndex,
                    label = { Text(label) }
                )
            }
        }
        if (filePaths.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No such file or directory. Download blueprints in home to get started.",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            scope.launch {
                snackbarHostState.showSnackbar("The specified path is not a directory or does not exist.")
            }
            return
        }
        Column {
            LazyColumn(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                items(filePaths) { filePath ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "info"
                        )
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .weight(1f),
                        ) {
                            Text(
                                text = File(filePath).name,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "${File(filePath).length().toLong() / 1024} KB",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "more"
                        )
                    }
                    HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(2.dp))
                }
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
                text = "Latest SFS Blueprints",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    icon = { Icon(Icons.Default.Share, contentDescription = "Upload") },
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
                    icon = { Icon(Icons.Default.Create, contentDescription = "Create") },
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
                    imageVector = Icons.Default.Add,
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
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
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



data class Blueprint(
    val name: String = "",
    val link: String = "",
    val image_url: String = "",
    val author: String = "",
    val key: String = ""
) : Serializable