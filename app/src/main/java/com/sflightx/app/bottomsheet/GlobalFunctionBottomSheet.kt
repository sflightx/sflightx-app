package com.sflightx.app.bottomsheet

import android.app.Activity
import android.content.*
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.content.pm.*
import coil.compose.*
import com.google.firebase.auth.*
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sflightx.app.*
import com.sflightx.app.R
import kotlinx.coroutines.*
import androidx.core.net.toUri
import com.sflightx.app.`class`.Comment
import com.sflightx.app.`class`.UserData
import com.sflightx.app.task.getUserByUid

@Composable
fun CheckUpdates(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    val versionName = packageInfo.versionName

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SFlightX App",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Version: $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Text("($versionCode)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(48.dp))

        VersionCheckIndicator(
            context = context,
            onUpdateNeeded = { latestVersion, updateUrl, changelog ->
                // Optionally handle update button pressed
            },
            onDismiss = onDismiss
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun CommentSection(
    key: String? = null,
    snackbarHostState: SnackbarHostState,
    scrollState: LazyListState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoaded by remember { mutableStateOf(false) }
    val comments = remember { mutableStateListOf<Comment>() }
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val user = FirebaseAuth.getInstance().currentUser

    val database = FirebaseDatabase.getInstance()
    val commentRef = database.getReference("comment/upload/blueprint/$key")

    // Use rememberUpdatedState so it doesn’t trigger multiple listeners
    val currentCommentRef by rememberUpdatedState(commentRef)

    DisposableEffect(key) {
        val commentsListener = currentCommentRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val comment = snapshot.getValue(Comment::class.java)
                comment?.let {
                    if (comments.none { existing -> existing.key == it.key }) {
                        comments.add(it)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updatedComment = snapshot.getValue(Comment::class.java)
                updatedComment?.let { updated ->
                    val index = comments.indexOfFirst { it.key == updated.key }
                    if (index != -1) {
                        comments[index] = updated
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val removedComment = snapshot.getValue(Comment::class.java)
                removedComment?.let { removed ->
                    comments.removeAll { it.key == removed.key }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("RealtimeError", "Error: $error", error.toException())
            }
        })

        onDispose {
            currentCommentRef.removeEventListener(commentsListener)
        }
    }

    LaunchedEffect(key) {
        fetchCommentsForPost(
            isRecent = false,
            postId = key.orEmpty(),
            onCommentsFetched = { commentsList ->
                comments.clear()
                comments.addAll(commentsList.distinctBy { it.key }) // Ensure no duplicates
                isLoaded = true
            },
            onError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
                isLoaded = true
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.close_24px),
                contentDescription = "close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onDismiss() }
            )
        }

        // Body
        when {
            !isLoaded -> {
                LoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .weight(1f)
                )
            }
            comments.isEmpty() -> {
                NoCommentsState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .weight(1f)
                )
            }
            else -> {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    reverseLayout = false
                ) {
                    itemsIndexed(comments) { _, comment ->
                        CommentItem(comment)
                    }
                }
            }
        }

        // Scroll to bottom once when fully loaded
        LaunchedEffect(isLoaded, comments.size) {
            if (isLoaded && comments.isNotEmpty()) {
                scrollState.animateScrollToItem(comments.lastIndex)
            }
        }

        // Comment Input
        CommentInputWithSend(
            text = text,
            onTextChange = { newText -> text = newText },
            onSendClick = { message ->
                if (user == null) {
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    return@CommentInputWithSend
                }
                val commentId = commentRef.push().key ?: return@CommentInputWithSend

                val comment = Comment(
                    author = user.uid,
                    message = message,
                    timestamp = System.currentTimeMillis(),
                    username = user.displayName.orEmpty(),
                    profilePictureUrl = user.photoUrl?.toString(),
                    key = commentId
                )

                commentRef.child(commentId).setValue(comment)
                    .addOnSuccessListener { text = "" }
                    .addOnFailureListener { e ->
                        Log.e("CommentUpload", "Failed to upload comment", e)
                        scope.launch { snackbarHostState.showSnackbar("Failed to upload comment") }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier) {
    Column(modifier) {
        Text("Loading comments...")
    }
}

@Composable
fun NoCommentsState(modifier: Modifier) {
    Column(modifier) {
        Text("No Comments")
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Column (
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column (
            modifier = Modifier.padding(horizontal = 16.dp).padding(vertical = 4.dp)
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
                modifier = Modifier.padding(start = 4.dp).padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        HorizontalDivider(Modifier.padding(top = 16.dp), 2.dp)
    }
}

fun openApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        // If the app is not installed, optionally open Play Store
        Toast.makeText(context, "Spaceflight Simulator not installed", Toast.LENGTH_SHORT).show()
        val playStoreIntent = Intent(Intent.ACTION_VIEW,
            "https://play.google.com/store/apps/details?id=$packageName".toUri())
        context.startActivity(playStoreIntent)
    }
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
    var userData by remember { mutableStateOf<UserData?>(null) }
    val user = FirebaseAuth.getInstance().currentUser
    val uid = user?.uid

    LaunchedEffect(uid) {
        if (uid != null) {
            val fetchedUser = getUserByUid(uid)
            userData = fetchedUser
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {


            IconButton(onClick = {
                if (user != null) {
                    val intent = Intent(context, ViewUserActivity::class.java)
                    intent.putExtra("key", uid)
                    intent.putExtra("data", userData)
                    context.startActivity(intent)
                } else {
                    val intent = Intent(context, LoginActivity::class.java)
                    context.startActivity(intent)
                    (context as Activity).finish()
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
            Column(
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