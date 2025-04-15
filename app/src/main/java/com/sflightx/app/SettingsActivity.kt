package com.sflightx.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.*
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.core.view.*
import com.google.accompanist.systemuicontroller.*
import com.sflightx.app.ui.theme.*
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.google.firebase.database.FirebaseDatabase
import com.sflightx.app.bottomsheet.GlobalBottomSheetHost
import com.sflightx.app.bottomsheet.LocalBottomSheetController
import java.nio.file.WatchEvent


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
                GlobalBottomSheetHost {
                    val currentCategory = remember { mutableStateOf<AppSettings?>(null) }
                    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    val scrollState = rememberScrollState()
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val context = LocalContext.current

                    val systemUiController = rememberSystemUiController()
                    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5
                    systemUiController.setStatusBarColor(
                        color = MaterialTheme.colorScheme.background,
                        darkIcons = isLightTheme
                    )

                    val dialogState = rememberDialogState()

                    if (dialogState.show.value) {
                        Log.d("DialogDebug", "Showing dialog now")
                        when (dialogState.type.value) {
                            DialogType.SWITCH -> {
                                RefinedSwitchDialog(
                                    title = dialogState.title.value,
                                    text = dialogState.message.value,
                                    checked = dialogState.switchState.value,
                                    onCheckedChange = { dialogState.switchState.value = it },
                                    actionText = dialogState.actionText.value,
                                    onDismiss = { dialogState.show.value = false },
                                    onConfirm = {
                                        dialogState.onConfirm.value.invoke()
                                        dialogState.show.value = false
                                    }
                                )
                            }
                            DialogType.TEXT_INPUT -> {
                                RefinedTextInputDialog(
                                    title = dialogState.title.value,
                                    text = dialogState.message.value,
                                    input = dialogState.inputText.value,
                                    onInputChange = { dialogState.inputText.value = it },
                                    actionText = dialogState.actionText.value,
                                    onDismiss = { dialogState.show.value = false },
                                    onConfirm = {
                                        dialogState.onConfirm.value.invoke()
                                        dialogState.show.value = false
                                    }
                                )
                            }
                            else -> {
                                RefinedShowDialog(
                                    onDismiss = { dialogState.show.value = false },
                                    dialogState = dialogState,
                                )
                            }
                        }
                    }


                    Scaffold(
                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            Column(
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
                                                currentCategory.value = null
                                            } else {
                                                finish()
                                            }
                                        }) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.arrow_back_24px),
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
                                MainSettingsScreen(
                                    onCategorySelected = { selected ->
                                        currentCategory.value = selected
                                    },
                                    dialogState = dialogState
                                )
                            } else {
                                SubcategoryScreen(
                                    category = currentCategory.value!!,
                                    onItemValueChanged = { item, isChecked ->
                                        if (item.type == SettingType.SWITCH) {
                                            item.onValueChanged?.invoke(isChecked)
                                        }
                                    },
                                    dialogState = dialogState
                                )
                            }

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettingsScreen(
    onCategorySelected: (AppSettings) -> Unit,
    dialogState: DialogState
) {
    val context = LocalContext.current

    val themePreferences = remember { ThemePreferences(context) }
    val materialYouState = themePreferences.isMaterialYouEnabled()
    val bottomSheetController = LocalBottomSheetController.current
    val categories = listOf(
        AppSettings(
            title = "Files",
            description = "Manage downloads and paths",
            icon = painterResource(id = R.drawable.folder_24px),
            items = listOf(
                SettingItem(
                    name = "Download Path",
                    description = "Choose where to save files",
                    iconResId = R.drawable.download_24px,
                    type = SettingType.ACTION,
                    onClick = {
                        dialogState.showInputDialog(
                            title = "Download Path",
                            message = "Currently, changing downlaod paths is not supported.",
                            initialValue = "Path"
                        ) { newName ->
                            //viewModel.updateNickname(newName)
                        }

                    }
                )
            )
        ),
        AppSettings(
            title = "Appearance",
            description = "Customize app display or behavior",
            icon = painterResource(id = R.drawable.edit_24px),
            items = listOf(
                SettingItem(
                    name = "Material You",
                    description = "Enable Material You color scheme on Android 12+.",
                    iconResId = R.drawable.edit_24px,
                    type = SettingType.SWITCH,
                    defaultValue = materialYouState,
                    onValueChanged = { isEnabled ->
                        themePreferences.saveMaterialYouEnabled(isEnabled)
                        dialogState.showDialog(
                            title = "Restart Required",
                            message = "Material You is now ${if (isEnabled) "enabled" else "disabled"}.\nPlease restart the app.",
                            actionText = "OK",
                            onConfirm = { restartApp(context) }
                        )
                    },
                    dialogState = dialogState
                )
            )
        ),
        AppSettings(
            title = "Permissions",
            description = "Manage app access and privileges",
            icon = painterResource(id = R.drawable.admin_panel_settings_24px),
            items = listOf(
                SettingItem(
                    name = "Open Permission",
                    description = "Access your permissions in Settings",
                    iconResId = R.drawable.admin_panel_settings_24px,
                    type = SettingType.ACTION,
                    onClick = {
                        openSettings(context)
                    }
                ),
                SettingItem(
                    name = "Enable Pro Mode",
                    description = "Unlock advanced tools",
                    iconResId = R.drawable.verified_user_24px,
                    type = SettingType.SWITCH,
                    defaultValue = true,
                    onValueChanged = { isEnabled ->

                    }
                )
            )
        ),
        AppSettings(
            title = "Connections",
            description = "Network settings for downloads",
            icon = painterResource(id = R.drawable.wifi_24px),
            items = listOf(
                SettingItem(
                    name = "Download Preferences",
                    description = "Set network usage",
                    iconResId = R.drawable.download_24px,
                    type = SettingType.ACTION,
                    onClick = {
                        Toast.makeText(context, "Soon!", Toast.LENGTH_SHORT).show()
                    }
                ),
                SettingItem(
                    name = "Download Limit",
                    description = "Control bandwidth",
                    iconResId = R.drawable.signal_cellular_alt_24px,
                    type = SettingType.ACTION,
                    onClick = {
                        Toast.makeText(context, "Soon!", Toast.LENGTH_SHORT).show()
                    },
                ),
            )
        ),
        AppSettings(
            title = "About",
            description = "Developer info and links",
            icon = painterResource(id = R.drawable.info_24px),
            items = listOf(
                SettingItem(
                    name = "YouTube",
                    description = "Visit our channel",
                    iconResId = R.drawable.youtube,
                    type = SettingType.ACTION,
                    onClick = {
                        openYouTubeChannel(context, "@sflightx")
                    }
                ),
                SettingItem(
                    name = "Discord",
                    description = "Join our community",
                    iconResId = R.drawable.discord,
                    type = SettingType.ACTION,
                    onClick = {
                        openDiscordServer(context, "ahrryjrBdn")
                    }
                ),
                SettingItem(
                    name = "X",
                    description = "Follow us",
                    iconResId = R.drawable.twitter,
                    type = SettingType.ACTION,
                    onClick = {
                        openXProfile(context, "sflightxjno")
                    },
                ),
                SettingItem(
                    content = {
                        DeveloperContent()
                    },
                ),
            )
        ),
        AppSettings(
            title = "Updates",
            description = "Check for updates and configure",
            icon = painterResource(id = R.drawable.update_24px),
            items = listOf(
                SettingItem(
                    name = "Check for Updates",
                    description = "Get the latest version",
                    iconResId = R.drawable.update_24px,
                    type = SettingType.ACTION,
                    onClick = {
                        bottomSheetController.show {
                            CheckUpdates()
                        }
                    }
                ),
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
    }
}

@Composable
fun SubcategoryScreen(
    category: AppSettings,
    onItemValueChanged: (SettingItem, Boolean) -> Unit,
    dialogState: DialogState
) {
    val switchStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            category.items.forEach { item ->
                put(item.name.toString(), item.defaultValue)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(category.items) { item ->
            val switchState = switchStates[item.name] ?: item.defaultValue
            SettingListItem(
                item = item,
                isChecked = switchState,
                onSwitchChanged = { isChecked ->
                    switchStates[item.name.toString()] = isChecked
                    onItemValueChanged(item, isChecked)
                    item.onValueChanged?.invoke(isChecked)
                },
                dialogState = dialogState
            )
        }
    }
}


@Composable
fun CheckUpdates() {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

    val packageName = context.packageName
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    val versionName = packageInfo.versionName

    Column (
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("SFlightX App", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row (
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Version: $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("($versionCode)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(48.dp))
        VersionCheckIndicator { latestVersion, updateUrl ->
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        )
    }
}

@Composable
fun SettingListItem(
    item: SettingItem,
    isChecked: Boolean = false,
    input: String? = null,
    onSwitchChanged: (Boolean) -> Unit,
    onTextChanged: ((String) -> Unit)? = null,
    dialogState: DialogState? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.type == SettingType.ACTION,
                onClick = {
                    item.onClick?.invoke()
                }
            )
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        item.iconResId?.let {
            Icon(
                painter = painterResource(id = it),
                contentDescription = item.name,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name.toString(),
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

        if (item.type == SettingType.SWITCH) {
            Switch(
                checked = isChecked,
                onCheckedChange = onSwitchChanged
            )
        }
        if (item.type == SettingType.TEXT_INPUT) {
            OutlinedTextField(
                value = input ?: "",  // Provide default empty string if input is null
                onValueChange = { newText ->
                    onTextChanged?.invoke(newText) // Only invoke if onTextChanged is not null
                },
                label = { Text(item.name.toString()) }
            )
        }
        item.content?.invoke()
    }
}


@Composable
fun CategoryListItem(category: AppSettings, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Icon(
            painter = category.icon,
            contentDescription = category.title,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(
                text = category.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RefinedShowDialog(
    dialogState: DialogState,
    onDismiss: () -> Unit
) {
    val type = dialogState.type.value
    val title = dialogState.title.value
    val message = dialogState.message.value
    val actionText = dialogState.actionText.value

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(Modifier.height(8.dp))
                when (type) {
                    DialogType.SWITCH -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = dialogState.switchState.value,
                                onCheckedChange = { dialogState.switchState.value = it }
                            )
                        }
                    }
                    DialogType.TEXT_INPUT -> {
                        OutlinedTextField(
                            value = dialogState.inputText.value,
                            onValueChange = { dialogState.inputText.value = it },
                            placeholder = { Text("Enter value...") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dialogState.onConfirm.value()
                    onDismiss()
                }
            ) {
                Text(actionText)
            }
        }
    )
}

@Composable
fun RefinedSwitchDialog(
    title: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    actionText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = text)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Option")
                    Switch(checked = checked, onCheckedChange = onCheckedChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text(actionText)
            }
        }
    )
}

@Composable
fun RefinedTextInputDialog(
    title: String,
    text: String,
    input: String,
    onInputChange: (String) -> Unit,
    actionText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = text)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Enter text...") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
            }) {
                Text(actionText)
            }
        }
    )
}


fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        val componentName = intent.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0) // Optional but ensures the app process is restarted
    }
}

@Composable
fun rememberDialogState(): DialogState {
    return DialogState(
        show = remember { mutableStateOf(false) },
        title = remember { mutableStateOf("") },
        message = remember { mutableStateOf("") },
        actionText = remember { mutableStateOf("OK") },
        type = remember { mutableStateOf(DialogType.ALERT) },
        inputText = remember { mutableStateOf("") },
        switchState = remember { mutableStateOf(false) },
        onConfirm = remember { mutableStateOf({}) }
    )
}


fun DialogState.showDialog(
    title: String,
    message: String,
    actionText: String = "OK",
    onConfirm: () -> Unit = {}
) {
    this.title.value = title
    this.message.value = message
    this.actionText.value = actionText
    this.onConfirm.value = onConfirm
    this.show.value = true
}

fun DialogState.showSwitchDialog(
    title: String,
    message: String,
    initialChecked: Boolean = false,
    actionText: String = "Apply",
    onConfirm: (Boolean) -> Unit
) {
    this.title.value = title
    this.message.value = message
    this.actionText.value = actionText
    this.switchState.value = initialChecked
    this.type.value = DialogType.SWITCH
    this.onConfirm.value = {
        onConfirm(this.switchState.value)
    }
    this.show.value = true
}

fun DialogState.showInputDialog(
    title: String,
    message: String,
    initialValue: String = "",
    actionText: String = "Save",
    onConfirm: (String) -> Unit
) {
    this.title.value = title
    this.message.value = message
    this.actionText.value = actionText
    this.inputText.value = initialValue
    this.type.value = DialogType.TEXT_INPUT
    this.onConfirm.value = {
        onConfirm(this.inputText.value)
    }
    this.show.value = true
}

fun openSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", context.packageName, null)
    intent.data = uri
    context.startActivity(intent)
}

@SuppressLint("UseKtx")
fun openYouTubeChannel(context: Context, channelUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "vnd.youtube://$channelUrl".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // If YouTube app is not installed, open in browser
        val intent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/$channelUrl".toUri())
        context.startActivity(intent)
    }
}

@SuppressLint("UseKtx")
fun openDiscordServer(context: Context, serverUrl: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, "discord://$serverUrl".toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // If Discord app is not installed, open in browser
        val intent = Intent(Intent.ACTION_VIEW, "https://discord.gg/$serverUrl".toUri())
        context.startActivity(intent)
    }
}

@SuppressLint("UseKtx")
fun openXProfile(context: Context, profileUrl: String) {
    val intent = Intent(Intent.ACTION_VIEW, "https://x.com/$profileUrl".toUri())
    context.startActivity(intent)
}

@Composable
fun VersionCheckIndicator(
    context: Context = LocalContext.current,
    onUpdateNeeded: (latestVersionName: String, updateUrl: String?) -> Unit
) {
    var isChecking by remember { mutableStateOf(true) }
    var isUpToDate by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        checkForAppUpdate(
            context = context,
            onUpdateNeeded = { latest, url ->
                isUpToDate = false
                isChecking = false
                latestVersion = latest
                onUpdateNeeded(latest, url)
            },
            onUpToDate = {
                isUpToDate = true
                isChecking = false
            }
        )
    }

    Column {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isChecking -> CircularProgressIndicator(
                    modifier = Modifier.size(96.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                )
                isUpToDate -> Icon(
                    modifier = Modifier.size(96.dp),
                    painter = painterResource(id = R.drawable.check_24px),
                    contentDescription = "Up to date",
                    tint = MaterialTheme.colorScheme.primary
                )
                else -> Icon(
                    modifier = Modifier.size(96.dp),
                    painter = painterResource(id = R.drawable.info_24px),
                    contentDescription = "Update needed",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isUpToDate) "Check for Updates" else "Update Now (v$latestVersion)", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}


fun checkForAppUpdate(
    context: Context,
    onUpdateNeeded: (latestVersionName: String, updateUrl: String?) -> Unit,
    onUpToDate: () -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val currentVersionName = context.packageManager
        .getPackageInfo(context.packageName, 0).versionName

    database.child("app/version").get().addOnSuccessListener { snapshot ->
        val latestVersionName = snapshot.child("name").getValue(Double::class.java)
        val updateUrl = snapshot.child("url").getValue(String::class.java)

        if (latestVersionName.toString() != currentVersionName) {
            onUpdateNeeded(latestVersionName.toString(), updateUrl)
        } else {
            onUpToDate()
        }
    }.addOnFailureListener {
        onUpToDate() // fallback: assume up-to-date if check fails
        Log.e("AppUpdateCheck", "Failed to check version", it)
    }
}

@Composable
fun DeveloperContent() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            Text(
                "Developer",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.starglenn),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .animateContentSize(),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(24.dp))
            Text(
                "STARGLENN",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "x.com/starglenn_",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}