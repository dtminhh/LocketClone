package com.mainpack.locketcameraclone.feature.camera.data

import android.graphics.Bitmap
import android.graphics.Matrix
import com.mainpack.locketcameraclone.feature.camera.domain.PhotoProcessor
import javax.inject.Inject

class CameraPhotoProcessor @Inject constructor() : PhotoProcessor {

    override fun process(
        bitmap: Bitmap,
        rotationDegrees: Int,
        isFrontCamera: Boolean,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap {
        val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees, isFrontCamera)
        return centerCrop(rotatedBitmap, targetWidth, targetHeight)
    }

    private fun centerCrop(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return bitmap
        }

        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetRatio = targetWidth.toFloat() / targetHeight.toFloat()

        val desiredWidth: Int
        val desiredHeight: Int

        if (bitmapRatio > targetRatio) {
            desiredWidth = (bitmap.height * targetRatio).toInt()
            desiredHeight = bitmap.height
        } else {
            desiredWidth = bitmap.width
            desiredHeight = (bitmap.width / targetRatio).toInt()
        }

        val cropWidth = desiredWidth.coerceIn(1, bitmap.width)
        val cropHeight = desiredHeight.coerceIn(1, bitmap.height)
        val cropX = ((bitmap.width - cropWidth) / CENTER_DIVIDER).coerceAtLeast(DEFAULT_OFFSET)
        val cropY = ((bitmap.height - cropHeight) / CENTER_DIVIDER).coerceAtLeast(DEFAULT_OFFSET)

        return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int,
        isFrontCamera: Boolean
    ): Bitmap {
        if (rotationDegrees == DEFAULT_ROTATION && !isFrontCamera) {
            return bitmap
        }

        val matrix = Matrix().apply {
            if (rotationDegrees != DEFAULT_ROTATION) {
                postRotate(rotationDegrees.toFloat())
            }
            if (isFrontCamera) {
                postScale(FRONT_CAMERA_SCALE_X, FRONT_CAMERA_SCALE_Y)
            }
        }

        return Bitmap.createBitmap(
            bitmap,
            DEFAULT_OFFSET,
            DEFAULT_OFFSET,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private companion object {
        const val DEFAULT_OFFSET = 0
        const val DEFAULT_ROTATION = 0
        const val CENTER_DIVIDER = 2
        const val FRONT_CAMERA_SCALE_X = -1f
        const val FRONT_CAMERA_SCALE_Y = 1f
    }
}
