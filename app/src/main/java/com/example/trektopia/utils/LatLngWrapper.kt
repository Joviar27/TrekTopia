package com.example.trektopia.utils

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.parcelize.Parcelize

@Parcelize
data class LatLngWrapper(
    val latLngList: List<LatLng>
    ) : Parcelable

