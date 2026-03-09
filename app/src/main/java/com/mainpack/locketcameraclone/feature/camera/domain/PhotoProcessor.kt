package com.mainpack.locketcameraclone.feature.camera.domain

import android.graphics.Bitmap

interface PhotoProcessor {
    fun process(
        bitmap: Bitmap,
        rotationDegrees: Int,
        isFrontCamera: Boolean,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap
}

