package com.mainpack.locketcameraclone.ui.manager

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils
import com.mainpack.locketcameraclone.ui.utils.PermissionUtils.hasCameraPermission

object PermissionManager {
    class CameraPermissionManager(
        private val fragment: Fragment,
        private val onCameraGranted: () -> Unit,
        private val onCameraDenied: () -> Unit
    ) {
        private val requestPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permission ->
            val allGranted = permission.entries.all { it.value }
            if (allGranted) {
                onCameraGranted
            } else {
                onCameraDenied
            }
        }

        fun requestCameraPermission() {
            if (fragment.requireContext().hasCameraPermission()) {
                onCameraGranted
            } else {
                requestPermissionLauncher.launch(PermissionUtils.CAMERA_PERMISSION)
            }
        }
    }
}