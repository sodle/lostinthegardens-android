package com.sjodle.lostinthegardens.park_data

import com.google.android.gms.maps.model.LatLng

private val yorkCenter = listOf(-104.96129012675392, 39.732100552323345)
private val yorkBounds = listOf(
    listOf(-104.96413897499555, 39.73285603018718),
    listOf(-104.96410496390875, 39.73103253654082),
    listOf(-104.96078019335204, 39.73104090567776),
    listOf(-104.96077757714613, 39.73060461115725),
    listOf(-104.96031190231545, 39.73060461115725),
    listOf(-104.96028835695881, 39.73073740120137),
    listOf(-104.95996657041857, 39.73101907620446),
    listOf(-104.95992209585599, 39.73104724364171),
    listOf(-104.95931968791837, 39.73104322030693),
    listOf(-104.95931707176769, 39.73164881745461),
    listOf(-104.95927782950702, 39.73185403521538),
    listOf(-104.95915225427127, 39.732077359730596),
    listOf(-104.95903723981468, 39.732264177440584),
    listOf(-104.95876450596603, 39.73260656328833),
    listOf(-104.95865724378612, 39.732898290181765),
    listOf(-104.95863893073086, 39.733445526266166),
    listOf(-104.9596880071754, 39.73344753815502),
    listOf(-104.96103794178013, 39.7334535713679),
    listOf(-104.96105102253351, 39.732888228199755),
    listOf(-104.96413897499555, 39.73285603018718),
)

data class ParkDataFile(
    val parkCenter: LatLng,
    val parkBounds: List<LatLng>,
    val parkMarkers: List<ParkMarker>,
)

object ParkDataManager {
    val YorkStreet: ParkDataFile
        get() = ParkDataFile(
            LatLng(yorkCenter[1], yorkCenter[0]),
            yorkBounds.map { LatLng(it[1], it[0]) },
            listOf(
                ParkMarker(
                    "Boettcher Memorial Center",
                    LatLng(39.732610401688675, -104.96051003177496),
                )
            ),
        )
}
