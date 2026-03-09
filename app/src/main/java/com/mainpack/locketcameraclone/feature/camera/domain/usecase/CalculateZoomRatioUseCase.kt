package com.mainpack.locketcameraclone.feature.camera.domain.usecase

import javax.inject.Inject

class CalculateZoomRatioUseCase @Inject constructor() {
    operator fun invoke(
        oldDist: Float,
        newDist: Float,
        startZoomRatio: Float,
        minZoomRatio: Float,
        maxZoomRatio: Float
    ): Float {
        if (oldDist <= 0f) {
            return startZoomRatio.coerceIn(minZoomRatio, maxZoomRatio)
        }

        val scale = newDist / oldDist
        val newZoom = startZoomRatio * scale
        return newZoom.coerceIn(minZoomRatio, maxZoomRatio)
    }
}

