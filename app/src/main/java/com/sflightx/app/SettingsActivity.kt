package com.sflightx.app

import android.content.pm.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.core.view.*
import com.google.accompanist.systemuicontroller.*
import com.sflightx.app.ui.theme.*


@Suppress("DEPRECATION")
class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = false
        setContent {
            SFlightXTheme {
                val currentCategory = remember { mutableStateOf<Settings?>(null) }
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                val scrollState = rememberScrollState()
                val collapsedFraction = scrollBehavior.state.collapsedFraction
                val systemUiController = rememberSystemUiController()
                val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5
                systemUiController.setStatusBarColor(
                    color = MaterialTheme.colorScheme.background,
                    darkIcons = isLightTheme
                )
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        Column (
                            modifier = Modifier.verticalScroll(scrollState)
                        ) {
                            LargeTopAppBar(
                                title = {
                                    Text(
                                        text = currentCategory.value?.title ?: "Settings",
                                        fontSize = (30 - 5 * collapsedFraction).sp,
                                        style = MaterialTheme.typography.headlineLarge
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        if (currentCategory.value != null) {
                                            currentCategory.value = null // Back to parent
                                        } else {
                                            finish() // Fully exit
                                        }
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
                        }
                    },
                    contentWindowInsets = WindowInsets.systemBars,
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (currentCategory.value == null) {
                            MainSettingsScreen { selected -> currentCategory.value = selected }
                        } else {
                            SubcategoryScreen(
                                category = currentCategory.value!!,
                                onItemClicked = { it.action?.invoke() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettingsScreen(onCategorySelected: (Settings) -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
        }
    }
    val versionCode = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
        }
    }
    val categories = listOf(
        Settings(
            title = "Files",
            description = "Manage downloads and paths",
            icon = Icons.Default.MoreVert,
            items = listOf(
                SettingItem("Download Path", "Choose where to save files", Icons.Default.MoreVert)
            )
        ),
        Settings(
            title = "Permissions",
            description = "Manage app access and privileges",
            icon = Icons.Default.MoreVert,
            items = listOf(
                SettingItem("Storage Permission", "Access your storage", Icons.Default.MoreVert),
                SettingItem("Enable Pro Mode", "Unlock advanced tools", Icons.Default.MoreVert)
            )
        ),
        Settings(
            title = "Connections",
            description = "Network settings for downloads",
            icon = Icons.Default.MoreVert,
            items = listOf(
                SettingItem("Download Preferences", "Set network usage", Icons.Default.MoreVert),
                SettingItem("Download Limit", "Control bandwidth", Icons.Default.MoreVert)
            )
        ),
        Settings(
            title = "About",
            description = "Developer info and links",
            icon = Icons.Default.MoreVert,
            items = listOf(
                SettingItem("YouTube", "Visit our channel", Icons.Default.PlayArrow),
                SettingItem("Discord", "Join our community", Icons.Default.MoreVert),
                SettingItem("X", "Follow us", Icons.Default.MoreVert)
            )
        ),
        Settings(
            title = "Updates",
            description = "Check for updates and configure",
            icon = Icons.Default.MoreVert,
            items = listOf(
                SettingItem("Check for Updates", "Get the latest version", Icons.Default.MoreVert),
                SettingItem("Configure Limits", "Adjust update preferences", Icons.Default.MoreVert)
            )
        )
    )
    Column {
        LazyColumn {
            items(categories) { category ->
                CategoryListItem(category = category) {
                    onCategorySelected(category)
                }
            }
        }
        Text(
            text = "App Version: $versionName (Code: $versionCode)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Composable
fun SubcategoryScreen(
    category: Settings,
    onItemClicked: (SettingItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(category.items) { item ->
            SettingListItem(item = item) {
                onItemClicked(item)
            }
        }
    }
}


@Composable
fun SettingListItem(item: SettingItem, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.name,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            item.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryListItem(category: Settings, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = category.title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

