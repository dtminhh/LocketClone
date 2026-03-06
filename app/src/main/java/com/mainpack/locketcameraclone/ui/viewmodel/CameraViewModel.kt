package com.mainpack.locketcameraclone.ui.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.ZoomState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mainpack.locketcameraclone.ui.extension.BitmapExtension.centerCrop
import com.mainpack.locketcameraclone.ui.extension.BitmapExtension.rotateBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@HiltViewModel
class CameraViewModel @Inject constructor(
) : ViewModel() {
    private val _cameraState = MutableLiveData(UiState())
    val cameraState: LiveData<UiState> = _cameraState

    private val _processedBitMap = MutableLiveData<Bitmap>()
    val processedBitMap: LiveData<Bitmap> = _processedBitMap

    fun cameraGranted(cameraGrantedState: Boolean){
        _cameraState.value = _cameraState.value?.copy(isCameraGranted = cameraGrantedState)
    }

    fun toggleCamera() {
        val currentState = _cameraState.value ?: UiState()

        val newLens = if (currentState.lensFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT
        else
            CameraSelector.LENS_FACING_BACK

        _cameraState.value = currentState.copy(lensFacing = newLens)
    }

    fun toggleFlash() {
        val currentState = _cameraState.value ?: UiState()

        val newFlash = if (currentState.flashMode == ImageCapture.FLASH_MODE_OFF)
            ImageCapture.FLASH_MODE_ON
        else
            ImageCapture.FLASH_MODE_OFF

        _cameraState.value = currentState.copy(flashMode = newFlash)
    }

    fun getZoomDist(x: Float, y: Float): Float {
        return (sqrt(x * x + y * y))
    }

    fun updateZoomRatio(
        oldDist: Float,
        newDist: Float,
        startZoomRatio: Float,
        zoomState: ZoomState
    ): Float {
        val scale = newDist / oldDist

        val newZoom = startZoomRatio * scale

        return newZoom.coerceIn(
            zoomState.minZoomRatio,
            zoomState.maxZoomRatio
        )
    }

    fun formatOutputImage(
        image: ImageProxy,
        viewHeight: Int,
        viewWidth: Int,
        lensFacing: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            image.use { image ->
                val rotation = image.imageInfo.rotationDegrees
                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap, rotation, lensFacing)
                _processedBitMap.postValue(centerCrop(rotatedBitmap, viewWidth, viewHeight))
            }
        }
    }
}

data class UiState(
    val isCameraGranted: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val zoomRatio: Float = 1.0f
)