package com.example.trektopia.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.trektopia.core.repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState =
        authRepository.getAuthState().asLiveData()

    fun logout() = authRepository.logout()


}