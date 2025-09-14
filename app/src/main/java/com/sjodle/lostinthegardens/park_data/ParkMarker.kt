package com.sjodle.lostinthegardens.park_data

import com.google.android.gms.maps.model.LatLng

data class ParkMarker(
    val name: String,
    val coordinate: LatLng,
)
