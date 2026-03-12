package com.mainpack.locketcameraclone.core.permission

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.mainpack.locketcameraclone.core.permission.PermissionUtils.hasCameraPermission

class CameraPermissionManager(
    private val fragment: Fragment,
    private val onCameraGranted: () -> Unit,
    private val onCameraDenied: () -> Unit
) {
    private val requestPermissionLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onCameraGranted()
        } else {
            onCameraDenied()
        }
    }

    fun requestCameraPermission() {
        if (fragment.requireContext().hasCameraPermission()) {
            onCameraGranted()
            return
        }

        requestPermissionLauncher.launch(PermissionUtils.CAMERA_PERMISSION)
    }
}
