package com.example.trektopia.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.model.Activity
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class RecordViewModel(
    private val repository: Repository,
    private val authRepository: AuthRepository
): ViewModel(){

    private lateinit var uid: String

    init{
        viewModelScope.launch {
            authRepository
                .getUid()
                .distinctUntilChanged()
                .collect{ uid = it }
        }
    }

    fun saveRecord(activity: Activity) =
        repository.addActivityAndUpdateProgress(uid, activity).asLiveData()
}