package com.mainpack.locketcameraclone.ui.fragment.mainFunc

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mainpack.locketcameraclone.databinding.FragmentCameraBinding
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils.hasRequirePermission
import java.text.SimpleDateFormat
import java.util.Locale

class CameraFragment : Fragment() {
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var imageCapture: ImageCapture? = null

    private var cameraProvider: ProcessCameraProvider? = null

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
            imageCapture = ImageCapture.Builder().build()

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

    private fun setUpListener() {
        binding.apply {
            shotBtn.setOnClickListener {
                val bitmapPreview = cameraView.bitmap

                if (bitmapPreview != null) {
                    previewImg.setImageBitmap(bitmapPreview)
                    previewImg.visibility = View.VISIBLE
                    showPreviewUI()
                }
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
        }
    }

    private fun showPreviewUI() {
        cameraProvider?.unbindAll()
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
        private const val FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val DELAY_VALUE = 500L
    }
}