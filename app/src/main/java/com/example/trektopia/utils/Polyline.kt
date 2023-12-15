package com.example.trektopia.utils

import android.content.Context
import com.example.trektopia.R
import com.google.android.gms.maps.model.LatLng

fun List<LatLng>.getStaticMapUri(): String {
    require(this.size <= 6) { "Up to 6 markers are allowed." }

    val baseUrl = "https://maps.geoapify.com/v1/staticmap"
    val style = "osm-carto"
    val width = 600
    val height = 600
    val zoom = 13.8

    val center = "lonlat:${this[0].longitude},${this[0].latitude}"

    val markers = this.mapIndexed { index, latLng ->
        "lonlat:${latLng.longitude},${latLng.latitude};color:%23ff0000;size:medium;text:$index"
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
