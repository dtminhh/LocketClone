package com.mainpack.locketcameraclone.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

fun ImageProxy.toBitmapCompat(): Bitmap {
    return when (format) {
        ImageFormat.JPEG -> decodeJpeg()
        ImageFormat.YUV_420_888 -> decodeYuv420888()
        else -> throw IllegalArgumentException("Unsupported image format: $format")
    }
}

private fun ImageProxy.decodeJpeg(): Bitmap {
    val jpegBytes = planes.firstOrNull()?.buffer?.toByteArray()
        ?: throw IllegalStateException("JPEG image has no available planes")

    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw IllegalStateException("Failed to decode JPEG image")
}

private fun ImageProxy.decodeYuv420888(): Bitmap {
    val nv21 = toNv21()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val outputStream = ByteArrayOutputStream()

    yuvImage.compressToJpeg(Rect(0, 0, width, height), JPEG_QUALITY, outputStream)
    val jpegBytes = outputStream.toByteArray()

    return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw IllegalStateException("Failed to decode YUV image")
}

private fun ImageProxy.toNv21(): ByteArray {
    val ySize = width * height
    val nv21 = ByteArray(ySize + ySize / 2)

    copyPlaneToByteArray(
        plane = planes[0],
        planeWidth = width,
        planeHeight = height,
        output = nv21,
        outputOffset = 0,
        outputStride = 1
    )
    copyPlaneToByteArray(
        plane = planes[2],
        planeWidth = width / 2,
        planeHeight = height / 2,
        output = nv21,
        outputOffset = ySize,
        outputStride = 2
    )
    copyPlaneToByteArray(
        plane = planes[1],
        planeWidth = width / 2,
        planeHeight = height / 2,
        output = nv21,
        outputOffset = ySize + 1,
        outputStride = 2
    )

    return nv21
}

private fun copyPlaneToByteArray(
    plane: ImageProxy.PlaneProxy,
    planeWidth: Int,
    planeHeight: Int,
    output: ByteArray,
    outputOffset: Int,
    outputStride: Int
) {
    val buffer = plane.buffer.duplicate()
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val rowData = ByteArray(rowStride)
    var outputIndex = outputOffset

    for (row in 0 until planeHeight) {
        val bytesToRead = if (pixelStride == 1 && outputStride == 1) {
            planeWidth
        } else {
            (planeWidth - 1) * pixelStride + 1
        }

        buffer.get(rowData, 0, bytesToRead)

        var inputIndex = 0
        repeat(planeWidth) {
            output[outputIndex] = rowData[inputIndex]
            outputIndex += outputStride
            inputIndex += pixelStride
        }

        if (row < planeHeight - 1) {
            buffer.position(buffer.position() + rowStride - bytesToRead)
        }
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val duplicate = duplicate()
    val bytes = ByteArray(duplicate.remaining())
    duplicate.get(bytes)
    return bytes
}

private const val JPEG_QUALITY = 100

