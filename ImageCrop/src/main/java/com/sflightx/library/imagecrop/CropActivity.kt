package com.sflightx.library.imagecrop

import android.net.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import com.sflightx.library.imagecrop.ui.theme.*

@Suppress("DEPRECATION")
class CropActivity : ComponentActivity() {
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        imageUri = intent.getParcelableExtra("imageUri")

        setContent {
            SFlightXTheme {
                CropIntent.SetIntent(this@CropActivity, imageUri)
            }
        }
    }
}
