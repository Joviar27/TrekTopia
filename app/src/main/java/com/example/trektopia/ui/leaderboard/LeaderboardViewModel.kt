package com.example.trektopia.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val gameRepository: GameRepository,
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

    val leaderboard = gameRepository.getLeaderboard().asLiveData()

    val currentUser = repository.getUserData(uid).asLiveData()

    fun getCurrentUserRank(point: Int) =
        gameRepository.getUserRank(point).asLiveData()
}