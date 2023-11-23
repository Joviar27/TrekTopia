package com.example.trektopia.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.trektopia.core.repository.AuthRepository

class RegisterViewModel(
    private val authRepository: AuthRepository
): ViewModel() {
    fun register(username: String, email: String, password: String) =
        authRepository.signUp(username,email,password).asLiveData()
}