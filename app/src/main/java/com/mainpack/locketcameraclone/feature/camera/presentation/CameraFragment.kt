package com.mainpack.locketcameraclone.feature.camera.presentation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mainpack.locketcameraclone.R
import com.mainpack.locketcameraclone.databinding.FragmentCameraBinding
import com.mainpack.locketcameraclone.core.permission.CameraPermissionManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class CameraFragment : Fragment() {
    private lateinit var cameraPermissionManager: CameraPermissionManager
    private lateinit var cameraXController: CameraXController
    private var originalBrightness = -1f
    private val cameraViewModel: CameraViewModel by viewModels()
    private var currentLensFacing: CameraLens? = null
    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraPermissionManager = CameraPermissionManager(
            fragment = this,
            onCameraGranted = {
                cameraViewModel.onCameraPermissionResult(true)
            },
            onCameraDenied = {
                cameraViewModel.onCameraPermissionResult(false)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_not_granted), Toast.LENGTH_SHORT
                )
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
        cameraXController = CameraXController(
            lifecycleOwner = viewLifecycleOwner,
            previewView = binding.cameraView,
            onZoomRatioChanged = ::updateZoomText
        )
        setUpListener()
        setUpObserver()
        cameraPermissionManager.requestCameraPermission()
    }

    override fun onDestroyView() {
        cameraXController.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun startCamera(state: CameraUiState) {
        cameraXController.startCamera(requireContext(), state) { exception ->
            Log.e(TAG, "Use case binding failed", exception)
        }
    }

    private fun setUpObserver() {
        cameraViewModel.cameraState.observe(viewLifecycleOwner) { state ->
            updateFlashButtonUI(state.flashMode)

            cameraXController.updateFlashMode(state.flashMode)

            if (!state.isCameraGranted) {
                cameraXController.clear()
                currentLensFacing = null
                return@observe
            }

            if (currentLensFacing != state.lensFacing || !cameraXController.isBound) {
                currentLensFacing = state.lensFacing
                startCamera(state)
            }
        }

        cameraViewModel.processedBitMap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap == null) {
                return@observe
            }

            binding.previewImg.setImageBitmap(bitmap)
            binding.previewImg.visibility = View.VISIBLE
            showPreviewUI()
        }
    }

    private fun takePhoto() {
        cameraXController.takePhoto(
            context = requireContext(),
            onSuccess = { image ->
                turnOffScreenFlash()
                val isFrontCamera = cameraViewModel.cameraState.value?.lensFacing?.isFrontCamera == true
                cameraViewModel.onPhotoCaptured(
                    image,
                    binding.cameraView.height,
                    binding.cameraView.width,
                    isFrontCamera
                )
            },
            onError = {
                turnOffScreenFlash()
                Toast.makeText(
                    requireContext(),
                    getString(R.string.take_photo_error), Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun setUpListener() {
        binding.apply {
            shotBtn.setOnClickListener {
                val currentState = cameraViewModel.cameraState.value ?: return@setOnClickListener
                if (shouldUseScreenFlash(currentState)) {
                    toggleScreenFlashMode()
                    root.postDelayed({
                        takePhoto()
                    }, DELAY_VALUE)
                } else
                    takePhoto()
            }
            slipCameraBtn.setOnClickListener {
                cameraViewModel.onSwitchCameraClicked()
            }

            cancelBtn.setOnClickListener {
                cameraViewModel.clearCapturedPhotoPreview()
                showCameraUI()
            }

            cameraFlashBtn.setOnClickListener {
                cameraViewModel.onFlashClicked()
            }

            scaleValueTxt.setOnClickListener {
                cameraXController.toggleZoomPreset(
                    defaultZoomValue = DEFAULT_ZOOM_VALUE,
                    zoomTolerance = ZOOM_TOLERANCE,
                    onMinZoomUnsupported = {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.device_is_not_supported_0_5x),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            cameraView.setOnTouchListener { view, event ->
                val handled = cameraXController.handlePreviewTouch(
                    event = event,
                    minPointerDistance = MIN_POINTER_DISTANCE,
                    calculateFingerSpacing = cameraViewModel::getZoomDist,
                    calculateZoomRatio = cameraViewModel::updateZoomRatio
                )
                if (handled && isTouchReleaseEvent(event)) {
                    view.performClick()
                }
                handled
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
        layoutParams.screenBrightness = if (originalBrightness >= 0f) {
            originalBrightness
        } else {
            android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = layoutParams
        binding.screenFlashOverlay.visibility = View.GONE
        originalBrightness = -1f
    }

    private fun updateFlashButtonUI(flashMode: CameraFlashMode) {
        val (colorRes, iconRes) = if (flashMode == CameraFlashMode.OFF) Pair(
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
        val state = cameraViewModel.cameraState.value
        if (state?.isCameraGranted == true) {
            currentLensFacing = state.lensFacing
            startCamera(state)
        }
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

    private fun shouldUseScreenFlash(state: CameraUiState): Boolean {
        return state.lensFacing == CameraLens.FRONT && state.flashMode == CameraFlashMode.ON
    }

    private fun updateZoomText(currentZoom: Float) {
        val zoomText = if (currentZoom % DEFAULT_ZOOM_VALUE == 0f) {
            "${currentZoom.toInt()}x"
        } else {
            String.format(Locale.US, getString(R.string.zoom_value_format), currentZoom)
        }
        binding.scaleValueTxt.text = zoomText
    }

    private fun isTouchReleaseEvent(event: MotionEvent): Boolean {
        return when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> true
            else -> false
        }
    }

    private companion object {
        const val TAG = "CameraFragment"
    }
}

