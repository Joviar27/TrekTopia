package com.example.trektopia.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DateChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.intent.action.DATE_CHANGED") {
            context?.let {
                startFirestoreWork(it)
                startReminderWork(it, 7)
                startReminderWork(it, 16)
            }
        }
    }

    private fun startFirestoreWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequest.Builder(FirestoreWorker::class.java)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun startReminderWork(context: Context, hour: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        if(!isPast(hour)){
            val reminderWorkRequest = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
                .setConstraints(constraints)
                .setInitialDelay(getDesiredDelay(hour), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(reminderWorkRequest)
        }
    }

    private fun isPast(hour: Int): Boolean {
        return System.currentTimeMillis() > getDesiredTime(hour)
    }

    private fun getDesiredDelay(hour: Int): Long {
        return getDesiredTime(hour) - System.currentTimeMillis()
    }

    private fun getDesiredTime(hour: Int): Long{
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        return calendar.timeInMillis
    }
}