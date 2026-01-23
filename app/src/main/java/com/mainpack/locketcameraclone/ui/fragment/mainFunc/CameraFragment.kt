package com.mainpack.locketcameraclone.ui.fragment.mainFunc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mainpack.locketcameraclone.databinding.FragmentCameraBinding
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils.hasRequirePermission

class CameraFragment : Fragment() {
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
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
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraView.surfaceProvider
            }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("CameraFragment", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setUpListener() {
        binding.apply {
            slipCameraBtn.setOnClickListener {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                startCamera()
            }
        }
    }
}