package com.sjodle.lostinthegardens

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
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
                MainView()
            }
        }
    }
}

data class BaseLayer(
    val name: String,
    val icon: Painter,
    val layer: MapType,
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainView() {
    val snackbarHostState = remember { SnackbarHostState() }

    var requestId by remember { mutableStateOf(UUID.randomUUID()) }
    val parkLoadingState by loadParkData(
        context = LocalContext.current,
        parkId = "YorkStreet",
        shapesFallback = R.raw.york_street_shapes,
        categoriesFallback = R.raw.york_street_categories,
        attemptId = requestId,
    )

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val isLocationAvailable = locationPermission.status.isGranted

    val isUsingStaleData = parkLoadingState is ParkLoadingState.StaleData

    val parkData = when (val state = parkLoadingState) {
        is ParkLoadingState.StaleData -> state.parkData
        is ParkLoadingState.Success -> state.parkData
        else -> null
    }

    var baseLayerIndex by rememberSaveable { mutableIntStateOf(0) }
    val baseLayerOptions = listOf(
        BaseLayer("Streets", painterResource(R.drawable.map), MapType.NORMAL),
        BaseLayer("Satellite", painterResource(R.drawable.globe), MapType.SATELLITE),
    )

    LaunchedEffect(isLocationAvailable) {
        if (!isLocationAvailable) {
            Log.d("LocationNag", "Deploying snackbar")
            val result = snackbarHostState.showSnackbar(
                "Lost in the Gardens can help you navigate through the park, if you allow it to access your precise location.",
                "Allow",
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    Log.d("LocationNag", "Location nag accepted.")
                    locationPermission.launchPermissionRequest()
                }

                SnackbarResult.Dismissed -> {
                    Log.d("LocationNag", "Location nag dismissed.")
                }
            }
        }
    }

    LaunchedEffect(requestId, isUsingStaleData) {
        if (isUsingStaleData) {
            Log.d("StaleDataNag", "Deploying snackbar")
            val result = snackbarHostState.showSnackbar(
                message = "Failed to load data from the server. This information may be out of date.",
                "Retry",
                duration = SnackbarDuration.Indefinite,
                withDismissAction = true,
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    Log.d("StaleDataNag", "Refresh triggered.")
                    requestId = UUID.randomUUID()
                }

                SnackbarResult.Dismissed -> {
                    Log.d("StaleDataNag", "Stale data nag dismissed.")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    parkData?.park?.name?.let {
                        Text(it)
                    } ?: Text("Loading...")
                },
                actions = {
                    SingleChoiceSegmentedButtonRow {
                        baseLayerOptions.forEachIndexed { index, layer ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = baseLayerOptions.size
                                ),
                                selected = index == baseLayerIndex,
                                onClick = { baseLayerIndex = index },
                                label = {
                                    Icon(layer.icon, layer.name)
                                }
                            )
                        }
                    }
                },
            )
        }
    ) { paddingValues ->
        parkData?.let {
            Map(
                modifier = Modifier.padding(paddingValues),
                parkData = it,
                locationAvailable = isLocationAvailable,
                baseLayer = baseLayerOptions[baseLayerIndex],
            )
        }
    }
}

@Composable
fun Map(
    modifier: Modifier = Modifier,
    parkData: ParkData,
    locationAvailable: Boolean,
    baseLayer: BaseLayer,
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(parkData.park.center, 18f)
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
                LocalContext.current,
                R.raw.map_style,
            ),
            isMyLocationEnabled = locationAvailable,
            mapType = baseLayer.layer,
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
}
