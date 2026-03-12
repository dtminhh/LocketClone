package com.mainpack.locketcameraclone.feature.camera.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

enum class CameraLens {
    BACK,
    FRONT;

    fun toCameraSelectorLensFacing(): Int {
        return if (this == BACK) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
    }

    val isFrontCamera: Boolean
        get() = this == FRONT
}

enum class CameraFlashMode {
    OFF,
    ON;

    fun toImageCaptureFlashMode(): Int {
        return if (this == OFF) {
            ImageCapture.FLASH_MODE_OFF
        } else {
            ImageCapture.FLASH_MODE_ON
        }
    }
}

data class CameraUiState(
    val isCameraGranted: Boolean = false,
    val lensFacing: CameraLens = CameraLens.BACK,
    val flashMode: CameraFlashMode = CameraFlashMode.OFF
)

