package com.sflightx.app.layout

import android.content.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import androidx.core.net.toUri
import coil.compose.*
import com.google.firebase.database.*
import com.sflightx.app.*
import com.sflightx.app.`class`.*
import com.sflightx.app.task.*
import kotlinx.coroutines.tasks.*

@Composable
fun LoadBlueprintDesign(
    blueprint: BlueprintData,
    modifier: Modifier,
    showAuthor: Boolean
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
    ) {
        Image(
            painter = rememberAsyncImagePainter(blueprint.image_url),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
                .clickable {
                    val intent = Intent(context, ViewPostActivity::class.java)
                    intent.putExtra("key", blueprint.key)
                    intent.putExtra("data", blueprint)
                    context.startActivity(intent)
                },
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = blueprint.name,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )

            if (showAuthor) {
                val result = remember(blueprint.author) {
                    mutableStateOf<Result<Map<String, Any>?>>(Result.success(null))
                }

                LaunchedEffect(blueprint.author) {
                    try {
                        val snapshot = FirebaseDatabase.getInstance()
                            .getReference("userdata")
                            .child(blueprint.author)
                            .get()
                            .await()

                        if (snapshot.exists()) {
                            @Suppress("UNCHECKED_CAST")
                            val data = snapshot.value as? Map<String, Any>
                            result.value = Result.success(data)
                        } else {
                            result.value = Result.success(null) // No data found
                        }
                    } catch (e: Exception) {
                        result.value = Result.failure(e)
                    }
                }

                result.value.onSuccess { data ->
                    val profileImageUrl = data?.get("profile") as? String
                    val authorName = data?.get("username") as? String

                    if (profileImageUrl != null && authorName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // UserData Profile Image in Circle
                            Image(
                                painter = rememberAsyncImagePainter(profileImageUrl),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            // Author Name Text
                            Text(
                                text = authorName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }.onFailure {
                    Text(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = "Error loading author data",
                        color = colorScheme.error
                    )
                }
            }

        }
    }
}

@Composable
fun LoadLaunchDesign(
    launch: LaunchData,
    modifier: Modifier = Modifier,
    showCountdown: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        Image(
            painter = rememberAsyncImagePainter(launch.thumbnail),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
                .clickable {
                    onClick?.invoke() ?: run {
                        val intent = Intent(context, ViewPostActivity::class.java).apply {
                            putExtra("key", launch.key)
                        }
                        context.startActivity(intent)
                    }
                },
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = launch.name,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Text(
                text = launch.description,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                maxLines = 2
            )

            if (showCountdown && launch.net.end > 0) {
                LaunchCountdownChip(endTimeMillis = launch.net.end)
            }
        }
    }
}

@Composable
fun LegalFooter(
    context: Context
) {
    Row (
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = "Terms and Conditions",
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurfaceVariant,
            fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * 0.9f).sp,
            modifier = Modifier
                .padding(end = 8.dp)
                .clickable {
                    val intent =
                        Intent(Intent.ACTION_VIEW, "https://sflightx.com/legal/terms".toUri())
                    context.startActivity(intent)
                },
        )
        Text(
            text = "Privacy Policy",
            style = MaterialTheme.typography.titleSmall,
            color = colorScheme.onSurfaceVariant,
            fontSize = (MaterialTheme.typography.titleSmall.fontSize.value * 0.9f).sp,
            modifier = Modifier.clickable {
                val intent =
                    Intent(Intent.ACTION_VIEW, "https://sflightx.com/legal/privacy".toUri())
                context.startActivity(intent)
            },
        )
    }
}