package com.example.trektopia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trektopia.R
import com.example.trektopia.core.ResultState
import com.example.trektopia.core.di.Injection
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var repository= Injection.provideRepository(context)
    private var authRepository= Injection.provideAuthRepository()

    private lateinit var notificationManager: NotificationManager

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val uid = authRepository.getUid().first()

            repository.checkLatestActiveDate(uid,true).collect{result ->
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

    private fun showNotification(isActiveToday: Boolean){
        if (!isActiveToday) {
            notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            createNotificationChannel()

            val notification = NotificationCompat.Builder(applicationContext, "reminder_channel_id")
                .setSmallIcon(R.drawable.ic_walk_small)
                .setContentTitle("Reminder")
                .setLargeIcon(ContextCompat.getDrawable(applicationContext,R.drawable.ic_warning)?.toBitmap())
                .setContentText("Don't forget to exercise today!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(1, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel_id",
                "Reminder Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}