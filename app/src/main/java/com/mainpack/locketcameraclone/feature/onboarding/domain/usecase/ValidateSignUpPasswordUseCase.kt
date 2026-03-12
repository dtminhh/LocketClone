package com.mainpack.locketcameraclone.feature.onboarding.domain.usecase

import javax.inject.Inject

class ValidateSignUpPasswordUseCase @Inject constructor() {
    operator fun invoke(password: String, confirmPassword: String): Boolean {
        if (password.isBlank() || confirmPassword.isBlank()) {
            return false
        }

        return password.length >= MIN_PASSWORD_LENGTH && password == confirmPassword
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
