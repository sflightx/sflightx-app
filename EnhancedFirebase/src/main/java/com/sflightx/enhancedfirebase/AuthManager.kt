package com.sflightx.enhancedfirebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import androidx.core.net.toUri

class AuthManager {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    // Update Firebase Auth Profile
    fun updateAuthProfile(
        displayName: String?,
        photoUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .apply {
                    displayName?.let { setDisplayName(it) }
                    photoUrl?.let { photoUri = it.toUri() }
                }
                .build()

            currentUser.updateProfile(profileUpdates)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { onFailure(it) }
        } else {
            onFailure(Exception("Could not set profile info. No authenticated user found."))
        }
    }

    // Get Firebase Auth Profile
    fun getAuthProfile(
        onSuccess: (AuthProfileData?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val profileData = AuthProfileData(
                uid = currentUser.uid,
                displayName = currentUser.displayName,
                photoURL = currentUser.photoUrl?.toString(),
                email = currentUser.email
            )
            onSuccess(profileData)
        } else {
            onFailure(Exception("Could not get profile info. No authenticated user found."))
        }
    }
}

// Data class for Auth Profile
data class AuthProfileData(
    val uid: String?,
    val displayName: String?,
    val photoURL: String?,
    val email: String?
)