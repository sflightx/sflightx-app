package com.sflightx.app.layout

import android.content.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.*
import coil.compose.*
import com.google.firebase.database.*
import com.sflightx.app.*
import kotlinx.coroutines.tasks.*

class Design {
}

@Composable
fun LoadBlueprintDesign(blueprint: BlueprintData, modifier: Modifier, showAuthor: Boolean) {
    val context = LocalContext.current
    Box(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .background(colorScheme.surfaceContainerLow)
                .clickable {
                    val intent = Intent(context, ViewPostActivity::class.java)
                    intent.putExtra("key", blueprint.key)
                    intent.putExtra("data", blueprint)
                    context.startActivity(intent)
                }
        ) {
            // Image on top
            Image(
                painter = rememberAsyncImagePainter(blueprint.image_url),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .height(120.dp)
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp).padding(start = 8.dp),
                text = blueprint.name,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.onSurface,
            )

            if (showAuthor) {
                Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.padding(8.dp)
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
    Spacer(modifier = Modifier.height(8.dp))
}