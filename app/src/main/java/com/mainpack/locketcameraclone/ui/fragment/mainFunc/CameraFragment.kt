package com.mainpack.locketcameraclone.ui.fragment.mainFunc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Builder
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentCameraBinding
import com.mainpack.locketcameraclone.ui.extension.BitmapExtension.centerCrop
import com.mainpack.locketcameraclone.ui.extension.BitmapExtension.rotateBitmap
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils.hasRequirePermission
import java.util.Locale
import kotlin.math.sqrt

class CameraFragment : Fragment() {
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var flashMode = FLASH_MODE_OFF
    private var originalBrightness = DEFAULT_BRIGHTNESS_VALUE

    private var camera: Camera? = null
    private var minZoomRatio = 1.0f
    private var oldDist: Float = 0f
    private var startZoomRatio: Float = 1f
    private var isZooming = false

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        val allGranted = permission.entries.all { it.value }
        if (allGranted) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireContext().hasRequirePermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(PermissionUtils.CAMERA_PERMISSION)
        }

        setUpListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraView.surfaceProvider
            }
            imageCapture = Builder()
                .setFlashMode(flashMode)
                .build()

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider?.unbindAll()

                camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                observerZoomState()
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    turnOffScreenFlash()
                    try {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = image.toBitmap()
                        val rotatedBitmap = rotateBitmap(bitmap, rotation, lensFacing)
                        val viewWidth = binding.cameraView.width
                        val viewHeight = binding.cameraView.height
                        val croppedBitmap = centerCrop(rotatedBitmap, viewWidth, viewHeight)

                        binding.previewImg.setImageBitmap(croppedBitmap)
                        binding.previewImg.visibility = View.VISIBLE
                        showPreviewUI()
                    } catch (e: java.lang.Exception) {
                        turnOffScreenFlash()
                        Log.e("CameraFragment", "Error converting image to bitmap", e)
                    } finally {
                        image.close()
                    }
                }
            }
        )
    }

    private fun setUpListener() {
        binding.apply {
            shotBtn.setOnClickListener {
                if (lensFacing == CameraSelector.LENS_FACING_FRONT && flashMode == FLASH_MODE_ON) {
                    toggleScreenFlashMode()
                    root.postDelayed({
                        takePhoto()
                    }, DELAY_VALUE)
                } else
                    takePhoto()
            }
            slipCameraBtn.setOnClickListener {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                startCamera()
            }
            cancelBtn.setOnClickListener {
                showCameraUI()
            }

            cameraFlashBtn.setOnClickListener {
                toggleFlashMode()
            }

            scaleValueTxt.setOnClickListener {
                val cameraControl = camera?.cameraControl ?: return@setOnClickListener
                val currentZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f

                if (currentZoom < 1.0f) {
                    cameraControl.setZoomRatio(1.0f)
                } else if (currentZoom == 1.0f) {
                    if (minZoomRatio < 1.0f) {
                        cameraControl.setZoomRatio(minZoomRatio)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Máy không hỗ trợ 0.5x",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    cameraControl.setZoomRatio(1.0f)
                }
            }

            cameraView.setOnTouchListener { view, event ->
                val cameraControl = camera?.cameraControl ?: return@setOnTouchListener false
                val cameraInfo = camera?.cameraInfo ?: return@setOnTouchListener false
                val zoomState = cameraInfo.zoomState.value ?: return@setOnTouchListener false

                when (event.action and MotionEvent.ACTION_MASK) {

                    ACTION_POINTER_DOWN -> {
                        oldDist = getFingerSpacing(event)

                        if (oldDist > 10f) {
                            startZoomRatio = zoomState.zoomRatio
                            isZooming = true
                        }
                    }

                    ACTION_MOVE -> {
                        if (isZooming && event.pointerCount == 2) {
                            val newDist = getFingerSpacing(event)

                            if (newDist > 10f) {
                                val scale = newDist / oldDist

                                val newZoom = startZoomRatio * scale

                                val clampedZoom = newZoom.coerceIn(
                                    zoomState.minZoomRatio,
                                    zoomState.maxZoomRatio
                                )

                                cameraControl.setZoomRatio(clampedZoom)
                            }
                        }
                    }

                    ACTION_UP,
                    ACTION_POINTER_UP -> {
                        isZooming = false
                        view.performClick()
                    }
                }
                true
            }
        }
    }

    private fun toggleFlashMode() {
        flashMode =
            if (flashMode == FLASH_MODE_OFF) FLASH_MODE_ON else FLASH_MODE_OFF

        imageCapture?.flashMode = flashMode

        updateFlashButtonUI()
    }

    private fun toggleScreenFlashMode() {
        val window = requireActivity().window
        val layoutParams = window.attributes
        originalBrightness = layoutParams.screenBrightness

        layoutParams.screenBrightness = SCREEN_BRIGHTNESS_MAX
        window.attributes = layoutParams

        binding.screenFlashOverlay.visibility = View.VISIBLE
    }

    private fun turnOffScreenFlash() {
        val window = requireActivity().window
        val layoutParams = window.attributes
        layoutParams.screenBrightness =
            android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
        binding.screenFlashOverlay.visibility = View.GONE
    }

    private fun updateFlashButtonUI() {
        val (colorRes, iconRes) = if (flashMode == FLASH_MODE_OFF) Pair(
            R.color.white,
            R.drawable.flash
        ) else Pair(R.color.locket_primary, R.drawable.flash)
        binding.cameraFlashBtn.setImageResource(iconRes)
        binding.cameraFlashBtn.setColorFilter(ContextCompat.getColor(requireContext(), colorRes))

    }

    private fun showPreviewUI() {
        binding.apply {
            cameraGroup.visibility = View.GONE
            previewModeBtnGroup.visibility = View.VISIBLE
        }
    }

    private fun showCameraUI() {
        startCamera()
        binding.apply {
            cameraGroup.visibility = View.VISIBLE
            cameraView.visibility = View.VISIBLE
            previewModeBtnGroup.visibility = View.GONE
            root.postDelayed(
                {
                    previewImg.visibility = View.GONE
                }, DELAY_VALUE
            )
            previewImg.setImageBitmap(null)
        }
    }

    private fun observerZoomState() {
        val cameraInfo = camera?.cameraInfo ?: return

        cameraInfo.zoomState.observe(viewLifecycleOwner) { state ->
            minZoomRatio = state.minZoomRatio

            val currentZoom = state.zoomRatio

            val zoomText = if (currentZoom % 1.0f == 0f) {
                "${currentZoom.toInt()}x"
            } else {
                String.format(Locale.US, "%.1fx", currentZoom)
            }
            binding.scaleValueTxt.text = zoomText
        }
    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return (sqrt(x * x + y * y))
    }

    companion object {
        private const val DELAY_VALUE = 500L

        private const val SCREEN_BRIGHTNESS_MAX = 1.0f

        private const val DEFAULT_BRIGHTNESS_VALUE = -1f
    }
}