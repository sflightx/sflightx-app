package com.sflightx.app.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.sflightx.app.R
import com.sflightx.app.layout.components.M3Button
import com.sflightx.app.layout.components.M3ButtonType

@Composable
fun UpdateDisplayNameDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Display Name") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it,
                        color = colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isLoading = true
                    updateFirebaseDisplayName(
                        newName = newName.trim(),
                        onSuccess = {
                            isLoading = false
                            onSuccess()
                            onDismiss()
                        },
                        onFailure = {
                            isLoading = false
                            errorMessage = it.message ?: "Update failed"
                        }
                    )
                },
                enabled = newName.isNotBlank() && !isLoading
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun updateFirebaseDisplayName(
    newName: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user != null) {
        val uid = user.uid
        val profileUpdates = userProfileChangeRequest {
            displayName = newName
        }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val databaseRef =
                        FirebaseDatabase.getInstance().getReference("userdata").child(uid)
                            .child("username")

                    databaseRef.setValue(newName)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { dbError -> onFailure(dbError) }

                } else {
                    onFailure(task.exception ?: Exception("Unknown error"))
                }
            }
    } else {
        onFailure(Exception("No authenticated user"))
    }
}

@Composable
fun M3ConfirmationDialog(
    title: String,
    message: String,
    confirmButtonText: String = "Yes",
    dismissButtonText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            M3Button(
                contentDescription = "Confirm",
                onClick = onConfirm,
                text = confirmButtonText,
                modifier = Modifier.height(64.dp),
                buttonType = M3ButtonType.Filled
            )
        },
        dismissButton = {
            M3Button(
                contentDescription = "Decline",
                onClick = onDismiss,
                text = dismissButtonText,
                modifier = Modifier.height(64.dp),
                buttonType = M3ButtonType.Outlined
            )
        }
    )
}