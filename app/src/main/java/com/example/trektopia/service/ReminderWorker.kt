package com.example.trektopia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.di.Injection
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var repository= Injection.provideRepository()
    private var authRepository= Injection.provideAuthRepository()

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val uid = authRepository.getUid().first()

            repository.checkLatestActiveDate(uid).collect{result ->
                if(result is ResultState.Success){
                    showNotification(result.data)
                }else if(result is ResultState.Error){
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

    private fun showNotification(latestActive: LocalDate){
        if (latestActive == LocalDate.now()) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(notificationManager)
            }

            //TODO: Set notification icon
            val notification = NotificationCompat.Builder(applicationContext, "reminder_channel_id")
                .setContentTitle("Reminder")
                .setContentText("You have a reminder for today!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(1, notification)
        }
    }

    private fun createNotificationChannel(manager: NotificationManager){
        val channel = NotificationChannel(
            "reminder_channel_id",
            "Reminder Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}