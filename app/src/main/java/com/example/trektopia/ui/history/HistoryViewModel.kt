package com.example.trektopia.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.Repository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: Repository,
    private val authRepository: AuthRepository
): ViewModel() {

    private lateinit var uid: String

    init{
        viewModelScope.launch {
            authRepository
                .getUid()
                .distinctUntilChanged()
                .collect{ uid = it }
        }
    }

    val activities = repository.getUserActivities(uid).asLiveData()
}