package com.example.trektopia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.model.enum.TaskType
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: Repository,
    private val gameRepository: GameRepository,
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

    val user = repository.getUserData(uid).asLiveData()

    val missions = gameRepository.getTaskWithProgress(uid, TaskType.MISSION).asLiveData()

    fun claimTaskReward(relationId:String, reward: Int) =
        gameRepository.claimTaskReward(uid, relationId, reward, TaskType.MISSION).asLiveData()

    fun getNotifStatus() = repository.getNotificationStatus()
    fun setNotifStatus(status: Boolean) = repository.setNotificationStatus(status)

    fun getResetStatus() = repository.getResetStatus()
    fun setResetStatus(status: Boolean) = repository.setResetStatus(status)

}