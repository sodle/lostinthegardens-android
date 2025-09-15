package com.sjodle.lostinthegardens.park_data.markers

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

data class Park(
    val name: String,
    val center: LatLng,
    val bounds: List<LatLng>,
    val markers: List<Marker>,
) {
    companion object {
        fun fromShapefile(geoJson: String): Park {
            val shapefile = JSONObject(geoJson)
            val features = shapefile.getJSONArray("features")

            lateinit var name: String
            lateinit var center: LatLng
            lateinit var bounds: MutableList<LatLng>
            var markers = mutableListOf<Marker>()

            for (i in 0..<features.length()) {
                val feature = features.get(i) as JSONObject

                if (feature.getString("type") != "Feature") {
                    Log.i("Park.fromShapefile", "Skipping non-feature at index $i")
                    continue
                }

                val properties = feature.get("properties") as JSONObject
                val geometry = feature.get("geometry") as JSONObject

                if (
                    properties.getString("category") == "park-geometry"
                    && geometry.getString("type") == "Point"
                ) {
                    name = properties.getString("name")
                    val coordinates = geometry.getJSONArray("coordinates")
                    center = LatLng(
                        coordinates.getDouble(1),
                        coordinates.getDouble(0),
                    )
                    Log.i("Park.fromShapefile", "Found center of $name at $center")
                    continue
                }

                if (
                    properties.getString("category") == "park-geometry"
                    && geometry.getString("type") == "Polygon"
                ) {
                    bounds = mutableListOf()
                    val coordinates = geometry.getJSONArray("coordinates").getJSONArray(0)
                    for (j in 0..<coordinates.length()) {
                        val coordinate = coordinates.getJSONArray(j)
                        bounds.add(
                            LatLng(
                                coordinate.getDouble(1),
                                coordinate.getDouble(0),
                            )
                        )
                    }
                    Log.i(
                        "Park.fromShapefile",
                        "Found ${bounds.count()} points representing park bounds"
                    )
                    continue
                }

                if (geometry.getString("type") == "Point") {
                    val coordinates = geometry.getJSONArray("coordinates")
                    val position = LatLng(
                        coordinates.getDouble(1),
                        coordinates.getDouble(0),
                    )
                    markers.add(
                        Marker(
                            name = properties.getString("name"),
                            position = position,
                            category = properties.getString("category"),
                            monogram = properties.optString("monogram"),
                            iconName = properties.optString("iconName"),
                        )
                    )
                    Log.d("Park.fromShapefile", "Found marker at $position")
                    continue
                }

                Log.w("Park.fromShapefile", "Should not have gotten here! $feature")
            }
            Log.i("Park.fromShapefile", "Found ${markers.count()} markers")

            return Park(
                name = name,
                center = center,
                bounds = bounds,
                markers = markers,
            )
        }
    }
}
