package com.sflightx.app

import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.lifecycle.*
import com.sflightx.app.ui.theme.*
import kotlinx.coroutines.*

class PreloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SFlightXTheme {
                PreloadLayout()
                preloadChecks()
            }
        }
    }
    private fun preloadChecks() {
        val preloadDuration = 1000L
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                checkUserLoggedIn()
                // You can add other checks like fetching configuration data
            }
            delay(preloadDuration)
            navigateToNextActivity()
        }
    }

    private fun checkUserLoggedIn(): Boolean {
        val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        return currentUser != null
    }

    private fun navigateToNextActivity() {
        val intent = if (checkUserLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

}

@Composable
fun PreloadLayout() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.sflightx_logo),
            contentDescription = "App Logo"
        )
    }
}