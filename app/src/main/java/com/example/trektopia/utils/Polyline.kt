package com.example.trektopia.utils

import android.content.Context
import com.example.trektopia.R
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

fun List<LatLng>.getStaticMapUri(): String {
    require(this.size <= 5) { "Up to 5 markers are allowed." }

    val baseUrl = "https://maps.geoapify.com/v1/staticmap"
    val style = "osm-carto"
    val width = 300
    val height = 300
    val zoom = 14

    val center = "lonlat:${this[0].longitude},${this[0].latitude}"

    val markers = this.take(5).mapIndexed { index, latLng ->
        "lonlat:${latLng.longitude},${latLng.latitude};color:%23ff0000;size:medium;text:${(index).toChar()}"
    }.joinToString("|")

    return "$baseUrl?style=$style&width=$width&height=$height&center=$center&zoom=$zoom&marker=$markers"
}

fun String.completeStaticMapUri(context : Context): String {
    return this + "&apiKey=${context.getString(R.string.geoapi_key)}"
}

/* When using Google Static Maps API
fun List<LatLng>.getStaticMapUri(context: Context): String {
    val route = PolyUtil.encode(this)
    return "https://maps.googleapis.com/maps/api/staticmap" +
            "?size=400x200" +
            "&path=color:0x0000ff|weight:5|$route"
}

fun String.completeStaticMapUri(context : Context): String {
    return this + "&key=${context.getString(R.string.google_maps_api_key)}"
}
 */
