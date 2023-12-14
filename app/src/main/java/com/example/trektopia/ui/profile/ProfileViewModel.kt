package com.example.trektopia.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.model.User
import com.example.trektopia.core.model.enum.TaskType
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ProfileViewModel(
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

    fun updateUserInfo(newUser:User) = repository.updateUserInfo(uid, newUser).asLiveData()

    fun updateProfile(newUri : Uri) = repository.updateProfilePicture(newUri, uid).asLiveData()

    val achievements = gameRepository.getTaskWithProgress(uid, TaskType.ACHIEVEMENT).asLiveData()

    fun claimTaskReward(relationId:String, reward: Int) =
        gameRepository.claimTaskReward(uid, relationId, reward, TaskType.ACHIEVEMENT).asLiveData()
}