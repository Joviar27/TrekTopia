package com.example.trektopia.core.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class Activity(
    val id: String,
    val timeStamp: Timestamp,
    val duration: Double,
    val stepCount: Int,
    val distance: Double,
    val speed: Double,
    val route: List<LatLng>
) : Parcelable