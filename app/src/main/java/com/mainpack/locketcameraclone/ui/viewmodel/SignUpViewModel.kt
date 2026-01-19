package com.mainpack.locketcameraclone.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor() : ViewModel() {
    private val _passwordValidation = MutableLiveData<Boolean>()
    val passwordValidation: LiveData<Boolean> get() = _passwordValidation

    fun validatePassword(password: String, confirmPassword: String) {
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            _passwordValidation.value = false
            return
        }
        val isValid = password.length >= 8 && password == confirmPassword
        _passwordValidation.value = isValid
    }
}