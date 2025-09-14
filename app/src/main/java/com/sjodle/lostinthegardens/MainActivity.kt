package com.sjodle.lostinthegardens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.sjodle.lostinthegardens.park_data.ParkDataFile
import com.sjodle.lostinthegardens.park_data.ParkDataManager
import com.sjodle.lostinthegardens.ui.theme.LostInTheGardensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LostInTheGardensTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Map(ParkDataManager.YorkStreet, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Map(park: ParkDataFile, modifier: Modifier = Modifier) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(park.parkCenter, 18f)
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                LocalContext.current,
                R.raw.map_style,
            )
        )
    ) {
        Polygon(
            points = park.parkBounds,
            fillColor = Color.Transparent,
            strokeColor = MaterialTheme.colorScheme.outline,
        )
        park.parkMarkers.forEach {
            Marker(
                state = rememberUpdatedMarkerState(position = it.coordinate),
                title = it.name,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapPreview() {
    LostInTheGardensTheme {
        Map(ParkDataManager.YorkStreet, Modifier.fillMaxSize())
    }
}
