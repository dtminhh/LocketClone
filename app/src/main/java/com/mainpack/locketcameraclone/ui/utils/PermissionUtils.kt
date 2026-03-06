package com.mainpack.locketcameraclone.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {
    val CAMERA_PERMISSION = arrayOf(
        Manifest.permission.CAMERA,
    )

    fun Context.hasCameraPermission(): Boolean {
        return CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}