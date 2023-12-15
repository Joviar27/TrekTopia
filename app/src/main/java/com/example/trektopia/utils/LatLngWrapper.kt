package com.example.trektopia.utils

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLngWrapper(
    val latLngList: ArrayList<LatLng>
    ) : Parcelable

