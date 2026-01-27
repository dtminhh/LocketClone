package com.mainpack.locketcameraclone.ui.extension

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector

object BitmapExtension {
    private const val CROP_DEFAULT_VALUE = 0
    private const val CROP_DIVIDE_VALUE = 2
    private const val ROTATE_VALUE = 0
    private const val SCALE_X_VALUE = -1f
    private const val SCALE_Y_VALUE = 1f

    fun centerCrop(bitmap: Bitmap, viewWidth: Int, viewHeight: Int): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val bitmapRatio = bitmapWidth.toFloat() / bitmapHeight.toFloat()
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        val cropX: Int
        val cropY: Int

        if (bitmapRatio > viewRatio) {
            cropWidth = bitmapHeight
            cropHeight = (bitmapHeight * viewRatio).toInt()
            cropX = (bitmapWidth - cropWidth) / CROP_DIVIDE_VALUE
            cropY = CROP_DEFAULT_VALUE
        } else {
            cropWidth = bitmapWidth
            cropHeight = (bitmapWidth * viewRatio).toInt()
            cropX = CROP_DEFAULT_VALUE
            cropY = (bitmapHeight - cropHeight) / CROP_DIVIDE_VALUE
        }
        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int, lensFacing: Int): Bitmap {
        if (rotationDegrees == ROTATE_VALUE) return bitmap

        val matrix = android.graphics.Matrix()

        matrix.postRotate(rotationDegrees.toFloat())

        if (lensFacing == CameraSelector.LENS_FACING_FRONT) matrix.postScale(
            SCALE_X_VALUE,
            SCALE_Y_VALUE
        )

        return Bitmap.createBitmap(
            bitmap,
            CROP_DEFAULT_VALUE,
            CROP_DEFAULT_VALUE,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}