package com.example.trektopia.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.di.Injection
import com.example.trektopia.core.repository.AuthRepository
import com.example.trektopia.core.repository.GameRepository
import com.example.trektopia.core.repository.Repository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class FirestoreWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var gameRepository= Injection.provideGameRepository()
    private var repository= Injection.provideRepository()
    private var authRepository= Injection.provideAuthRepository()

    override suspend fun doWork(): Result  = coroutineScope{
        try {
            val uid = authRepository.getUid().first()

            repository.checkLatestActiveDate(uid).collect(){result ->
                if(result is ResultState.Success){
                    updateData(uid, result.data)
                } else if(result is ResultState.Error){
                    throw FirebaseFirestoreException(result.error,
                        FirebaseFirestoreException.Code.NOT_FOUND)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("FirestoreWoker", "doWork: $e")
            Result.failure()
        }
    }

    private suspend fun updateData(uid: String, latestActive: LocalDate){
        if(!isPreviousDay(latestActive)){
            gameRepository.resetStreak(uid).collect(){resetStreakResult ->
                if(resetStreakResult is ResultState.Error)
                    throw FirebaseFirestoreException(resetStreakResult.error,
                        FirebaseFirestoreException.Code.NOT_FOUND)
            }
        }
        gameRepository.resetDailyMission().collect(){ resetMissionResult ->
            if(resetMissionResult is ResultState.Error)
                throw FirebaseFirestoreException(resetMissionResult.error,
                    FirebaseFirestoreException.Code.NOT_FOUND)
        }
    }

    private fun isPreviousDay(localDate: LocalDate): Boolean {
        val currentDate = LocalDate.now()
        val previousDate = currentDate.minus(1, ChronoUnit.DAYS)
        return localDate.isEqual(previousDate)
    }
}