package com.example.trektopia.utils

import android.content.Intent
import android.os.Build
import android.os.Parcelable
import java.lang.NullPointerException

inline fun <reified T : Parcelable> getParcelableExtra(intent: Intent, extraKey: String): T {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(extraKey, T::class.java)
    } else {
        @Suppress("Deprecation")
        intent.getParcelableExtra(extraKey)
    } ?: throw NullPointerException("$extraKey parcelable is null")
}
