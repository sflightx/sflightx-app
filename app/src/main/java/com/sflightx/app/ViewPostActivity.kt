package com.sflightx.app

import android.app.*
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.*
import com.sflightx.app.ui.theme.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

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

    // Derived state: is top bar not fully expanded?
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
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
                    Button(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Download")
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsLayout(blueprint: Blueprint?, scrollBehavior: TopAppBarScrollBehavior, snackbarHostState: SnackbarHostState) {
    val painter = rememberAsyncImagePainter(blueprint?.image_url)
    val collapsedFraction = scrollBehavior.state.collapsedFraction
    // Animate alpha based on scroll

    Column {
        PosterInfo(blueprint, collapsedFraction)
        RatingInfo(blueprint, collapsedFraction)
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }
        CommentInfo(blueprint, collapsedFraction, snackbarHostState)
    }
}

@Composable
fun RatingInfo(blueprint: Blueprint?, collapsedFraction: Float) {
    val rating = remember { mutableIntStateOf(0) }
    Box (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
fun CommentInfo(blueprint: Blueprint?, collapsedFraction: Float, snackbarHostState: SnackbarHostState) {
    val comments = remember { mutableStateListOf<Comment>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postId = blueprint?.key
    var isLoaded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

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
            .background(MaterialTheme.colorScheme.surfaceContainer)
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
                                .size(32.dp) // Adjust size of profile picture
                                .clip(CircleShape) // Make it circular
                                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
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
                    // Handle sending the message (e.g., save to database, etc.)
                    println("Message sent: $message")
                    text = "" // Optionally clear the input after sending
                }
            )
        }
    }
}

@Composable
fun PosterInfo(blueprint: Blueprint?, collapsedFraction: Float) {

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
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Account",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxHeight() // Ensure the Column takes up the full height
            ) {

                Text(
                    text = user?.username ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {}
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Follow",
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("Follow", style = MaterialTheme.typography.bodySmall)
                    }
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
                Button(
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Download")
                }
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
                imageVector = Icons.Default.Send,  // Using Material Icon for Send
                contentDescription = "Send Message",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}


data class User(
    val profile: String? = null,
    val username: String? = null,
    val email: String? = null,
    val uid: String? = null
)

data class Comment(
    val author: String = "",
    var username: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    var profilePictureUrl: String? = null
)

