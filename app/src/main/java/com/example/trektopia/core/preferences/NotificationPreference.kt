package com.example.trektopia.core.preferences

import android.content.Context

class NotificationPreference(context: Context) {
    private val notifSharedPref = context.getSharedPreferences("NotifPref", Context.MODE_PRIVATE)

    fun getNotificationStatus() = notifSharedPref.getBoolean("reminderStarted", false)

    fun setNotificationStatus(status: Boolean){
        notifSharedPref
            .edit()
            .putBoolean("reminderStarted", status)
            .apply()
    }
}