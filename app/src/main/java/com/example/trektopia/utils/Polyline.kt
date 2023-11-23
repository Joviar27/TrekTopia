package com.example.trektopia.utils

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

fun List<LatLng>.getStaticMapUri(): String {
    val route = PolyUtil.encode(this)
    return "https://maps.googleapis.com/maps/api/staticmap" +
            "?size=400x200" +
            "&path=color:0x0000ff|weight:5|$route" +
            "&key=${BuildConfig.googleMapsApiKey}"
}
