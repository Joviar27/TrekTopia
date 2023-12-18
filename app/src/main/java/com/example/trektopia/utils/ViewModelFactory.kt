package com.example.trektopia.utils

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.trektopia.core.di.Injection
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import com.example.trektopia.ui.history.HistoryViewModel
import com.example.trektopia.ui.home.HomeViewModel
import com.example.trektopia.ui.leaderboard.LeaderboardViewModel
import com.example.trektopia.ui.login.LoginViewModel
import com.example.trektopia.ui.main.AuthViewModel
import com.example.trektopia.ui.profile.ProfileViewModel
import com.example.trektopia.ui.record.RecordViewModel
import com.example.trektopia.ui.register.RegisterViewModel

class ViewModelFactory (
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val repository: Repository,
): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(authRepository,repository) as T
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(authRepository) as T
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> RegisterViewModel(authRepository) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository,gameRepository,authRepository) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) -> HistoryViewModel(repository,authRepository) as T
            modelClass.isAssignableFrom(RecordViewModel::class.java) -> RecordViewModel(repository,authRepository) as T
            modelClass.isAssignableFrom(LeaderboardViewModel::class.java) -> LeaderboardViewModel(gameRepository,repository,authRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(repository,gameRepository,authRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ViewModelFactory? = null

        @JvmStatic
        fun getInstance(context: Context): ViewModelFactory {
            return INSTANCE ?: synchronized(ViewModelFactory::class.java) {
                INSTANCE ?: ViewModelFactory(
                    Injection.provideAuthRepository(),
                    Injection.provideGameRepository(),
                    Injection.provideRepository(context),
                )
            }
        }
    }
}