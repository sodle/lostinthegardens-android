package com.sjodle.lostinthegardens.park_data.markers

import com.google.android.gms.maps.model.LatLng

data class Marker(
    val name: String,
    val position: LatLng,
    val category: String,
    val monogram: String? = null,
    val iconName: String? = null,
)
