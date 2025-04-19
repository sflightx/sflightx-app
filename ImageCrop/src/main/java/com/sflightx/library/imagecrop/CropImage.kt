package com.sflightx.library.imagecrop

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.core.graphics.createBitmap

class CropImage {
    companion object {
        fun advancedAccurateCropBitmap(
            bitmap: Bitmap,
            scale: Float,
            rotation: Float,
            cropBoxSize: Size,
            imageOffset: Offset
        ): Bitmap {
            val cropSize = cropBoxSize.width.toInt()

            //val outputBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
            val outputBitmap = createBitmap(cropSize, cropSize)
            val canvas = Canvas(outputBitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            val matrix = Matrix()

            // Move origin to center of the image
            matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)

            // Apply scale and rotation
            matrix.postScale(scale, scale)
            matrix.postRotate(rotation)

            // Apply imageOffset (user pan), scaled to match transformed space
            matrix.postTranslate(imageOffset.x * scale, imageOffset.y * scale)

            // Center the transformed image in the output canvas
            matrix.postTranslate(cropSize / 2f, cropSize / 2f)

            canvas.drawBitmap(bitmap, matrix, paint)

            return outputBitmap
        }
    }
}
