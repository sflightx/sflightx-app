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
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
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
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.database.ktx.*
import com.google.firebase.ktx.*
import com.google.gson.*
import com.google.gson.reflect.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*
import java.io.*


@Suppress("DEPRECATION")
class ViewPostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val key = intent.getStringExtra("key") ?: "null"
        val data = intent.getSerializableExtra("data") as? Blueprint

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                ViewPostLayout(key, data)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewPostLayout(key: String, data: Blueprint?) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val showBottomBar by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction > 0f }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = data?.name ?: "Unnamed Post",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        Icon(painter = painterResource(id = R.drawable.share_24px), contentDescription = "Share")
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
            if (showBottomBar) {
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                DetailsLayout(data, scrollBehavior, snackbarHostState)
            }
        }
    }
}

@SuppressLint("AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsLayout(blueprint: Blueprint?, scrollBehavior: TopAppBarScrollBehavior, snackbarHostState: SnackbarHostState) {
    val painter = rememberAsyncImagePainter(blueprint?.image_url)
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    var imageHeight by remember { mutableIntStateOf(250) }
    // Animate alpha based on scroll

    Column {
        PosterInfo(blueprint, collapsedFraction, snackbarHostState)
        FileInfo(blueprint)
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
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
                    imageHeight = if (imageHeight == 250) { 750 } else { 250 }
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
        RatingInfo()
        CommentInfo(blueprint, snackbarHostState)
    }
}

@Composable
fun FileInfo(blueprint: Blueprint?) {
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download_24px),
                    contentDescription = "Downloads",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprint?.downloads.toString())
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight(1f), thickness = 1.dp)
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.star_24px),
                    contentDescription = "Downloads",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("%.1f".format(blueprint?.rating ?: 0.0))
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight(1f), thickness = 1.dp)
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.check_24px),
                    contentDescription = "Type",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprint?.req_type.toString())
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight(1f), thickness = 1.dp)
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info_24px),
                    contentDescription = "Game",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(blueprint?.req_game.toString())
            }
        }
    }
}

@Composable
fun RatingInfo() {
    val rating = remember { mutableIntStateOf(0) }
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(bottom = 16.dp),
                text = "Ratings",
                style = MaterialTheme.typography.headlineSmall

            )
            RatingBar(rating = rating, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CommentInfo(blueprint: Blueprint?, snackbarHostState: SnackbarHostState) {
    val comments = remember { mutableStateListOf<Comment>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postId = blueprint?.key
    var isLoaded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var user = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(postId) {
        fetchCommentsForPost(
            postId = postId.toString(),
            onCommentsFetched = { commentsList ->
                comments.clear()
                comments.addAll(commentsList)
                isLoaded = true
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
                isLoaded = true
            }
        )
    }
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column (
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row {
                Text(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .weight(1f),
                    text = "User Comments",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (!isLoaded) {
                Column(Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    Text("Loading comments...")
                }
            } else if (comments.isEmpty()) {
                Column(Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    Text("No Comments")
                }
            } else {
                comments.forEach { comment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp), // Consistent vertical padding
                        verticalAlignment = Alignment.CenterVertically // Align text and profile picture in the center
                    ) {
                        // Profile picture (optional)
                        // You could use a profile picture here
                        Image(
                            painter = rememberAsyncImagePainter(comment.profilePictureUrl), // Load the profile picture
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(24.dp) // Adjust size of profile picture
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

                        // Comment message
                        Text(
                            text = comment.message,
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            CommentInputWithSend(
                text = text,
                onTextChange = { newText -> text = newText },
                onSendClick = { message ->
                    if (user == null) {
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                    println("Message sent: $message")
                    text = "" // Optionally clear the input after sending
                }
            )
        }
    }
}

@Composable
fun PosterInfo(blueprint: Blueprint?, collapsedFraction: Float, snackbarHostState: SnackbarHostState) {

    var user by remember { mutableStateOf<User?>(null) }
    val showButton = collapsedFraction < 1f
    val scale by animateFloatAsState(
        targetValue = if (showButton) 1f else 0.95f,
        animationSpec = tween(300),
        label = "Scale"
    )

    val alphaCol by animateFloatAsState(
        targetValue = if (showButton) 1f else 0.85f,
        animationSpec = tween(300),
        label = "AlphaColumn"
    )

    LaunchedEffect(blueprint?.author) {
        if (blueprint?.author != null) {
            val fetchedUser = getUserByUid(blueprint.author)
            user = fetchedUser
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = alphaCol
            }
            .animateContentSize(animationSpec = tween(300))
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            if (!user?.profile.isNullOrEmpty()) {
                AsyncImage(
                    model = user?.profile,
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
            Text(
                text = user?.username ?: "Unknown User",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
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
                onClick = {}
            ) {
                Row(
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

        AnimatedContent(
            targetState = showButton,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            label = "DownloadButtonAnimation"
        ) { visible ->
            if (visible) {
                OpenLinkButton(
                    key = blueprint?.file_link ?: "null",
                    postKey = blueprint?.key ?: "null",
                    name = blueprint?.name ?: "null",
                    snackbarHostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            } else {
                Spacer(modifier = Modifier.height(0.dp)) // Keeps animation smooth
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun getUserByUid(uid: String): User? = suspendCancellableCoroutine { cont ->
    val userRef = FirebaseDatabase.getInstance().getReference("userdata").child(uid)

    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val user = snapshot.getValue(User::class.java)
            cont.resume(user) {}
        }

        override fun onCancelled(error: DatabaseError) {
            cont.resume(null) {}
        }
    }

    userRef.addListenerForSingleValueEvent(listener)

    cont.invokeOnCancellation {
        userRef.removeEventListener(listener)
    }
}

fun fetchCommentsForPost(
    postId: String,
    onCommentsFetched: (List<Comment>) -> Unit,
    onError: (String) -> Unit
) {
    val ref = FirebaseDatabase.getInstance().reference
        .child("comment")
        .child("upload")
        .child("blueprint")
        .child(postId)

    ref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                onCommentsFetched(emptyList())
                return
            }

            // Collect all unique UIDs (authors)
            val uids = mutableSetOf<String>()
            val comments = mutableListOf<Comment>()

            snapshot.children.forEach { commentSnapshot ->
                val comment = commentSnapshot.getValue(Comment::class.java)
                comment?.let {
                    uids.add(it.author) // Collect UID from the author field
                    comments.add(it)
                }
            }

            // Fetch profile pictures and usernames for all UIDs
            fetchCommentUserData(uids) { profilesAndUsernames ->
                // Now associate profile pictures and usernames with comments
                comments.forEach { comment ->
                    val (profilePictureUrl, username) = profilesAndUsernames[comment.author] ?: Pair("", "")
                    comment.profilePictureUrl = profilePictureUrl
                    comment.username = username
                }
                onCommentsFetched(comments)
            }
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

    val profilesAndUsernames = mutableMapOf<String, Pair<String, String>>() // Map<uid, Pair(profilePictureUrl, username)>

    val tasks = mutableListOf<Task<DataSnapshot>>()  // List to store our tasks for concurrent fetching

    uids.forEach { uid ->
        // Get both profile picture and username
        val task = ref.child(uid).get()
        tasks.add(task)

        // Once the data for each user is fetched
        task.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val userDataSnapshot = result.result
                val profilePictureUrl = userDataSnapshot?.child("profile")?.getValue(String::class.java)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentInputWithSend(
    onSendClick: (String) -> Unit,  // Callback when the "Send" button is clicked
    onTextChange: (String) -> Unit,  // Callback when the text is changed
    text: String,  // The current text in the input field
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TextField for input
        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)  // This makes the TextField take up all available space
                .padding(end = 8.dp),  // Space between TextField and Send button
            placeholder = { Text("Type a message...", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        )

        // Send button
        IconButton(onClick = { onSendClick(text) }) {
            Icon(
                painter = painterResource(id = R.drawable.send_24px),  // Using Material Icon for Send
                contentDescription = "Send Message",
                tint = MaterialTheme.colorScheme.primary
            )
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
