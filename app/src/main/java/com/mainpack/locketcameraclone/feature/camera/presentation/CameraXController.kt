package com.mainpack.locketcameraclone.feature.camera.presentation

import android.content.Context
import android.view.MotionEvent
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Builder
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

class CameraXController(
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onZoomRatioChanged: (Float) -> Unit
) {
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var zoomStateLiveData: LiveData<ZoomState>? = null
    private var oldDist = 0f
    private var startZoomRatio = DEFAULT_ZOOM_RATIO
    private var isZooming = false

    val isBound: Boolean
        get() = camera != null

    fun startCamera(
        context: Context,
        state: CameraUiState,
        onBindingError: (Exception) -> Unit
    ) {
        previewView.post {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                imageCapture = Builder()
                    .setFlashMode(state.flashMode.toImageCaptureFlashMode())
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(state.lensFacing.toCameraSelectorLensFacing())
                    .build()

                try {
                    cameraProvider?.unbindAll()
                    camera = cameraProvider?.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    observeZoomState()
                } catch (exception: Exception) {
                    onBindingError(exception)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun clear() {
        zoomStateLiveData?.removeObservers(lifecycleOwner)
        zoomStateLiveData = null
        cameraProvider?.unbindAll()
        cameraProvider = null
        camera = null
        imageCapture = null
        oldDist = 0f
        startZoomRatio = DEFAULT_ZOOM_RATIO
        isZooming = false
    }

    fun updateFlashMode(flashMode: CameraFlashMode) {
        imageCapture?.flashMode = flashMode.toImageCaptureFlashMode()
    }

    fun takePhoto(
        context: Context,
        onSuccess: (ImageProxy) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val currentImageCapture = imageCapture ?: return
        currentImageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    onSuccess(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    onError(exception)
                }
            }
        )
    }

    fun toggleZoomPreset(
        defaultZoomValue: Float,
        zoomTolerance: Float,
        onMinZoomUnsupported: () -> Unit
    ) {
        val cameraControl = camera?.cameraControl ?: return
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return

        val currentZoom = zoomState.zoomRatio
        val minZoom = zoomState.minZoomRatio
        val minRange = defaultZoomValue - zoomTolerance
        val maxRange = defaultZoomValue + zoomTolerance
        val isAtDefaultZoom = currentZoom in minRange..maxRange

        when {
            currentZoom < minRange -> cameraControl.setZoomRatio(defaultZoomValue)
            isAtDefaultZoom -> {
                if (minZoom < defaultZoomValue) {
                    cameraControl.setZoomRatio(minZoom)
                } else {
                    onMinZoomUnsupported()
                }
            }
            else -> cameraControl.setZoomRatio(defaultZoomValue)
        }
    }

    fun handlePreviewTouch(
        event: MotionEvent,
        minPointerDistance: Float,
        calculateFingerSpacing: (Float, Float) -> Float,
        calculateZoomRatio: (Float, Float, Float, Float, Float) -> Float
    ): Boolean {
        val cameraControl = camera?.cameraControl ?: return false
        val cameraInfo = camera?.cameraInfo ?: return false
        val zoomState = cameraInfo.zoomState.value ?: return false

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = getFingerSpacing(event, calculateFingerSpacing)
                if (oldDist > minPointerDistance) {
                    startZoomRatio = zoomState.zoomRatio
                    isZooming = true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isZooming && event.pointerCount == POINTER_COUNT_FOR_PINCH) {
                    val newDist = getFingerSpacing(event, calculateFingerSpacing)
                    if (newDist > minPointerDistance) {
                        val clampedZoom = calculateZoomRatio(
                            oldDist,
                            newDist,
                            startZoomRatio,
                            zoomState.minZoomRatio,
                            zoomState.maxZoomRatio
                        )
                        cameraControl.setZoomRatio(clampedZoom)
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                isZooming = false
            }
        }
        return true
    }

    private fun observeZoomState() {
        val nextZoomState = camera?.cameraInfo?.zoomState ?: return
        zoomStateLiveData?.removeObservers(lifecycleOwner)
        zoomStateLiveData = nextZoomState
        nextZoomState.observe(lifecycleOwner) { state ->
            onZoomRatioChanged(state.zoomRatio)
        }
    }

    private fun getFingerSpacing(
        event: MotionEvent,
        calculateFingerSpacing: (Float, Float) -> Float
    ): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return calculateFingerSpacing(x, y)
    }

    private companion object {
        const val DEFAULT_ZOOM_RATIO = 1f
        const val POINTER_COUNT_FOR_PINCH = 2
    }
}

