package com.mainpack.locketcameraclone.ui.fragment.mainFunc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class CameraFragment : Fragment() {
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var flashMode = FLASH_MODE_OFF
    private var originalBrightness = DEFAULT_BRIGHTNESS_VALUE

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

                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
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

    companion object {
        private const val DELAY_VALUE = 500L

        private const val SCREEN_BRIGHTNESS_MAX = 1.0f

        private const val DEFAULT_BRIGHTNESS_VALUE = -1f
    }
}