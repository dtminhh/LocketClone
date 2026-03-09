package com.mainpack.locketcameraclone.feature.camera.domain.usecase

import javax.inject.Inject
import kotlin.math.sqrt

class CalculateFingerSpacingUseCase @Inject constructor() {
    operator fun invoke(x: Float, y: Float): Float {
        return sqrt(x * x + y * y)
    }
}

