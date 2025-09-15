package com.sjodle.lostinthegardens.park_data

import android.content.Context
import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.sjodle.lostinthegardens.park_data.markers.Park
import java.net.URL
import java.util.UUID

data class ParkData(
    val park: Park,
)

sealed class ParkLoadingState {
    object Loading : ParkLoadingState()
    object Error : ParkLoadingState()
    class Success(val parkData: ParkData) : ParkLoadingState()
    class StaleData(val parkData: ParkData) : ParkLoadingState()
}

@Composable
fun loadParkData(
    context: Context,
    baseUrl: URL = URL("https://lostinthegardens.com"),
    parkId: String,
    @RawRes shapesFallback: Int,
    @RawRes categoriesFallback: Int,
    attemptId: UUID,
): State<ParkLoadingState> {
    return produceState<ParkLoadingState>(
        initialValue = ParkLoadingState.Loading,
        baseUrl,
        parkId,
        attemptId
    ) {
        val shapefile = context.resources.openRawResource(shapesFallback)
        val shapeJson = String(shapefile.readBytes())
        shapefile.close()
        value = ParkLoadingState.StaleData(ParkData(Park.fromShapefile(shapeJson)))
    }
}