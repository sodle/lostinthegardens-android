package com.sjodle.lostinthegardens.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.sjodle.lostinthegardens.park_data.categories.CategoryFile
import com.sjodle.lostinthegardens.park_data.markers.Marker

fun Modifier.circleLayout() =
    layout { measurable, constraints ->
        // Measure the composable
        val placeable = measurable.measure(constraints)

        //get the current max dimension to assign width=height
        val currentHeight = placeable.height
        val currentWidth = placeable.width
        val newDiameter = maxOf(currentHeight, currentWidth)

        //assign the dimension and the center position
        layout(newDiameter, newDiameter) {
            // Where the composable gets placed
            placeable.placeRelative(
                (newDiameter - currentWidth) / 2,
                (newDiameter - currentHeight) / 2
            )
        }
    }

@Composable
fun ParkMarker(marker: Marker, categoryFile: CategoryFile) {
    val position = rememberUpdatedMarkerState(marker.position)
    val category = categoryFile.getCategory(marker.category)

    val backgroundColor =
        category?.color?.color ?: MaterialTheme.colorScheme.primaryContainer
    val markerText = marker.monogram ?: ""

    MarkerComposable(
        state = position,
        title = marker.name,
        snippet = category?.name,/**/
    ) {
        Text(
            modifier = Modifier
                .background(backgroundColor, CircleShape)
                .circleLayout()
                .padding(8.dp),
            text = markerText,
        )
    }
}
