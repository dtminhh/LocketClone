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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.Builder
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentCameraBinding
import com.mainpack.locketcameraclone.ui.manager.PermissionManager.CameraPermissionManager
import com.mainpack.locketcameraclone.ui.viewmodel.CameraViewModel
import com.mainpack.locketcameraclone.ui.viewmodel.UiState
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class CameraFragment : Fragment() {
    private lateinit var cameraPermissionManager: CameraPermissionManager
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var originalBrightness = DEFAULT_BRIGHTNESS_VALUE
    private var camera: Camera? = null
    private var oldDist: Float = 0f
    private var startZoomRatio: Float = 1f
    private var isZooming = false
    private val cameraViewModel: CameraViewModel by viewModels()
    private var currentLensFacing: Int? = null
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionManager = CameraPermissionManager(
            fragment = this,
            onCameraGranted = {
                cameraViewModel.cameraGranted(true)
            },
            onCameraDenied = {
                Toast.makeText(requireContext(),
                    getString(R.string.permission_not_granted), Toast.LENGTH_SHORT)
                    .show()
            }
        )
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
        cameraPermissionManager.requestCameraPermission()
        setUpListener()
        setUpObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startCamera() {
        binding.cameraView.post {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

            cameraProviderFuture.addListener({
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.cameraView.surfaceProvider
                }

                val currentState = cameraViewModel.cameraState.value ?: UiState()
                val currentLens = currentState.lensFacing
                val currentFlash = currentState.flashMode

                imageCapture = Builder()
                    .setFlashMode(currentFlash)
                    .build()

                val cameraSelector = CameraSelector.Builder().requireLensFacing(currentLens).build()
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
    }

    private fun setUpObserver() {
        cameraViewModel.cameraState.observe(viewLifecycleOwner) { value ->
            updateFlashButtonUI(value.flashMode)

            imageCapture?.flashMode = value.flashMode

            if (currentLensFacing != value.lensFacing) {
                currentLensFacing = value.lensFacing
                startCamera()
            }
        }

        cameraViewModel.processedBitMap.observe(viewLifecycleOwner) { bitmap ->
            binding.previewImg.setImageBitmap(bitmap)
            binding.previewImg.visibility = View.VISIBLE
            showPreviewUI()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    turnOffScreenFlash()
                    cameraViewModel.formatOutputImage(
                        image,
                        binding.cameraView.height,
                        binding.cameraView.width,
                        cameraViewModel.cameraState.value?.lensFacing
                            ?: CameraSelector.LENS_FACING_BACK
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Toast.makeText(requireContext(),
                        getString(R.string.take_photo_error), Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun setUpListener() {
        binding.apply {
            shotBtn.setOnClickListener {
                val currentState = cameraViewModel.cameraState.value ?: return@setOnClickListener
                if (currentState.lensFacing == CameraSelector.LENS_FACING_FRONT && currentState.flashMode == FLASH_MODE_ON) {
                    toggleScreenFlashMode()
                    root.postDelayed({
                        takePhoto()
                    }, DELAY_VALUE)
                } else
                    takePhoto()
            }
            slipCameraBtn.setOnClickListener {
                cameraViewModel.toggleCamera()
            }

            cancelBtn.setOnClickListener {
                showCameraUI()
            }

            cameraFlashBtn.setOnClickListener {
                cameraViewModel.toggleFlash()
            }

            scaleValueTxt.setOnClickListener {
                val cameraControl = camera?.cameraControl ?: return@setOnClickListener
                val zoomState = camera?.cameraInfo?.zoomState?.value ?: return@setOnClickListener

                val currentZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
                val minZoom = zoomState.minZoomRatio

                val isAtx1 = currentZoom in 0.95f..1.05f

                when {
                    currentZoom < 0.95f -> {
                        cameraControl.setZoomRatio(1.0f)
                    }

                    isAtx1 -> {
                        if (minZoom < 1.0f)
                            cameraControl.setZoomRatio(minZoom)
                        else
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.device_is_not_supported_0_5x),
                                Toast.LENGTH_SHORT
                            ).show()
                    }

                    else -> {
                        cameraControl.setZoomRatio(1.0f)
                    }
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
                                val clampedZoom = cameraViewModel.updateZoomRatio(
                                    oldDist = oldDist,
                                    newDist = newDist,
                                    startZoomRatio = startZoomRatio,
                                    zoomState = zoomState
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

    private fun updateFlashButtonUI(flashMode: Int) {
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
            val currentZoom = state.zoomRatio

            val zoomText = if (currentZoom % 1.0f == 0f) {
                "${currentZoom.toInt()}x"
            } else {
                String.format(Locale.US, getString(R.string.zoom_value_format), currentZoom)
            }
            binding.scaleValueTxt.text = zoomText
        }
    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return cameraViewModel.getZoomDist(x, y)
    }

    companion object {
        private const val DELAY_VALUE = 500L

        private const val SCREEN_BRIGHTNESS_MAX = 1.0f

        private const val DEFAULT_BRIGHTNESS_VALUE = -1f
    }
}