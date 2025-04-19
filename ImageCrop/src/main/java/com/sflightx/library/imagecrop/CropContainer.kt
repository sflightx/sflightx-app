package com.sflightx.library.imagecrop

import android.graphics.*
import android.net.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*

class CropContainer {
    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun AdvancedCropScreen(
            imageUri: Uri,
            onCrop: (Bitmap) -> Unit,
            onCancel: () -> Unit
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var rotation by remember { mutableFloatStateOf(0f) }
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }

            var cropBoxSize by remember { mutableStateOf(Size(500f, 500f)) }
            var imageOffset by remember { mutableStateOf(Offset(0f, 0f)) } // To track image offset (position)

            val context = LocalContext.current
            LaunchedEffect(imageUri) {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                bitmap = BitmapFactory.decodeStream(inputStream)
            }

            var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(bitmap, scale, rotation, imageOffset, cropBoxSize) {
                bitmap?.let {
                    previewBitmap = CropImage.advancedAccurateCropBitmap(
                        it,
                        scale,
                        rotation,
                        cropBoxSize,
                        imageOffset // Add this param in your crop function
                    )
                }
            }



            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Crop Image") },
                        navigationIcon = {
                            IconButton(onClick = { onCancel() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                },
                bottomBar = {
                    BottomAppBar {
                        OutlinedButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = { onCancel() }
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = {
                                bitmap?.let {
                                    val croppedBitmap = CropImage.advancedAccurateCropBitmap(it, scale, rotation, cropBoxSize, imageOffset)
                                    onCrop(croppedBitmap)
                                }
                            }
                        ) {
                            Text("Crop")
                        }
                        previewBitmap?.let {
                            Spacer(modifier = Modifier.weight(1f))

                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Cropped Preview",
                                modifier = Modifier
                                    .size(64.dp)
                                    .aspectRatio(1f) // Force square
                                    .padding(end = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    bitmap?.let { originalBitmap ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            // Draw the image inside the movable box, which is constrained to the crop box size
                            Image(
                                bitmap = originalBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        rotationZ = rotation
                                        translationX = imageOffset.x
                                        translationY = imageOffset.y
                                    }
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(0.75f, 3f)

                                            // Normalize the pan delta based on scale
                                            imageOffset = Offset(
                                                imageOffset.x + pan.x / scale,
                                                imageOffset.y + pan.y / scale
                                            )
                                        }
                                    }
                            )

                            // Draw the fixed red crop box
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(cropBoxSize.width.dp, cropBoxSize.height.dp)
                            )
                        }

                        // Controls for scale and rotation
                        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Refer to the image below as your guide.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                // Scale Slider (No Steps)
                                Slider(
                                    value = scale,
                                    onValueChange = { scale = it },
                                    valueRange = 0.75f..3f,
                                    modifier = Modifier.fillMaxWidth()
                                    // No steps, continuous slider
                                )
                                Text(text = "Scale: ${(scale * 100).toInt()}%")

                                Spacer(modifier = Modifier.height(16.dp))

                                // Rotation Slider (No Steps)
                                Slider(
                                    value = rotation,
                                    onValueChange = { rotation = it },
                                    valueRange = -180f..180f,
                                    modifier = Modifier.fillMaxWidth()
                                    // No steps, continuous slider
                                )
                                Text(text = "Rotation: ${rotation.toInt()}°")
                            }
                        }
                    } ?: run {
                        Text("Loading image...", modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }
        }
    }
}
