package com.example.trektopia.core.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class Activity(
    val id: String = "",
    val timeStamp: Timestamp = Timestamp.now(),
    val duration: Double = 0.0,
    val startTime: Timestamp = Timestamp.now(),
    val stepCount: Int = 0,
    val distance: Double = 0.0,
    val speed: Double = 0.0,
    val route: String = ""
) : Parcelable