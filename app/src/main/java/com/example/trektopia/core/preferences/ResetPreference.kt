package com.example.trektopia.core.preferences

import android.content.Context

class ResetPreference(context: Context) {
    private val resetSharedPref = context.getSharedPreferences("ResetPref", Context.MODE_PRIVATE)

    fun getResetStatus() = resetSharedPref.getBoolean("resetStarted", false)

    fun setResetStatus(status : Boolean){
        resetSharedPref
            .edit()
            .putBoolean("resetStarted", status)
            .apply()
    }
}