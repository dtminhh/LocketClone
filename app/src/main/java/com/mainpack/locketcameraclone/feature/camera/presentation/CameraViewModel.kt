package com.mainpack.locketcameraclone.feature.camera.presentation

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mainpack.locketcameraclone.feature.camera.domain.usecase.CalculateFingerSpacingUseCase
import com.mainpack.locketcameraclone.feature.camera.domain.usecase.CalculateZoomRatioUseCase
import com.mainpack.locketcameraclone.feature.camera.domain.usecase.ProcessCapturedPhotoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val processCapturedPhotoUseCase: ProcessCapturedPhotoUseCase,
    private val calculateZoomRatioUseCase: CalculateZoomRatioUseCase,
    private val calculateFingerSpacingUseCase: CalculateFingerSpacingUseCase
) : ViewModel() {
    private val _cameraState = MutableLiveData(CameraUiState())
    val cameraState: LiveData<CameraUiState> = _cameraState

    private val _processedBitMap = MutableLiveData<Bitmap?>(null)
    val processedBitMap: LiveData<Bitmap?> = _processedBitMap

    fun onCameraPermissionResult(isGranted: Boolean) {
        _cameraState.value = _cameraState.value?.copy(isCameraGranted = isGranted)
    }

    fun onSwitchCameraClicked() {
        val currentState = _cameraState.value ?: CameraUiState()

        val newLens = if (currentState.lensFacing == CameraLens.BACK) {
            CameraLens.FRONT
        } else {
            CameraLens.BACK
        }

        _cameraState.value = currentState.copy(lensFacing = newLens)
    }

    fun onFlashClicked() {
        val currentState = _cameraState.value ?: CameraUiState()

        val newFlash = if (currentState.flashMode == CameraFlashMode.OFF) {
            CameraFlashMode.ON
        } else {
            CameraFlashMode.OFF
        }

        _cameraState.value = currentState.copy(flashMode = newFlash)
    }

    fun getZoomDist(x: Float, y: Float): Float {
        return calculateFingerSpacingUseCase(x, y)
    }

    fun updateZoomRatio(
        oldDist: Float,
        newDist: Float,
        startZoomRatio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ): Float {
        return calculateZoomRatioUseCase(
            oldDist = oldDist,
            newDist = newDist,
            startZoomRatio = startZoomRatio,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio
        )
    }

    fun onPhotoCaptured(
        image: ImageProxy,
        viewHeight: Int,
        viewWidth: Int,
        isFrontCamera: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _processedBitMap.postValue(
                processCapturedPhotoUseCase(
                    image = image,
                    viewHeight = viewHeight,
                    viewWidth = viewWidth,
                    isFrontCamera = isFrontCamera
                )
            )
        }
    }

    fun clearCapturedPhotoPreview() {
        _processedBitMap.value = null
    }
}
