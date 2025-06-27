@file:Suppress("DEPRECATION")

package com.sflightx.app

import android.annotation.*
import android.app.*
import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.browser.customtabs.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.core.net.*
import coil.compose.*
import com.google.android.gms.tasks.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.*
import com.google.firebase.ktx.*
import com.google.gson.*
import com.google.gson.reflect.*
import com.sflightx.app.bottomsheet.CommentSection
import com.sflightx.app.bottomsheet.GlobalBottomSheetHost
import com.sflightx.app.bottomsheet.LocalBottomSheetController
import com.sflightx.app.`class`.BlueprintData
import com.sflightx.app.`class`.Comment
import com.sflightx.app.`class`.LibraryEntry
import com.sflightx.app.`class`.UserData
import com.sflightx.app.task.getUserByUid
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*
import java.io.*



@Suppress("DEPRECATION")
class ViewPostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val key = intent.getStringExtra("key") ?: "null"
        val data = intent.getSerializableExtra("data") as? BlueprintData

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                GlobalBottomSheetHost {
                    ViewPostLayout(key, data)
                }

            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPostLayout(key: String, data: BlueprintData?) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    val scrollState = rememberLazyListState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = data?.name ?: "Unnamed Post",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = (38 - 16 * collapsedFraction).sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val url = "https://sflightx.com/bp/$key"
                        shareBlueprint(context, url)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.share_24px),
                            contentDescription = "Share"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                OpenLinkButton(
                    key = data?.file_link ?: "null",
                    postKey = data?.key ?: "null",
                    name = data?.name ?: "null",
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    snackbarHostState = snackbarHostState
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),  // Use fillMaxHeight() instead of fillMaxSize() to avoid infinite height
                state = scrollState
            ) {
                item {
                    DetailsLayout(data, snackbarHostState, scrollState)
                }
            }
        }
    }
}

@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsLayout(blueprintData: BlueprintData?, snackbarHostState: SnackbarHostState, scrollState: LazyListState) {
    val painter = rememberAsyncImagePainter(blueprintData?.image_url)
    var imageHeight by remember { mutableIntStateOf(250) }

    Column {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .animateContentSize()
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .animateContentSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    imageHeight = if (imageHeight == 250) {
                        750
                    } else {
                        250
                    }
                }) {
                    Icon(
                        painter = painterResource(if (imageHeight == 250) R.drawable.expand_content_24px else R.drawable.collapse_content_24px),
                        contentDescription = "Open",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
        AuthorInfo(blueprintData)
        FileInfo(blueprintData)
        //RatingInfo(blueprintData)
        CommentInfo(blueprintData, snackbarHostState, scrollState)
    }
}

@Composable
fun FileInfo(blueprintData: BlueprintData?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download_24px),
                    contentDescription = "Downloads",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprintData?.downloads.toString())
            }
            VerticalDivider(thickness = 1.dp)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.star_24px),
                    contentDescription = "Downloads",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(/*"%.1f".format(blueprintData?.rating ?: 0.0)*/"N/A")
            }
            VerticalDivider(thickness = 1.dp)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.check_24px),
                    contentDescription = "Type",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprintData?.req_type.toString())
            }
            VerticalDivider(thickness = 1.dp)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info_24px),
                    contentDescription = "Game",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprintData?.req_game.toString())
            }
        }
    }
}

@Composable
fun RatingInfo(blueprintData: BlueprintData?) {
    val rating = remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(bottom = 16.dp),
                text = "%.1f".format(blueprintData?.rating ?: 0.0),
                style = MaterialTheme.typography.titleMedium

            )
            RatingBar(rating = rating, modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInfo(blueprintData: BlueprintData?, snackbarHostState: SnackbarHostState, scrollState: LazyListState) {
    val bottomSheetController = LocalBottomSheetController.current
    val comments = remember { mutableStateListOf<Comment>() }
    val scope = rememberCoroutineScope()
    val postId = blueprintData?.key
    var isLoaded by remember { mutableStateOf(false) }
    var lastCommentKey: String? = null

    LaunchedEffect(postId) {
        if (postId != null) {
            fetchCommentsForPost(
                isRecent = true,
                postId = postId,
                onCommentsFetched = { commentsList ->
                    // Clear the existing list before adding new comments
                    comments.clear()
                    comments.addAll(commentsList.reversed())

                    // Save the key of the last fetched comment for pagination
                    if (comments.isNotEmpty()) {
                        lastCommentKey =
                            comments.last().key // Assuming 'key' is the unique identifier for each comment
                    }
                    isLoaded = true
                },
                onError = { message ->
                    // Show error message using Snackbar
                    scope.launch {
                        snackbarHostState.showSnackbar(message)
                    }
                    isLoaded = true
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .padding(vertical = 24.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f),
                    text = "Comments",
                    style = MaterialTheme.typography.headlineMedium
                )
                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_24px),
                    contentDescription = "more",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            bottomSheetController.show {
                                CommentSection(
                                    key = postId,
                                    onDismiss = { bottomSheetController.hide() },
                                    snackbarHostState = snackbarHostState,
                                    scrollState = scrollState
                                )
                            }
                        },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isLoaded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(24.dp)
                ) {
                    Text("Loading comments...")
                }
            } else if (comments.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .padding(24.dp)
                ) {
                    Text("No Comments")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between items in the column
                ) {
                    // Loop through each comment and display
                    comments.forEach { comment ->
                        Column {
                            Column (
                                modifier = Modifier.padding(vertical = 4.dp).padding(horizontal = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp), // Consistent vertical padding
                                    verticalAlignment = Alignment.CenterVertically // Align text and profile picture in the center
                                ) {
                                    // Profile picture (optional)
                                    Image(
                                        painter = rememberAsyncImagePainter(comment.profilePictureUrl), // Load the profile picture
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(32.dp) // Adjust size of profile picture
                                            .clip(CircleShape) // Make it circular
                                    )

                                    Spacer(modifier = Modifier.width(8.dp)) // Spacing between the image and the text

                                    // Author's name
                                    Text(
                                        text = comment.username,
                                        modifier = Modifier.padding(end = 4.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold // Bold the author's name
                                    )
                                }
                                Text(
                                    text = comment.message,
                                    modifier = Modifier.padding(start = 4.dp).padding(top = 4.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row (
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box {
                                        VoteElement(
                                            modifier = Modifier
                                                .padding(0.dp)
                                                .background(Color.LightGray, RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(Modifier.padding(top = 16.dp), 2.dp)
                        }
                    }
                }
            }
            Text(
                text = "Loading the first 5 comments.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AuthorInfo(blueprintData: BlueprintData?) {
    var userData by remember { mutableStateOf<UserData?>(null) }
    val context = LocalContext.current

    LaunchedEffect(blueprintData?.author) {
        if (blueprintData?.author != null) {
            val fetchedUser = getUserByUid(blueprintData.author)
            userData = fetchedUser
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp)
        ) {
            if (!userData?.profile.isNullOrEmpty()) {
                AsyncImage(
                    model = userData?.profile,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.person_24px),
                    contentDescription = "Account",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = userData?.username ?: "Unknown UserData",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
        ) {
            OutlinedButton(
                onClick = {}
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_24px),
                        contentDescription = "Follow",
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Follow", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(context, ViewUserActivity::class.java)
                    intent.putExtra("key", userData?.uid)
                    intent.putExtra("data", userData)
                    context.startActivity(intent)
                }
            ) {
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.person_24px),
                        contentDescription = "View Profile",
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("View Profile", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun fetchCommentsForPost(
    isRecent: Boolean = false,
    postId: String,
    onCommentsFetched: (List<Comment>) -> Unit,
    onError: (String) -> Unit,
    lastCommentKey: String? = null // For pagination, pass the last comment key
) {
    val ref = FirebaseDatabase.getInstance().reference
        .child("comment")
        .child("upload")
        .child("blueprint")
        .child(postId)

    var query = ref.orderByKey()

    when {
        isRecent -> {
            query = query.limitToLast(5)
        }
        lastCommentKey != null -> {
            query = query.endBefore(lastCommentKey).limitToLast(5)
        }
        else -> {
        }
    }

    query.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                onCommentsFetched(emptyList())
                return
            }

            // Collect comments and unique UIDs
            val uids = mutableSetOf<String>()
            val comments = mutableListOf<Comment>()
            comments.clear()

            snapshot.children.forEach { commentSnapshot ->
                val comment = commentSnapshot.getValue(Comment::class.java)
                comment?.let {
                    uids.add(it.author)
                    comments.add(it.copy(key = commentSnapshot.key ?: "")) // Store comment ID for pagination
                }
            }


            onCommentsFetched(comments)
        }

        override fun onCancelled(error: DatabaseError) {
            onError(error.message)
        }
    })
}

fun fetchCommentUserData(
    uids: Set<String>,
    onProfilesFetched: (Map<String, Pair<String, String>>) -> Unit // Map<uid, Pair<profilePictureUrl, username>>
) {
    val ref = FirebaseDatabase.getInstance().reference.child("userdata")

    val profilesAndUsernames =
        mutableMapOf<String, Pair<String, String>>() // Map<uid, Pair(profilePictureUrl, username)>

    val tasks =
        mutableListOf<Task<DataSnapshot>>()  // List to store our tasks for concurrent fetching

    uids.forEach { uid ->
        // Get both profile picture and username
        val task = ref.child(uid).get()
        tasks.add(task)

        // Once the data for each user is fetched
        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val userDataSnapshot = result.result
                val profilePictureUrl =
                    userDataSnapshot?.child("profile")?.getValue(String::class.java)
                val username = userDataSnapshot?.child("username")?.getValue(String::class.java)

                if (profilePictureUrl != null && username != null) {
                    profilesAndUsernames[uid] = Pair(profilePictureUrl, username)
                }
            }

            // When all tasks are completed, call onProfilesFetched
            if (tasks.all { it.isComplete }) {
                onProfilesFetched(profilesAndUsernames)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInputWithSend(
    onSendClick: (String) -> Unit,
    onTextChange: (String) -> Unit,
    text: String,
    modifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f),
            placeholder = {
                Text(
                    "Type a message...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { onSendClick(text) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.send_24px),
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        )
    }
}

@Composable
fun RatingBar(
    rating: MutableState<Int>,
    modifier: Modifier = Modifier,
    totalStars: Int = 5
) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        for (i in 1..totalStars) {
            IconButton(
                onClick = { rating.value = i },
                modifier = Modifier
                    .clickable { rating.value = i }
            ) {
                val starIcon = if (i <= rating.value) {
                    painterResource(id = R.drawable.star_filled_24px) // Replace with your filled star icon
                } else {
                    painterResource(id = R.drawable.star_24px) // Replace with your empty star icon
                }
                Icon(painter = starIcon, contentDescription = "Star $i")
            }
        }
    }
}

@Composable
fun OpenLinkButton(
    key: String,
    postKey: String,
    name: String,
    modifier: Modifier = Modifier,
    label: String = "Download",
    onClick: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val color = MaterialTheme.colorScheme.background.toArgb()
    val coroutineScope = rememberCoroutineScope()
    val url = key

    Button(
        onClick = {
            onClick?.invoke()
            incrementDownloadCount(postKey)

            saveToUserLibrary(context, postKey, name) { success, message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }

            openCustomTab(context, url, color)
        },
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.download_24px),
            contentDescription = "Follow",
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}


fun incrementDownloadCount(postKey: String) {
    val database = Firebase.database.reference
    val blueprintRef = database.child("upload").child("blueprint").child(postKey)

    // Access the current download count
    blueprintRef.child("downloads").get().addOnSuccessListener { snapshot ->
        val currentDownloads = snapshot.getValue(Long::class.java) ?: 0
        val newDownloads = currentDownloads + 1

        // Update the download count in the database
        blueprintRef.child("downloads").setValue(newDownloads)
            .addOnSuccessListener {
            }
            .addOnFailureListener { exception ->
            }
    }
}

fun openCustomTab(context: Context, url: String, color: Int) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setToolbarColor(color)  // Directly use the ARGB color value
        .build()
    customTabsIntent.launchUrl(context, url.toUri())
}

fun shareBlueprint(context: Context, url: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this rocket!")
        putExtra(Intent.EXTRA_TEXT, url)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share blueprint via"))
}

fun saveToUserLibrary(
    context: Context,
    postKey: String,
    name: String,
    onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
) {
    val gson = Gson()
    val entry = LibraryEntry(postKey, name)

    val dir = File(context.getExternalFilesDir(null), "library/blueprint")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "library.json")

    val type = object : TypeToken<MutableMap<String, LibraryEntry>>() {}.type
    val currentData: MutableMap<String, LibraryEntry> = if (file.exists()) {
        try {
            gson.fromJson(file.readText(), type) ?: mutableMapOf()
        } catch (e: Exception) {
            onResult(false, "Error reading library: ${e.message}")
            return
        }
    } else {
        mutableMapOf()
    }

    currentData[postKey] = entry

    try {
        file.writeText(gson.toJson(currentData))
        onResult(true, "Saved to library, opening app...")
    } catch (e: IOException) {
        onResult(false, "Error saving to library: ${e.message}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteElement(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<String?>(null) }

    Box (
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            IconButton(
                onClick = {
                    selected = if (selected == "like") null else "like"
                },
                modifier = Modifier
                    .background(
                        if (selected == "like") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    .height(32.dp)
                    .width(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(if (selected == "like") R.drawable.thumb_up_filled_24px else R.drawable.thumb_up_24px),
                    contentDescription = "Like",
                    tint = if (selected == "like") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = {
                    selected = if (selected == "dislike") null else "dislike"
                },
                modifier = Modifier
                    .background(
                        if (selected == "dislike") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                    .height(32.dp)
                    .width(48.dp)
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(if (selected == "dislike") R.drawable.thumb_down_filled_24px else R.drawable.thumb_down_24px),
                    contentDescription = "Dislike",
                    tint = if (selected == "dislike") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}