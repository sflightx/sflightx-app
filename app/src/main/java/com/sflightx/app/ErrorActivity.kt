package com.sflightx.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sflightx.app.ui.theme.SFlightXTheme

class ErrorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val error = intent.getStringExtra("error") ?: "Unknown Error"

        setContent {
            SFlightXTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ErrorScreen(error)
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(error: String) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Oops! The app crashed.",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = error,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .verticalScroll(rememberScrollState()),
            readOnly = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

    }
}
