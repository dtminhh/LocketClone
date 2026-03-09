package com.mainpack.locketcameraclone.feature.camera.domain.usecase

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.mainpack.locketcameraclone.core.image.toBitmapCompat
import com.mainpack.locketcameraclone.feature.camera.domain.PhotoProcessor
import javax.inject.Inject

class ProcessCapturedPhotoUseCase @Inject constructor(
    private val photoProcessor: PhotoProcessor
) {
    operator fun invoke(
        image: ImageProxy,
        viewHeight: Int,
        viewWidth: Int,
        isFrontCamera: Boolean
    ): Bitmap {
        image.use { capturedImage ->
            return photoProcessor.process(
                bitmap = capturedImage.toBitmapCompat(),
                rotationDegrees = capturedImage.imageInfo.rotationDegrees,
                isFrontCamera = isFrontCamera,
                targetWidth = viewWidth,
                targetHeight = viewHeight
            )
        }
    }
}


