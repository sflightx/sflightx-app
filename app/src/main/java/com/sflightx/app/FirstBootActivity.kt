package com.sflightx.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.*
import android.os.*
import android.widget.*
import androidx.activity.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.core.app.*
import androidx.core.content.*
import com.sflightx.app.animation.*
import com.sflightx.app.ui.theme.*


@Suppress("DEPRECATION")
class FirstBootActivity : ComponentActivity() {
    private val storagePermissionCode = 101
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                AnimatedGradientBackground (
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        FirstBootAppLayout(requestStoragePermission = ::requestStoragePermission)
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            storagePermissionCode
        )
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, // Fixed to match exact expected type
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed
            } else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
fun FirstBootAppLayout(requestStoragePermission: () -> Unit) {
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    currentProgress = (selectedTab.toFloat() + 1) / 3
    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 800f),
        label = "ProgressAnimation"
    )
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomAppBar(
                actions = {
                    FilledTonalButton(onClick = {
                        if (selectedTab > 0) {
                            selectedTab--
                            currentProgress = (selectedTab.toFloat() + 1) / 3
                            return@FilledTonalButton
                        }
                    },
                        Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back_24px),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                    }

                    LinearWavyProgressIndicator(
                        progress = { animatedProgress }, // Lambda for determinate progress
                        modifier = Modifier.padding(8.dp).weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
                        trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
                        gapSize = WavyProgressIndicatorDefaults.LinearIndicatorTrackGapSize,
                        amplitude = WavyProgressIndicatorDefaults.indicatorAmplitude,
                        wavelength = WavyProgressIndicatorDefaults.LinearDeterminateWavelength,
                        waveSpeed = 40.dp
                    )

                    FilledTonalButton(onClick = {
                        val prefsName = "sflightx.settings"
                        val keyFirstBoot = "first_boot"
                        if (selectedTab < 2) {
                            selectedTab++
                            currentProgress = (selectedTab.toFloat() + 1) / 3
                            return@FilledTonalButton
                        }
                        val intent = Intent(context, MainActivity::class.java)
                        context.startActivity(intent)
                        val sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                        sharedPreferences.edit { putBoolean(keyFirstBoot, false) }
                        (context as? Activity)?.finish()

                    },
                        Modifier.padding(8.dp)) {
                        Icon(
                            painter = painterResource(id = if (selectedTab == 2) R.drawable.check_24px else R.drawable.arrow_forward_24px),
                            contentDescription = if (selectedTab == 2) "Finish Icon" else "Next Icon"
                        )


                    }
                }

            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            when (selectedTab) {
                0 -> TabContent1()
                1 -> TabContent2(requestStoragePermission = requestStoragePermission)
                2 -> TabContent3()
            }
        }
    }
}

@Composable
fun TabContent1() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(8.dp) // Adds padding to the entire column
        ) {
            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (MaterialTheme.typography.titleLarge.fontSize.value * 3f).sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "The new SFlightX App is here, with new modern features.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TabContent2(requestStoragePermission: () -> Unit) {
    val context = LocalContext.current
    var stateGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    if (stateGranted) {
        requestStoragePermission()
    }

    Box (
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column {
            Text(
                "Required Permissions",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "The app uses the following permissions to function properly.",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,

            )
            Surface (
                modifier = Modifier
                    .fillMaxWidth().padding(top = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = Color.Transparent,
                shape = MaterialTheme.shapes.large
            ) {
                var checked by remember { mutableStateOf(true) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        "READ_EXTERNAL_STORAGE permission",
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            requestStoragePermission()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabContent3() {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        item {
            Text(
                "New Features",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Material Theming",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "The app follows Google's Material You Theme, where you can also customize according to the system color scheme.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Content Creation Integration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Seamlessly post updates to your followers, with your virtual space agency.",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        "Supported Games",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AssistChip(onClick = {}, label = { Text("Juno: New Origins") })
                        AssistChip(onClick = {}, label = { Text("Spaceflight Simulator") })
                        AssistChip(onClick = {}, label = { Text("Kerbal Space Program") })
                    }
                }
            }
        }
    }
}