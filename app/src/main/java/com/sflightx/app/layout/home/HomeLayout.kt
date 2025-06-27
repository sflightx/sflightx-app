package com.sflightx.app.layout.home

import android.util.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.unit.*
import com.google.accompanist.placeholder.*
import com.google.firebase.database.*
import com.sflightx.app.`class`.*
import com.sflightx.app.layout.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTabContent(snackbarHostState: SnackbarHostState) {
    val scrollState = rememberScrollState()
    Column (
        modifier = Modifier
            .verticalScroll(scrollState)
    ) {
        HomeTabPrimaryContent(snackbarHostState)
        LaunchGridSection(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }

}

@Composable
fun HomeTabPrimaryContent(snackbarHostState: SnackbarHostState) {
    val database = remember { FirebaseDatabase.getInstance() }
    var showAllItems by rememberSaveable { mutableStateOf(false) }
    var blueprintData by remember { mutableStateOf<List<BlueprintData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) } // Track loading state

    LaunchedEffect(Unit) {
        val blueprintRef = database.getReference("upload/blueprint")
        try {
            val snapshot = blueprintRef.get().await()
            val blueprints = snapshot.children.mapNotNull { child ->
                try {
                    child.getValue(BlueprintData::class.java)?.copy(key = child.key ?: "")
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Error: $e")
                    }
                    null
                }
            }
            blueprintData = blueprints.reversed()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                snackbarHostState.showSnackbar("Error: $e")
            }
        } finally {
            isLoading = false // Set loading to false when done
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (isLoading) {
            // Skeleton UI
            SkeletonGrid()
        } else {
            // Actual content
            val displayedItems = if (showAllItems) blueprintData else blueprintData.take(10)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (displayedItems.size / 2 + 1) * 260.dp)
                    .padding(bottom = 8.dp),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(displayedItems.size) { index ->
                        LoadBlueprintDesign(
                            blueprint = displayedItems[index],
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            showAuthor = true
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun LaunchGridSection(
    modifier: Modifier = Modifier,
    title: String = "",
    maxItems: Int = 4,
    snackbarHostState: SnackbarHostState
) {
    val database = remember { FirebaseDatabase.getInstance() }
    var launchData by remember { mutableStateOf<List<LaunchData>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = database.getReference("launch_manifest/JNO/upcoming").get().await()
            Log.d("LaunchData", "Fetched snapshot: ${snapshot.value}")
            val launches = snapshot.children.mapNotNull { child ->
                try {
                    val launch = child.getValue(LaunchData::class.java) // Directly deserialize the data
                    launch?.copy(key = child.key ?: "")
                } catch (e: Exception) {
                    Log.e("LaunchData", "Error processing child: ${e.localizedMessage}")
                    null
                }
            }

            launchData = launches
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("Error loading launches: ${e.localizedMessage}")
        }
    }

    // Check if launch data exists and display it
    if (launchData.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (maxItems / 2 + 1) * 260.dp),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(launchData.take(maxItems)) { launch ->
                        LoadLaunchDesign(
                            launch = launch,
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun SkeletonGrid() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp) // Adjust based on expected content size
            .padding(bottom = 8.dp),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            items(4) { // Show 4 placeholder items (adjust as needed)
                SkeletonItem()
            }
        }
    )
}

@Composable
fun SkeletonItem() {
    Column(
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Placeholder for image or main content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .placeholder(
                    visible = true,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = MaterialTheme.colorScheme.surfaceBright
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Placeholder for title
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .placeholder(
                    visible = true,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = MaterialTheme.colorScheme.surfaceBright
                    )
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Placeholder for author
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .placeholder(
                    visible = true,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = MaterialTheme.colorScheme.surfaceBright
                    )
                )
        )
    }
}