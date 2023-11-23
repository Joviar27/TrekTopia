package com.example.trektopia.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.AuthState
import com.example.trektopia.core.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState =
        authRepository.getAuthState().asLiveData()

}