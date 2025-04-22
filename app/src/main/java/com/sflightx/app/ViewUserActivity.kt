@file:Suppress("CAST_NEVER_SUCCEEDS")

package com.sflightx.app

import android.app.*
import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import coil.compose.*
import com.google.firebase.database.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.tasks.*
import android.util.Log
import androidx.compose.ui.Modifier
import com.sflightx.app.layout.LoadBlueprintDesign


@Suppress("DEPRECATION", "CAST_NEVER_SUCCEEDS")
class ViewUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val key = intent.getStringExtra("key") ?: "null"
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("data", UserData::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra("data") as? UserData
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                ViewUserLayout(key, data)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ViewUserLayout(key: String, data: UserData?) {
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val tabTitles = listOf("Home", "Uploads", "Library")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var allKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var blueprints by remember { mutableStateOf<List<BlueprintData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val pageSize = 10

    val listState = rememberLazyListState()

    // Load all keys once
    LaunchedEffect(data?.uid) {
        Log.d("ViewUserActivity", "Received data: $data")
        if (data?.uid != null) {
            fetchUploadKeys(data.uid.toString()) { keys ->
                allKeys = keys
                loadNextPage(keys, 0, pageSize) { nextPage ->
                    blueprints = nextPage
                    isLoading = false
                    currentIndex = nextPage.size
                }
            }
        } else {
            Log.e("ViewUserActivity", "User data is null or uid is missing")
        }
    }

    // Detect scroll to bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collect { visibleItems ->
                val lastVisible = visibleItems.lastOrNull()?.index ?: 0
                if (!isLoading && lastVisible >= blueprints.size - 1 && currentIndex < allKeys.size) {
                    isLoading = true
                    loadNextPage(allKeys, currentIndex, pageSize) { nextPage ->
                        blueprints = blueprints + nextPage
                        currentIndex += nextPage.size
                        isLoading = false
                    }
                }
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row (
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val scale = (64f - (scrollBehavior.state.collapsedFraction * 24f)).coerceIn(40f, 64f)
                        if (!data?.profile.isNullOrEmpty()) {
                            AsyncImage(
                                model = data.profile,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(scale.dp)
                                    .clip(CircleShape)
                            )

                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.person_24px),
                                contentDescription = "Account",
                                modifier = Modifier.size(scale.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = data?.username ?: "Unnamed Person",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
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
                        val url = "https://sflightx.com/u/$key"
                        //shareUser(context, url)
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
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            state = listState,
        ) {
            // Profile Section Header
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Followers and Following
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                            // Followers
                            Text(
                                buildAnnotatedString {
                                    append("120K")
                                    withStyle(style = SpanStyle(color = onSurfaceVariant)) {
                                        append(" Followers")
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Following
                            Text(
                                buildAnnotatedString {
                                    append("75")
                                    withStyle(style = SpanStyle(color = onSurfaceVariant)) {
                                        append(" Following")
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Follow Button
                        Row {
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
                        }
                    }
                }
            }

            // Bio Section
            item {
                data?.bio?.let { bio ->
                    Text(
                        text = bio,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Sticky TabRow
            stickyHeader {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            // Content based on selected tab index
            when (selectedTabIndex) {
                0 -> item {
                    UserHomeContent() // Replace with your home content
                }
                1 -> item {
                    UserUploadContent(blueprints) // Replace with your upload content
                }
                2 -> item {
                    // Handle additional content for tab 2
                    Text(text = "Content for the third tab")
                }
            }

            // Loading Indicator
            if (isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator() // Display loading spinner
                    }
                }
            }
        }

    }
}

@Composable
fun UserHomeContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.padding(24.dp)
        ) {
            Text("No uploads available.")
        }
    }
}

@Composable
fun UserUploadContent(blueprints: List<BlueprintData>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp)
    ) {
        blueprints.forEach { blueprint ->
            LoadBlueprintDesign(
                blueprint,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth(),
                showAuthor = false
            )
        }
    }
}

fun fetchUploadKeys(uid: String, onResult: (List<String>) -> Unit) {
    Log.d("ViewUserActivity", "Fetching upload keys for UID: $uid")
    val ref = FirebaseDatabase.getInstance().reference
        .child("userdata").child(uid).child("upload")

    ref.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val keys = snapshot.children.mapNotNull { it.getValue(String::class.java) }
            Log.d("ViewUserActivity", "Fetched keys: $keys")
            onResult(keys)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("ViewUserActivity", "Error fetching keys: ${error.message}")
            onResult(emptyList())
        }
    })
}

fun loadNextPage(keys: List<String>, start: Int, size: Int, onResult: (List<BlueprintData>) -> Unit) {
    Log.d("ViewUserActivity", "Loading next page from index $start with size $size")
    val database = FirebaseDatabase.getInstance().reference
    val subKeys = keys.drop(start).take(size)

    if (subKeys.isEmpty()) {
        Log.d("ViewUserActivity", "No more keys to load.")
        onResult(emptyList())
        return
    }

    val result = mutableListOf<BlueprintData>()
    var loaded = 0

    for (key in subKeys) {
        Log.d("ViewUserActivity", "Fetching blueprint for key: $key")
        val ref = database.child("upload/blueprint").child(key)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(BlueprintData::class.java)?.let {
                    Log.d("ViewUserActivity", "Blueprint loaded: $it")
                    result.add(it)
                }
                loaded++
                if (loaded == subKeys.size) {
                    Log.d("ViewUserActivity", "Finished loading all blueprints.")
                    onResult(result)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ViewUserActivity", "Error loading blueprint for key $key: ${error.message}")
                loaded++
                if (loaded == subKeys.size) {
                    Log.d("ViewUserActivity", "Finished loading blueprints with errors.")
                    onResult(result)
                }
            }
        })
    }
}