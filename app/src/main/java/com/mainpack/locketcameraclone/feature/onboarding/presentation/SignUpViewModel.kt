package com.mainpack.locketcameraclone.feature.onboarding.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mainpack.locketcameraclone.feature.onboarding.domain.usecase.ValidateSignUpPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val validateSignUpPasswordUseCase: ValidateSignUpPasswordUseCase
) : ViewModel() {
    private val _uiState = MutableLiveData(SignUpUiState())
    val uiState: LiveData<SignUpUiState> = _uiState

    fun onPasswordInputChanged(password: String, confirmPassword: String) {
        val isPasswordValid = validateSignUpPasswordUseCase(password, confirmPassword)
        _uiState.value = _uiState.value?.copy(isPasswordValid = isPasswordValid)
            ?: SignUpUiState(isPasswordValid = isPasswordValid)
    }
}
