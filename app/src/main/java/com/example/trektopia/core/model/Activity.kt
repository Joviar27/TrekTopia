package com.example.trektopia.core.model

import com.google.firebase.Timestamp
import com.google.type.LatLng

data class Activity(
    val id: String,
    val timeStamp: Timestamp,
    val duration: Double,
    val stepCount: Int,
    val distance: Double,
    val speed: Double,
    val route: List<LatLng>
)