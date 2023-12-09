package com.example.trektopia.utils

import android.content.Context
import com.example.trektopia.R
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

fun List<LatLng>.getStaticMapUri(context: Context): String {
    val route = PolyUtil.encode(this)
    return "https://maps.googleapis.com/maps/api/staticmap" +
            "?size=400x200" +
            "&path=color:0x0000ff|weight:5|$route" +
            "&key=${context.getString(R.string.google_maps_api_key)}"
}
