package com.example.trektopia.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.di.Injection
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class ResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var gameRepository= Injection.provideGameRepository()
    private var repository= Injection.provideRepository(context)
    private var authRepository= Injection.provideAuthRepository()

    override suspend fun doWork(): Result  = coroutineScope{
        try {
            val uid = authRepository.getUid().first()

            repository.checkLatestActiveDate(uid,false).collect{result ->
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

    private suspend fun updateData(uid: String, isActiveToday: Boolean){
        if(!isActiveToday){
            gameRepository.resetStreak(uid).collect{resetStreakResult ->
                if(resetStreakResult is ResultState.Error)
                    throw FirebaseFirestoreException(resetStreakResult.error,
                        FirebaseFirestoreException.Code.NOT_FOUND)
            }
        }
        gameRepository.resetDailyMission(uid).collect{ resetMissionResult ->
            if(resetMissionResult is ResultState.Error)
                throw FirebaseFirestoreException(resetMissionResult.error,
                    FirebaseFirestoreException.Code.NOT_FOUND)
        }
    }
}