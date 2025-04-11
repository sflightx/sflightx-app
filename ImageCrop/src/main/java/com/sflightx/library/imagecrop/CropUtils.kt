package com.sflightx.library.imagecrop

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class CropUtils {
    companion object {
        fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): Uri? {
            val uniqueFileName = "${fileName}_${UUID.randomUUID()}.jpg"
            val file = File(context.externalCacheDir, uniqueFileName) // Using externalCacheDir for accessibility
            try {
                FileOutputStream(file).use { outputStream ->
                    bitmap.compress(format, 100, outputStream)
                }
                return Uri.fromFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
    }
}
