package com.example.trektopia.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DAILY_RESET_ACTION) {
            startFirestoreWork(context)
        }
        if(intent.action==SHOW_NOTIF_ACTION){
            startReminderWork(context)
        }
    }

    fun setReminderAlarm(context: Context, hour: Int, action: String, id : Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        alarmIntent.action = action

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getBroadcast(context, id, alarmIntent, flag)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelAlarm(context: Context, alarmId: Int, action: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.action = action

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, flag)

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun startReminderWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val reminderWorkRequest = OneTimeWorkRequest.Builder(ReminderWorker::class.java)
            .setConstraints(constraints)
            .addTag("Reminder Worker")
            .build()

        WorkManager.getInstance(context).enqueue(reminderWorkRequest)
    }

    private fun startFirestoreWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequest.Builder(FirestoreWorker::class.java)
            .setConstraints(constraints)
            .addTag("Reset worker")
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    companion object{
        const val SHOW_NOTIF_ACTION = "com.example.trektopia.SHOW_NOTIF_ACTION"
        const val DAILY_RESET_ACTION = "com.example.trektopia.DAILY_RESET_ACTION"

        const val REMINDER_ALARM_ID_1 = 202
        const val REMINDER_ALARM_ID_2 = 206
        const val RESET_ALARM_ID = 303
    }
}