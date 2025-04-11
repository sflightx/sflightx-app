package com.sflightx.library.imagecrop

import android.app.*
import android.app.Activity.RESULT_OK
import android.content.*
import android.graphics.Bitmap
import android.net.*
import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

class CropIntent {
    companion object {
        private var croppedImage: Bitmap? = null
        @Composable
        fun SetIntent(context: Context, imageUri: Uri?) {
            if (context is Activity) {
                if (imageUri != null) {
                    Log.d("ImageReceiver", "Image received.")
                    CropContainer.AdvancedCropScreen(
                        imageUri = imageUri,
                        onCrop = { bitmap ->
                            croppedImage = bitmap
                            postIntent(context, bitmap)
                        },
                        onCancel = { context.finish() }
                    )
                } else {
                    Text("No image provided.")
                }
            }
        }
        fun postIntent(context: Context, bitmap: Bitmap) {
            val uri = CropUtils.saveBitmapToFile(context, bitmap, "image")
            Log.d("ImageReceiver", "Image cropped at Uri $uri.")
            val resultIntent = Intent().apply {
                putExtra("croppedImageUri", uri)
            }
            (context as? Activity)?.setResult(RESULT_OK, resultIntent)
            (context as? Activity)?.finish()
        }
    }
}
