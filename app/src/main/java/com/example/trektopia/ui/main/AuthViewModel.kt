package com.example.trektopia.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.Repository

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val repository: Repository
) : ViewModel() {

    val authState =
        authRepository.getAuthState().asLiveData()

    fun logout() = authRepository.logout()

    fun getNotifStatus() = repository.getNotificationStatus()
    fun setNotifStatus(status: Boolean) = repository.setNotificationStatus(status)

    fun getResetStatus() = repository.getResetStatus()
    fun setResetStatus(status: Boolean) = repository.setResetStatus(status)
}