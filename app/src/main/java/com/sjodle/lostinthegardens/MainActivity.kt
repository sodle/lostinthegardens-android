package com.sjodle.lostinthegardens

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.sjodle.lostinthegardens.park_data.ParkData
import com.sjodle.lostinthegardens.park_data.ParkLoadingState
import com.sjodle.lostinthegardens.park_data.loadParkData
import com.sjodle.lostinthegardens.ui.composable.ParkMarker
import com.sjodle.lostinthegardens.ui.composable.circleLayout
import com.sjodle.lostinthegardens.ui.theme.LostInTheGardensTheme
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
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

    val cameraPositionState = rememberCameraPositionState {}

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(parkData) {
        parkData?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(parkData.park.center, 18f)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
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
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(painterResource(R.drawable.list), "Locations")
                    }
                },
            )
        }
    ) { paddingValues ->
        parkData?.let { parkData ->
            Map(
                cameraPositionState = cameraPositionState,
                modifier = Modifier.padding(paddingValues),
                parkData = parkData,
                locationAvailable = isLocationAvailable,
                baseLayer = baseLayerOptions[baseLayerIndex],
            )
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        parkData.categories.sortedCategories().forEach { (key, category) ->
                            item {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                )
                            }
                            items(parkData.park.markersForCategory(key)) {
                                Card(onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(it.position, 20f)
                                        )
                                    }
                                    showBottomSheet = false
                                }) {
                                    Row(
                                        modifier = Modifier.padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        it.monogram?.let { modifier ->
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                modifier = Modifier
                                                    .background(
                                                        category.color.color,
                                                        CircleShape
                                                    )
                                                    .circleLayout()
                                                    .padding(2.dp),
                                                text = modifier,
                                                fontSize = 12.sp,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(
                                            it.name,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(8.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Map(
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    parkData: ParkData,
    locationAvailable: Boolean,
    baseLayer: BaseLayer,
) {
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
            latLngBoundsForCameraTarget = parkData.park.cameraBounds(),
        ),
        mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
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
