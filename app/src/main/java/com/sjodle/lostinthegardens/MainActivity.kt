package com.sjodle.lostinthegardens

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.sjodle.lostinthegardens.park_data.ParkData
import com.sjodle.lostinthegardens.park_data.ParkLoadingState
import com.sjodle.lostinthegardens.park_data.loadParkData
import com.sjodle.lostinthegardens.ui.composable.ParkMarker
import com.sjodle.lostinthegardens.ui.theme.LostInTheGardensTheme
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LostInTheGardensTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainView(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainView(modifier: Modifier = Modifier) {
    var requestId by remember { mutableStateOf(UUID.randomUUID()) }
    val parkLoadingState by loadParkData(
        context = LocalContext.current,
        parkId = "YorkStreet",
        shapesFallback = R.raw.york_street_shapes,
        categoriesFallback = R.raw.york_street_categories,
        attemptId = requestId
    )
    val refresh = { requestId = UUID.randomUUID() }
    Box(modifier) {
        when (val state = parkLoadingState) {
            is ParkLoadingState.Loading -> Text("Loading...")
            is ParkLoadingState.StaleData -> Map(
                parkData = state.parkData,
                isStaleData = true,
                refresh = refresh,
            )

            is ParkLoadingState.Success -> Map(
                parkData = state.parkData,
                refresh = refresh,
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Map(
    parkData: ParkData,
    isStaleData: Boolean = false,
    refresh: () -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(parkData.park.center, 18f)
    }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    Column(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.weight(1f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                    LocalContext.current,
                    R.raw.map_style,
                ),
                isMyLocationEnabled = locationPermission.status.isGranted,
            ),
        ) {
            Polygon(
                points = parkData.park.bounds,
                fillColor = Color.Transparent,
                strokeColor = MaterialTheme.colorScheme.outline,
            )
            parkData.park.markers.map {
                ParkMarker(it, parkData.categories)
            }
        }
        if (isStaleData) {
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            ) {
                Text(
                    "Couldn't load data from the server. This information may be out of date.",
                    modifier = Modifier.weight(1f),
                )
                Button(
                    refresh,
                ) {
                    Text("Try again")
                }
            }
        }
        if (!locationPermission.status.isGranted) {
            Row(
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            ) {
                Text(
                    "Location permission is needed for us to help you navigate.",
                    modifier = Modifier.weight(1f),
                )
                Button(
                    { locationPermission.launchPermissionRequest() },
                ) {
                    Text("Grant permission")
                }
            }
        }
    }
}
